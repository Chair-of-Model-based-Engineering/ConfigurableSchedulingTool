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
    private final SolverReturn solverReturn;

    private BaseModel oneWiseNormalized = null;
    private BaseModel twoWiseNormalized = null;

    public ProblemNormalizer(BaseModel baseModel, SolverReturn solverReturn) {
        this.baseModel = baseModel;
        this.solverReturn = solverReturn;
    }

    /**
     * Returns the one-wise normalized model, or {@code null} if the normalization wasn't performed successfully yet (see {@link #oneWise()}).
     *
     * @return the one-wise normalized model, or {@code null}.
     */
    public BaseModel getOneWiseNormalizedModel() {
        return oneWiseNormalized;
    }

    /**
     * Returns the two-normalized model, or {@code null} if the normalization wasn't performed successfully yet (see {@link #oneWise()}).
     *
     * @return the two-normalized model, or {@code null}.
     */
    public BaseModel getTwoWiseNormalized() {
        return twoWiseNormalized;
    }

    /**
     * Execute all normalizations in the correct order.
     *
     * @return an array of each normalization's solver times.
     * @see #oneWise() the first executed normalization.
     * @see #twoWise() the second executed normalization.
     */
    public double[] normalizeEverything() {
        return new double[] {
                oneWise(),
                twoWise()
        };
    }

    /**
     * Normalizes the scheduling problem one-wise.
     *
     * <p>The method returns immediately if one of the following conditions is met:
     * <ul>
     *     <li>The scheduling problem doesn't have a deadline.</li>
     *     <li>The scheduling problem is not solvable.</li>
     * </ul>
     *
     * @return the cumulative time of all solver calls.
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

        this.oneWiseNormalized = buildNormalizedModel(taskUncertainties, falseOptionalTasks, machineStatuses);
        return overallTime.get();
    }

    /**
     * Normalizes the scheduling problem two-wise.
     *
     * <p>The problem has to have been normalized one-wise beforehand.
     *
     * @see #oneWise()
     * @return the cumulative time of all solver calls.
     */
    public double twoWise() {
        if (this.oneWiseNormalized == null)
            return -1;

        boolean[][] schemas = {
                new boolean[] {false, false},
                new boolean[] {false, true},
                new boolean[] {true, false},
                new boolean[] {true, true}
        };

        double solverTime = 0;
        Set<SchedulingProblem.ExclusionConstraint> excludes = new HashSet<>();
        for (SpElement[] pair : generateValidPairs()) {
            SpElement left = pair[0];
            SpElement right = pair[1];

            for (boolean[] schema : schemas) {
                boolean left_value = schema[0];
                boolean right_value = schema[1];

                CpModel model = this.oneWiseNormalized.getModel().getClone();
                model.getBuilder().setName(String.format(
                        "two wise %s=%b %s=%b",
                        left.getName(), left_value,
                        right.getName(), right_value
                ));

                setState(left, left_value, model);
                setState(right, right_value, model);

                CpSolver solver = new CpSolver();
                CpSolverStatus status = solver.solve(model);
                solverTime += solver.userTime();

                if (status == CpSolverStatus.INFEASIBLE) {
                    excludes.add(new SchedulingProblem.ExclusionConstraint(left, left_value, right, right_value));
                }
            }
        }

        SchedulingProblem sp = new SchedulingProblem(this.oneWiseNormalized.getSchedulingProblem(), excludes);
        this.twoWiseNormalized = new BaseModel(sp);

        return solverTime;
    }

    private List<SpElement[]> generateValidPairs() {
        List<SpElement> optionalSpElements = new ArrayList<>();
        for (Machine machine : this.oneWiseNormalized.getSchedulingProblem().getMachines()) {
            if (machine.isOptional())
                optionalSpElements.add(machine);
        }
        for (Task task : this.oneWiseNormalized.getSchedulingProblem().getTasks()) {
            if (task.isOptional())
                optionalSpElements.add(task);

            // If a task has only one possible duration, then that would already be covered by the parent task feature.
            if (task.hasUncertainDurations()) {
                // We don't have to consider unbound durations since we work with a one-wise normalized model here.
                for (int duration : task.getDurations()) {
                    optionalSpElements.add(new SpElement.TaskDuration(task, duration));
                }
            }
        }

        List<SpElement[]> pairs = new ArrayList<>();
        for (int i = 0; i < optionalSpElements.size(); i++) {
            for (int j = i + 1; j < optionalSpElements.size(); j++) {
                SpElement left = optionalSpElements.get(i);
                SpElement right = optionalSpElements.get(j);

                boolean taskDurationsOfSameTask = left instanceof SpElement.TaskDuration l && right instanceof SpElement.TaskDuration r
                        && l.getTask().equals(r.getTask());
                boolean taskDurationOfTask1 = left instanceof SpElement.TaskDuration l && right instanceof Task r
                        && l.getTask().equals(r);
                boolean taskDurationOfTask2 = left instanceof Task l && right instanceof SpElement.TaskDuration r
                        && l.equals(r.getTask());
                if (taskDurationsOfSameTask || taskDurationOfTask1 || taskDurationOfTask2) {
                    // These combinations would not pose valid selections
                    continue;
                }

                pairs.add(new SpElement[] {left, right});
            }
        }

        return pairs;
    }

    private void setState(SpElement element, boolean state, CpModel model) {
        Literal modelState = state ? model.trueLiteral() : model.falseLiteral();

        // TODO: This should probably rather be realized with an abstract method SpElement.setActive()
        //       but this doesn't work currently with TaskDuration and Task not knowing the corresponding TaskType
        switch (element) {
            case SpElement.TaskDuration td -> {
                TaskType taskType = this.oneWiseNormalized.getAllTaskTypes().get(td.getTask());
                BoolVar activeVar = model.getBoolVarFromProtoIndex(taskType.getActive().getIndex());
                IntVar durationVar = model.getIntVarFromProtoIndex(taskType.getIntervalDomainIndex());
                if (state) {
                    model.addEquality(durationVar, td.getDuration());
                    model.addEquality(activeVar, modelState);
                } else {
                    model.addDifferent(durationVar, td.getDuration());
                }
            }
            case Task task -> {
                TaskType taskType = this.oneWiseNormalized.getAllTaskTypes().get(task);
                BoolVar activeVar = model.getBoolVarFromProtoIndex(taskType.getActive().getIndex());
                model.addEquality(activeVar, modelState);
            }
            case Machine machine -> {
                model.addEquality(machine.getActive(), modelState);
            }
            default ->
                    throw new IllegalStateException("Unexpected actual type for SpElement for element: " + element.getName());
        }
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
