package org.cassandraunit.assertion;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The rule that an expectation does not run after the test already failed.
 * <p>
 * Worth a unit test of its own rather than leaving it to the integrations, because getting it wrong
 * is silent: the suite still goes red, just with a mismatch report where the real exception should
 * have been. Calling the shared entry point directly proves it without standing up a test engine,
 * and needs no Cassandra - a session is never touched on the declining path, which is exactly what
 * passing {@code null} here demonstrates.
 */
class ExpectedDataSetVerifierTest {

    @Test
    void shouldNotVerifyWhenTheTestAlreadyFailed() {
        assertThatCode(() -> ExpectedDataSetVerifier.verifyUnlessFailed(
                null, annotation("rows/does-not-exist.yaml"), Optional.of(new AssertionError("boom"))))
                .doesNotThrowAnyException();
    }

    /**
     * The mirror of the above: with no failure it does proceed, and here that means reaching a
     * dataset that does not exist. Reaching the file at all is the assertion.
     */
    @Test
    void shouldVerifyWhenTheTestPassed() {
        assertThatCode(() -> ExpectedDataSetVerifier.verifyUnlessFailed(
                null, annotation("rows/does-not-exist.yaml"), Optional.empty()))
                .isInstanceOf(RuntimeException.class);
    }

    private static ExpectedCassandraDataSet annotation(String location) {
        return new ExpectedCassandraDataSet() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ExpectedCassandraDataSet.class;
            }

            @Override
            public String[] value() {
                return new String[]{location};
            }

            @Override
            public String keyspace() {
                return "somekeyspace";
            }

            @Override
            public String[] ignoreColumns() {
                return new String[0];
            }

            @Override
            public MatchMode mode() {
                return MatchMode.STRICT;
            }

            @Override
            public Scope scope() {
                return Scope.TABLE;
            }

            @Override
            public boolean checkClusteringOrder() {
                return false;
            }

            @Override
            public double numericTolerance() {
                return 0d;
            }
        };
    }
}
