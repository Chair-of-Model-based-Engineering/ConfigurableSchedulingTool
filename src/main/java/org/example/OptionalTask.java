package org.example;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

public class OptionalTask extends FeatureTask{
    BoolVar status;

    OptionalTask(IntVar start, IntVar end, String name, int machine, IntVar duration, IntervalVar interval, BoolVar status) {
        this.start = start;
        this.end = end;
        this.name = name;
        this.machine = machine;
        this.duration = duration;
        this.interval = interval;
        this.status = status;
    }

    OptionalTask() {}
}
