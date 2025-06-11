package org.mbe.configSchedule.util;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

public class TaskType {
    // I haven't found out how to extract these IntVars from the IntervalVar below. Therefore, we also store them separately.
    // With `model.getIntVarFromProtoIndex(interval.getEndExpr().getVariableIndex(0))`,
    // we could get a rebuilt IntVar for the end, but I'm not sure if that would work.
    private IntVar start;
    private IntVar end;
    private IntervalVar interval;
    private BoolVar active;
    private final Task task;

    /**
     * Constructs a new TaskType corresponding to a {@link SchedulingProblem}'s {@link Task}.
     *
     * @param task the corresponding task.
     */
    public TaskType(Task task) {
        this.task = task;
    }

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
     * Gets the {@link Task} corresponding to this TaskType.
     *
     * @return the corresponding task.
     */
    public Task getTask() {
        return task;
    }

    /**
     * Gets the name of the corresponding task. This is a shortcut for {@code TaskType.getTask().getName()}
     * @return the name of the corresponding task.
     */
    public String getName() {
        return this.task.getName();
    }
}
