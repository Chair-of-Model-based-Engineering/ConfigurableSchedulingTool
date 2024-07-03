package org.example;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

public class MandatoryTask extends FeatureTask{

    MandatoryTask(IntVar start, IntVar end, String name, int machine, IntVar duration, IntervalVar interval) {
        this.start = start;
        this.end = end;
        this.name = name;
        this.machine = machine;
        this.duration = duration;
        this.interval = interval;
    }

    MandatoryTask() {}
}
