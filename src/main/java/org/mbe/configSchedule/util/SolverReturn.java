package org.example;

import com.google.ortools.sat.CpSolverStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolverReturn {
    public Double time;
    public CpSolverStatus status;
    public String output;
    public Map<Machine, List<AssignedTask>> assignedJobs;

    public SolverReturn(Double time, CpSolverStatus status, String output, Map<Machine, List<AssignedTask>> assignedJobs) {
        this.time = time;
        this.status = status;
        this.output = output;
        this.assignedJobs = assignedJobs;
    }

    public SolverReturn() {};
}
