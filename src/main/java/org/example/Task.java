package org.example;

public class Task {
    int machine;
    int[] duration;
    String name;
    boolean optional;
    Task(int machine, int[] duration, String name, boolean optional) {
        this.machine = machine;
        this.duration = duration;
        this.name = name;
        this.optional = optional;
    }
}
