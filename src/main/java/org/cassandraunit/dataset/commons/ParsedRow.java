package org.cassandraunit.dataset.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class ParsedRow {

    private String key;
    private List<ParsedSuperColumn> superColumns = new ArrayList<ParsedSuperColumn>();
    private List<ParsedColumn> columns = new ArrayList<ParsedColumn>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<ParsedSuperColumn> getSuperColumns() {
        return superColumns;
    }

    public void setSuperColumns(List<ParsedSuperColumn> superColumns) {
        this.superColumns = superColumns;
    }

    public void setColumns(List<ParsedColumn> columns) {
        this.columns = columns;
    }

    public List<ParsedColumn> getColumns() {
        return columns;
    }

}
