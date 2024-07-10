package org.example;

import com.google.ortools.sat.BoolVar;

public class Machine {
    public String name;
    int id;
    boolean optional;
    BoolVar active;

    Machine(int id, boolean optional) {
        this.id = id;
        this.optional = optional;
    }
}
