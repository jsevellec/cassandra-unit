package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.cassandraunit.assertion.ExpectedCassandraDataSet;
import org.cassandraunit.assertion.ExpectedCassandraDataSetExtension;
import org.cassandraunit.assertion.ExpectedDataSetVerifier;
import org.junit.jupiter.api.extension.AfterEachCallback;
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
 * <p>
 * A test method carrying {@link ExpectedCassandraDataSet} also has its expectation verified
 * afterwards, and only if it passed. Without the annotation nothing extra happens.
 */
public class CassandraUnitExtension
        implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private final CQLDataSet dataSet;
    private final String configurationFileName;
    private final long startupTimeoutMillis;
    private Duration requestTimeout;
    private CQLDataLoader.Isolation isolation = CQLDataLoader.Isolation.DATASET;

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

    /**
     * How each per-test load clears the previous test's data. Defaults to
     * {@link CQLDataLoader.Isolation#DATASET}, which is what this extension has always done.
     * <p>
     * {@link CQLDataLoader.Isolation#TRUNCATE} keeps the keyspace and empties its tables, so the
     * dataset given to this extension must then be rows only - see that enum constant.
     */
    public CassandraUnitExtension withIsolation(CQLDataLoader.Isolation isolation) {
        if (isolation == null) {
            throw new IllegalArgumentException("isolation must not be null");
        }
        this.isolation = isolation;
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
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession()).load(dataSet, isolation);
    }

    /**
     * Verifies an {@link ExpectedCassandraDataSet} on the test, if there is one.
     * <p>
     * Nothing happens without the annotation, so this changes no existing behaviour - in
     * particular it is still true that this extension never wipes anything for you.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        ExpectedCassandraDataSetExtension.annotationFor(context).ifPresent(annotation ->
                ExpectedDataSetVerifier.verifyUnlessFailed(
                        getSession(), annotation, context.getExecutionException()));
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
