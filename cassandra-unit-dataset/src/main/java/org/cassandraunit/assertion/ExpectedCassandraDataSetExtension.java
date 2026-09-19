package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs {@link ExpectedCassandraDataSet} against a session you supply.
 * <p>
 * The counterpart to {@code CassandraUnitExtension}, which does the same for the embedded server.
 * Pair it with {@code CqlDataSetExtension} when the Cassandra is yours:
 *
 * <pre>
 * &#64;RegisterExtension
 * static final CqlDataSetExtension fixtures = CqlDataSetExtension.using(...).build();
 *
 * &#64;RegisterExtension
 * final ExpectedCassandraDataSetExtension expectations =
 *         new ExpectedCassandraDataSetExtension(fixtures::getSession);
 * </pre>
 *
 * The supplier is explicit rather than discovered: there is no reliable way to find "the session"
 * in a test class, and guessing wrong would assert against the wrong database.
 *
 * @author Jeremy Sevellec
 */
public class ExpectedCassandraDataSetExtension implements AfterEachCallback {

    private final Supplier<CqlSession> session;

    public ExpectedCassandraDataSetExtension(Supplier<CqlSession> session) {
        if (session == null) {
            throw new IllegalArgumentException("session supplier must not be null");
        }
        this.session = session;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Optional<ExpectedCassandraDataSet> annotation = annotationFor(context);
        if (annotation.isEmpty()) {
            return;
        }
        ExpectedDataSetVerifier.verifyUnlessFailed(
                session.get(), annotation.get(), context.getExecutionException());
    }

    /**
     * The annotation governing this test: the method's if it has one, otherwise the class's, so a
     * class-level default can be overridden per test.
     * <p>
     * Public because {@code CassandraUnitExtension} performs the same lookup and the rule must not
     * be implemented twice. Anyone able to call this already has JUnit 5 on the classpath.
     */
    public static Optional<ExpectedCassandraDataSet> annotationFor(ExtensionContext context) {
        Optional<ExpectedCassandraDataSet> onMethod = context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, ExpectedCassandraDataSet.class));
        if (onMethod.isPresent()) {
            return onMethod;
        }
        return context.getTestClass()
                .flatMap(type -> AnnotationSupport.findAnnotation(type, ExpectedCassandraDataSet.class));
    }
}
