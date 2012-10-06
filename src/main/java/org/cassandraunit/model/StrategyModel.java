package org.cassandraunit.model;

/**
 * @author Jeremy Sevellec
 */
public enum StrategyModel {

    LOCAL_STRATEGY("org.apache.cassandra.locator.LocalStrategy"), NETWORK_TOPOLOGY_STRATEGY(
            "org.apache.cassandra.locator.NetworkTopologyStrategy"), SIMPLE_STRATEGY(
            "org.apache.cassandra.locator.SimpleStrategy");

    private final String value;

    StrategyModel(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static StrategyModel fromValue(String v) {
        for (StrategyModel c : StrategyModel.values()) {
            if (c.value.equalsIgnoreCase(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    @Override
    public String toString() {
        return value;
    }

}
