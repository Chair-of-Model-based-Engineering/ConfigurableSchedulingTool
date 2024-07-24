package org.example;

import java.util.ArrayList;
import java.util.List;

public class Task {
    public int machine;
    public int[] duration;
    public String name;
    public boolean optional;
    public List<String> excludeTasks = new ArrayList<>();

    Task(int machine, int[] duration, String name, boolean optional, List<String> excludeTasks) {
        this.machine = machine;
        this.duration = duration;
        this.name = name;
        this.optional = optional;
        this.excludeTasks = excludeTasks;
    }

    Task(int machine, int[] duration, String name, boolean optional) {
        this.machine = machine;
        this.duration = duration;
        this.name = name;
        this.optional = optional;
    }

    Task() {}
}
