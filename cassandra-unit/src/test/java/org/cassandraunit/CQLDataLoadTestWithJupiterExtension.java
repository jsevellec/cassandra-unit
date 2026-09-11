package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JUnit 5 counterpart of {@link CQLDataLoadTestWithJunitRule}, asserting the same things
 * through {@link CassandraUnitExtension} so that both integrations stay honest.
 */
class CQLDataLoadTestWithJupiterExtension {

    @RegisterExtension
    static CassandraUnitExtension cassandra =
            new CassandraUnitExtension(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));

    @Test
    void shouldResolveTheSessionAsAParameter(CqlSession session) {
        assertThat(session).as("the extension should resolve a CqlSession parameter").isNotNull();
        assertThat(valueOfTestRow(session)).isEqualTo("Cql loaded string");
    }

    @Test
    void shouldAlsoExposeTheSessionOnTheExtension() {
        assertThat(valueOfTestRow(cassandra.getSession())).isEqualTo("Cql loaded string");
    }

    /** Reloading the dataset before each test must leave the keyspace usable, not half-dropped. */
    @Test
    void shouldReloadTheDatasetForEachTest() {
        assertThat(valueOfTestRow(cassandra.getSession())).isEqualTo("Cql loaded string");
    }

    private static String valueOfTestRow(CqlSession session) {
        ResultSet result = session.execute(
                "select * from testCQLTable WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570737");
        return result.iterator().next().getString("value");
    }
}
