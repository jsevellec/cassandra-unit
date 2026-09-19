package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;

import java.util.Optional;
import java.util.Set;

/**
 * Turns an {@link ExpectedCassandraDataSet} annotation into an assertion.
 * <p>
 * One place, so JUnit 5, JUnit 4 and Spring cannot drift apart in how they read the annotation or
 * in when they decline to run it. Plain statics taking the test outcome as a parameter, so the
 * "don't verify after a failure" rule is unit-testable without standing up a test engine.
 *
 * @author Jeremy Sevellec
 */
public final class ExpectedDataSetVerifier {

    private ExpectedDataSetVerifier() {
    }

    /**
     * Verifies every dataset the annotation names.
     *
     * @throws DataSetMismatchError if the data does not match
     */
    public static void verify(CqlSession session, ExpectedCassandraDataSet annotation) {
        for (String location : annotation.value()) {
            expectedDataSet(location, annotation).verify(session);
        }
    }

    /**
     * As {@link #verify}, unless the test already failed - in which case this does nothing.
     * <p>
     * A test that threw has not reached the state its expectation describes, so the expectation
     * would fail too, and the mismatch report would be the last thing printed. The real error is
     * the one worth seeing.
     *
     * @param testFailure the exception the test threw, empty if it passed
     */
    public static void verifyUnlessFailed(CqlSession session, ExpectedCassandraDataSet annotation,
                                          Optional<Throwable> testFailure) {
        if (testFailure.isPresent()) {
            return;
        }
        verify(session, annotation);
    }

    private static ExpectedDataSet expectedDataSet(String location, ExpectedCassandraDataSet annotation) {
        String keyspace = annotation.keyspace().isEmpty() ? null : annotation.keyspace();
        return ExpectedDataSetFactory.fromClassPath(location, keyspace)
                .withOptions(new ExpectedDataSetOptions(
                        annotation.mode(),
                        annotation.scope(),
                        Set.of(annotation.ignoreColumns()),
                        annotation.checkClusteringOrder(),
                        annotation.numericTolerance()));
    }
}
