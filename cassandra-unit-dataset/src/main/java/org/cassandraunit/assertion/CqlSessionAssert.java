package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.assertj.core.api.AbstractAssert;
import org.cassandraunit.dataset.ParseException;

/**
 * Assertions on a session, and the way in to a keyspace.
 *
 * @author Jeremy Sevellec
 */
public class CqlSessionAssert extends AbstractAssert<CqlSessionAssert, CqlSession> {

    CqlSessionAssert(CqlSession actual) {
        super(actual, CqlSessionAssert.class);
    }

    public CqlSessionAssert hasKeyspace(String keyspace) {
        isNotNull();
        if (!keyspaceExists(actual, keyspace)) {
            failWithMessage("%nExpected a keyspace named%n  %s%nbut the server has none",
                    keyspace);
        }
        return this;
    }

    public CqlSessionAssert doesNotHaveKeyspace(String keyspace) {
        isNotNull();
        if (keyspaceExists(actual, keyspace)) {
            failWithMessage("%nExpected no keyspace named%n  %s%nbut the server has one", keyspace);
        }
        return this;
    }

    /**
     * Narrows to one keyspace.
     * <p>
     * A keyspace that does not exist is a {@link ParseException}, not an assertion failure: you
     * cannot have meant to assert something about data in a keyspace that was never created, so
     * the test is wrong rather than the code under test. {@link #hasKeyspace} is how you assert
     * that it exists.
     */
    public KeyspaceAssert keyspace(String keyspace) {
        isNotNull();
        if (!keyspaceExists(actual, keyspace)) {
            throw new ParseException("CqlAssertions: no keyspace " + keyspace
                    + ". Check the name, and that the schema was loaded before the assertion ran."
                    + " Use assertThat(session).hasKeyspace(...) if its absence is what you meant"
                    + " to assert.");
        }
        return new KeyspaceAssert(actual, keyspace);
    }

    /**
     * Asks the server rather than the driver's metadata, matching {@code CqlOperations}: the
     * driver's {@code refreshed-keyspaces} filter would otherwise make the answer depend on driver
     * configuration instead of on what is really there.
     */
    private static boolean keyspaceExists(CqlSession session, String keyspace) {
        return session.execute(SimpleStatement.newInstance(
                        "SELECT keyspace_name FROM system_schema.keyspaces WHERE keyspace_name = ?",
                        keyspace))
                .one() != null;
    }
}
