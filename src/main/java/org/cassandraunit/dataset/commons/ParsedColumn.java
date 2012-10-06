package org.cassandraunit.dataset.commons;

/**
 * @author Jeremy Sevellec
 */
public class ParsedColumn {

    private String name;
    private String value;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
