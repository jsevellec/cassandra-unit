package org.cassandraunit.dataset.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class ParsedSuperColumn {

    private String name;
    private List<ParsedColumn> columns = new ArrayList<ParsedColumn>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParsedColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ParsedColumn> columns) {
        this.columns = columns;
    }

}
