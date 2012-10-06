package org.cassandraunit.model;

import org.cassandraunit.type.GenericType;

/**
 * @author Jeremy Sevellec
 */
public class ColumnModel {

    private GenericType name;
    private GenericType value;

    public GenericType getName() {
        return name;
    }

    public void setName(GenericType name) {
        this.name = name;
    }

    public GenericType getValue() {
        return value;
    }

    public void setValue(GenericType value) {
        this.value = value;
    }

}
