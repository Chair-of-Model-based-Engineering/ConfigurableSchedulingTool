package org.mbe.configschedule.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Task implements Serializable {
    private Machine machine;
    private int[] duration;
    private String name;
    private boolean optional;
    private List<String> excludeTasks = new ArrayList<>();

    Task(Machine machine, int[] duration, String name, boolean optional, List<String> excludeTasks) {
        this.machine = machine;
        this.duration = duration;
        this.name = name;
        this.optional = optional;
        this.excludeTasks = excludeTasks;
    }

    Task(Machine machine, int[] duration, String name, boolean optional) {
        this.machine = machine;
        this.duration = duration;
        this.name = name;
        this.optional = optional;
    }

    public Task() {}

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public int[] getDuration() {
        return duration;
    }

    public void setDuration(int[] duration) {
        this.duration = duration;
        Arrays.sort(this.duration);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public List<String> getExcludeTasks() {
        return excludeTasks;
    }

    public void setExcludeTasks(List<String> excludeTasks) {
        this.excludeTasks = excludeTasks;
    }
}
