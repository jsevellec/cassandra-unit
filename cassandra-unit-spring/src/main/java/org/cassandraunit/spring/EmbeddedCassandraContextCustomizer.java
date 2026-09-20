package org.cassandraunit.spring;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Starts the embedded node and puts its address into the test's {@code Environment}, so beans built
 * during the refresh that follows can connect to it.
 * <p>
 * Created only for a class carrying {@link EmbeddedCassandra} - see
 * {@link EmbeddedCassandraContextCustomizerFactory}, which is the class that is always loaded and
 * which deliberately names nothing from {@code cassandra-unit}.
 *
 * <h2>Why this runs early enough</h2>
 * {@code customizeContext} is called after the bean definitions are loaded and <b>before</b>
 * {@code refresh()} - Spring Boot's loader applies customizers as {@code ApplicationContextInitializer}s,
 * which is the same point. So the node is listening and the properties are readable before the
 * first bean is created. That ordering is the whole reason this is a customizer rather than a
 * listener callback, and it is the only way {@code cu-cassandra-rndport.yaml} can work at all: the
 * port does not exist until the node has started, so it cannot be written in a properties file.
 *
 * <h2>Why equals and hashCode are not boilerplate</h2>
 * Spring folds context customizers into {@link MergedContextConfiguration}, which is the key of the
 * context cache. If two instances created for equivalent {@code @EmbeddedCassandra} attributes did
 * not compare equal, every annotated test class would get a context of its own and the cache would
 * stop working. Nothing would fail - the suite would just get slower, which is why
 * {@code EmbeddedCassandraContextReuseTest} asserts the sharing rather than trusting it.
 *
 * @author Jeremy Sevellec
 */
class EmbeddedCassandraContextCustomizer implements ContextCustomizer {

    static final String PROPERTY_SOURCE_NAME = "cassandra-unit-embedded";

    /**
     * The datacenter name the embedded node reports, mirroring the session
     * {@code EmbeddedCassandraServerHelper} builds for itself. A custom yaml that changes the snitch
     * changes this, and then it is yours to override.
     */
    static final String LOCAL_DATACENTER = "datacenter1";

    private final String configuration;
    private final String tmpDir;
    private final long timeout;

    EmbeddedCassandraContextCustomizer(String configuration, String tmpDir, long timeout) {
        this.configuration = configuration;
        this.tmpDir = tmpDir;
        this.timeout = timeout;
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context,
                                 MergedContextConfiguration mergedConfig) {
        // An empty tmpDir means "use the library default"; see @EmbeddedCassandra#tmpDir.
        String directory = tmpDir.isEmpty() ? EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR : tmpDir;
        try {
            // Safe to call even though a listener may also call it: startEmbeddedCassandra returns
            // immediately when the daemon already exists, and that check comes *before* it deletes
            // tmpDir. Were the order the other way round, a second call would wipe the data
            // directory of a running node.
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(configuration, directory, timeout);
        } catch (Exception e) {
            // Broad on purpose. Besides the declared IOException and ConfigurationException, this
            // is where "We can't launch two Cassandra configurations in the same JVM instance"
            // surfaces when a test asks for a different yaml than the fork already started. That
            // one is worth catching: the message does not say *which* class asked, and naming it
            // is the difference between a puzzle and a one-line fix (give it its own fork).
            throw new IllegalStateException("could not start the embedded Cassandra for "
                    + mergedConfig.getTestClass().getName(), e);
        }

        // Deliberately not spring.cassandra.keyspace-name: Boot would build the session with
        // withKeyspace(...) during the refresh, before any dataset has had the chance to create
        // that keyspace, and the context would fail to start.
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.cassandra.contact-points", EmbeddedCassandraServerHelper.getHost());
        properties.put("spring.cassandra.port", EmbeddedCassandraServerHelper.getNativeTransportPort());
        properties.put("spring.cassandra.local-datacenter", LOCAL_DATACENTER);

        // First, so the running node wins over a stale literal in an application-test.yml. Opt out
        // with @EmbeddedCassandra(exposeProperties = false).
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmbeddedCassandraContextCustomizer other = (EmbeddedCassandraContextCustomizer) o;
        return timeout == other.timeout
                && configuration.equals(other.configuration)
                && tmpDir.equals(other.tmpDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, tmpDir, timeout);
    }

    @Override
    public String toString() {
        return "EmbeddedCassandraContextCustomizer[configuration=" + configuration
                + ", tmpDir=" + tmpDir + ", timeout=" + timeout + "]";
    }
}
