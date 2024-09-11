package org.example;

import com.google.ortools.sat.BoolVar;

import java.io.Serializable;

public class Machine implements Serializable {
    public String name;
    int id;
    boolean optional;
    BoolVar active;

    Machine(int id, boolean optional) {
        this.id = id;
        this.optional = optional;
    }
}
