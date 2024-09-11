package org.example;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;

import java.util.ArrayList;
import java.util.List;

public class TaskType {
    String name;
    //IntVar hat einen lower bound, upper bound und Namen
    IntVar start;
    IntVar end;
    //IntervalVar besitzt start position, duration und end date
    IntervalVar interval;
    BoolVar active;
    List<String> excludeTasks = new ArrayList<>();

}
