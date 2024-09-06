package org.example;

import java.io.Serializable;
import java.util.List;

public class SchedulingProblem implements Serializable {
    List<List<Task>> jobs;
    List<Machine> machines;
    int deadline;

    SchedulingProblem(List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.jobs = jobs;
        this.machines = machines;
        this.deadline = deadline;
    }
}
