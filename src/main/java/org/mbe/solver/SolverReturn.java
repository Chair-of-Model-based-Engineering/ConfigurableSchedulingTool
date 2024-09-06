package org.mbe.solver;

import com.google.ortools.sat.CpSolverStatus;

public class SolverReturn {
    private Double time;
    private CpSolverStatus status;
    private String output;

    public SolverReturn(Double time, CpSolverStatus status, String output) {
        this.time = time;
        this.status = status;
        this.output = output;
    }

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
}