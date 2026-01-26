package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import org.mbe.configSchedule.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class for analyzing a given problem on its solvability with respect to the tasks' configurable durations.
 */
public class ProblemAnalyzer {

    private final BaseModel baseModel;
    private final SolverReturn solverReturn;
    private Map<Task, List<Integer>> taskPossibleDurations;
    private CpModel normalizedModel;

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

}
