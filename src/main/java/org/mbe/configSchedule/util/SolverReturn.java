package org.mbe.configSchedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.Optional;

public class SolverReturn {
    private Double time;
    private CpSolverStatus status;
    private Schedule schedule;

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
}
