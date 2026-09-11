package org.cassandraunit.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate test for the Cassandra 5.0 / JDK 17 migration.
 * <p>
 * Every other part of the modernization assumes that Cassandra 5.0's {@code CassandraDaemon}
 * can still be activated in-process. The internal APIs this project depends on all still
 * exist at 5.0.8, but that is not the same as a working embedded boot: 5.0 dropped Java 8 and
 * pulled in new native and off-heap machinery. This test answers that question directly, and
 * deliberately exercises the daemon rather than any of the dataset-loading conveniences.
 * <p>
 * It requires the JVM flags in the {@code cu.cassandra.argLine} property of the parent pom -
 * without them the daemon fails on module access long before it opens a port.
 */
class EmbeddedCassandra50SmokeTest {

    @Test
    void shouldBootCassandra50InProcessAndServeCql() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(60_000L);

        CqlSession session = EmbeddedCassandraServerHelper.getSession();
        assertNotNull(session, "session should have been created");

        Row release = session.execute("SELECT release_version FROM system.local").one();
        assertNotNull(release, "system.local should return a row");
        String version = release.getString("release_version");
        assertTrue(version != null && version.startsWith("5."),
                "expected an embedded Cassandra 5.x, got " + version);

        session.execute("CREATE KEYSPACE smoke WITH replication = "
                + "{'class':'SimpleStrategy','replication_factor':1}");
        session.execute("CREATE TABLE smoke.widget (id int PRIMARY KEY, label text)");
        session.execute("INSERT INTO smoke.widget (id, label) VALUES (1, 'boots')");

        Row widget = session.execute("SELECT label FROM smoke.widget WHERE id = 1").one();
        assertNotNull(widget, "the row just inserted should be readable");
        assertEquals("boots", widget.getString("label"));

        // The ports actually bound must match what the helper reports, otherwise the yaml
        // rewriting and the session bootstrap have diverged.
        assertEquals(9142, EmbeddedCassandraServerHelper.getNativeTransportPort());
        assertEquals("Test Cluster", EmbeddedCassandraServerHelper.getClusterName());
    }
}
