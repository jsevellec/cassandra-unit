package org.cassandraunit.dataset;

import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.cassandraunit.dataset.rows.TableRows;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The builder on its own, with no Cassandra: does it produce the {@link TableRows} a parser would?
 * <p>
 * Type conversion is deliberately not tested here - the builder does none, exactly as the parsers
 * do none. That is {@code BuiltDataSetTest}'s job in the cassandra-unit module, because it needs a
 * real schema to convert against.
 *
 * @author Jeremy Sevellec
 */
class CQLDataSetBuilderTest {

    private static final UUID ID = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570701");

    @Test
    void shouldProduceTablesAndRowsInTheOrderTheyWereWritten() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder("myKeySpace")
                .table("widget").columns("id", "label", "quantity")
                    .row(ID, "one", 42)
                    .row(ID, "two", 7)
                .table("event").columns("day", "kind")
                    .row("2026-09-19", "start")
                .build();

        assertThat(dataSet.parse()).extracting(TableRows::table).containsExactly("widget", "event");
        assertThat(dataSet.parse().get(0).rows()).containsExactly(
                row("id", ID, "label", "one", "quantity", 42),
                row("id", ID, "label", "two", "quantity", 7));
        assertThat(dataSet.parse().get(1).rows()).containsExactly(row("day", "2026-09-19", "kind", "start"));
    }

    /**
     * The values are handed over as they are, untouched. Everything that makes an {@code Instant}
     * or its string form both work is the converter's, and it runs later against the real column
     * type - the same place a file's values are reconciled.
     */
    @Test
    void shouldNotConvertValues() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder()
                .table("widget").columns("id", "quantity")
                    .row("1690e8da-5bf8-49e8-9583-4dff8a570701", 42)
                .build();

        Map<String, Object> only = dataSet.parse().get(0).rows().get(0);
        assertThat(only.get("id")).isInstanceOf(String.class);
        assertThat(only.get("quantity")).isEqualTo(42);
    }

    /** An explicit null, the thing that writes a tombstone. Absent means unset, which is not this. */
    @Test
    void aNullValueShouldBeAPresentKey() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder()
                .table("widget").columns("id", "label")
                    .row(ID, null)
                .build();

        Map<String, Object> only = dataSet.parse().get(0).rows().get(0);
        assertThat(only).containsKey("label");
        assertThat(only.get("label")).isNull();
    }

    @Test
    void aColumnLeftOutOfAMapRowShouldStayAbsent() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder()
                .table("widget")
                    .row(Map.of("id", ID))
                .build();

        assertThat(dataSet.parse().get(0).rows().get(0)).containsOnlyKeys("id");
    }

    /**
     * {@code row} is overloaded on {@code Map} and on varargs, and Java's generics are invariant:
     * against a {@code Map<String, Object>} parameter a {@code Map<String, Integer>} variable is
     * not applicable, so the call would quietly bind to the varargs overload and make the whole map
     * one positional value. The wildcard in the signature is what stops that.
     */
    @Test
    void aMapRowShouldNotCareWhatItsValueTypeIs() {
        Map<String, Integer> quantities = Map.of("quantity", 7);

        RowsCQLDataSet dataSet = CQLDataSetFactory.builder().table("widget").row(quantities).build();

        assertThat(dataSet.parse().get(0).rows()).containsExactly(row("quantity", 7));
    }

    /** A map row needs no declared columns and declares none, so it can hold whatever it likes. */
    @Test
    void returningToATableShouldAppendRowsAndKeepItsColumns() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder()
                .table("widget").columns("id", "label")
                    .row(ID, "one")
                .table("event").columns("day")
                    .row("2026-09-19")
                .table("widget")
                    .row(ID, "two")
                    .row(Map.of("id", ID, "quantity", 7))
                .build();

        assertThat(dataSet.parse()).extracting(TableRows::table).containsExactly("widget", "event");
        assertThat(dataSet.parse().get(0).rows()).containsExactly(
                row("id", ID, "label", "one"),
                row("id", ID, "label", "two"),
                row("id", ID, "quantity", 7));
    }

    @Test
    void declaringColumnsTwiceForOneTableShouldFail() {
        assertThatThrownBy(() -> CQLDataSetFactory.builder()
                .table("widget").columns("id", "label")
                    .row(ID, "one")
                .table("widget").columns("id", "quantity"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("already declared as id, label")
                .hasMessageContaining("row(Map)");
    }

    @Test
    void aPositionalRowBeforeAnyColumnsShouldFail() {
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table("widget").row(ID, "one"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("no columns yet")
                .hasMessageContaining("columns(...)");
    }

    @Test
    void aRowOfTheWrongWidthShouldFailSayingWhichRow() {
        assertThatThrownBy(() -> CQLDataSetFactory.builder()
                .table("widget").columns("id", "label", "quantity")
                    .row(ID, "one", 42)
                    .row(ID, "two"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Row 1 of table widget has 2 values but 3 columns")
                .hasMessageContaining("id, label, quantity");
    }

    @Test
    void blankNamesAndEmptyRowsShouldFail() {
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table(" "))
                .isInstanceOf(ParseException.class).hasMessageContaining("table name");
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table("widget").columns("id", " "))
                .isInstanceOf(ParseException.class).hasMessageContaining("column name");
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table("widget").columns())
                .isInstanceOf(ParseException.class).hasMessageContaining("no columns");
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table("widget").columns("id", "id"))
                .isInstanceOf(ParseException.class).hasMessageContaining("declared twice");
        assertThatThrownBy(() -> CQLDataSetFactory.builder().table("widget").row(Map.of()))
                .isInstanceOf(ParseException.class).hasMessageContaining("is empty");
    }

    /** How an expectation says "this table holds nothing". A dataset with no tables at all is not. */
    @Test
    void aTableWithNoRowsShouldBeAllowedButAnEmptyDataSetShouldNot() {
        assertThat(CQLDataSetFactory.builder().table("widget").build().parse())
                .containsExactly(new TableRows("widget", List.of()));

        assertThatThrownBy(() -> CQLDataSetFactory.builder().build())
                .isInstanceOf(ParseException.class).hasMessageContaining("has no tables");
    }

    /**
     * A parsed dataset is read afresh on every parse(); a built one hands out the same rows every
     * time. These are what keep that from being a difference anyone can observe.
     */
    @Test
    void whatWasBuiltShouldBeFrozen() {
        CQLDataSetBuilder builder = CQLDataSetFactory.builder();
        RowsCQLDataSet dataSet = builder.table("widget").columns("id", "label").row(ID, "one").build();

        builder.table("widget").row(ID, "two");
        builder.table("gadget").columns("id").row(ID);

        assertThat(dataSet.parse()).hasSize(1);
        assertThat(dataSet.parse().get(0).rows()).hasSize(1);
        assertThat(dataSet.parse()).isEqualTo(dataSet.parse());
        assertThatThrownBy(() -> dataSet.parse().get(0).rows().get(0).put("label", "two"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aBuiltDataSetShouldNotTouchItsKeyspace() {
        RowsCQLDataSet dataSet = CQLDataSetFactory.builder("myKeySpace").table("widget").build();

        // Lowercased, as every other dataset does it, because USE would otherwise break.
        assertThat(dataSet.getKeyspaceName()).isEqualTo("mykeyspace");
        // Rows only, never schema: dropping the keyspace would destroy the tables they need.
        assertThat(dataSet.isKeyspaceCreation()).isFalse();
        assertThat(dataSet.isKeyspaceDeletion()).isFalse();

        RowsCQLDataSet owning = CQLDataSetFactory.builder("myKeySpace", true, true).table("widget").build();
        assertThat(owning.isKeyspaceCreation()).isTrue();
        assertThat(owning.isKeyspaceDeletion()).isTrue();
    }

    /** describe() prefixes every parse error and every assertion failure, so it has to say something. */
    @Test
    void shouldDescribeItself() {
        assertThat(CQLDataSetFactory.builder().table("widget").build().describe())
                .isEqualTo("a dataset built in code");
        assertThat(CQLDataSetFactory.builder().named("the shipping fixture").table("widget").build().describe())
                .isEqualTo("the shipping fixture");
        assertThat(CQLDataSetFactory.builder().table("widget").named("named later").build().describe())
                .isEqualTo("named later");
    }

    private static Map<String, Object> row(Object... keysAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }
}
