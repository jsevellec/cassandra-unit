package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A broken expectation is a broken <em>test</em>, and must be reported as an error rather than a
 * failure.
 * <p>
 * The distinction is the reason {@link DataSetMismatchError} extends {@link AssertionError} and
 * these throw {@link ParseException}: a failure says the code under test is wrong, an error says
 * the test is. Getting that backwards sends someone hunting through production code for a typo in
 * a fixture.
 */
class ExpectedDataSetErrorsTest {

    private static final String KEYSPACE = "asserterrorskeyspace";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @Test
    void aCqlScriptShouldBeRefusedAsAnExpectation() {
        assertThatThrownBy(() -> ExpectedDataSetFactory.fromClassPath("cql/simple.cql", KEYSPACE))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("is a CQL script, not a row dataset")
                .hasMessageContaining(".yaml");
    }

    @Test
    void aColumnThatDoesNotExistShouldListTheOnesThatDo(CqlSession session) {
        assertThatThrownBy(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-unknown-column.yaml", KEYSPACE).verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("has no column 'nosuchcolumn'")
                .hasMessageContaining("label");
    }

    @Test
    void aRowMissingPartOfItsPrimaryKeyShouldSayWhichPart(CqlSession session) {
        assertThatThrownBy(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-missing-key.yaml", KEYSPACE).verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("does not set at")
                .hasMessageContaining("full primary key (day, at)");
    }

    @Test
    void ignoringAPrimaryKeyColumnShouldBeRefused(CqlSession session) {
        assertThatThrownBy(() -> ExpectedDataSetFactory
                .fromClassPath("rows/assertion-data.yaml", KEYSPACE)
                .ignoringColumns("id")
                .verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot ignore column 'id'")
                .hasMessageContaining("part of the primary key");
    }

    /**
     * Both scopes, because they fail differently on their own: in {@code TABLE} the row simply
     * matches nothing and reports as missing plus unexpected, while {@code MENTIONED_PARTITIONS}
     * binds it into a {@code WHERE} and the driver answers {@code InvalidQueryException}. Neither
     * tells the reader their file is wrong, which is what this is.
     */
    @Test
    void aNullPrimaryKeyColumnShouldBeRefusedInEitherScope(CqlSession session) {
        ExpectedDataSet expected =
                ExpectedDataSetFactory.fromClassPath("rows/expected-null-key.yaml", KEYSPACE);

        assertThatThrownBy(() -> expected.verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("sets day to null")
                .hasMessageContaining("No primary-key column can be null");

        assertThatThrownBy(() -> expected.withinMentionedPartitions().verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("sets day to null");
    }

    @Test
    void anUnknownTableShouldNameIt(CqlSession session) {
        assertThatThrownBy(() -> ExpectedDataSetFactory
                .fromClassPath("rows/expected-unknown-table.yaml", KEYSPACE).verify(session))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("nosuchtable");
    }
}
