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

    /**
     * Creates new object of type SchedulingProblem.
     *
     * @param jobs     a {@link List} of {@link List} of {@link Task Tasks}.
     * @param machines a {@link List} of {@link Machine Machines}.
     * @param deadline deadline of the scheduling problem.
     */
    public SchedulingProblem(List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.jobs = jobs;
        this.machines = machines;
        this.deadline = deadline;
    }

    /**
     * Creates new objrct of type Scheduling problem from a feature model.
     *
     * @param featureModel parsed feature model.
     */
    public SchedulingProblem(FeatureModel featureModel) {
        this.deadline = UVLReader.parseDeadline(featureModel);
        this.machines = UVLReader.parseMachines(featureModel);
        this.jobs = UVLReader.parseJobs(featureModel, machines);
    }

    /**
     * Get jobs of scheduling problem.
     *
     * @return a {@link List} of {@link List} of {@link Task Tasks}.
     */
    public List<List<Task>> getJobs() {
        return jobs;
    }

    /**
     * Set jobs of scheduling problem.
     *
     * @param jobs a {@link List} of {@link List} of {@link Task Tasks}.
     */
    public void setJobs(List<List<Task>> jobs) {
        this.jobs = jobs;
    }

    /**
     * Get machines of scheduling problem.
     *
     * @return {@link List} of {@link Machine Machines}.
     */
    public List<Machine> getMachines() {
        return machines;
    }

    /**
     * Set machines of scheduling problem.
     *
     * @param machines {@link List} of {@link Machine Machines}.
     */
    public void setMachines(List<Machine> machines) {
        this.machines = machines;
    }

    /**
     * Get deadline of scheduling problem.
     *
     * @return deadline as an integer.
     */
    public int getDeadline() {
        return deadline;
    }

    /**
     * Set deadline of scheduling Problem.
     *
     * @param deadline new deadline.
     */
    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

}
