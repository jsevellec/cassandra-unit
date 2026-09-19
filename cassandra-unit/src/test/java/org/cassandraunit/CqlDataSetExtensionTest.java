package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CqlDataSetExtension} against a session it did not create.
 * <p>
 * The extension's whole point is loading into a Cassandra someone else is running, so the honest
 * test is one where something else owns the session. The only Cassandra this build has is the
 * embedded one, so that plays the part - {@code EmbeddedCassandraServerHelper.getSession()} is,
 * from the extension's side, indistinguishable from a session built against a Testcontainers
 * container. Nothing here touches {@code CassandraUnitExtension}.
 * <p>
 * Note where the server is started: inside the supplier, not in a {@code @BeforeAll} method.
 * Jupiter runs a {@code @RegisterExtension} callback before any {@code @BeforeAll} method, so a
 * method would run too late. That is exactly the ordering trap a Testcontainers user hits, and
 * this test is shaped the way the documented example is for that reason.
 */
class CqlDataSetExtensionTest {

    private static final String KEYSPACE = "byosessionkeyspace";

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(CqlDataSetExtensionTest::embeddedSession)
            .schemaOnce(new ClassPathCQLDataSet("cql/rowsSchema.cql", KEYSPACE))
            .rowsPerTest(CQLDataSetFactory.fromClassPath("rows/widget.yaml", false, false, KEYSPACE))
            .build();

    /**
     * Stands in for "build a session against the container you already started". Starting the
     * server here rather than in a {@code @BeforeAll} method is the point - see the class comment.
     * {@code startEmbeddedCassandra} is idempotent, so sharing the JVM with other test classes is
     * fine.
     */
    private static CqlSession embeddedSession() {
        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        } catch (Exception e) {
            throw new IllegalStateException("could not start the embedded Cassandra", e);
        }
        return EmbeddedCassandraServerHelper.getSession();
    }

    private static Row widget(CqlSession session, String id) {
        return session.execute("SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + id).one();
    }

    @Test
    void shouldLoadRowsThroughASuppliedSession(CqlSession session) {
        Row row = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701");

        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(row.getLong("quantity")).isEqualTo(42L);
    }

    @Test
    void shouldResolveTheSuppliedSessionAsAParameter(CqlSession session) {
        assertThat(session).isSameAs(EmbeddedCassandraServerHelper.getSession());
        assertThat(fixtures.getSession()).isSameAs(session);
    }

    /**
     * The schema dataset drops and recreates its keyspace, so if it ran per test it would wipe the
     * rows. It runs once, and only because the keyspace was absent - which is what makes "schema
     * once, rows per test" usable across several test classes sharing a JVM.
     */
    @Test
    void shouldNotReloadTheSchemaOncePerTest(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701")).isNotNull();

        boolean loadedAgain = new CQLDataLoader(session)
                .loadIfKeyspaceAbsent(new ClassPathCQLDataSet("cql/rowsSchema.cql", KEYSPACE));

        assertThat(loadedAgain).isFalse();
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701")).isNotNull();
    }

    @Test
    void shouldNotCloseASessionItDidNotCreate() {
        assertThat(EmbeddedCassandraServerHelper.getSession().isClosed()).isFalse();
    }
}
