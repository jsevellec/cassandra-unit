package org.cassandraunit.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.CqlDataSetExtension;
import org.cassandraunit.SpringSessions;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CqlDataSetExtension} loading through a {@link CqlSession} that <em>Spring</em> owns.
 * <p>
 * This is the shape a Spring Boot test has, minus Boot: the application context builds the session
 * from {@code spring.cassandra.*}, and the fixtures go into that session rather than into one the
 * extension built for itself. {@link SpringSessions#fromApplicationContext()} is what connects the
 * two, and nothing here touches {@code CassandraUnitTestExecutionListener} or
 * {@code @CassandraDataSet} - this is the other Spring path.
 * <p>
 * The embedded server stands in for a Testcontainers container. From the extension's side the two
 * are indistinguishable; what is being tested is that the session came out of the
 * {@code ApplicationContext}.
 * <p>
 * Note there is no {@code closingSession()}. The session is a Spring bean, so Spring closes it when
 * the context shuts down - see {@link SpringSessions} for why closing it here would be wrong twice
 * over.
 *
 * @author Jeremy Sevellec
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CqlDataSetExtensionWithSpringContextTest.Config.class)
public class CqlDataSetExtensionWithSpringContextTest {

    private static final String KEYSPACE = "springcontextkeyspace";

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(SpringSessions.fromApplicationContext())
            .schemaOnce(new ClassPathCQLDataSet("cql/widgetSchema.cql", KEYSPACE))
            .rowsPerTest(CQLDataSetFactory.fromClassPath("rows/widgets.yaml", false, false, KEYSPACE))
            .build();

    @Autowired
    private CqlSession autowiredSession;

    /**
     * Starts the node and publishes its address, the way a Testcontainers user publishes a
     * container's. The server has to be started here rather than in a {@code @BeforeAll} method:
     * this runs while the context is being built, which is before any test callback.
     */
    @DynamicPropertySource
    static void embeddedCassandra(DynamicPropertyRegistry registry) throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        registry.add("spring.cassandra.contact-points", EmbeddedCassandraServerHelper::getHost);
        registry.add("spring.cassandra.port", EmbeddedCassandraServerHelper::getNativeTransportPort);
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
    }

    /**
     * The fixture is readable through the context's own bean - so the rows really did go into
     * Spring's session and not a second one.
     */
    @Test
    public void should_load_rows_into_the_context_session() {
        Row row = autowiredSession.execute(
                "select * from " + KEYSPACE + ".widget"
                        + " where id = 1690e8da-5bf8-49e8-9583-4dff8a570c01").one();

        assertThat(row).isNotNull();
        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
    }

    /**
     * And that it is the <em>same</em> session object, not merely another one pointed at the same
     * node. A second session would still see the rows, so identity is the assertion that
     * discriminates.
     */
    @Test
    public void should_resolve_the_bean_itself_rather_than_build_a_session() {
        assertThat(fixtures.getSession()).isSameAs(autowiredSession);
    }

    /**
     * A bare {@code CqlSession} parameter resolves to that same bean.
     * <p>
     * Deliberately not annotated: {@code SpringExtension} claims a parameter only when it carries
     * {@code @Autowired}, {@code @Qualifier} or {@code @Value}, so a bare one belongs to this
     * extension alone. Annotating it would make both resolvers claim it and Jupiter would fail the
     * test with "Discovered multiple competing ParameterResolvers".
     */
    @Test
    public void should_resolve_a_bare_session_parameter(CqlSession session) {
        assertThat(session).isSameAs(autowiredSession);
    }

    /**
     * The published properties are the node's actual address. Compared against the helper rather
     * than against {@code "localhost"}: {@code getHost()} is a reverse lookup on the rpc address
     * and its result varies by machine.
     */
    @Test
    public void should_publish_the_real_address(@Autowired Environment environment) {
        assertThat(environment.getProperty("spring.cassandra.contact-points"))
                .isEqualTo(EmbeddedCassandraServerHelper.getHost());
        assertThat(environment.getProperty("spring.cassandra.port", Integer.class))
                .isEqualTo(EmbeddedCassandraServerHelper.getNativeTransportPort());
    }

    /**
     * Builds the session the way Spring Data Cassandra would, from the environment. Spring closes
     * it at context shutdown, because {@code CqlSession} is {@code AutoCloseable} and that is the
     * destroy method Spring infers.
     */
    @Configuration(proxyBeanMethods = false)
    static class Config {

        @Bean
        CqlSession cqlSession(Environment environment) {
            return CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(
                            environment.getRequiredProperty("spring.cassandra.contact-points"),
                            environment.getRequiredProperty("spring.cassandra.port", Integer.class)))
                    .withLocalDatacenter(
                            environment.getRequiredProperty("spring.cassandra.local-datacenter"))
                    .build();
        }
    }
}
