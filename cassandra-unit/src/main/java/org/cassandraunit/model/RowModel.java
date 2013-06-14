package org.cassandraunit.model;

import org.cassandraunit.type.GenericType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class RowModel {

    private GenericType key;
    private List<SuperColumnModel> superColumns = new ArrayList<SuperColumnModel>();
    private List<ColumnModel> columns = new ArrayList<ColumnModel>();

    public GenericType getKey() {
        return key;
    }

    public void setKey(GenericType key) {
        this.key = key;
    }

    public List<SuperColumnModel> getSuperColumns() {
        return superColumns;
    }

    public void setSuperColumns(List<SuperColumnModel> superColumns) {
        this.superColumns = superColumns;
    }

    public void setColumns(List<ColumnModel> columns) {
        this.columns = columns;
    }

    public List<ColumnModel> getColumns() {
        return columns;
    }

}
