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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * How much of a table is asserted, and the one ordering that can be.
 */
class ExpectedDataSetScopeTest {

    private static final String KEYSPACE = "assertscopekeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    @Test
    void containsShouldTolerateARowTheDatasetDoesNotList(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".widget (id, label)"
                + " VALUES (1690e8da-5bf8-49e8-9583-4dff8a5707ff, 'stray')");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .isInstanceOf(DataSetMismatchError.class);

        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .containing()
                .verify(session))
                .doesNotThrowAnyException();
    }

    /**
     * A row added to a partition the dataset never mentions. Strict over the whole table sees it;
     * strict within the mentioned partitions does not.
     */
    @Test
    void mentionedPartitionsShouldIgnoreOtherPartitions(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".event (day, at, kind)"
                + " VALUES ('2026-09-21', '2026-09-21T08:00:00Z', 'other')");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE).verify(session))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("unexpected");

        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .withinMentionedPartitions()
                .verify(session))
                .doesNotThrowAnyException();
    }

    /** Within a named partition, strict still means exactly the listed rows. */
    @Test
    void mentionedPartitionsShouldStillBeStrictInsideEachPartition(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".event (day, at, kind)"
                + " VALUES ('2026-09-19', '2026-09-19T23:00:00Z', 'late')");

        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .withinMentionedPartitions()
                .verify(session))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("unexpected");
    }

    @Test
    void theRestrictedStatementShouldNameThePartitionAndNotFilter(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".event SET kind = 'changed'"
                + " WHERE day = '2026-09-19' AND at = '2026-09-19T12:00:00Z'");

        DataSetMismatchError error = catchThrowableOfType(DataSetMismatchError.class,
                () -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                        .withinMentionedPartitions()
                        .verify(session));

        assertThat(error.getMessage())
                .contains("WHERE day = ?")
                .doesNotContain("ALLOW FILTERING");
    }

    /**
     * The event table is declared WITH CLUSTERING ORDER BY (at DESC), and assertion-data.yaml lists
     * the 2026-09-19 partition newest-first to match. Off by default the order is not compared, so
     * a file listing them the other way still passes; switched on, it does not.
     */
    @Test
    void clusteringOrderShouldBeOptionalButRealWhenAskedFor(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-event-misordered.yaml", KEYSPACE).verify(session))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-event-misordered.yaml", KEYSPACE)
                .checkingClusteringOrder()
                .verify(session))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("out of clustering order");
    }

    @Test
    void clusteringOrderShouldPassWhenTheFileMatchesTheDeclaredOrder(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE)
                .checkingClusteringOrder()
                .verify(session))
                .doesNotThrowAnyException();
    }
}
