package org.mbe.configSchedule.util;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskType {
    private String name;
    //IntVar hat einen lower bound, upper bound und Namen
    private IntVar start;
    private IntVar end;
    //IntervalVar besitzt start position, duration und end date
    private IntervalVar interval;
    private BoolVar active;
    private List<String> excludeTasks = new ArrayList<>();
    private Map<Integer, List<TaskType>> durationsConstraints = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IntVar getStart() {
        return start;
    }

    public void setStart(IntVar start) {
        this.start = start;
    }

    public IntVar getEnd() {
        return end;
    }

    public void setEnd(IntVar end) {
        this.end = end;
    }

    public IntervalVar getInterval() {
        return interval;
    }

    public void setInterval(IntervalVar interval) {
        this.interval = interval;
    }

    public BoolVar getActive() {
        return active;
    }

    public void setActive(BoolVar active) {
        this.active = active;
    }

    public List<String> getExcludeTasks() {
        return excludeTasks;
    }

    public void setExcludeTasks(List<String> excludeTasks) {
        this.excludeTasks = excludeTasks;
    }

    public Map<Integer, List<TaskType>> getDurationsConstraints() {
        return durationsConstraints;
    }

    public void addDurationsConstraint(Integer key, List<TaskType> tasks) {
        durationsConstraints.put(key, tasks);
    }

    public void addTaskTypeToDuration(Integer key, TaskType taskType) {
        durationsConstraints.get(key).add(taskType);
    }
}
