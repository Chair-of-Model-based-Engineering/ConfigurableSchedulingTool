package org.mbe.configSchedule.util;

import com.google.ortools.sat.BoolVar;

import java.io.Serializable;

public class Machine implements Serializable {
    private String name;
    private boolean optional;
    private BoolVar active;

    /**
     * To create a object of type machine the {@link String String} attribute name and the {@link boolean boolean} attribute optional have to be
     * specified the {@link BoolVar BoolVar} attribute active is set to null by default.
     *
     * @param name     contains the name of the machine.
     * @param optional is true if the machine is optional, false otherwise.
     */
    public Machine(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
        this.active = null;
    }

    /**
     * Gets the attribute name.
     *
     * @return Variable of type {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the attribute name.
     *
     * @param name contains name of the machine.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if the machine is optional.
     *
     * @return Variable of type {@link boolean}
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the attribute optional.
     *
     * @param optional contains new value for optional.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Gets the attribute active
     *
     * @return Variable of type {@link BoolVar}
     */
    public BoolVar getActive() {
        return active;
    }

    /**
     * Sets the attribute active.
     *
     * @param active contains new value of type {@link BoolVar}
     */
    public void setActive(BoolVar active) {
        this.active = active;
    }
}
