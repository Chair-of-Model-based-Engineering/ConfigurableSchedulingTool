package org.example;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

public class FeatureTask {
    IntVar start;
    IntVar end;
    IntVar duration;
    String name;
    int machine;
    IntervalVar interval;
}
