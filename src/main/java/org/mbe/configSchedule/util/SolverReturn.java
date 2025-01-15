package org.mbe.configSchedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolverReturn {
    private Double time;
    private CpSolverStatus status;
    private String output;
    private Map<Machine, List<AssignedTask>> assignedJobs;

    /**
     * Creates new object of type SolverReturn.
     *
     * @param time         time it took to solve the problem.
     * @param status       status of the solver.
     * @param output       String output for display.
     * @param assignedJobs Jobs executed in solution.
     */
    public SolverReturn(Double time, CpSolverStatus status, String output, Map<Machine, List<AssignedTask>> assignedJobs) {
        this.time = time;
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
     * Set time spend to solve the problem.
     *
     * @param time new time in ms.
     */
    public void setTime(Double time) {
        this.time = time;
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
     * Sets status of solver.
     *
     * @param status new status of solver.
     */
    public void setStatus(CpSolverStatus status) {
        this.status = status;
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
     * Set output String.
     *
     * @param output output String.
     */
    public void setOutput(String output) {
        this.output = output;
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
     * Set Jobs assigned to each machine.
     *
     * @param assignedJobs new assigned jobs.
     */
    public void setAssignedJobs(Map<Machine, List<AssignedTask>> assignedJobs) {
        this.assignedJobs = assignedJobs;
    }
}
