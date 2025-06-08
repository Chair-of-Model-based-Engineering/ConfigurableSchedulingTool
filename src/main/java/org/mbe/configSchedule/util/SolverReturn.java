package org.mbe.configSchedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolverReturn {
    private double makespan;
    private Double time;
    private CpSolverStatus status;
    private String output;
    private Map<Machine, List<AssignedTask>> assignedJobs;

    /**
     * Creates new object of type SolverReturn.
     *
     * @param time         time it took to solve the problem.
     * @param makespan     makespan of the found schedule.
     * @param status       status of the solver.
     * @param output       String output for display.
     * @param assignedJobs Jobs executed in solution.
     */
    public SolverReturn(double time, CpSolverStatus status, double makespan, String output, Map<Machine, List<AssignedTask>> assignedJobs) {
        this.time = time;
        this.makespan = makespan;
        this.status = status;
        this.output = output;
        this.assignedJobs = assignedJobs;
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
     * Get output String.
     *
     * @return output String.
     */
    public String getOutput() {
        return output;
    }

    /**
     * Get all jobs assigned to each machine in solution.
     *
     * @return all assigned jobs.
     */
    public Map<Machine, List<AssignedTask>> getAssignedJobs() {
        return assignedJobs;
    }

    /**
     * Get the makespan of the found schedule.
     * @return the makespan of the found schedule.
     */
    public double getMakespan() {
        return makespan;
    }
}
