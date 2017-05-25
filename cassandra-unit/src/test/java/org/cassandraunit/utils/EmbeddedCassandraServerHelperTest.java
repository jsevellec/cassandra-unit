package org.cassandraunit.utils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Session;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * UnitTest for EmbeddedCassandra with random port. Because Cassandra basically can only be started once per JVM, this test is
 * disabled, and should be manually enabled for single tests only. (CassandraDaemon#deactivate is a bad joke. There may be some
 * workaround with surefire-fork or classloaders or whatever, but one shouldnt invest too much in a workaround for a broken
 * external functionality)
 * 
 * @author Markus Kull
 */
@Ignore("Cassandra can only be started once. If you want to run this test, then enable it and run only this test")
public class EmbeddedCassandraServerHelperTest {

    @Test
    public void shouldStartupOnRandomFreePort() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
        int nativePort = EmbeddedCassandraServerHelper.getNativeTransportPort();
        assertThat(nativePort > 0, is(true));
        testIfTheEmbeddedCassandraServerIsUpOnHost("127.0.0.1", nativePort);
    }

    private void testIfTheEmbeddedCassandraServerIsUpOnHost(String host, int port) {
        try (Cluster cluster = Cluster.builder()
                .addContactPoints(host)
                .withPort(port)
                .build()) {
            Session session = cluster.connect();

            assertThat(session.getState().getConnectedHosts().size(), is(1));
            KeyspaceMetadata system = session.getCluster().getMetadata().getKeyspace("system");
            assertThat(system.getTables().size(), not(0));
        }
    }
}
