package org.cassandraunit.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests the embedded server on a randomly chosen free port.
 * <p>
 * This used to be {@code @Ignore}d, with the note that "Cassandra can only be started once
 * per JVM". The first half of that is still true and always will be - Cassandra's
 * DatabaseDescriptor, Schema and StorageService hold static state that cannot be reset
 * in-process - but it was never a reason to skip the test. It only meant the test could not
 * share a JVM with a test that uses a different configuration. The build now sets
 * surefire's {@code reuseForks=false}, so every test class gets its own JVM and this runs
 * normally.
 *
 * @author Markus Kull
 */
public class EmbeddedCassandraServerHelperTest {

    /**
     * Started once for the class. Every test method below used to call
     * startEmbeddedCassandra itself, but only the first call actually boots - the rest hit the
     * "already started" early return - so which method did the booting depended on JUnit's
     * method ordering. Starting it here makes that explicit instead of incidental.
     */
    @BeforeClass
    public static void startCassandra() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
    }

    @Test
    public void shouldStartupOnRandomFreePort() {
        int nativePort = EmbeddedCassandraServerHelper.getNativeTransportPort();
        assertThat(nativePort, is(greaterThan(0)));

        // Deliberately not asserting the port differs from the fixed default: the port comes
        // from ServerSocket(0), i.e. the OS ephemeral range, so such an assertion would pass
        // because of the platform's port range rather than because of anything in the code.
        // That the port is actually bound and serving is the real property, checked below.
        testIfTheEmbeddedCassandraServerIsUpOnHost("127.0.0.1", nativePort);
    }

    private void testIfTheEmbeddedCassandraServerIsUpOnHost(String host, int port) {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter("datacenter1")
                .build()) {

            assertThat(session.getMetadata().getNodes().size(), is(1));
            // Deliberately a query and not session.getMetadata().getKeyspace("system"): the
            // driver excludes system keyspaces from schema metadata by default, so the
            // metadata route returns an empty Optional and would assert nothing useful.
            assertThat(keyspaceNames(session), hasItem("system"));
            long systemTables = session
                    .execute("SELECT table_name FROM system_schema.tables WHERE keyspace_name = 'system'")
                    .all().size();
            assertThat(systemTables, is(greaterThan(0L)));
        }
    }

    /**
     * The clean path previously had no live coverage at all: the only test for it sat inside
     * an {@code @Ignore}d class, and it asserted nothing - {@code cleanEmbeddedCassandra()}
     * silently no-ops when no session was ever created, so it passed without doing anything.
     */
    @Test
    public void shouldDropNonSystemKeyspacesButKeepSystemOnes() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        session.execute("CREATE KEYSPACE IF NOT EXISTS to_be_dropped WITH replication = "
                + "{'class':'SimpleStrategy','replication_factor':1}");
        assertThat(keyspaceNames(session), hasItem("to_be_dropped"));

        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();

        assertThat(keyspaceNames(session), not(hasItem("to_be_dropped")));
        // The system keyspaces must survive.
        assertThat(keyspaceNames(session), hasItem("system"));
        assertThat(keyspaceNames(session), hasItem("system_schema"));
        assertThat(keyspaceNames(session), hasItem("system_auth"));
    }

    @Test
    public void shouldDropAKeyspaceWhoseNameIsNotLowerCase() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        // Quoted, so Cassandra stores the name case-sensitively. Dropping this used to fail
        // because the identifier was concatenated into the DROP statement unquoted (#222).
        session.execute("CREATE KEYSPACE IF NOT EXISTS \"MixedCase\" WITH replication = "
                + "{'class':'SimpleStrategy','replication_factor':1}");
        assertThat(keyspaceNames(session), hasItem("MixedCase"));

        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();

        assertThat(keyspaceNames(session), not(hasItem("MixedCase")));
    }

    /**
     * Reads the keyspace list from system_schema rather than from the driver's metadata,
     * which by default excludes every system keyspace (refreshed-keyspaces in the driver's
     * reference.conf) - so the metadata route could not verify that they survive.
     */
    private static Set<String> keyspaceNames(CqlSession session) {
        return session.execute("SELECT keyspace_name FROM system_schema.keyspaces")
                .all().stream()
                .map(row -> row.getString("keyspace_name"))
                .collect(Collectors.toSet());
    }
}
