package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.CQLDataSet;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Loads datasets into a Cassandra you already have.
 * <p>
 * The counterpart to {@code CassandraUnitExtension}, which starts an embedded server. This one
 * starts nothing: you hand it a {@link CqlSession} and it loads fixtures through that. So it works
 * against a Testcontainers container, a local node, ScyllaDB, Astra - anything speaking CQL - and
 * it needs no {@code cassandra-all}, no jamm agent, none of the JPMS flags an embedded daemon
 * requires, and it has no JDK ceiling.
 *
 * <pre>
 * &#64;Testcontainers
 * class WidgetIT {
 *
 *     &#64;Container
 *     static final CassandraContainer cassandra =
 *             new CassandraContainer("cassandra:5.0").withReuse(true);
 *
 *     &#64;RegisterExtension
 *     static final CqlDataSetExtension fixtures = CqlDataSetExtension
 *             .using(() -&gt; CqlSession.builder()
 *                     .addContactPoint(cassandra.getContactPoint())
 *                     .withLocalDatacenter(cassandra.getLocalDatacenter())
 *                     .build())
 *             .closingSession()
 *             .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
 *             .rowsPerTest(CQLDataSetFactory.fromClassPath("data/widget.yaml", false, false, "mykeyspace"))
 *             .build();
 *
 *     &#64;Test
 *     void readsTheFixture(CqlSession session) {    // resolved by this extension
 *         ...
 *     }
 * }
 * </pre>
 *
 * <b>The supplier has to build the session itself, and is called lazily.</b> Jupiter runs
 * {@code beforeAll} callbacks registered declaratively - which is how {@code @Testcontainers}
 * registers - before those registered with {@code @RegisterExtension}, and runs {@code @BeforeAll}
 * <em>methods</em> after every callback. So a {@code static CqlSession} field populated in a
 * {@code @BeforeAll} method is still null when this extension starts. Building it in the supplier
 * sidesteps the ordering entirely.
 * <p>
 * The session is built once, on first use. This extension <b>never closes a session it did not
 * create</b>; {@link Builder#closingSession()} opts in to closing it in {@code afterAll}.
 *
 * @author Jeremy Sevellec
 */
public final class CqlDataSetExtension
        implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, ParameterResolver {

    private final Function<ExtensionContext, CqlSession> sessionFn;
    private final CQLDataSet schemaOnce;
    private final CQLDataSet rowsPerTest;
    private final boolean closeSession;

    private volatile CqlSession session;

    private CqlDataSetExtension(Builder builder) {
        this.sessionFn = builder.sessionFn;
        this.schemaOnce = builder.schemaOnce;
        this.rowsPerTest = builder.rowsPerTest;
        this.closeSession = builder.closeSession;
    }

    public static Builder using(Supplier<CqlSession> sessionSupplier) {
        if (sessionSupplier == null) {
            throw new IllegalArgumentException("sessionSupplier must not be null");
        }
        return using(context -> sessionSupplier.get());
    }

    /**
     * For a session that depends on the test context - a Spring bean, say, or one stored by
     * another extension.
     */
    public static Builder using(Function<ExtensionContext, CqlSession> sessionFn) {
        if (sessionFn == null) {
            throw new IllegalArgumentException("sessionFn must not be null");
        }
        return new Builder(sessionFn);
    }

    public static final class Builder {

        private final Function<ExtensionContext, CqlSession> sessionFn;
        private CQLDataSet schemaOnce;
        private CQLDataSet rowsPerTest;
        private boolean closeSession;

        private Builder(Function<ExtensionContext, CqlSession> sessionFn) {
            this.sessionFn = sessionFn;
        }

        /**
         * Loaded once per class, and only if its keyspace does not already exist. For schema that
         * is expensive to build and the same for every test.
         */
        public Builder schemaOnce(CQLDataSet schema) {
            this.schemaOnce = schema;
            return this;
        }

        /** Loaded before every test method. */
        public Builder rowsPerTest(CQLDataSet rows) {
            this.rowsPerTest = rows;
            return this;
        }

        /**
         * The simple case: one dataset, loaded before every test, keyspace handling left to the
         * dataset's own creation and deletion flags. Same as {@link #rowsPerTest}.
         */
        public Builder load(CQLDataSet dataSet) {
            return rowsPerTest(dataSet);
        }

        /**
         * Close the session in {@code afterAll}. Off by default: a session handed to this
         * extension may well outlive it, and closing someone else's is not ours to do.
         * <p>
         * Jupiter fires {@code afterAll} once per container, so do not turn this on for an
         * extension shared with a {@code @Nested} class - the inner class finishing would close
         * the session while the outer one still has tests to run. Register a separate extension
         * per container, or leave this off and close the session yourself.
         */
        public Builder closingSession() {
            this.closeSession = true;
            return this;
        }

        public CqlDataSetExtension build() {
            if (schemaOnce == null && rowsPerTest == null) {
                throw new IllegalArgumentException(
                        "nothing to load: call schemaOnce(...), rowsPerTest(...) or load(...)");
            }
            return new CqlDataSetExtension(this);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        CqlSession active = session(context);
        if (schemaOnce != null) {
            new CQLDataLoader(active).loadIfKeyspaceAbsent(schemaOnce);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (rowsPerTest != null) {
            new CQLDataLoader(session(context)).load(rowsPerTest);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        CqlSession active = session;
        session = null;
        if (closeSession && active != null) {
            active.close();
        }
    }

    /**
     * The session in use. Only available once the extension has started; prefer declaring a
     * {@link CqlSession} parameter on the test method, which this extension resolves.
     */
    public CqlSession getSession() {
        CqlSession active = session;
        if (active == null) {
            throw new IllegalStateException("no session yet - CqlDataSetExtension builds it in "
                    + "beforeAll, so this is only available from a test or a lifecycle method that "
                    + "runs after it");
        }
        return active;
    }

    private CqlSession session(ExtensionContext context) {
        CqlSession active = session;
        if (active == null) {
            synchronized (this) {
                active = session;
                if (active == null) {
                    active = sessionFn.apply(context);
                    if (active == null) {
                        throw new IllegalStateException("the session supplier returned null");
                    }
                    session = active;
                }
            }
        }
        return active;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return CqlSession.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return session(extensionContext);
    }
}
