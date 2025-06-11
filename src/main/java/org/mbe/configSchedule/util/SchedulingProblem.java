package org.mbe.configSchedule.util;

import de.vill.model.FeatureModel;
import org.mbe.configSchedule.parser.UVLReader;

import java.io.Serializable;
import java.util.*;


public class SchedulingProblem implements Serializable {
    private final String name;
    private List<List<Task>> jobs;
    private List<Machine> machines;
    private int deadline;

    /**
     * Creates new object of type SchedulingProblem.
     *
     * @param name     the name/identifier of the problem.
     * @param jobs     a {@link List} of {@link List} of {@link Task Tasks}.
     * @param machines a {@link List} of {@link Machine Machines}.
     * @param deadline deadline of the scheduling problem.
     */
    public SchedulingProblem(String name, List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.name = name;
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
        this.name = UVLReader.parseName(featureModel);
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
     * Get machines of scheduling problem.
     *
     * @return {@link List} of {@link Machine Machines}.
     */
    public List<Machine> getMachines() {
        return machines;
    }

    /**
     * Get deadline of scheduling problem.
     *
     * <p>If the deadline is at infinity, this method returns {@code -1}.
     * Only non-negative numbers (>=0) should be interpreted as actual deadlines.
     *
     * @return deadline as an integer.
     */
    public int getDeadline() {
        return deadline;
    }

    /**
     * Get the name of the scheduling problem.
     *
     * @return the scheduling problem's name.
     */
    public String getName() {
        return name;
    }
}
