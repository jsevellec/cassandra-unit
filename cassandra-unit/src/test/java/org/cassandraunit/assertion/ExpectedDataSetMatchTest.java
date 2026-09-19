package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An expectation that holds.
 * <p>
 * The headline case is the first test: the same file states the setup and the expectation. That
 * only works because the two directions agree on what a row means - absent is not asserted,
 * explicit null is - and it is the property most worth protecting.
 */
class ExpectedDataSetMatchTest {

    private static final String KEYSPACE = "assertmatchkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    @Test
    void theSameFileShouldStateTheSetupAndTheExpectation(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    /**
     * Rows are listed here in a different order than the file loads them and than Cassandra returns
     * them. Across partitions that is never compared, because the order is partition-token order.
     */
    @Test
    void rowOrderAcrossPartitionsShouldNotMatter(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-widget-reordered.yaml", KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    /**
     * A counter cannot be written by a row dataset, but it reads back as a bigint, so it can be
     * asserted. Seeded by assertionSchema.cql.
     */
    @Test
    void countersShouldBeAssertable(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-tally.yaml", KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    @Test
    void aColumnTheDatasetDoesNotMentionShouldNotBeAsserted(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET ratio = 99.0"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570702");

        // The second row of the dataset never mentions ratio, so changing it changes nothing here.
        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoringAColumnShouldSurviveItChanging(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'changed'"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570701");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .isInstanceOf(DataSetMismatchError.class);

        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .ignoringColumns("label")
                .verify(session))
                .doesNotThrowAnyException();
    }

    /**
     * SetCodec decodes null or empty bytes to an empty set, so an empty collection in the fixture
     * and a null column in the database are the same value and must compare equal.
     */
    @Test
    void anEmptyCollectionAndAnAbsentOneShouldBeTheSameValue(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET tags = null"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570702");

        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    @Test
    void aDoubleShouldBeExactByDefaultAndTolerantWhenAsked(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET ratio = 1.5001"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570701");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .isInstanceOf(DataSetMismatchError.class);

        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .withNumericTolerance(0.001)
                .verify(session))
                .doesNotThrowAnyException();
    }

    /** A static column's value repeats on every row of its partition; every listed row must agree. */
    @Test
    void staticColumnsShouldBeAsserted(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .doesNotThrowAnyException();

        session.execute("UPDATE " + KEYSPACE + ".event SET total = 99 WHERE day = '2026-09-19'");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("total");
    }
}
