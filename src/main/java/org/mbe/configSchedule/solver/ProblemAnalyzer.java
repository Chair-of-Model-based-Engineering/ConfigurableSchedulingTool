package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import org.mbe.configSchedule.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A class for analyzing a given problem on its solvability with respect to the tasks' configurable durations.
 */
public class ProblemAnalyzer {

    private final BaseModel baseModel;
    private final SolverReturn solverReturn;
    private Map<Task, List<Integer>> taskPossibleDurations;
    private CpModel normalizedModel;
    private double decisionTreeTime;

    public ProblemAnalyzer(BaseModel baseModel, SolverReturn solverReturn) {
        this.baseModel = baseModel;
        this.solverReturn = solverReturn;
    }

    /**
     * Analyzes uncertainty of the scheduling problem.
     */
    public void analyzeUncertainty() {
        if (this.baseModel.getSchedulingProblem().getDeadline() < 0
                || this.baseModel.getTasksWithUncertainty().isEmpty()
                || !this.solverReturn.isAtLeastFeasible()) {
            return;
        }

        analyzeUncertaintyPerTask();

        buildNormalizedModel();

        analyzeOverallUncertainty();
        createDecisionTree();
    }

    private void analyzeUncertaintyPerTask() {
        Map<Task, List<Integer>> taskUncertainty = new HashMap<>();
        AtomicReference<Double> overallTime = new AtomicReference<>(0.0);
        for (TaskType uncertainTask : this.baseModel.getTasksWithUncertainty()) {
            CpModel maximumDurationModel = this.baseModel.getModel().getClone();
            maximumDurationModel.getBuilder().setName("Maximum Duration " + uncertainTask.getName());
            LinearExpr durationExpr = uncertainTask.getInterval().getSizeExpr();
            maximumDurationModel.maximize(durationExpr);

            CpSolver solver = new CpSolver();
            CpSolverStatus _ = solver.solve(maximumDurationModel);
            overallTime.updateAndGet(v -> v + solver.userTime());
            // We only allow integer durations. Therefore, this should be an allowed cast.
            int maximumDuration = (int) solver.objectiveValue();

            IntStream uncertainDurations = Arrays.stream(uncertainTask.getTask().getDurations()).filter(
                    d -> d < maximumDuration
            );
            IntStream unboundDurations = uncertainTask.getTask().getUnboundDurations().stream().flatMapToInt(
                    lowerBound -> IntStream.range(lowerBound, maximumDuration)
            );
            List<Integer> possibleDurations = IntStream.concat(uncertainDurations, unboundDurations)
                    .sorted().distinct()
                    .filter(duration -> {
                        // We only need to check durations `d` with duration constraints separately.
                        // If the dependency of the duration `d` has a big duration, the model might not be solvable with `d`
                        // despite `d` being smaller than `maximumDuration`.
                        if (!uncertainTask.getTask().getDurationCons().containsKey(duration))
                            return true;

                        CpModel durationModel = this.baseModel.getModel().getClone();
                        durationModel.getBuilder().setName("Solvable duration constraint check %s = %d".formatted(uncertainTask.getName(), duration));
                        IntVar domain = durationModel.getIntVarFromProtoIndex(uncertainTask.getIntervalDomainIndex());
                        durationModel.addEquality(domain, duration);

                        // Not maximizing anything in the durationModel because we only care if it is solvable in any way.
                        CpSolverStatus status = solver.solve(durationModel);
                        overallTime.updateAndGet(v -> v + solver.userTime());
                        return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
                    })
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));
            // We already know that maximumDuration is a possible duration, no matter if there are duration constraints on it
            possibleDurations.add(maximumDuration);

            taskUncertainty.put(uncertainTask.getTask(), possibleDurations);
        }

        this.taskPossibleDurations = taskUncertainty;

        if (this.solverReturn != null)
            this.solverReturn.setPerTaskUncertainty(new SolverReturn.UncertaintyResult(null, taskUncertainty, overallTime.get()));
    }

    private void buildNormalizedModel() {
        this.normalizedModel = this.baseModel.getModel().getClone();
        this.normalizedModel.getBuilder().setName("Normalized model");
        for (TaskType taskType : this.baseModel.getTasksWithUncertainty()) {
            IntVar domain = this.normalizedModel.getIntVarFromProtoIndex(taskType.getIntervalDomainIndex());

            // `addAllDomain` below only allows a correctly "formatted" array of longs as argument.
            // For more details refer to the documentation of the method.
            // Furthermore, `Domain.fromValues` expects a `long[]` but `addAllDomain` expects `Iterable<Long>`.
            long[] possibleDurations = this.taskPossibleDurations.get(taskType.getTask()).stream().mapToLong(Long::valueOf).toArray();
            List<Long> newDomain = Arrays.stream(Domain.fromValues(possibleDurations).flattenedIntervals()).boxed().toList();
            domain.getBuilder().clearDomain().addAllDomain(newDomain);
        }
    }

    private void analyzeOverallUncertainty() {
        CpModel uncertaintyModel = this.normalizedModel.getClone();
        uncertaintyModel.getBuilder().setName("Summed uncertainty model");

        IntVar[] durationVariables = this.baseModel.getTasksWithUncertainty().stream()
                .map(tt -> uncertaintyModel.getIntVarFromProtoIndex(tt.getIntervalDomainIndex()))
                .toArray(IntVar[]::new);
        long[] possibleDurationsSizes = this.baseModel.getTasksWithUncertainty().stream()
                .map(TaskType::getTask)
                .mapToLong(t -> this.taskPossibleDurations.get(t).size())
                .toArray();
        long sum = Arrays.stream(possibleDurationsSizes).sum();
        double[] weights = Arrays.stream(possibleDurationsSizes)
                // Inverting the percentage to prioritize maximizing the duration of tasks with small uncertainty ranges.
                .mapToDouble(d -> 1 - (float) d / sum)
                .toArray();
        // Unfortunately, I have not found a more elegant way to get the coefficients to sum up to 1 in the end.
        double weightSum = 1 / Arrays.stream(weights).sum();
        double[] coefficients = Arrays.stream(weights).map(w -> w * weightSum).toArray();

        uncertaintyModel.maximize(DoubleLinearExpr.weightedSum(durationVariables, coefficients));

        CpSolver solver = new CpSolver();
        solver.solve(uncertaintyModel);

        Map<Task, List<Integer>> taskDurations = new HashMap<>();
        for (TaskType taskType : this.baseModel.getTasksWithUncertainty()) {
            BoolVar activeVar = uncertaintyModel.getBoolVarFromProtoIndex(taskType.getActive().getIndex());
            IntVar domain = uncertaintyModel.getIntVarFromProtoIndex(taskType.getIntervalDomainIndex());
            taskDurations.put(taskType.getTask(), solver.booleanValue(activeVar) ? List.of((int) solver.value(domain)) : List.of());
        }

        this.solverReturn.setSummedUncertainty(new SolverReturn.UncertaintyResult(ProblemSolver.createSchedule(this.baseModel, solver), taskDurations, solver.userTime()));
    }

    private void createDecisionTree() {
        //noinspection OptionalGetWithoutIsPresent // We only analyze scheduling problems with solutions
        Schedule makespanSchedule = this.solverReturn.getSchedule().get();
        List<Machine> machines = this.baseModel.getSchedulingProblem().getMachines();

        List<TaskType> orderedTasks = new ArrayList<>(this.baseModel.getAllTaskTypes().size());
        int taskNr = 0;
        int machineNr = 0;
        for (int i = 0; i < this.baseModel.getAllTaskTypes().size(); i++) {
            List<AssignedTask> assignedTasks = makespanSchedule.getTasks(machines.get(machineNr));
            if (assignedTasks != null && taskNr < assignedTasks.size()) {
                AssignedTask assignedTask = assignedTasks.get(taskNr);
                if (assignedTask.getTask().hasUncertainDurations())
                    orderedTasks.add(this.baseModel.getAllTaskTypes().get(assignedTask.getTask()));
            }

            if (++machineNr == machines.size()) {
                taskNr++;
                machineNr = 0;
            }
        }

        DecisionTree.TaskDecisions root = createDecisionTreeRecursive(orderedTasks.getFirst(), new HashMap<>(), orderedTasks);
        this.solverReturn.setDecisionTree(new DecisionTree(this.decisionTreeTime, root));
    }

    private DecisionTree.TaskDecisions createDecisionTreeRecursive(
            TaskType taskType,
            HashMap<TaskType, Integer> processedTasks,
            List<TaskType> orderedTasks
    ) {
        CpModel fixedTasksModel = this.normalizedModel.getClone();
        for (Map.Entry<TaskType, Integer> entry : processedTasks.entrySet()) {
            IntVar previousDomain = fixedTasksModel.getIntVarFromProtoIndex(entry.getKey().getIntervalDomainIndex());
            fixedTasksModel.addEquality(previousDomain, entry.getValue());
        }

        DecisionTree.TaskDecisions taskDecisions = new DecisionTree.TaskDecisions(taskType.getTask());
        IntVar domain = fixedTasksModel.getIntVarFromProtoIndex(taskType.getIntervalDomainIndex());

        for (Integer duration : this.taskPossibleDurations.get(taskType.getTask())) {
            CpModel model = fixedTasksModel.getClone();
            model.getBuilder().setName("Decision Tree %s = %d %s".formatted(taskType.getName(), duration, processedTasks.values()));
            model.addEquality(domain, duration);
            IntVar makespan = fixedTasksModel.getIntVarFromProtoIndex(this.baseModel.getMakespanVar().getIndex());
            model.minimize(makespan);

            CpSolver solver = new CpSolver();
            CpSolverStatus solverStatus = solver.solve(model);
            if (solverStatus == CpSolverStatus.INFEASIBLE) {
                // The model is not solvable with the durations found during previous rounds.
                taskDecisions.addInfeasibleDecision(duration);
            } else {
                Schedule schedule = ProblemSolver.createSchedule(this.baseModel, solver);
                taskDecisions.addDecision(duration, schedule);
            }
            this.decisionTreeTime += solver.userTime();
        }

        // TODO: Get next level task from (simple) schedule
        TaskType nextLevelTaskType;
        try {
            nextLevelTaskType = orderedTasks.get(processedTasks.size() + 1);
        } catch (IndexOutOfBoundsException e) {
            return taskDecisions;
        }

        Integer lastDecisionDuration = taskDecisions.getDecisionDurations().getLast();
        List<Integer> decisionDurations = Stream.concat(
                Stream.of(taskType.getTask().getMinimumDuration()),
                // Adding the lower bounds of each decision span, so that there is a guarantee that at least one model
                // of the next level is actually solvable/feasible.
                taskDecisions.getDecisionDurations().stream()
                        .flatMap(d -> Stream.of(d, d + 1))
                        .filter(d -> taskType.getTask().hasDuration(d) && d <= lastDecisionDuration)
        ).distinct().toList();

        // Doing this in a separate loop because decision durations can be a span of multiple durations.
        for (Integer decisionDuration : decisionDurations) {
            HashMap<TaskType, Integer> nextProcessedTasks = new HashMap<>(processedTasks);
            nextProcessedTasks.put(taskType, decisionDuration);
            DecisionTree.TaskDecisions nextLevel = createDecisionTreeRecursive(nextLevelTaskType, nextProcessedTasks, orderedTasks);
            taskDecisions.addNextLevel(decisionDuration, nextLevel);
        }

        return taskDecisions;
    }
}
