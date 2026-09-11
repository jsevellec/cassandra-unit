package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertNotNull(session, "the extension should resolve a CqlSession parameter");
        assertEquals("Cql loaded string", valueOfTestRow(session));
    }

    @Test
    void shouldAlsoExposeTheSessionOnTheExtension() {
        assertEquals("Cql loaded string", valueOfTestRow(cassandra.getSession()));
    }

    /** Reloading the dataset before each test must leave the keyspace usable, not half-dropped. */
    @Test
    void shouldReloadTheDatasetForEachTest() {
        assertEquals("Cql loaded string", valueOfTestRow(cassandra.getSession()));
    }

    private static String valueOfTestRow(CqlSession session) {
        ResultSet result = session.execute(
                "select * from testCQLTable WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570737");
        return result.iterator().next().getString("value");
    }
}
