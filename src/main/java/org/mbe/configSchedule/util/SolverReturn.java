package org.mbe.configSchedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: Maybe introduce SolverReturnFactory and keep SolverReturn immutable.
public class SolverReturn {
    private Double time;
    private CpSolverStatus status;
    private Schedule schedule;
    private final Map<Task, Integer> uncertaintyResults = new HashMap<>();

    /**
     * Creates new object of type SolverReturn.
     *
     * @param time     time it took to solve the problem.
     * @param status   status of the solver.
     * @param schedule Jobs executed in solution.
     */
    public SolverReturn(double time, CpSolverStatus status, Schedule schedule) {
        this.time = time;
        this.status = status;
        this.schedule = schedule;
    }

    /**
     * Create new empty object of type SolverReturn.
     */
    public SolverReturn() {
    }

    /**
     * Get time spend to solve the problem.
     *
     * @return time in ms.
     */
    public Double getTime() {
        return time;
    }

    /**
     * Get status of solver.
     *
     * @return status of solver.
     */
    public CpSolverStatus getStatus() {
        return status;
    }

    /**
     * Returns whether the schedule is at least feasible, e.g. feasible or optimal.
     *
     * @return {@code true} if the schedule is feasible, {@code false} otherwise.
     */
    public boolean isAtLeastFeasible() {
        return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
    }

    /**
     * Returns whether the schedule is optimal.
     *
     * @return {@code true} if the schedule is optimal, {@code false} otherwise.
     */
    public boolean isOptimal() {
        return status == CpSolverStatus.OPTIMAL;
    }

    /**
     * Get all jobs assigned to each machine in solution.
     *
     * @return all assigned jobs or {@link Optional#empty()} if no feasible solution was found.
     */
    public Optional<Schedule> getSchedule() {
        return Optional.ofNullable(schedule);
    }

    /**
     * Sets the maximum duration with a feasible solution for the given task.
     *
     * @param task        the task for which to set the duration.
     * @param maxDuration the maximum allowed duration.
     */
    public void setUncertaintyResult(Task task, int maxDuration) {
        this.uncertaintyResults.put(task, maxDuration);
    }

    /**
     * Returns the results of the analyses of the tasks' uncertainty.
     *
     * @return a map of
     */
    public Map<Task, Integer> getUncertaintyResults() {
        return uncertaintyResults;
    }
}
