package org.cassandraunit.dataset.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class ParsedKeyspace {

    private String name;
    private int replicationFactor = 1;
    private String strategy = "org.apache.cassandra.locator.SimpleStrategy";
    private List<ParsedColumnFamily> columnFamilies = new ArrayList<ParsedColumnFamily>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String stategy) {
        this.strategy = stategy;
    }

    public void setColumnFamilies(List<ParsedColumnFamily> columnFamilies) {
        this.columnFamilies = columnFamilies;
    }

    public List<ParsedColumnFamily> getColumnFamilies() {
        return columnFamilies;
    }

}
