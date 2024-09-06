package org.mbe.model;

import java.util.List;

public class SchedulingProblem {
    private List<List<Task>> jobs;
    private List<Machine> machines;
    private int deadline;

    public SchedulingProblem(List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.jobs = jobs;
        this.machines = machines;
        this.deadline = deadline;
    }

    public List<List<Task>> getJobs() {
        return jobs;
    }

    public void setJobs(List<List<Task>> jobs) {
        this.jobs = jobs;
    }

    public List<Machine> getMachines() {
        return machines;
    }

    public void setMachines(List<Machine> machines) {
        this.machines = machines;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }
}