package org.cassandraunit.dataset;

import com.datastax.oss.driver.api.core.CqlSession;

import java.util.List;

/**
 * A dataset that needs the live session in order to load itself.
 * <p>
 * {@link CQLDataSet} can only express a dataset as text: a list of CQL statements, which
 * {@link org.cassandraunit.CQLDataLoader} hands straight to the session. That is enough for a CQL
 * script, where the author has already written every value in CQL literal syntax. It is not enough
 * for a dataset that describes <em>rows</em> - YAML, JSON, CSV, XML - because rendering a row as
 * text requires knowing each column's type, and getting that wrong is silent: a {@code text} column
 * holding {@code "1"} would render as {@code VALUES (1)}.
 * <p>
 * The types are already in the database by the time rows load, so the fix is to read them from
 * there rather than make the fixture re-declare them. That needs the session, hence this
 * sub-interface.
 * <p>
 * This is deliberately a sub-interface rather than a change to {@link CQLDataSet}: every existing
 * implementation, including third-party ones, keeps working untouched. {@code CQLDataLoader}
 * dispatches on {@code instanceof}.
 *
 * @author Jeremy Sevellec
 */
public interface SessionAwareDataSet extends CQLDataSet {

    /**
     * Load this dataset's data using the given session. Called by
     * {@link org.cassandraunit.CQLDataLoader} after the keyspace has been created and selected, so
     * unqualified table names resolve against the dataset's keyspace.
     */
    void load(CqlSession session);

    /**
     * A session-aware dataset emits no plain-text statements - it binds prepared ones inside
     * {@link #load(CqlSession)} instead - so this is empty by default.
     */
    @Override
    default List<String> getCQLStatements() {
        return List.of();
    }
}
