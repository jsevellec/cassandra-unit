package org.cassandraunit.model;

import org.cassandraunit.type.GenericType;

/**
 * @author Jeremy Sevellec
 */
public class ColumnModel {

    private GenericType name;
    private GenericType value;
    private Long timestamp;

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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}
