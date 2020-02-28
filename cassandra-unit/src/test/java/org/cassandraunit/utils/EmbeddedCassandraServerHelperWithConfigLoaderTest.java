package org.cassandraunit.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfig;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

/**
 * UnitTest for EmbeddedCassandra with DriverConfigLoader. Because Cassandra basically can only be started once per JVM, this test is
 * disabled, and should be manually enabled for single tests only. (CassandraDaemon#deactivate is a bad joke. There may be some
 * workaround with surefire-fork or classloaders or whatever, but one shouldnt invest too much in a workaround for a broken
 * external functionality)
 *
 * @author Tetsuya Morimoto
 */
@Ignore("Cassandra can only be started once. If you want to run this test, then enable it and run only this test")
public class EmbeddedCassandraServerHelperWithConfigLoaderTest {

    private static final String CONF_PATH = "src/test/resources/driver-application.conf";

    @Test
    public void shouldStartupWithApplicationConfig() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        Path absolutePath = Paths.get(CONF_PATH).toAbsolutePath();
        try (CqlSession session = EmbeddedCassandraServerHelper.getSession(absolutePath)) {
            assertThat(session.getMetadata().getNodes().size(), is(1));
            KeyspaceMetadata system = session.getMetadata().getKeyspace("system").get();
            assertThat(system.getTables().size(), not(0));

            DriverConfig config = session.getContext().getConfig();
            DriverExecutionProfile profile = config.getDefaultProfile();
            assertEquals(1234, profile.getInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS));
        }
    }

    @Test
    public void shouldClean() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        assertTrue(true);
    }
}
