package org.mbe.configschedule.util;

import de.vill.model.Feature;
import de.vill.model.FeatureModel;

import java.io.Serializable;
import java.util.List;

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
        this.deadline = parseDeadline(featureModel);
        this.jobs = parseJobs(featureModel);
        this.machines = parseMachines(featureModel);
    }

    private int parseDeadline(FeatureModel featureModel) {
        String deadlineString = featureModel.getRootFeature().getAttributes().get("deadline").getValue().toString();
        try {
            deadline = Integer.parseInt(deadlineString);
        } catch (NumberFormatException e) {
            System.out.println("Deadline konnte nicht konvertiet werden, überprüfe Description von root");
        }
        return deadline;
    }

    private List<Machine> parseMachines(FeatureModel featureModel) {
        return List.of();
    }

    private List<List<Task>> parseJobs(FeatureModel featureModel) {
        return List.of();
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
