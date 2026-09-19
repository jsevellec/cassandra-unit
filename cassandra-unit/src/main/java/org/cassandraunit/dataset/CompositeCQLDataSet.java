package org.cassandraunit.dataset;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.utils.CqlOperations;

import java.util.List;

/**
 * Several datasets loaded in order as though they were one.
 * <p>
 * This exists because a row dataset describes data only, so it always needs a schema loaded first -
 * and {@code CassandraCQLUnit} and {@code CassandraUnitExtension} each take exactly one dataset.
 * Without a composite, a row dataset simply cannot be used from the JUnit rule or the JUnit 5
 * extension: with the default flags the keyspace is dropped and recreated empty and the first
 * INSERT has no table to go to, and with the flags off nothing creates the keyspace at all.
 * <p>
 * The keyspace is handled once, for the whole chain, from the flags this composite reports -
 * {@code CQLDataLoader} calls {@code initKeyspaceContext} on the composite and not on its members,
 * so the members' own flags are ignored. That is the same rule the Spring listener already applies
 * to {@code @CassandraDataSet({...})}, where only the first location drops and creates.
 *
 * @author Jeremy Sevellec
 */
public class CompositeCQLDataSet implements SessionAwareDataSet {

    private final List<CQLDataSet> dataSets;
    private final boolean keyspaceCreation;
    private final boolean keyspaceDeletion;
    private final String keyspaceName;

    public CompositeCQLDataSet(List<CQLDataSet> dataSets, boolean keyspaceCreation,
                               boolean keyspaceDeletion, String keyspaceName) {
        if (dataSets == null || dataSets.isEmpty()) {
            throw new ParseException("A composite dataset needs at least one dataset");
        }
        this.dataSets = List.copyOf(dataSets);
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
        this.keyspaceName = keyspaceName == null ? null : keyspaceName.toLowerCase();
    }

    @Override
    public void load(CqlSession session) {
        for (CQLDataSet dataSet : dataSets) {
            if (dataSet instanceof SessionAwareDataSet sessionAware) {
                sessionAware.load(session);
            } else {
                dataSet.getCQLStatements().forEach(CqlOperations.execute(session));
            }
        }
    }

    /** The datasets this composite loads, in load order. */
    public List<CQLDataSet> getDataSets() {
        return dataSets;
    }

    @Override
    public String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    public boolean isKeyspaceCreation() {
        return keyspaceCreation;
    }

    @Override
    public boolean isKeyspaceDeletion() {
        return keyspaceDeletion;
    }
}
