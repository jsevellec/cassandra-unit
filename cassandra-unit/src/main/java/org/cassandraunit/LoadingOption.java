package org.cassandraunit;

import org.cassandraunit.model.StrategyModel;

public class LoadingOption {

    private boolean onlySchema = false;

    private boolean overrideReplicationFactor = false;
    private int replicationFactor = 0;

    private boolean overrideStrategy = false;
    private StrategyModel strategy = null;

    public boolean isOnlySchema() {
        return onlySchema;
    }

    public void setOnlySchema(boolean onlySchema) {
        this.onlySchema = onlySchema;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        if (replicationFactor < 0) {
            throw new IllegalArgumentException("Replication factor must be greater than 0");
        }
        this.replicationFactor = replicationFactor;
        overrideReplicationFactor = true;
    }

    public boolean isOverrideReplicationFactor() {
        return overrideReplicationFactor;
    }

    public StrategyModel getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyModel strategy) {
        this.strategy = strategy;
        overrideStrategy = true;

    }

    public boolean isOverrideStrategy() {
        return overrideStrategy;
    }
}
