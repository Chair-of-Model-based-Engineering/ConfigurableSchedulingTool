package org.mbe.configSchedule.util;

import java.io.Serializable;
import java.util.*;

public class Task implements Serializable {
    private Machine machine;
    private int[] durations = new int[0];
    private String name;
    private boolean optional;
    private List<String> excludeTasks = new ArrayList<>();
    private Map<Integer, List<Task>> durationCons = new HashMap<>();

    /**
     * Creates a new task.
     *
     * @param machine      contains on which machine this task is executed.
     * @param durations    contains all possible durations of the task.
     * @param name         contains the name of the task.
     * @param optional     contains wether the task is optional.
     * @param excludeTasks contains all tasks which are excluded with this task.
     */
    Task(Machine machine, int[] durations, String name, boolean optional, List<String> excludeTasks) {
        this.machine = machine;
        this.durations = durations;
        Arrays.sort(this.durations);
        this.name = name;
        this.optional = optional;
        this.excludeTasks = excludeTasks;
    }

    /**
     * Creates a new task.
     *
     * @param machine   contains on which machine this task is executed.
     * @param durations contains all possible durations of the task.
     * @param name      contains the name of the task.
     * @param optional  contains wether the task is optional.
     */
    Task(Machine machine, int[] durations, String name, boolean optional) {
        this(machine, durations, name, optional, new ArrayList<>());
    }

    /**
     * Creates a new Task with no values set.
     */
    public Task() {
    }

    /**
     * Gets the machine the task runs on.
     *
     * @return Variable of type {@link Machine}
     */
    public Machine getMachine() {
        return machine;
    }

    /**
     * Sets the machine
     *
     * @param machine defined the new machine.
     */
    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    /**
     * Gets possible durations of the task.
     *
     * @return Array of Integers.
     */
    public int[] getDurations() {
        return this.durations;
    }

    /**
     * Gets the minimum duration of the task.
     *
     * @return the minimum duration of the task
     */
    public int getMinimumDuration() {
        if (this.durations.length == 0)
            return 0;
        return this.durations[0];
    }

    /**
     * Gets the maximum duration of the task.
     *
     * @return the maximum duration of the task
     */
    public int getMaximumDuration() {
        if (this.durations.length == 0)
            return 0;
        return this.durations[this.durations.length - 1];
    }

    /**
     * Sets possible durations of the task.
     *
     * @param durations Array of Integers.
     */
    public void setDurations(int[] durations) {
        this.durations = durations;
        Arrays.sort(this.durations);
    }

    /**
     * Add a possible duration to the task.
     *
     * @param duration The duration to add.
     */
    public void addDuration(int duration) {
        if (this.durations == null)
            this.durations = new int[1];
        else
            this.durations = Arrays.copyOf(this.durations, this.durations.length + 1);

        this.durations[this.durations.length - 1] = duration;
        Arrays.sort(this.durations);
    }

    /**
     * Gets name of machine.
     *
     * @return Variable of type {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of machine.
     *
     * @param name contains new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if the task is optional.
     *
     * @return Variable of type {@link boolean}
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the attribute optional.
     *
     * @param optional contains new value for optional.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Gets List of excluded tasks.
     *
     * @return {@link List} of {@link String Strings} containing names of tasks.
     */
    public List<String> getExcludeTasks() {
        return excludeTasks;
    }

    /**
     * Sets new List of excluded tasks.
     *
     * @param excludeTasks {@link List} of {@link String Strings} containing names of tasks.
     */
    public void setExcludeTasks(List<String> excludeTasks) {
        this.excludeTasks = excludeTasks;
    }

    /**
     * Gets duration constraints.
     *
     * @return {@link Map} which maps {@link Integer Integers} to {@link List Lists} of {@link Task Tasks}.
     */
    public Map<Integer, List<Task>> getDurationCons() {
        return durationCons;
    }

    /**
     * Adds new duration constraint.
     *
     * @param key   {@link Integer}
     * @param value {@link List List} of {@link Task Tasks}
     */
    public void addDurationCon(int key, List<Task> value) {
        durationCons.put(key, value);
    }

    /**
     * Adds new task to existing duration constraint
     *
     * @param key  {@link Integer} corresponding to existing constraint.
     * @param task {@link Task} to be added.
     */
    public void addTaskToDurationCon(int key, Task task) {
        durationCons.get(key).add(task);
    }

    /**
     * Overrides {@link Object#toString()}.
     *
     * @return {@link String} which contains all information of the task.
     */
    @Override
    public String toString() {
        return String.format("p: %s/ d: %s/ m: %s/ o: %b",
                Optional.ofNullable(this.name).orElse("-"),
                Optional.ofNullable(this.durations).map(Arrays::toString).orElse("-"),
                Optional.ofNullable(this.machine).map(Machine::getName).orElse("-"),
                this.optional);
    }
}
