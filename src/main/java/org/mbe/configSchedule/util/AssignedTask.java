package org.mbe.configSchedule.util;

public class AssignedTask {
    private int jobID;
    private int taskID;
    private int start;
    private int duration;
    private String name;
    private boolean isActive;

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
     * Sets ID of job.
     *
     * @param jobID new ID.
     */
    public void setJobID(int jobID) {
        this.jobID = jobID;
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
     * Sets ID of this task.
     *
     * @param taskID new ID
     */
    public void setTaskID(int taskID) {
        this.taskID = taskID;
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
     * Sets start time.
     *
     * @param start new start time.
     */
    public void setStart(int start) {
        this.start = start;
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
     * Sets duration of this task.
     *
     * @param duration new duration.
     */
    public void setDuration(int duration) {
        this.duration = duration;
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
     * Sets name of this task.
     *
     * @param name new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the active state of this task.
     *
     * @return isActive.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets active state of this task.
     *
     * @param active new active state.
     */
    public void setActive(boolean active) {
        isActive = active;
    }
}
