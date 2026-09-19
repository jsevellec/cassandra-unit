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

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cassandraunit.assertion.CqlAssertions.assertThat;

/**
 * Column-level assertions: the value semantics, and the coercion that makes them usable.
 */
class RowAssertTest {

    private static final String KEYSPACE = "rowassertkeyspace";
    private static final String DATA = "rows/assertion-data.yaml";
    private static final String FULL = "1690e8da-5bf8-49e8-9583-4dff8a570701";
    private static final String EMPTY_TAGS = "1690e8da-5bf8-49e8-9583-4dff8a570702";
    private static final String NULL_LABEL = "1690e8da-5bf8-49e8-9583-4dff8a570703";

    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/assertionSchema.cql", KEYSPACE));

    @BeforeEach
    void loadRows(CqlSession session) {
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(DATA, false, false, KEYSPACE));
    }

    private RowAssert widget(CqlSession session, String id) {
        return assertThat(session).keyspace(KEYSPACE).table("widget").row("id", id);
    }

    /** Values given as the exact driver type, which must always work. */
    @Test
    void shouldAssertValuesGivenAsTheirDriverType(CqlSession session) {
        widget(session, FULL)
                .hasValue("id", UUID.fromString(FULL))
                .hasValue("label", "1")
                .hasValue("tags", Set.of("alpha", "beta"))
                .hasValue("created", Instant.parse("2026-09-19T10:00:00Z"))
                .hasValue("quantity", 42L)
                .hasValue("ratio", 1.5d)
                .hasValue("props", Map.of("a", 1, "b", 2));
    }

    /**
     * The same assertions written the way someone actually writes them: an int literal against a
     * bigint, a string against a uuid and a timestamp. Without coercion every one of these fails
     * on a Java type mismatch and the API looks broken.
     */
    @Test
    void shouldCoerceValuesTheWayARowDatasetDoes(CqlSession session) {
        widget(session, FULL)
                .hasValue("id", FULL)
                .hasValue("quantity", 42)
                .hasValue("created", "2026-09-19T10:00:00Z");
    }

    /** A text column holding "1" is the string, never the number - the defining bug of the 4.x formats. */
    @Test
    void shouldNotTurnAQuotedNumberIntoANumber(CqlSession session) {
        widget(session, FULL).hasValue("label", "1");

        assertThatThrownBy(() -> widget(session, FULL).hasValue("label", "one"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldAssertNull(CqlSession session) {
        widget(session, NULL_LABEL).hasNull("label");
        widget(session, FULL).hasNonNull("label");
    }

    /**
     * A collection never reads back as null - the codecs decode absent bytes to an empty
     * collection - so "null" and "empty" have to be the same assertion, exactly as the
     * expected-dataset path treats them.
     */
    @Test
    void anEmptyCollectionShouldMatchNullEitherWay(CqlSession session) {
        widget(session, EMPTY_TAGS)
                .hasNull("tags")
                .hasValue("tags", Set.of());
    }

    /** Counters cannot be written by a row dataset, but they read back as a bigint. */
    @Test
    void shouldAssertOnACounter(CqlSession session) {
        assertThat(session).keyspace(KEYSPACE).table("tally").row("id", "a")
                .hasValue("hits", 3);
    }

    @Test
    void shouldAssertSeveralColumnsAtOnce(CqlSession session) {
        widget(session, FULL).hasValues(Map.of("label", "1", "quantity", 42));
    }

    @Test
    void aMismatchShouldRenderBothValuesAsCqlLiterals(CqlSession session) {
        assertThatThrownBy(() -> widget(session, FULL).hasValue("label", "wrong"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("column label")
                .hasMessageContaining("'wrong'")     // quoted, so it pastes into cqlsh
                .hasMessageContaining("'1'");
    }

    /** No such column means the test is wrong, not the code under test. */
    @Test
    void anUnknownColumnShouldBeAnErrorAndListTheRealOnes(CqlSession session) {
        assertThatThrownBy(() -> widget(session, FULL).hasValue("nosuchcolumn", "x"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("no column 'nosuchcolumn'")
                .hasMessageContaining("label");
    }

    @Test
    void shouldAssertOnARowFetchedByHand(CqlSession session) {
        assertThat(session.execute(
                "SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + FULL).one())
                .hasValue("label", "1")
                .hasValue("quantity", 42);
    }
}
