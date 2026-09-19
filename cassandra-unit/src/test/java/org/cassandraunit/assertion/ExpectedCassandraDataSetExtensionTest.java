package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CqlDataSetExtension;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CqlDataSetExtension} and {@link ExpectedCassandraDataSetExtension} side by side - load
 * with one, assert with the other, against a session neither of them created.
 * <p>
 * This pair is what {@code docs/assertions.md} leads with for anyone not on the embedded server, so
 * it is worth a test of its own rather than trusting that two separately-tested extensions compose.
 * The embedded server stands in for the container, exactly as in {@code CqlDataSetExtensionTest};
 * the session is built inside the supplier for the ordering reason documented there.
 */
class ExpectedCassandraDataSetExtensionTest {

    private static final String KEYSPACE = "assertbyosessionkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(ExpectedCassandraDataSetExtensionTest::embeddedSession)
            .schemaOnce(new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE))
            .rowsPerTest(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE))
            .build();

    /** Non-static, as documented: it needs nothing before the test and the session is memoized. */
    @RegisterExtension
    final ExpectedCassandraDataSetExtension expectations =
            new ExpectedCassandraDataSetExtension(fixtures::getSession);

    private static CqlSession embeddedSession() {
        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        } catch (Exception e) {
            throw new IllegalStateException("could not start the embedded Cassandra", e);
        }
        return EmbeddedCassandraServerHelper.getSession();
    }

    /** Passes only if the standalone extension picked the annotation up and verified it. */
    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE)
    void theAnnotationShouldBeVerifiedAgainstTheSuppliedSession(CqlSession session) {
        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isEqualTo(3);
    }

    /**
     * And {@code rowsPerTest} really does reload between tests, which is what makes the assertion
     * above repeatable: this one empties the table and the next still finds three rows.
     */
    @Test
    void anUnannotatedTestShouldBeLeftAlone(CqlSession session) {
        session.execute("TRUNCATE " + KEYSPACE + ".widget");

        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isZero();
    }
}
