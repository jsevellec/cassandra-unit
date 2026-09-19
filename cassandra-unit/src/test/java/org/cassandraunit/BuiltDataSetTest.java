package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.assertion.ExpectedDataSetFactory;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.CompositeCQLDataSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A dataset written in Java, end to end against a real schema.
 * <p>
 * The point of the first two tests is that a builder is not a second way of loading rows. The
 * fixture below is {@code rows/assertion-data.yaml} rewritten in code, and each direction is
 * checked against the other: rows built in code satisfy the file's expectation, and the file's rows
 * satisfy the expectation built in code. If the builder and the YAML parser ever stopped producing
 * the same thing, one of the two would fail.
 *
 * @author Jeremy Sevellec
 */
class BuiltDataSetTest {

    private static final String KEYSPACE = "builtkeyspace";
    private static final String FILE = "rows/assertion-data.yaml";

    private static final UUID ONE = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570701");
    private static final UUID TWO = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570702");
    private static final UUID THREE = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570703");

    /**
     * Real Java objects where the file has to spell things as text - a {@code UUID}, an
     * {@code Instant}, a {@code Set} - which is the one thing the builder can do that a file cannot.
     * <p>
     * The first row is positional and the other two are maps, because they are not the same shape:
     * the file leaves {@code created}, {@code ratio} and {@code props} out of them, and absent is
     * not the same as null. The third row's {@code label} <em>is</em> an explicit null, so it needs
     * a LinkedHashMap - {@link Map#of} rejects null values.
     */
    private static final RowsCQLDataSet FIXTURE = CQLDataSetFactory.builder(KEYSPACE)
            .named("the assertion fixture, built in code")
            .table("widget").columns("id", "label", "tags", "created", "quantity", "ratio", "props")
                .row(ONE, "1", Set.of("alpha", "beta"), Instant.parse("2026-09-19T10:00:00Z"),
                        42L, 1.5, Map.of("a", 1, "b", 2))
                .row(Map.of("id", TWO, "label", "two", "tags", Set.of(), "quantity", 7L))
                .row(nullLabel(THREE))
            .table("event").columns("day", "at", "kind", "total")
                .row("2026-09-19", Instant.parse("2026-09-19T12:00:00Z"), "start", 2)
                .row("2026-09-19", Instant.parse("2026-09-19T10:00:00Z"), "retry", 2)
                .row("2026-09-20", Instant.parse("2026-09-20T09:00:00Z"), "start", 1)
            .build();

    /**
     * The built dataset drives the extension like any other: schema first, then rows, the keyspace
     * dropped and recreated once for the pair before every test.
     */
    @RegisterExtension
    static final CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new CompositeCQLDataSet(
                    List.of(new ClassPathCQLDataSet("cql/assertionSchema.cql", false, false, KEYSPACE), FIXTURE),
                    true, true, KEYSPACE));

    @Test
    void rowsBuiltInCodeShouldSatisfyTheFileExpectation(CqlSession session) {
        assertThatCode(() -> ExpectedDataSetFactory.fromClassPath(FILE, KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    @Test
    void rowsFromTheFileShouldSatisfyTheExpectationBuiltInCode(CqlSession session) {
        session.execute("TRUNCATE " + KEYSPACE + ".widget");
        session.execute("TRUNCATE " + KEYSPACE + ".event");
        new CQLDataLoader(session).load(CQLDataSetFactory.fromClassPath(FILE, false, false, KEYSPACE));

        assertThatCode(() -> ExpectedDataSetFactory.of(FIXTURE, KEYSPACE).verify(session))
                .doesNotThrowAnyException();
    }

    /**
     * Handing over the object you already have is the convenience; its string form still works,
     * because both go through the converter a CSV file's values go through.
     */
    @Test
    void aStringShouldConvertJustAsTheObjectDoes(CqlSession session) {
        UUID id = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a5707ff");
        new CQLDataLoader(session).load(CQLDataSetFactory.builder(KEYSPACE)
                .table("widget").columns("id", "created", "quantity", "ratio", "tags")
                    .row(id.toString(), "2026-09-19T10:00:00Z", "42", "1.5", List.of("alpha"))
                .build());

        Row row = session.execute("SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + id).one();
        assertThat(row.getInstant("created")).isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
        assertThat(row.getLong("quantity")).isEqualTo(42L);
        assertThat(row.getDouble("ratio")).isEqualTo(1.5);
        assertThat(row.getSet("tags", String.class)).containsExactly("alpha");
    }

    /**
     * {@code builder()} with no keyspace, which is documented as "against whatever the session is
     * already using". Nothing in the dataset may then touch the keyspace - there is none to touch -
     * so this is the one path where a wrong default would surface as {@code USE null}.
     */
    @Test
    void aBuilderWithNoKeyspaceShouldUseTheSessionsOwn(CqlSession session) {
        UUID id = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a5707fe");
        session.execute("USE " + KEYSPACE);

        new CQLDataLoader(session).load(CQLDataSetFactory.builder()
                .table("widget").columns("id", "label")
                    .row(id, "no keyspace named")
                .build());

        assertThat(widget(session, id).getString("label")).isEqualTo("no keyspace named");
    }

    /** An explicit null writes a tombstone, exactly as {@code label: null} in the file does. */
    @Test
    void anExplicitNullShouldBeWritten(CqlSession session) {
        assertThat(widget(session, THREE).getString("label")).isNull();
        assertThat(widget(session, THREE).getLong("quantity")).isZero();
    }

    /**
     * A dataset with no file still has to say where it came from: {@code describe()} is what every
     * failure message is prefixed with, and "a dataset built in code" three times over would be
     * useless in a suite with three of them.
     */
    @Test
    void aFailureShouldNameTheDataSet(CqlSession session) {
        RowsCQLDataSet wrong = CQLDataSetFactory.builder(KEYSPACE).named("the shipping fixture")
                .table("widget").columns("id", "label")
                    .row(ONE, "not what is there")
                .build();

        assertThatThrownBy(() -> ExpectedDataSetFactory.of(wrong, KEYSPACE).verify(session))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("the shipping fixture")
                .hasMessageContaining("not what is there");
    }

    private static Row widget(CqlSession session, UUID id) {
        return session.execute("SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + id).one();
    }

    private static Map<String, Object> nullLabel(UUID id) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", null);
        row.put("quantity", 0L);
        return row;
    }
}
