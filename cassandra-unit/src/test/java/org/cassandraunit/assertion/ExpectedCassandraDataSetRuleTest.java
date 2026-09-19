package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JUnit 4 rule, on the vintage engine.
 * <p>
 * A separate rule rather than a change to {@code CassandraCQLUnit}, because that extends
 * {@code ExternalResource}, which is handed no {@code Description} and so cannot see a method
 * annotation. Chained with {@code RuleChain} so the server is up before the expectation runs.
 */
public class ExpectedCassandraDataSetRuleTest {

    private static final String KEYSPACE = "assertrulekeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    private final CassandraCQLUnit cassandra = new CassandraCQLUnit(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @Rule
    public RuleChain rules = RuleChain.outerRule(cassandra)
            .around(new ExpectedCassandraDataSetRule(EmbeddedCassandraServerHelper::getSession));

    @Before
    public void loadRows() {
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession())
                .load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    @Test
    @ExpectedCassandraDataSet(value = DATA, keyspace = KEYSPACE)
    public void shouldVerifyTheExpectationAfterTheTest() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();

        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isEqualTo(3);
    }

    @Test
    public void anUnannotatedTestShouldBeLeftAlone() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();
        session.execute("TRUNCATE " + KEYSPACE + ".widget");

        assertThat(session.execute("SELECT count(*) FROM " + KEYSPACE + ".widget").one().getLong(0))
                .isZero();
    }
}
