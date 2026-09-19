package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cassandraunit.assertion.CqlAssertions.assertThat;

/**
 * The two assertion paths must agree.
 * <p>
 * This package now offers two ways to say the same thing - a dataset file compared by
 * {@link DataSetComparator}, and the fluent {@link CqlAssertions}. They share
 * {@link ValueEquality} and {@code RowValueConverter} precisely so that they cannot drift, but
 * sharing code is an implementation detail and this is the assertion about behaviour. Two paths in
 * one library disagreeing about whether a row matches would be a defect, not a quirk, and it would
 * otherwise only surface in a user's suite.
 */
class CqlAssertionsAgreementTest {

    private static final String KEYSPACE = "agreementkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";
    private static final String FULL = "1690e8da-5bf8-49e8-9583-4dff8a570701";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    private ExpectedDataSet expected() {
        return ExpectedDataSetFactory.fromClassPath(DATA, KEYSPACE);
    }

    @Test
    void bothShouldPassOnTheLoadedData(CqlSession session) {
        assertThatCode(() -> expected().verify(session)).doesNotThrowAnyException();

        assertThatCode(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .hasRowCount(3)
                .row("id", FULL)
                .hasValue("label", "1")
                .hasValue("tags", java.util.Set.of("alpha", "beta"))
                .hasValue("quantity", 42))
                .doesNotThrowAnyException();
    }

    @Test
    void bothShouldFailOnTheSameChange(CqlSession session) {
        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'changed' WHERE id = " + FULL);

        assertThatThrownBy(() -> expected().verify(session))
                .isInstanceOf(DataSetMismatchError.class)
                .hasMessageContaining("changed");

        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("id", FULL).hasValue("label", "1"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("changed");
    }

    /**
     * The normalization most likely to drift, because plain {@code equals} gets it wrong: an
     * absent collection reads back empty, never null. Both paths have to call that a match.
     */
    @Test
    void bothShouldTreatAnEmptyCollectionAsNull(CqlSession session) {
        String emptyTags = "1690e8da-5bf8-49e8-9583-4dff8a570702";

        // the dataset says `tags: []` for this row and passes
        assertThatCode(() -> expected().verify(session)).doesNotThrowAnyException();

        assertThatCode(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("id", emptyTags).hasNull("tags"))
                .doesNotThrowAnyException();
    }

    /** And the bridge, so a chain can hand off to the file-driven engine mid-assertion. */
    @Test
    void theKeyspaceAssertShouldBridgeToAnExpectedDataSet(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE)
                .hasTable("widget")
                .matches(expected());

        session.execute("UPDATE " + KEYSPACE + ".widget SET label = 'changed' WHERE id = " + FULL);

        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).matches(expected()))
                .isInstanceOf(DataSetMismatchError.class);
    }
}
