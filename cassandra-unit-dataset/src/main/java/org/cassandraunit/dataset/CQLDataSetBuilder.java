package org.cassandraunit.dataset;

import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.cassandraunit.dataset.rows.TableRows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fixture rows written in Java, as a sixth dataset format beside YAML, JSON, XML, CSV and CQL.
 * <pre>{@code
 * RowsCQLDataSet fixtures = CQLDataSetFactory.builder("mykeyspace")
 *         .table("widget").columns("id", "label", "quantity")
 *             .row(id1, "one", 42)
 *             .row(id2, "two", 7)
 *             .row(id3, null, 0)
 *         .table("event").columns("day", "at", "kind")
 *             .row("2026-09-19", instant, "start")
 *         .build();
 *
 * new CQLDataLoader(session).load(fixtures);
 * }</pre>
 * For three rows a file is a lot of ceremony, and it puts the fixture somewhere other than the test
 * that depends on it. This is the same dataset, next to the test.
 * <p>
 * It is not a second way of loading rows: {@link #build()} returns an ordinary
 * {@link RowsCQLDataSet}, so from there on everything - reading the column types from the live
 * schema, the prepared-statement binding, the keyspace handling, the extensions and the JUnit 4
 * rule - is the code a file goes through.
 * <p>
 * The same object can state the expectation, because an expected dataset is a row dataset read the
 * other way round:
 * <pre>{@code
 * ExpectedDataSetFactory.of(fixtures, "mykeyspace").verify(session);
 * }</pre>
 * Declare the variable as {@code RowsCQLDataSet} rather than {@code CQLDataSet} if you want both
 * uses from one object - {@code ExpectedDataSetFactory.of} is typed on the concrete class.
 *
 * <h2>Values</h2>
 *
 * Values are real Java objects, converted against the real column type by the same
 * {@link org.cassandraunit.dataset.rows.RowValueConverter} a file goes through. Anything the
 * column's codec already accepts is passed straight through, so the advantage over a file is that
 * you can hand over the object you already have - a {@code UUID}, an {@code Instant}, a
 * {@code Set<String>} - and its string form still works if that is what you have instead.
 * <p>
 * A {@code null} is an explicit null, exactly as in a row file: it writes a tombstone on load and
 * asserts "reads back null" on compare. Leaving a column out of a row means unset, and nothing is
 * written for it. Note that {@link Map#of} rejects null values, so {@link Table#row(Object...)} or
 * a {@link LinkedHashMap} is the way to express one.
 *
 * <h2>Mistakes</h2>
 *
 * A malformed dataset raises {@link ParseException} at the call that malformed it, not at
 * {@code build()}, so the stack trace points at the row that is wrong. That is the same split the
 * rest of this package keeps: a dataset that cannot be used at all means the test is wrong, while
 * data that does not match means the code under test is.
 *
 * @author Jeremy Sevellec
 */
public final class CQLDataSetBuilder {

    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final boolean keyspaceCreation;
    private final boolean keyspaceDeletion;
    private final String keyspaceName;
    private String origin = "a dataset built in code";

    CQLDataSetBuilder(String keyspaceName, boolean keyspaceCreation, boolean keyspaceDeletion) {
        this.keyspaceName = keyspaceName;
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
    }

    /**
     * What to call this dataset in error and failure messages, where a file would show its path.
     * Defaults to {@code "a dataset built in code"}, which is honest but says nothing about
     * <em>which</em> one when a suite has several.
     */
    public CQLDataSetBuilder named(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new ParseException("A dataset name must not be blank");
        }
        this.origin = origin;
        return this;
    }

    /**
     * Starts, or returns to, the rows of one table. Naming a table twice appends to it and keeps
     * the columns already declared for it, so a loop can add rows to a table it opened earlier.
     */
    public Table table(String name) {
        if (name == null || name.isBlank()) {
            throw new ParseException("A table name must not be blank");
        }
        return tables.computeIfAbsent(name.trim(), Table::new);
    }

    /**
     * The dataset. Rows are copied on the way out, so the builder can be reused or changed
     * afterwards without touching what was built.
     */
    public RowsCQLDataSet build() {
        if (tables.isEmpty()) {
            throw new ParseException(origin + " has no tables. A dataset with nothing in it would"
                    + " load nothing and assert nothing - name at least one table, with"
                    + " table(\"...\"), even if it holds no rows.");
        }
        List<TableRows> built = new ArrayList<>(tables.size());
        tables.values().forEach(table -> built.add(new TableRows(table.name, table.rows)));
        return RowsCQLDataSet.of(built, origin, keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    /**
     * The rows of one table. Returned by {@link CQLDataSetBuilder#table(String)}, and carrying
     * {@code table}, {@code named} and {@code build} so a whole dataset is one chain.
     */
    public final class Table {

        private final String name;
        private final List<Map<String, Object>> rows = new ArrayList<>();
        private List<String> columns;

        private Table(String name) {
            this.name = name;
        }

        /**
         * Declares the columns the positional {@link #row(Object...)} fills in, once for the table.
         * <p>
         * Declaring them a second time is a {@link ParseException} rather than a redefinition: the
         * rows already added were read against the first list, and two shapes under one table would
         * leave nothing in the API to say which row used which. Use {@link #row(Map)} for a row
         * whose columns differ from the rest.
         */
        public Table columns(String... columns) {
            if (this.columns != null) {
                throw new ParseException("Columns of table " + name + " are already declared as "
                        + String.join(", ", this.columns) + ". Declare them once - returning to a"
                        + " table keeps them - or use row(Map) for a row of a different shape.");
            }
            if (columns == null || columns.length == 0) {
                throw new ParseException("Table " + name + " was declared with no columns");
            }
            Set<String> unique = new LinkedHashSet<>();
            for (String column : columns) {
                if (column == null || column.isBlank()) {
                    throw new ParseException("A column name of table " + name + " is blank");
                }
                if (!unique.add(column.trim())) {
                    throw new ParseException("Column " + column.trim() + " is declared twice on"
                            + " table " + name);
                }
            }
            this.columns = List.copyOf(unique);
            return this;
        }

        /**
         * A row, its values in the order {@link #columns(String...)} declared. A {@code null} is an
         * explicit null; a column that should stay unset has to be left out, which means
         * {@link #row(Map)}.
         */
        public Table row(Object... values) {
            if (columns == null) {
                throw new ParseException("Table " + name + " has no columns yet, so there is no way"
                        + " to tell what these values are. Call columns(...) first, or pass a map:"
                        + " row(Map.of(\"id\", ...)).");
            }
            Object[] given = values == null ? new Object[0] : values;
            if (given.length != columns.size()) {
                throw new ParseException("Row " + rows.size() + " of table " + name + " has "
                        + given.length + " value" + (given.length == 1 ? "" : "s") + " but "
                        + columns.size() + " columns were declared: " + String.join(", ", columns)
                        + ". " + Arrays.toString(given));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < given.length; i++) {
                row.put(columns.get(i), given[i]);
            }
            rows.add(row);
            return this;
        }

        /**
         * A row naming its own columns, for one that does not fit the declared shape - a column the
         * other rows leave unset, say. Needs no {@link #columns(String...)} at all, and does not
         * declare any.
         * <p>
         * The wildcard is load-bearing rather than tidiness: {@code Map<String, ?>} also accepts a
         * {@code Map<String, Integer>}, where {@code Map<String, Object>} would not and the call
         * would bind to {@link #row(Object...)} instead - silently making the whole map one
         * positional value.
         */
        public Table row(Map<String, ?> values) {
            if (values == null || values.isEmpty()) {
                throw new ParseException("Row " + rows.size() + " of table " + name + " is empty."
                        + " A row with no columns cannot be written or compared.");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            values.forEach((column, value) -> {
                if (column == null || column.isBlank()) {
                    throw new ParseException("Row " + rows.size() + " of table " + name
                            + " has a blank column name");
                }
                row.put(column.trim(), value);
            });
            rows.add(row);
            return this;
        }

        /** Moves on to another table - see {@link CQLDataSetBuilder#table(String)}. */
        public Table table(String table) {
            return CQLDataSetBuilder.this.table(table);
        }

        /** Names the dataset - see {@link CQLDataSetBuilder#named(String)}. */
        public Table named(String origin) {
            CQLDataSetBuilder.this.named(origin);
            return this;
        }

        /** Builds the dataset - see {@link CQLDataSetBuilder#build()}. */
        public RowsCQLDataSet build() {
            return CQLDataSetBuilder.this.build();
        }
    }
}
