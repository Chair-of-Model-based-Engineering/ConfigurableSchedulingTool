package org.mbe.configschedule.util;

import com.google.ortools.sat.CpSolverStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolverReturn {
    private Double time;
    private CpSolverStatus status;
    private String output;
    private Map<Machine, List<AssignedTask>> assignedJobs;

    public SolverReturn(Double time, CpSolverStatus status, String output, Map<Machine, List<AssignedTask>> assignedJobs) {
        this.time = time;
        this.status = status;
        this.output = output;
        this.assignedJobs = assignedJobs;
    }

    public SolverReturn() {};

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public CpSolverStatus getStatus() {
        return status;
    }

    public void setStatus(CpSolverStatus status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Map<Machine, List<AssignedTask>> getAssignedJobs() {
        return assignedJobs;
    }

    public void setAssignedJobs(Map<Machine, List<AssignedTask>> assignedJobs) {
        this.assignedJobs = assignedJobs;
    }
}
