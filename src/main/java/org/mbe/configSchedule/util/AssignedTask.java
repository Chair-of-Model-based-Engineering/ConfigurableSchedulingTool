package org.mbe.configSchedule.util;

import java.util.Comparator;

/**
 * Represents a final task in a schedule.
 */
@SuppressWarnings("ClassCanBeRecord")
public class AssignedTask implements Comparable<AssignedTask> {
    private final int jobID;
    private final int taskID;
    private final int start;
    private final int duration;
    private final String name;

    /**
     * Creates new a new assigned task.
     *
     * @param jobID    ID of the corresponding Job.
     * @param taskID   ID of this task.
     * @param start    point in time when this task started its execution.
     * @param duration duration of this task.
     * @param name     name of this task.
     */
    public AssignedTask(int jobID, int taskID, int start, int duration, String name) {
        this.jobID = jobID;
        this.taskID = taskID;
        this.start = start;
        this.name = name;
        this.duration = duration;
    }

    /**
     * Gets the job ID.
     *
     * @return jobID
     */
    public int getJobID() {
        return jobID;
    }

    /**
     * Gets the ID of this task.
     *
     * @return taskID
     */
    public int getTaskID() {
        return taskID;
    }

    /**
     * Gets the point in time when the execution of this task began.
     *
     * @return start of task.
     */
    public int getStart() {
        return start;
    }

    /**
     * Gets duration of this task.
     *
     * @return duration.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets name of this task.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(AssignedTask o) {
        if (this.getStart() != o.getStart()) {
            return this.getStart() - o.getStart();
        } else {
            return this.getDuration() - o.getDuration();
        }
    }
}
