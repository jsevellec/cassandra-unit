package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cassandraunit.assertion.CqlAssertions.assertThat;

/**
 * Navigation and table-level assertions.
 * <p>
 * Note the single static import of {@link CqlAssertions#assertThat} sitting beside AssertJ's own
 * {@code assertThatThrownBy}: the overloads take driver types, so the two coexist. That is not
 * incidental, it is the reason the entry points are shaped this way.
 */
class CqlAssertionsTest {

    private static final String KEYSPACE = "cqlassertionskeyspace";
    private static final String DATA = "rows/assertion-data.yaml";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    @Test
    void shouldAssertOnKeyspacesAndTables(CqlSession session) {
        assertThat(session)
                .hasKeyspace(KEYSPACE)
                .doesNotHaveKeyspace("nosuchkeyspace");

        assertThat(session).keyspace(KEYSPACE)
                .hasTable("widget")
                .doesNotHaveTable("nosuchtable");
    }

    @Test
    void shouldAssertOnRowCounts(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("widget").hasRowCount(3).isNotEmpty();

        session.execute("TRUNCATE " + KEYSPACE + ".widget");

        assertThat(session).keyspace(KEYSPACE).table("widget").isEmpty().hasRowCount(0);
    }

    @Test
    void shouldAssertOnASingleRowByItsKey(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("id", "1690e8da-5bf8-49e8-9583-4dff8a570701")
                .hasValue("label", "1");
    }

    /** event is PRIMARY KEY (day, at), so the whole key is needed. */
    @Test
    void shouldAssertOnARowWithACompoundKey(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("event")
                .row(Map.of("day", "2026-09-19", "at", "2026-09-19T12:00:00Z"))
                .hasValue("kind", "start");
    }

    @Test
    void shouldAssertARowIsAbsent(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("widget")
                .hasNoRow("id", "1690e8da-5bf8-49e8-9583-4dff8a5707ff");
    }

    @Test
    void shouldAssertOnAllRows(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("widget").rows()
                .hasSize(3)
                .extracting("label")
                .containsExactlyInAnyOrder("1", "two", null);
    }

    @Test
    void shouldAssertOnAResultSetYouAlreadyHave(CqlSession session) {
        assertThat(session.execute("SELECT * FROM " + KEYSPACE + ".event WHERE day = '2026-09-19'"))
                .hasSize(2)
                .first()
                .hasValue("kind", "start");   // clustering order is at DESC, so 12:00 comes first
    }

    @Test
    void aMismatchShouldFailWithTheTableAndBothValues(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("widget").hasRowCount(99))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(KEYSPACE + ".widget")
                .hasMessageContaining("99")
                .hasMessageContaining("3");
    }

    /**
     * Addressing a compound-key table by one column is refused rather than matching whatever a
     * partial key happens to return - the same class of mistake as a null primary key, reported
     * the same way.
     */
    @Test
    void addressingACompoundKeyByOneColumnShouldBeRefused(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("event")
                .row("day", "2026-09-19"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("primary key of " + KEYSPACE + ".event is (day, at)")
                .hasMessageContaining("row(Map.of(...))");
    }

    @Test
    void aNullPrimaryKeyShouldBeRefused(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("id", null))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("sets id to null")
                .hasMessageContaining("No primary-key column can be null");
    }

    @Test
    void aKeyColumnThatIsNotPartOfTheKeyShouldBeRefused(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("label", "one"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot be addressed by 'label' alone");
    }

    /** Navigating somewhere that does not exist is a broken test, not a failing one. */
    @Test
    void navigatingToSomethingAbsentShouldBeAnError(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace("nosuchkeyspace"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("hasKeyspace");

        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("nosuchtable"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("hasTable");
    }

    /** A row that should be there but is not is a failure, because that is a claim about data. */
    @Test
    void anAbsentRowShouldBeAFailureNotAnError(CqlSession session) {
        assertThatThrownBy(() -> assertThat(session).keyspace(KEYSPACE).table("widget")
                .row("id", "1690e8da-5bf8-49e8-9583-4dff8a5707ff"))
                .isInstanceOf(AssertionError.class)
                .isNotInstanceOf(ParseException.class)
                .hasMessageContaining("no such row");
    }
}
