package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.function.Supplier;

/**
 * JUnit 4 equivalent of {@link ExpectedCassandraDataSetExtension}.
 *
 * <pre>
 * public class WidgetTest {
 *
 *     public CassandraCQLUnit cassandra = new CassandraCQLUnit(...);
 *
 *     &#64;Rule
 *     public RuleChain rules = RuleChain.outerRule(cassandra)
 *             .around(new ExpectedCassandraDataSetRule(cassandra::getSession));
 *
 *     &#64;Test
 *     &#64;ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace")
 *     public void shipping_marks_it_dispatched() { ... }
 * }
 * </pre>
 *
 * A separate rule rather than a change to {@code CassandraCQLUnit}, because that extends
 * {@code ExternalResource}, whose {@code before}/{@code after} are handed no {@link Description} and
 * so cannot see a method annotation. A {@link TestRule} is.
 *
 * @author Jeremy Sevellec
 */
public class ExpectedCassandraDataSetRule implements TestRule {

    private final Supplier<CqlSession> session;

    public ExpectedCassandraDataSetRule(Supplier<CqlSession> session) {
        if (session == null) {
            throw new IllegalArgumentException("session supplier must not be null");
        }
        this.session = session;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        ExpectedCassandraDataSet annotation = description.getAnnotation(ExpectedCassandraDataSet.class);
        if (annotation == null) {
            annotation = description.getTestClass() == null
                    ? null
                    : description.getTestClass().getAnnotation(ExpectedCassandraDataSet.class);
        }
        if (annotation == null) {
            return base;
        }
        ExpectedCassandraDataSet expected = annotation;
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // No try/finally: if the test throws, that exception propagates and the
                // expectation never runs. Verifying after a failure would bury the real error
                // under a mismatch report for state the test never reached.
                base.evaluate();
                ExpectedDataSetVerifier.verify(session.get(), expected);
            }
        };
    }
}
