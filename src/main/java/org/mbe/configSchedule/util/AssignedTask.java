package org.mbe.configSchedule.util;

public class AssignedTask {
    private int jobID;
    private int taskID;
    private int start;
    private int duration;
    private String name;
    private boolean isActive;

    // Constructor
    public AssignedTask(int jobID, int taskID, int start, int duration, String name) {
        this.jobID = jobID;
        this.taskID = taskID;
        this.start = start;
        this.name = name;
        this.duration = duration;
    }

    public int getJobID() {
        return jobID;
    }

    public void setJobID(int jobID) {
        this.jobID = jobID;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
