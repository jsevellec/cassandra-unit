package org.cassandraunit.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.assertion.ExpectedCassandraDataSet;
import org.cassandraunit.assertion.ExpectedDataSetVerifier;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ExpectedCassandraDataSet} through the Spring listener.
 * <p>
 * The annotation is the one from the core module, not a Spring copy - three copies of seven
 * attributes would drift apart.
 *
 * @author Jeremy Sevellec
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(value = {"classpath:/default-context.xml"})
@TestExecutionListeners({CassandraUnitTestExecutionListener.class})
@CassandraDataSet(value = {"cql/widgetSchema.cql", "rows/widgets.yaml"})
@EmbeddedCassandra
public class CassandraExpectedDataSetAnnotationTest {

    /**
     * This passing is the proof that verification runs <em>before</em> {@code cleanServer()}.
     * <p>
     * That listener's {@code afterTestMethod} drops every non-system keyspace. Were the expectation
     * checked afterwards, the widget table would not exist and strict mode would report its one row
     * as missing - so this test would fail. It does not, so the order is right.
     */
    @Test
    @ExpectedCassandraDataSet("rows/widgets.yaml")
    public void should_verify_the_expectation_before_the_keyspace_is_dropped() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        assertThat(session.execute("select count(*) from widget").one().getLong(0)).isEqualTo(1);
    }

    /**
     * And that a mismatch is actually detected, rather than the annotation being read and quietly
     * ignored. Invoked directly: a listener failing a test is not something the test itself can
     * catch, so the shared entry point is called with the same annotation instance the listener
     * would have found.
     */
    @Test
    public void should_fail_when_the_data_does_not_match() throws Exception {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();
        session.execute("update widget set label = 'changed'"
                + " where id = 1690e8da-5bf8-49e8-9583-4dff8a570c01");

        Method annotated = getClass()
                .getMethod("should_verify_the_expectation_before_the_keyspace_is_dropped");
        ExpectedCassandraDataSet expected = annotated.getAnnotation(ExpectedCassandraDataSet.class);

        assertThatThrownBy(() -> ExpectedDataSetVerifier.verify(session, expected))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("changed");
    }
}
