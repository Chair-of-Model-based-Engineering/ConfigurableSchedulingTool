package org.mbe.configschedule.util;

import com.google.ortools.sat.BoolVar;

import java.io.Serializable;

public class Machine implements Serializable {
    private String name;
    private int id;
    private boolean optional;
    private BoolVar active;

    public Machine(String name, int id, boolean optional) {
        this.name = name;
        this.id = id;
        this.optional = optional;
        this.active = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    //public void setId(int id) {
    //    this.id = id;
    //}

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public BoolVar getActive() {
        return active;
    }

    public void setActive(BoolVar active) {
        this.active = active;
    }
}
