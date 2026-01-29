package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import org.mbe.configSchedule.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class for analyzing a given problem on its solvability with respect to the tasks' configurable durations.
 */
public class ProblemNormalizer {

    private final BaseModel baseModel;
    private BaseModel normalizedModel = null;
    private final SolverReturn solverReturn;

    public ProblemNormalizer(BaseModel baseModel, SolverReturn solverReturn) {
        this.baseModel = baseModel;
        this.solverReturn = solverReturn;
    }

    /**
     * Returns the normalized model or {@code null} if the normalization wasn't performed successfully yet (see {@link #oneWise()}).
     *
     * @return the normalized model, or {@code null}.
     */
    public BaseModel getNormalizedModel() {
        return normalizedModel;
    }

    /**
     * Analyzes uncertainty of the scheduling problem.
     *
     * <p>The method returns immediately if one of the following conditions is met:
     * <ul>
     *     <li>The scheduling problem doesn't have a deadline.</li>
     *     <li>The scheduling problem is not solvable.</li>
     * </ul>
     *
     * @return the cumulative time of all solver calls
     */
    public double oneWise() {
        if (this.baseModel.getSchedulingProblem().getDeadline() < 0 || !this.solverReturn.isAtLeastFeasible()) {
            return -1;
        }

        Map<Task, List<Integer>> taskUncertainties = new HashMap<>();
        AtomicReference<Double> overallTime = new AtomicReference<>(0.0);

        for (TaskType uncertainTask : this.baseModel.getTasksWithUncertainty()) {
            List<Integer> possibleDurations = findPossibleDurationsOfTask(uncertainTask, overallTime);
            taskUncertainties.put(uncertainTask.getTask(), possibleDurations);
        }

        Set<Task> falseOptionalTasks = new HashSet<>();
        for (TaskType optionalTask : this.baseModel.getOptionalTasks()) {
            CpModel falseOptionalModel = this.baseModel.getModel().getClone();
            falseOptionalModel.getBuilder().setName("False optional " + optionalTask.getName());

            BoolVar activeVar = falseOptionalModel.getBoolVarFromProtoIndex(optionalTask.getActive().getIndex());
            falseOptionalModel.addEquality(activeVar, falseOptionalModel.falseLiteral());

            CpSolver solver = new CpSolver();
            CpSolverStatus status = solver.solve(falseOptionalModel);
            // TODO: userTime or wallTime?
            overallTime.updateAndGet(v -> v + solver.userTime());

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
                falseOptionalTasks.add(optionalTask.getTask());
            }
        }

        Map<Machine, Optional<Boolean>> machineStatuses = new HashMap<>();
        for (Machine machine : this.baseModel.getSchedulingProblem().getMachines()) {
            if (!machine.isOptional() || !machineStatusPossible(machine, false, overallTime)) {
                machineStatuses.put(machine, Optional.of(true));
            } else if (!machineStatusPossible(machine, true, overallTime)) {
                machineStatuses.put(machine, Optional.of(false));
            } else {
                // The machine might be active or not
                machineStatuses.put(machine, Optional.empty());
            }
        }

        this.normalizedModel = buildNormalizedModel(taskUncertainties, falseOptionalTasks, machineStatuses);
        return overallTime.get();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean machineStatusPossible(Machine machine, boolean active, AtomicReference<Double> overallTime) {
        CpModel model = this.baseModel.getModel().getClone();
        model.getBuilder().setName("Status " + active + " possible " + machine.getName());

        BoolVar activeVar = model.getBoolVarFromProtoIndex(machine.getActive().getIndex());
        model.addEquality(activeVar, active ? model.trueLiteral() : model.falseLiteral());

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);
        overallTime.updateAndGet(v -> v + solver.userTime());

        return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
    }

    private List<Integer> findPossibleDurationsOfTask(TaskType uncertainTask, AtomicReference<Double> overallTime) {
        CpModel maximumDurationModel = this.baseModel.getModel().getClone();
        maximumDurationModel.getBuilder().setName("Maximum Duration " + uncertainTask.getName());

        // Force the task to be active. This only has an effect if the task is optional.
        BoolVar activeVar = maximumDurationModel.getBoolVarFromProtoIndex(uncertainTask.getActive().getIndex());
        maximumDurationModel.addEquality(activeVar, maximumDurationModel.trueLiteral());

        LinearExpr durationExpr = uncertainTask.getInterval().getSizeExpr();
        maximumDurationModel.maximize(durationExpr);

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(maximumDurationModel);
        overallTime.updateAndGet(v -> v + solver.userTime());

        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
            return List.of();
        }

        // We only allow integer durations. Therefore, this should be an allowed cast.
        int maximumDuration = (int) solver.objectiveValue();

        IntStream uncertainDurations = Arrays.stream(uncertainTask.getTask().getDurations()).filter(
                // Since we already know that the model is solvable with d == maximumDuration,
                // we don't have to check it again
                d -> d < maximumDuration
        );
        IntStream unboundDurations = uncertainTask.getTask().getUnboundDurations().stream().flatMapToInt(
                lowerBound -> IntStream.range(lowerBound, maximumDuration)
        );
        List<Integer> possibleDurations = IntStream.concat(uncertainDurations, unboundDurations)
                .sorted().distinct()
                .filter(duration -> {
                    CpModel durationModel = this.baseModel.getModel().getClone();
                    durationModel.getBuilder().setName("Solvable duration constraint check %s = %d".formatted(uncertainTask.getName(), duration));

                    IntVar domain = durationModel.getIntVarFromProtoIndex(uncertainTask.getIntervalDomainIndex());
                    durationModel.addEquality(domain, duration);

                    BoolVar activeVar_ = durationModel.getBoolVarFromProtoIndex(uncertainTask.getActive().getIndex());
                    durationModel.addEquality(activeVar_, durationModel.trueLiteral());

                    // Not maximizing anything in the durationModel because we only care if it is solvable in any way.
                    CpSolverStatus status_ = solver.solve(durationModel);
                    overallTime.updateAndGet(v -> v + solver.userTime());
                    return status_ == CpSolverStatus.OPTIMAL || status_ == CpSolverStatus.FEASIBLE;
                })
                .boxed()
                // Need mutable List, so we can still add the maximum duration
                .collect(Collectors.toCollection(ArrayList::new));

        // We already know that maximumDuration is a possible duration, no matter if there are duration constraints on it
        possibleDurations.add(maximumDuration);
        return possibleDurations;
    }

    /**
     * Build a normalized {@link BaseModel} from a newly built {@link SchedulingProblem}.
     *
     * <p>The new, underlying {@code SchedulingProblem} is copied from the old {@code SchedulingProblem} to prevent
     * changes in a task of one to affect the other's tasks.
     *
     * <p>The normalization steps include:
     * <ul>
     *     <li>Setting false optional machines to be mandatory.</li>
     *     <li>Removing dead machines.</li>
     *     <li>Dead tasks (i.e. tasks with no possible durations) are removed from the scheduling problem.</li>
     *     <li>False optional tasks are set to be mandatory.</li>
     *     <li>The tasks' durations are restricted to possible ones. This includes removing unbound durations.</li>
     * </ul>
     *
     * <p>The machine status map indicates the core/dead status of a machine in the scheduling problem:
     * <ul>
     *     <li>{@code Optional.of(true)} indicates a core/mandatory feature.</li>
     *     <li>{@code Optional.of(false)} indicates a dead feature.</li>
     *     <li>{@code Optional.empty()} indicates no statement.</li>
     * </ul>
     *
     * @param taskUncertainties  the possible durations for each task.
     * @param falseOptionalTasks the false optional tasks.
     * @param machineStatus      the status of the machines.
     * @return a {@link BaseModel} on the basis of a newly built {@link SchedulingProblem}.
     */
    private BaseModel buildNormalizedModel(
            Map<Task, List<Integer>> taskUncertainties, Set<Task> falseOptionalTasks, Map<Machine, Optional<Boolean>> machineStatus
    ) {
        // Tasks without any possible durations are dead.
        // This can only occur for tasks that are safe to delete since there would otherwise not be
        // a feasible solution to the scheduling problem at all.
        // NOTE: Storing strings to simplify dealing with stringy exclusion constraints later
        Set<String> deadTasks = taskUncertainties.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .map(Task::getName)
                .collect(Collectors.toSet());

        Map<Machine, Machine> machines = buildNormalizedMachines(machineStatus);
        Map<Task, Task> tasks = buildNormalizedTasks(taskUncertainties, falseOptionalTasks, deadTasks, machines);
        Map<Task, List<Task>> precedenceOrder = buildNormalizedPrecedenceOrder(deadTasks, tasks);

        return new BaseModel(new SchedulingProblem(
                this.baseModel.getSchedulingProblem().getName() + "_normalized",
                new ArrayList<>(tasks.values()),
                precedenceOrder,
                new ArrayList<>(machines.values()),
                this.baseModel.getSchedulingProblem().getDeadline()
        ));
    }

    private Map<Task, List<Task>> buildNormalizedPrecedenceOrder(Set<String> deadTasks, Map<Task, Task> tasks) {
        Map<Task, List<Task>> precedenceOrder = new HashMap<>();
        for (Map.Entry<Task, List<Task>> entry : this.baseModel.getSchedulingProblem().getPrecedenceOrder().entrySet()) {
            if (deadTasks.contains(entry.getKey().getName())) {
                // A non-existent task doesn't depend on anything
                continue;
            }

            Task newDependent = tasks.get(entry.getKey());
            List<Task> newDependencies = entry.getValue().stream()
                    .filter(t -> !deadTasks.contains(t.getName()))
                    .map(tasks::get)
                    .toList();

            precedenceOrder.put(newDependent, newDependencies);
        }
        return precedenceOrder;
    }

    private Map<Task, Task> buildNormalizedTasks(Map<Task, List<Integer>> taskUncertainties, Set<Task> falseOptionalTasks, Set<String> deadTasks, Map<Machine, Machine> machines) {
        Map<Task, Task> tasks = new HashMap<>();
        for (Task task : this.baseModel.getSchedulingProblem().getTasks()) {
            if (deadTasks.contains(task.getName())) {
                // We can remove dead tasks from the normalized model.
                continue;
            }

            boolean isOptional = !falseOptionalTasks.contains(task) && task.isOptional();
            int[] durations = task.hasUncertainDurations()
                    ? taskUncertainties.get(task).stream().mapToInt(Integer::intValue).toArray()
                    : task.getDurations();
            List<String> excludeConstraints = task.getExcludeTasks().stream()
                    .filter(deadTasks::contains)
                    .toList();

            Task newTask = new Task(
                    machines.get(task.getMachine()),
                    durations,
                    task.getName(),
                    isOptional,
                    excludeConstraints
            );
            tasks.put(task, newTask);
        }
        return tasks;
    }

    private Map<Machine, Machine> buildNormalizedMachines(Map<Machine, Optional<Boolean>> machineStatus) {
        Map<Machine, Machine> machines = new HashMap<>();
        for (Machine machine : this.baseModel.getSchedulingProblem().getMachines()) {
            Optional<Boolean> status = machineStatus.get(machine);

            if (status.isEmpty()) {
                machines.put(machine, new Machine(machine.getName(), machine.isOptional()));
            } else if (status.get()) {
                machines.put(machine, new Machine(machine.getName(), false));
            }
            // Otherwise: status.get() == false -> machine is dead and doesn't need to be added
        }
        return machines;
    }
}
