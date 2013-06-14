package org.cassandraunit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class KeyspaceModel {

    private String name;
    private int replicationFactor = 1;
    private StrategyModel strategy = StrategyModel.SIMPLE_STRATEGY;
    private List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();

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

    public void setStrategy(StrategyModel strategy) {
        this.strategy = strategy;
    }

    public StrategyModel getStrategy() {
        return strategy;
    }

    public void setColumnFamilies(List<ColumnFamilyModel> columnFamilies) {
        this.columnFamilies = columnFamilies;
    }

    public List<ColumnFamilyModel> getColumnFamilies() {
        return columnFamilies;
    }

}
