package org.mbe.configSchedule.util;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskType extends Task {
    //IntVar has a lower bound, upper bound and name
    private IntVar start;
    private IntVar end;
    //IntervalVar does hava a start position, duration and an end date
    private IntervalVar interval;
    private BoolVar active;
    private Map<Integer, List<TaskType>> durationsConstraints = new HashMap<>();

    /**
     * Get lower bound.
     *
     * @return value of variable start.
     */
    public IntVar getStart() {
        return start;
    }

    /**
     * Sets lower bound.
     *
     * @param start contains new {@link IntVar} value.
     */
    public void setStart(IntVar start) {
        this.start = start;
    }

    /**
     * Gets upper bound.
     *
     * @return value of variable end.
     */
    public IntVar getEnd() {
        return end;
    }

    /**
     * Sets upper bound.
     *
     * @param end contains new {@link IntVar} value.
     */
    public void setEnd(IntVar end) {
        this.end = end;
    }

    /**
     * Gets interval.
     *
     * @return {@link IntervalVar} variable.
     */
    public IntervalVar getInterval() {
        return interval;
    }

    /**
     * Sets new interval.
     *
     * @param interval new {@link IntervalVar} value.
     */
    public void setInterval(IntervalVar interval) {
        this.interval = interval;
    }

    /**
     * Returns whether the task is active or not.
     *
     * @return value of type {@link BoolVar}.
     */
    public BoolVar getActive() {
        return active;
    }

    /**
     * Sets whether task is active or not.
     *
     * @param active new {@link BoolVar} value.
     */
    public void setActive(BoolVar active) {
        this.active = active;
    }

    /**
     * Gets duration constraints.
     *
     * @return {@link Map} which maps {@link Integer Integers} to {@link List Lists} of {@link TaskType TaskTypes}.
     */
    public Map<Integer, List<TaskType>> getDurationsConstraints() {
        return durationsConstraints;
    }

    /**
     * Adds new duration constraint.
     *
     * @param key   {@link Integer}
     * @param tasks {@link List List} of {@link TaskType TaskTypes}
     */
    public void addDurationsConstraint(Integer key, List<TaskType> tasks) {
        durationsConstraints.put(key, tasks);
    }

    /**
     * Adds new task to existing duration constraint
     *
     * @param key      {@link Integer} corresponding to existing constraint.
     * @param taskType {@link TaskType} to be added.
     */
    public void addTaskTypeToDuration(Integer key, TaskType taskType) {
        durationsConstraints.get(key).add(taskType);
    }
}
