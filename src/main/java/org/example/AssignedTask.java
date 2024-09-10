package org.example;

public class AssignedTask {
    int jobID;
    int taskID;
    int start;
    int duration;
    String name;
    boolean isActive;

    // Constructor
    AssignedTask(int jobID, int taskID, int start, int duration, String name) {
        this.jobID = jobID;
        this.taskID = taskID;
        this.start = start;
        this.name = name;
        this.duration = duration;
    }
}
