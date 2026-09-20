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
 * The case that cannot be solved by writing the port down: {@code cu-cassandra-rndport.yaml} asks
 * the OS for a free port at startup, so nothing knows it until the node is running.
 * <p>
 * This is the strongest argument for publishing the address from a context customizer rather than
 * documenting a properties file. A Boot test against a random-port embedded node is impossible to
 * configure by hand, and here it takes no configuration at all.
 * <p>
 * <b>Runs in a fork of its own</b>, via the {@code isolated-config-tests} surefire execution. A JVM
 * is pinned to the first Cassandra configuration it starts - {@code checkConfigNameForRestart}
 * throws on a second one - so this cannot share the fork with the tests that use the default yaml.
 *
 * @author Jeremy Sevellec
 */
@SpringBootTest(classes = EmbeddedCassandraRandomPortSpringBootTest.BootApplication.class)
@TestExecutionListeners(value = {CassandraUnitTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@CassandraDataSet(value = {"cql/widgetSchema.cql", "rows/widgets.yaml"})
@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
public class EmbeddedCassandraRandomPortSpringBootTest {

    @Autowired
    private CqlSession bootSession;

    @Autowired
    private Environment environment;

    @Test
    public void should_publish_a_port_nobody_could_have_written_down() {
        int actual = EmbeddedCassandraServerHelper.getNativeTransportPort();

        assertThat(environment.getProperty("spring.cassandra.port", Integer.class))
                .isEqualTo(actual);
        assertThat(actual)
                .as("cu-cassandra-rndport.yaml asks for a free port, so it should be neither the "
                        + "driver default nor the fixed embedded one")
                .isNotEqualTo(9042)
                .isNotEqualTo(9142);
    }

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
