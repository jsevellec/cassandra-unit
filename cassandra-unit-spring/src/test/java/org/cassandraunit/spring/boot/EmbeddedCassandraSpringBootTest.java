package org.cassandraunit.spring.boot;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The claim this module could not previously make: {@code @SpringBootTest} plus
 * {@code @EmbeddedCassandra}, and the application's own auto-configured {@code CqlSession} reaches
 * the embedded node. No {@code @DynamicPropertySource}, no contact points in a properties file,
 * nothing wired by the test.
 * <p>
 * What makes it work is {@code EmbeddedCassandraContextCustomizer}, which starts the node and
 * publishes its address before the context refreshes. Without it Boot's
 * {@code CassandraAutoConfiguration} would build a session against its own default of 9042 and the
 * embedded node would be sitting on 9142, unreachable.
 * <p>
 * This test is also the only thing in the build that proves Boot actually <em>binds</em> the
 * property names the customizer publishes. Everything else asserts what we put into the
 * {@code Environment}; this asserts what Boot did with it.
 *
 * @author Jeremy Sevellec
 */
@SpringBootTest(classes = EmbeddedCassandraSpringBootTest.BootApplication.class)
@TestExecutionListeners(value = {CassandraUnitTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@CassandraDataSet(value = {"cql/widgetSchema.cql", "rows/widgets.yaml"})
@EmbeddedCassandra
public class EmbeddedCassandraSpringBootTest {

    /**
     * Autowired, not built here. If this is injected at all, Boot's auto-configuration found a
     * reachable node - a session pointed at the wrong port fails when the context refreshes, so
     * the injection itself carries most of the assertion.
     */
    @Autowired
    private CqlSession bootSession;

    @Autowired
    private Environment environment;

    /**
     * The port Boot connected on is the embedded node's, not the driver default. Asserting it is
     * not 9042 is the part that would have failed before the customizer existed.
     */
    @Test
    public void should_autoconfigure_against_the_embedded_node() {
        int embeddedPort = EmbeddedCassandraServerHelper.getNativeTransportPort();

        assertThat(environment.getProperty("spring.cassandra.port", Integer.class))
                .isEqualTo(embeddedPort)
                .isNotEqualTo(9042);
        assertThat(environment.getProperty("spring.cassandra.contact-points"))
                .isEqualTo(EmbeddedCassandraServerHelper.getHost());
    }

    /**
     * And the session Boot built really is talking to the node the dataset was loaded into. The
     * dataset goes in through the listener, which uses the embedded helper's own session; reading
     * it back through Boot's separate session is what proves both point at the same server.
     * <p>
     * The table is qualified because the listener's {@code USE} statement applied to the helper's
     * session, not to this one.
     */
    @Test
    public void should_read_the_fixture_through_the_boot_session() {
        long rows = bootSession
                .execute("select count(*) from cassandra_unit_keyspace.widget")
                .one().getLong(0);

        assertThat(rows).isEqualTo(1);
    }

    @SpringBootApplication
    static class BootApplication {
    }
}
