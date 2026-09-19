package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.CQLDataLoader.Isolation;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.CqlOperations;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link Isolation} - how a load clears what the last test left behind.
 * <p>
 * Driven through {@link CQLDataLoader} directly rather than through an extension, because the
 * extensions reload per test and would keep resetting the state each case is trying to observe.
 * Every test establishes its own precondition, so none depends on the order the others run in.
 */
class CQLDataLoadWithIsolationTest {

    private static final String KEYSPACE = "isolationkeyspace";
    private static final String FRESH_KEYSPACE = "isolationfreshkeyspace";
    private static final String STRAY = "1690e8da-5bf8-49e8-9583-4dff8a5707ff";

    private static CqlSession session;
    private CQLDataLoader loader;

    @BeforeAll
    static void startServer() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        session = EmbeddedCassandraServerHelper.getSession();
    }

    @BeforeEach
    void freshState() {
        loader = new CQLDataLoader(session);
        loader.loadIfKeyspaceAbsent(schema(KEYSPACE));
        CqlOperations.truncateKeyspace(session, KEYSPACE);
        loader.load(rows());
    }

    private static CQLDataSet schema(String keyspace) {
        return new ClassPathCQLDataSet("cql/assertionSchema.cql", keyspace);
    }

    /** Rows only, and neither creating nor deleting the keyspace - what TRUNCATE mode is built for. */
    private static CQLDataSet rows() {
        return CQLDataSetFactory.fromClassPath("rows/assertion-data.yaml", false, false, KEYSPACE);
    }

    private static long widgetCount() {
        return session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0);
    }

    private static void insertStray() {
        session.execute("INSERT INTO " + KEYSPACE + ".widget (id, label) VALUES (" + STRAY + ", 'stray')");
    }

    private static Row stray() {
        return session.execute("SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + STRAY).one();
    }

    @Test
    void truncateShouldEmptyTheTablesAndKeepTheSchema() {
        insertStray();
        assertThat(widgetCount()).isEqualTo(4);

        loader.load(rows(), Isolation.TRUNCATE);

        // Three, not four: the stray was truncated away and only the dataset's rows came back. The
        // count answering at all is the other half - the table survived, so TRUNCATE did not turn
        // into a drop.
        assertThat(widgetCount()).isEqualTo(3);
        assertThat(stray()).isNull();
    }

    /**
     * Counters cannot be written by a row dataset, so nothing reloads this one. It must come back
     * empty, which is what proves TRUNCATE reached every table rather than only the ones the
     * dataset names.
     */
    @Test
    void truncateShouldResetACounterTable() {
        session.execute("UPDATE " + KEYSPACE + ".tally SET hits = hits + 5 WHERE id = 'b'");
        assertThat(session.execute("SELECT hits FROM " + KEYSPACE + ".tally WHERE id = 'b'").one())
                .isNotNull();

        loader.load(rows(), Isolation.TRUNCATE);

        assertThat(session.execute("SELECT hits FROM " + KEYSPACE + ".tally WHERE id = 'b'").one())
                .isNull();
    }

    /**
     * The first test of a run has no keyspace to keep. TRUNCATE has to create it and carry on, or
     * the mode would only work from the second test onwards.
     */
    @Test
    void truncateShouldCreateTheKeyspaceWhenItIsNotThere() {
        CqlOperations.dropKeyspace(session).accept(FRESH_KEYSPACE);

        assertThatCode(() -> loader.load(schema(FRESH_KEYSPACE), Isolation.TRUNCATE))
                .doesNotThrowAnyException();

        assertThat(session.execute("SELECT count(*) FROM " + FRESH_KEYSPACE + ".widget").one()
                .getLong(0)).isZero();
    }

    /**
     * Two loads in one method rather than two tests, so this says nothing about the order tests
     * run in - which is the kind of assumption that makes a suite pass alone and fail together.
     */
    @Test
    void noneShouldClearNothing() {
        insertStray();

        loader.load(rows(), Isolation.NONE);
        loader.load(rows(), Isolation.NONE);

        assertThat(stray()).isNotNull();
        assertThat(widgetCount()).isEqualTo(4);
    }

    /** NONE still selects the keyspace, or an unqualified statement would go somewhere else. */
    @Test
    void noneShouldStillSelectTheKeyspace() {
        session.execute("USE system");

        loader.load(rows(), Isolation.NONE);

        assertThat(session.execute("SELECT count(*) FROM widget").one().getLong(0)).isEqualTo(3);
    }

    /** The default is unchanged: the dataset's own flags still decide. */
    @Test
    void datasetShouldRemainTheDefault() {
        insertStray();
        CQLDataSet dropAndCreate = schema(KEYSPACE);
        assertThat(dropAndCreate.isKeyspaceDeletion()).isTrue();

        loader.load(dropAndCreate);

        assertThat(widgetCount()).isZero();
    }
}
