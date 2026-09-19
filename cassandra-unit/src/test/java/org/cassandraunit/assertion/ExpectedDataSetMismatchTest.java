package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Every kind of mismatch, and the message each produces.
 * <p>
 * The message is the feature. A comparison that only says "did not match" would be worse than the
 * hand-written {@code SELECT} plus AssertJ it replaces, so the message is asserted here rather than
 * left to eyeballing.
 */
class ExpectedDataSetMismatchTest {

    private static final String KEYSPACE = "assertmismatchkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    private DataSetMismatchError verifyAndCatch(CqlSession session) {
        return catchThrowableOfType(DataSetMismatchError.class,
                () -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session));
    }

    @Test
    void aChangedValueShouldReportOneColumnOnTheRightRow(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'one', quantity = 41"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570701");

        DataSetMismatchError error = verifyAndCatch(session);

        assertThat(error.getDifferences())
                .extracting(Difference::kind, Difference::column)
                .contains(tuple(Difference.Kind.VALUE, "label"),
                        tuple(Difference.Kind.VALUE, "quantity"));
        assertThat(error).hasMessageContaining("different")
                .hasMessageContaining("expected '1'")
                .hasMessageContaining("but was 'one'")
                // matched by key, so this is one row with two bad columns, not two row differences
                .hasMessageNotContaining("missing")
                .hasMessageNotContaining("unexpected");
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }

    @Test
    void aDeletedRowShouldReportAsMissing(CqlSession session) {
        session.execute("DELETE FROM " + KEYSPACE + ".widget"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570702");

        DataSetMismatchError error = verifyAndCatch(session);

        assertThat(error.getDifferences()).extracting(Difference::kind)
                .contains(Difference.Kind.MISSING_ROW);
        assertThat(error).hasMessageContaining("1 missing")
                .hasMessageContaining("missing (expected, not found in the database)");
    }

    /**
     * The failure strict mode exists for: an upsert that landed under the wrong key. There is no
     * unique constraint to catch it, and a contains-style assertion would pass.
     */
    @Test
    void anExtraRowShouldReportAsUnexpected(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".widget (id, label)"
                + " VALUES (1690e8da-5bf8-49e8-9583-4dff8a5707ff, 'stray')");

        DataSetMismatchError error = verifyAndCatch(session);

        assertThat(error.getDifferences()).extracting(Difference::kind)
                .contains(Difference.Kind.UNEXPECTED_ROW);
        assertThat(error).hasMessageContaining("1 unexpected")
                .hasMessageContaining("unexpected (in the database, not in the expected dataset)")
                .hasMessageContaining("'stray'");
    }

    @Test
    void theMessageShouldNameTheFileTheModeAndTheStatement(CqlSession session) {
        session.execute("DELETE FROM " + KEYSPACE + ".widget"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570702");

        DataSetMismatchError error = verifyAndCatch(session);

        assertThat(error.getMessage())
                .contains("classpath:" + DATA)
                .contains("mode     : strict")
                .contains("SELECT")
                .contains(KEYSPACE + ".widget")
                // the echoed statement proves what was compared, and that it was not a filtered scan
                .doesNotContain("ALLOW FILTERING");
    }

    @Test
    void anExplicitNullShouldBeAssertedRatherThanIgnored(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'no longer null'"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570703");

        DataSetMismatchError error = verifyAndCatch(session);

        assertThat(error).hasMessageContaining("expected NULL")
                .hasMessageContaining("but was 'no longer null'");
    }

    /** Printed once so the report design is reviewable, not only assertable. */
    @Test
    void theWholeReportShouldReadWell(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'one', quantity = 41"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570701");
        session.execute("DELETE FROM " + KEYSPACE + ".widget"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570702");
        session.execute("INSERT INTO " + KEYSPACE + ".widget (id, label)"
                + " VALUES (1690e8da-5bf8-49e8-9583-4dff8a5707ff, 'stray')");

        DataSetMismatchError error = verifyAndCatch(session);

        System.out.println("\n---8<--- expected-dataset failure report ---8<---");
        System.out.println(error.getMessage());
        System.out.println("---8<--- end ---8<---\n");

        assertThat(error.getMessage())
                .contains("1 missing")
                .contains("1 unexpected")
                .contains("1 different");
    }
}
