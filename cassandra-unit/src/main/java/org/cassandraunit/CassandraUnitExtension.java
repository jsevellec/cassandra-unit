package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.time.Duration;

/**
 * JUnit 5 equivalent of the {@link CassandraCQLUnit} rule.
 * <p>
 * Register it with {@code @RegisterExtension}, which keeps the dataset visible at the call
 * site the way the rule did:
 *
 * <pre>
 * class MyTest {
 *
 *     &#64;RegisterExtension
 *     static CassandraUnitExtension cassandra =
 *             new CassandraUnitExtension(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));
 *
 *     &#64;Test
 *     void queries(CqlSession session) {
 *         session.execute("select * from my_table");
 *     }
 * }
 * </pre>
 *
 * A {@link CqlSession} parameter on a test method, constructor or lifecycle method is
 * resolved automatically, so {@link #getSession()} is only needed when a field is preferred.
 * <p>
 * Semantics match the rule exactly. Cassandra starts once per JVM - a permanent invariant,
 * since Cassandra's static state cannot be reset in-process - and the dataset is loaded before
 * each test, with the dataset's own keyspace creation and deletion flags deciding how much is
 * torn down in between. There is deliberately no automatic wipe after each test; call
 * {@link EmbeddedCassandraServerHelper#cleanEmbeddedCassandra()} if that is what you want.
 * <p>
 * This is the entry point for Jupiter-based suites, including Spring Boot 3+, which previously
 * had none (issue #293).
 */
public class CassandraUnitExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private final CQLDataSet dataSet;
    private final String configurationFileName;
    private final long startupTimeoutMillis;
    private Duration requestTimeout;

    public CassandraUnitExtension(CQLDataSet dataSet) {
        this(dataSet, null, EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT);
    }

    public CassandraUnitExtension(CQLDataSet dataSet, String configurationFileName) {
        this(dataSet, configurationFileName, EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT);
    }

    public CassandraUnitExtension(CQLDataSet dataSet, String configurationFileName, long startupTimeoutMillis) {
        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet must not be null");
        }
        this.dataSet = dataSet;
        this.configurationFileName = configurationFileName;
        this.startupTimeoutMillis = startupTimeoutMillis;
    }

    /**
     * Sets the request timeout on the shared session. Only effective before the session is
     * first created, because there is a single session per JVM.
     */
    public CassandraUnitExtension withRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (configurationFileName != null) {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(configurationFileName, startupTimeoutMillis);
        } else {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(startupTimeoutMillis);
        }
        if (requestTimeout != null) {
            EmbeddedCassandraServerHelper.setRequestTimeout(requestTimeout);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession()).load(dataSet);
    }

    public CqlSession getSession() {
        return EmbeddedCassandraServerHelper.getSession();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return CqlSession.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getSession();
    }
}
