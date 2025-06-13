package org.mbe.configSchedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.Map;
import java.util.Optional;

// TODO: Maybe introduce SolverReturnFactory and keep SolverReturn immutable.
public class SolverReturn {
    /**
     * Represents the results of an uncertainty analysis of a scheduling problem.
     *
     * @param schedule        a schedule for the scheduling problem, possibly {@code null}.
     * @param taskUncertainty the maximum duration with a feasible solution per task.
     */
    public record UncertaintyResult(Schedule schedule, Map<Task, Integer> taskUncertainty) {
    }

    private Double time;
    private CpSolverStatus status;
    private Schedule schedule;

    private UncertaintyResult perTaskUncertainty;

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
     * Sets results of the uncertainty analysis.
     *
     * @param uncertaintyResult the result of the analysis of uncertainties.
     */
    public void setUncertaintyResult(UncertaintyResult uncertaintyResult) {
        this.perTaskUncertainty = uncertaintyResult;
    }

    /**
     * Returns the results of the analyses of the tasks' uncertainty.
     *
     * @return a map of
     */
    public UncertaintyResult getUncertaintyResults() {
        return this.perTaskUncertainty;
    }
}
