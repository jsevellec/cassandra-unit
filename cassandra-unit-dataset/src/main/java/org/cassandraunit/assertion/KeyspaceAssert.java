package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.assertj.core.api.AbstractAssert;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.rows.TableNames.QualifiedTable;

/**
 * Assertions on one keyspace, and the way in to a table.
 *
 * @author Jeremy Sevellec
 */
public class KeyspaceAssert extends AbstractAssert<KeyspaceAssert, String> {

    private final CqlSession session;

    KeyspaceAssert(CqlSession session, String keyspace) {
        super(keyspace, KeyspaceAssert.class);
        this.session = session;
    }

    public KeyspaceAssert hasTable(String table) {
        isNotNull();
        if (!tableExists(table)) {
            failWithMessage("%nExpected keyspace %s to hold a table named%n  %s%nbut it does not",
                    actual, table);
        }
        return this;
    }

    public KeyspaceAssert doesNotHaveTable(String table) {
        isNotNull();
        if (tableExists(table)) {
            failWithMessage("%nExpected keyspace %s not to hold a table named%n  %s%nbut it does",
                    actual, table);
        }
        return this;
    }

    /**
     * The bridge to the file-driven half of this package: assert that this keyspace matches an
     * expected dataset, inside an AssertJ chain.
     * <p>
     * It sits on the keyspace rather than on a table because that is the scope
     * {@link ExpectedDataSet#verify} really works at - a dataset can name several tables, and
     * pretending otherwise on a table-level method would be a lie about what was checked.
     */
    public KeyspaceAssert matches(ExpectedDataSet expected) {
        isNotNull();
        expected.verify(session);
        return this;
    }

    /**
     * Narrows to one table. A table that does not exist is a {@link ParseException} - see
     * {@link CqlSessionAssert#keyspace} for why - and {@link #hasTable} is how you assert on its
     * existence.
     */
    public TableAssert table(String table) {
        isNotNull();
        if (!tableExists(table)) {
            throw new ParseException("CqlAssertions: no table " + actual + '.' + table
                    + ". Check the name, and that the schema was loaded before the assertion ran."
                    + " Use hasTable(...) if its absence is what you meant to assert.");
        }
        return new TableAssert(session, new QualifiedTable(actual, table));
    }

    private boolean tableExists(String table) {
        return session.execute(SimpleStatement.newInstance(
                        "SELECT table_name FROM system_schema.tables"
                                + " WHERE keyspace_name = ? AND table_name = ?",
                        actual, table))
                .one() != null;
    }
}
