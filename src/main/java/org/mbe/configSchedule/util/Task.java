package org.mbe.configSchedule.util;

import java.util.*;

public class Task {
    private Machine machine;
    private int[] durations = new int[0];
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Integer> unboundDurations = Optional.empty();
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
     * @param optional     contains whether the task is optional.
     * @param excludeTasks contains all tasks which are excluded with this task.
     */
    public Task(Machine machine, int[] durations, String name, boolean optional, List<String> excludeTasks) {
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
     * @param machine          the machine on which this task has to be executed.
     * @param durations        all possible durations of the task.
     * @param unboundDurations the lower bound of the unbound durations
     * @param name             the name of the task.
     * @param optional         whether the task is optional.
     * @param excludeTasks     all tasks which are excluded with this task.
     */
    Task(Machine machine, int[] durations, int unboundDurations, String name, boolean optional, List<String> excludeTasks) {
        this(machine, durations, name, optional, excludeTasks);
        this.unboundDurations = Optional.of(unboundDurations);
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
     * <p>This also takes unbound durations into account. This returns the smaller duration
     * between the smallest normal duration and the lower bound of the unbound durations.
     *
     * @return the minimum duration of the task
     */
    public int getMinimumDuration() {
        if (this.durations.length == 0)
            return this.unboundDurations.orElse(0);
        else
            return this.unboundDurations
                    .map(lowerBound -> Math.min(lowerBound, this.durations[0]))
                    .orElseGet(() -> this.durations[0]);
    }

    /**
     * Gets the maximum duration of the task.
     *
     * <p>If the task has unbound durations, this only returns the bigger value between
     * the biggest normal duration and the lower bound of the unbound durations.
     *
     * @return the maximum duration of the task
     */
    public int getMaximumDuration() {
        if (this.durations.length == 0)
            return this.unboundDurations.orElse(0);
        else
            return this.unboundDurations
                    .map(lowerBound -> Math.max(lowerBound, this.durations[this.durations.length - 1]))
                    .orElseGet(() -> this.durations[this.durations.length - 1]);
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
        if (this.durations == null) {
            this.durations = new int[] {duration};
        } else {
            // Don't include durations multiple times.
            for (int d : this.durations)
                if (d == duration) return;

            this.durations = Arrays.copyOf(this.durations, this.durations.length + 1);
            this.durations[this.durations.length - 1] = duration;
            Arrays.sort(this.durations);
        }
    }

    /**
     * Returns whether the task's duration is uncertain.
     *
     * @return {@code true} if the task has more than one possible duration, {@code false} otherwise.
     */
    public boolean hasUncertainDurations() {
        return this.durations.length > 1 || hasUnboundDurations();
    }

    /**
     * Returns whether the task has unbound durations.
     *
     * @return {@code true} if the task has unbound durations
     */
    public boolean hasUnboundDurations() {
        return this.unboundDurations.isPresent();
    }

    /**
     * Returns the lower bound of the unbound durations, if available.
     *
     * @return {@link Optional} of the lower bound of the unbound durations or {@link Optional#empty()} if none are set.
     * @see #hasUnboundDurations() for checking if unbound durations are set.
     */
    public Optional<Integer> getUnboundDurations() {
        return this.unboundDurations;
    }

    /**
     * Sets the lower bound of the unbound durations.
     *
     * @param lowerBound the lower bound of the unbound durations.
     */
    public void setUnboundDurations(int lowerBound) {
        this.unboundDurations = Optional.of(lowerBound);
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
        durationCons.computeIfAbsent(key, _ -> new ArrayList<>()).add(task);
    }

    /**
     * Overrides {@link Object#toString()}.
     *
     * @return {@link String} which contains all information of the task.
     */
    @Override
    public String toString() {
        return String.format("p: %s/ d: %s/ ud: %s/ m: %s/ o: %b",
                Optional.ofNullable(this.name).orElse("-"),
                Optional.ofNullable(this.durations).map(Arrays::toString).orElse("-"),
                this.unboundDurations.map(String::valueOf).orElse("-"),
                Optional.ofNullable(this.machine).map(Machine::getName).orElse("-"),
                this.optional);
    }

    public boolean hasDuration(int duration) {
        if (this.unboundDurations.isPresent() && this.unboundDurations.get() <= duration) {
            return true;
        }

        for (int d : this.durations) {
            if (d == duration)
                return true;
        }

        return false;
    }
}
