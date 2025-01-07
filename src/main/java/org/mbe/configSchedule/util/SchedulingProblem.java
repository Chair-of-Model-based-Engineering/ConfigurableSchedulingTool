package org.mbe.configSchedule.util;

import de.vill.model.FeatureModel;
import org.mbe.configSchedule.parser.UVLReader;

import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.stream;


public class SchedulingProblem implements Serializable {
    private List<List<Task>> jobs;
    private List<Machine> machines;
    private int deadline;

    public SchedulingProblem(List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.jobs = jobs;
        this.machines = machines;
        this.deadline = deadline;
    }

    public SchedulingProblem(FeatureModel featureModel) {
        this.deadline = UVLReader.parseDeadline(featureModel);
        this.machines = UVLReader.parseMachines(featureModel);
        this.jobs = UVLReader.parseJobs(featureModel, machines);
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
