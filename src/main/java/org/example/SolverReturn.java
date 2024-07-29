package org.example;

import com.google.ortools.sat.CpSolverStatus;

public class SolverReturn {
    public Double time;
    public CpSolverStatus status;
    public String output;

    public SolverReturn(Double time, CpSolverStatus status, String output) {
        this.time = time;
        this.status = status;
        this.output = output;
    }
}
