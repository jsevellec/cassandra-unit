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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExpectedCassandraDataSet} through {@code CassandraUnitExtension}, which is the zero-setup
 * path for anyone already on the embedded server.
 * <p>
 * Only the passing direction can be tested from inside a test method - an expectation that fails
 * fails the test that carries it, which is the point but is awkward to assert on. The declining
 * direction ("do not verify after a failure") is covered by {@code ExpectedDataSetVerifierTest},
 * which calls the shared entry point directly.
 */
class ExpectedCassandraDataSetAnnotationTest {

    private static final String KEYSPACE = "assertannotationkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    /** Passes only if the extension actually ran the expectation afterwards. */
    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE)
    void theAnnotationShouldBeVerifiedAfterTheTest(CqlSession session) {
        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isEqualTo(3);
    }

    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE, ignoreColumns = "label")
    void ignoreColumnsShouldBeHonoured(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'changed'"
                + " WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570701");
    }

    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE, mode = MatchMode.CONTAINS)
    void modeShouldBeHonoured(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".widget (id, label)"
                + " VALUES (1690e8da-5bf8-49e8-9583-4dff8a5707ff, 'stray')");
    }

    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE,
            scope = Scope.MENTIONED_PARTITIONS)
    void scopeShouldBeHonoured(CqlSession session) {
        session.execute("INSERT INTO " + KEYSPACE + ".event (day, at, kind)"
                + " VALUES ('2026-09-21', '2026-09-21T08:00:00Z', 'other')");
    }

    /**
     * {@code value} is a {@code String[]}, and every location in it must be verified. Every other
     * test here lists one, so a loop that stopped after the first would pass all of them.
     */
    @Test
    @ExpectedCassandraDataSet(
            value = {"rows/expected-split-widget.yaml", "rows/expected-split-event.yaml"},
            keyspace = KEYSPACE)
    void everyLocationShouldBeVerified() {
    }

    /**
     * And the proof that the second location is really compared, which the test above cannot give:
     * a short-circuit after the first would leave it passing. Breaking the event half - the second
     * location - must fail. Invoked directly, because a failure raised by an extension is not
     * something the test carrying the annotation can catch.
     */
    @Test
    void breakingTheSecondLocationShouldFail(CqlSession session) throws Exception {
        session.execute("UPDATE " + KEYSPACE + ".event SET kind = 'changed'"
                + " WHERE day = '2026-09-20' AND at = '2026-09-20T09:00:00Z'");
        ExpectedCassandraDataSet annotation = getClass()
                .getDeclaredMethod("everyLocationShouldBeVerified")
                .getAnnotation(ExpectedCassandraDataSet.class);

        assertThatThrownBy(() -> ExpectedDataSetVerifier.verify(session, annotation))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("changed");
    }

    /** A test with no annotation must be entirely unaffected. */
    @Test
    void anUnannotatedTestShouldBeLeftAlone(CqlSession session) {
        session.execute("TRUNCATE " + KEYSPACE + ".widget");

        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isZero();
    }
}
