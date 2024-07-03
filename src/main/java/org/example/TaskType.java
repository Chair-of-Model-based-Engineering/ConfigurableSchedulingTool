package org.example;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

public class TaskType {
    //IntVar hat einen lower bound, upper bound und Namen
    IntVar start;
    IntVar end;
    //IntervalVar besitzt start position, duration und end date
    IntervalVar interval;
    BoolVar active;

}
