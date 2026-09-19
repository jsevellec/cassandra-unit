package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.rows.RowValueConverter;
import org.cassandraunit.dataset.rows.TableNames;
import org.cassandraunit.dataset.rows.TableNames.QualifiedTable;
import org.cassandraunit.dataset.rows.TableRows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares what a dataset says a table should hold against what it does hold.
 * <p>
 * The design decisions worth knowing, because Cassandra is not SQL:
 * <ul>
 * <li><b>Rows are matched by primary key</b>, never by position. A wrong value then reports as one
 * column difference on the right row, rather than as a missing row plus an unexpected one.</li>
 * <li><b>Order across partitions is never compared.</b> An unrestricted {@code SELECT} returns
 * partition-token order, which is stable but arbitrary. Order <em>within</em> a partition is
 * meaningful and can be asserted with
 * {@link ExpectedDataSetOptions#checkClusteringOrder()}.</li>
 * <li><b>A column absent from an expected row is not asserted</b>, and is not even selected. A
 * column present with {@code null} asserts the column reads back as null. On read-back a tombstone
 * and a never-written cell are indistinguishable, so "was tombstoned" is not expressible.</li>
 * <li><b>Never {@code SELECT *}, and never {@code ALLOW FILTERING}.</b> The projection is the
 * primary key plus whatever columns the dataset mentions, and the only two statement shapes are
 * unrestricted or restricted by a complete partition key.</li>
 * </ul>
 *
 * @author Jeremy Sevellec
 */
final class DataSetComparator {

    /**
     * Refuse to pull more than this from one table. An expected dataset aimed at a real table
     * should fail quickly and say so, not exhaust the test JVM's heap.
     */
    private static final int MAX_ROWS = 10_000;

    private final CqlSession session;
    private final CodecRegistry codecRegistry;
    private final RowValueConverter converter;
    private final CqlFormat format;
    private final ExpectedDataSetOptions options;
    private final String origin;

    DataSetComparator(CqlSession session, ExpectedDataSetOptions options, String origin) {
        this.session = session;
        this.codecRegistry = session.getContext().getCodecRegistry();
        this.converter = new RowValueConverter(codecRegistry, origin);
        this.format = new CqlFormat(codecRegistry);
        this.options = options;
        this.origin = origin;
    }

    void verify(List<TableRows> expectedTables, String datasetKeyspace) {
        List<Difference> differences = new ArrayList<>();
        StringBuilder report = new StringBuilder();

        for (TableRows expected : expectedTables) {
            compareTable(expected, datasetKeyspace, differences, report);
        }

        if (!differences.isEmpty()) {
            throw new DataSetMismatchError(header(datasetKeyspace) + report, differences);
        }
    }

    private String header(String datasetKeyspace) {
        StringBuilder out = new StringBuilder("Expected dataset does not match");
        if (datasetKeyspace != null) {
            out.append(" keyspace ").append(datasetKeyspace);
        }
        out.append('\n');
        out.append("  expected : ").append(origin).append('\n');
        out.append("  mode     : ").append(options.mode() == MatchMode.STRICT
                ? "strict - every row in the asserted scope must be listed"
                : "contains - extra rows in the database are ignored").append('\n');
        if (options.scope() == Scope.MENTIONED_PARTITIONS) {
            out.append("  scope    : only the partitions the dataset mentions\n");
        }
        if (!options.ignoredColumns().isEmpty()) {
            out.append("  ignoring : ")
                    .append(options.ignoredColumns().stream().sorted().collect(Collectors.joining(", ")))
                    .append('\n');
        }
        return out.toString();
    }

    private void compareTable(TableRows expectedRows, String datasetKeyspace,
                              List<Difference> differences, StringBuilder report) {
        QualifiedTable table = TableNames.resolve(expectedRows.table(), datasetKeyspace,
                session.getKeyspace().orElse(null), origin);
        TableSchema schema = TableSchema.of(session, table, origin);

        List<String> primaryKey = schema.primaryKey();
        requireUsableKey(table, schema, primaryKey);

        List<String> projection = projection(table, schema, expectedRows, primaryKey);

        PreparedStatement select = session.prepare(
                "SELECT " + projection.stream().map(TableNames::identifier).collect(Collectors.joining(", "))
                        + " FROM " + table.asCql());
        Map<String, DataType> types = typesOf(select.getResultSetDefinitions());

        List<ExpectedRow> expected = convert(expectedRows, table, primaryKey, projection, types);
        Selection selection = read(table, schema, select, projection, types, expected);

        new TableComparison(table, schema, projection, types, expected, selection)
                .run(differences, report);
    }

    private void requireUsableKey(QualifiedTable table, TableSchema schema, List<String> primaryKey) {
        if (primaryKey.isEmpty()) {
            throw new ParseException(origin + ": could not read the primary key of " + table.asCql()
                    + ", so rows cannot be matched. Does the table exist?");
        }
        for (String ignored : options.ignoredColumns()) {
            if (primaryKey.contains(ignored)) {
                throw new ParseException(origin + ": cannot ignore column '" + ignored + "' of "
                        + table.asCql() + " - it is part of the primary key, which is how an expected"
                        + " row is matched to a row in the database.");
            }
        }
    }

    /** Primary key, plus every column any expected row mentions, minus the ignored ones. */
    private List<String> projection(QualifiedTable table, TableSchema schema,
                                    TableRows expectedRows, List<String> primaryKey) {
        Set<String> columns = new LinkedHashSet<>(primaryKey);
        for (Map<String, Object> row : expectedRows.rows()) {
            for (String column : row.keySet()) {
                if (!schema.hasColumn(column)) {
                    throw new ParseException(origin + ": " + table.asCql() + " has no column '"
                            + column + "'. It has: "
                            + schema.columns().stream().sorted().collect(Collectors.joining(", ")) + '.');
                }
                if (!options.ignoredColumns().contains(column)) {
                    columns.add(column);
                }
            }
        }
        return List.copyOf(columns);
    }

    private static Map<String, DataType> typesOf(ColumnDefinitions definitions) {
        Map<String, DataType> types = new LinkedHashMap<>();
        for (ColumnDefinition definition : definitions) {
            types.put(definition.getName().asInternal(), definition.getType());
        }
        return types;
    }

    private List<ExpectedRow> convert(TableRows expectedRows, QualifiedTable table,
                                      List<String> primaryKey, List<String> projection,
                                      Map<String, DataType> types) {
        List<ExpectedRow> converted = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : expectedRows.rows()) {
            index++;
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, Object> cell : row.entrySet()) {
                if (options.ignoredColumns().contains(cell.getKey())) {
                    continue;
                }
                DataType type = types.get(cell.getKey());
                values.put(cell.getKey(),
                        converter.convert(type, cell.getValue(), expectedRows.table(), cell.getKey()));
            }
            requireFullKey(table, primaryKey, row, values, index);
            converted.add(new ExpectedRow(key(primaryKey, values), values));
        }
        return converted;
    }

    private void requireFullKey(QualifiedTable table, List<String> primaryKey,
                                Map<String, Object> row, Map<String, Object> values, int index) {
        List<String> missing = primaryKey.stream().filter(column -> !row.containsKey(column)).toList();
        if (!missing.isEmpty()) {
            throw new ParseException(origin + ": row " + index + " of table " + table.table()
                    + " does not set " + String.join(", ", missing)
                    + ". Every expected row must give the full primary key ("
                    + String.join(", ", primaryKey) + "), which is how it is matched to a row in the"
                    + " database.");
        }
        // Present but null is a different mistake with the same cause, and it has to be caught here
        // rather than left to fail later: Cassandra permits no null in any primary-key column, so
        // such a row matches nothing in TABLE scope, and in MENTIONED_PARTITIONS it reaches the
        // driver as "WHERE pk = null" and comes back as InvalidQueryException - an error about the
        // query, for what is really a mistake in the file.
        List<String> nulls = primaryKey.stream().filter(column -> values.get(column) == null).toList();
        if (!nulls.isEmpty()) {
            throw new ParseException(origin + ": row " + index + " of table " + table.table()
                    + " sets " + String.join(", ", nulls) + " to null. No primary-key column can be"
                    + " null in Cassandra, so no row can ever match this one. Give it a value, or"
                    + " remove the row.");
        }
    }

    private static List<Object> key(List<String> primaryKey, Map<String, Object> values) {
        return primaryKey.stream().map(values::get).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Reads the actual rows, either the whole table or only the partitions the dataset mentions.
     * Returns them in the order Cassandra gave them, which the clustering-order check needs.
     */
    private Selection read(QualifiedTable table, TableSchema schema, PreparedStatement unrestricted,
                           List<String> projection, Map<String, DataType> types,
                           List<ExpectedRow> expected) {
        List<Row> rows;
        String cql;
        if (options.scope() == Scope.MENTIONED_PARTITIONS) {
            List<String> partitionKey = schema.partitionKey();
            String where = partitionKey.stream()
                    .map(column -> TableNames.identifier(column) + " = ?")
                    .collect(Collectors.joining(" AND "));
            cql = unrestricted.getQuery() + " WHERE " + where;
            PreparedStatement restricted = session.prepare(cql);

            rows = new ArrayList<>();
            for (List<Object> partition : distinctPartitions(partitionKey, projection, expected)) {
                BoundStatementBuilder bound = restricted.boundStatementBuilder();
                for (int i = 0; i < partitionKey.size(); i++) {
                    Object value = partition.get(i);
                    bound = value == null
                            ? bound.setToNull(i)
                            : bound.set(i, value, codecRegistry.codecFor(types.get(partitionKey.get(i))));
                }
                rows.addAll(all(table, session.execute(bound.build()).all(), rows.size()));
            }
        } else {
            cql = unrestricted.getQuery();
            rows = all(table, session.execute(unrestricted.bind()).all(), 0);
        }
        return new Selection(cql, rows);
    }

    private List<Row> all(QualifiedTable table, List<Row> rows, int already) {
        if (already + rows.size() > MAX_ROWS) {
            throw new ParseException(origin + ": " + table.asCql() + " holds more than " + MAX_ROWS
                    + " rows, which is more than an expected dataset should be comparing. Use"
                    + " withinMentionedPartitions() to assert only the partitions the dataset names.");
        }
        return rows;
    }

    private List<List<Object>> distinctPartitions(List<String> partitionKey, List<String> projection,
                                                  List<ExpectedRow> expected) {
        Set<List<Object>> partitions = new LinkedHashSet<>();
        for (ExpectedRow row : expected) {
            partitions.add(partitionKey.stream().map(row.values()::get).toList());
        }
        return List.copyOf(partitions);
    }

    /** One expected row: its primary-key tuple, and the converted values it asserts. */
    record ExpectedRow(List<Object> key, Map<String, Object> values) {
    }

    /** The rows actually read, and the CQL that read them - echoed in the failure message. */
    record Selection(String cql, List<Row> rows) {
    }

    /**
     * The comparison itself, split out so the reading above stays readable. Holds no state beyond
     * one table's worth of already-gathered data.
     */
    private final class TableComparison {

        private final QualifiedTable table;
        private final TableSchema schema;
        private final List<String> projection;
        private final Map<String, DataType> types;
        private final List<ExpectedRow> expected;
        private final Selection selection;

        TableComparison(QualifiedTable table, TableSchema schema, List<String> projection,
                        Map<String, DataType> types, List<ExpectedRow> expected, Selection selection) {
            this.table = table;
            this.schema = schema;
            this.projection = projection;
            this.types = types;
            this.expected = expected;
            this.selection = selection;
        }

        void run(List<Difference> differences, StringBuilder report) {
            List<String> primaryKey = schema.primaryKey();
            Map<List<Object>, Map<String, Object>> actual = new LinkedHashMap<>();
            for (Row row : selection.rows()) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (String column : projection) {
                    values.put(column, row.getObject(TableNames.identifier(column)));
                }
                actual.put(key(primaryKey, values), values);
            }

            List<Difference> mine = new ArrayList<>();
            StringBuilder missing = new StringBuilder();
            StringBuilder unexpected = new StringBuilder();
            StringBuilder different = new StringBuilder();

            for (ExpectedRow row : expected) {
                Map<String, Object> found = actual.get(row.key());
                String renderedKey = renderKey(primaryKey, row.key());
                if (found == null) {
                    mine.add(Difference.missingRow(table.asCql(), renderedKey));
                    missing.append("    ").append(renderedKey).append('\n')
                            .append(format.row("      ", nonKey(primaryKey, row.values().keySet()),
                                    types, row.values()));
                    continue;
                }
                compareValues(row, found, renderedKey, mine, different);
            }

            if (options.mode() == MatchMode.STRICT) {
                Set<List<Object>> listed = expected.stream()
                        .map(ExpectedRow::key).collect(Collectors.toSet());
                for (Map.Entry<List<Object>, Map<String, Object>> entry : actual.entrySet()) {
                    if (!listed.contains(entry.getKey())) {
                        String renderedKey = renderKey(primaryKey, entry.getKey());
                        mine.add(Difference.unexpectedRow(table.asCql(), renderedKey));
                        unexpected.append("    ").append(renderedKey).append('\n')
                                .append(format.row("      ",
                                        nonKey(primaryKey, entry.getValue().keySet()),
                                        types, entry.getValue()));
                    }
                }
            }

            if (options.checkClusteringOrder()) {
                checkClusteringOrder(mine, different);
            }

            if (mine.isEmpty()) {
                return;
            }
            differences.addAll(mine);
            appendReport(report, mine, missing, unexpected, different);
        }

        private void compareValues(ExpectedRow row, Map<String, Object> found, String renderedKey,
                                   List<Difference> mine, StringBuilder different) {
            List<String> mismatched = new ArrayList<>();
            for (Map.Entry<String, Object> cell : row.values().entrySet()) {
                String column = cell.getKey();
                DataType type = types.get(column);
                Object actualValue = found.get(column);
                if (!ValueEquality.equal(type, cell.getValue(), actualValue, options.numericTolerance())) {
                    mine.add(Difference.value(table.asCql(), renderedKey, column,
                            format.value(type, cell.getValue()), format.value(type, actualValue)));
                    mismatched.add(column);
                }
            }
            if (mismatched.isEmpty()) {
                return;
            }
            different.append("    ").append(renderedKey).append('\n');
            int nameWidth = mismatched.stream().mapToInt(String::length).max().orElse(0);
            // Pad the expected value too, so "but was" lines up down the block and the eye can
            // scan one column rather than hunting along each line.
            int valueWidth = mismatched.stream()
                    .mapToInt(column -> format.value(types.get(column), row.values().get(column)).length())
                    .max().orElse(0);
            for (String column : mismatched) {
                DataType type = types.get(column);
                different.append("      ").append(CqlFormat.pad(column, nameWidth))
                        .append("  expected ")
                        .append(CqlFormat.pad(format.value(type, row.values().get(column)), valueWidth))
                        .append("  but was ").append(format.value(type, found.get(column)))
                        .append('\n');
            }
        }

        /**
         * Within each partition, the sequence of clustering keys the dataset lists must equal the
         * sequence Cassandra returned. Across partitions nothing is compared.
         */
        private void checkClusteringOrder(List<Difference> mine, StringBuilder different) {
            if (schema.clusteringColumns().isEmpty()) {
                return;
            }
            List<String> partitionKey = schema.partitionKey();
            List<String> clustering = schema.clusteringColumns();

            Map<List<Object>, List<String>> expectedOrder = new LinkedHashMap<>();
            for (ExpectedRow row : expected) {
                expectedOrder.computeIfAbsent(prefix(partitionKey, row.values()), k -> new ArrayList<>())
                        .add(renderPart(clustering, row.values()));
            }

            Map<List<Object>, List<String>> actualOrder = new LinkedHashMap<>();
            for (Row row : selection.rows()) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (String column : projection) {
                    values.put(column, row.getObject(TableNames.identifier(column)));
                }
                actualOrder.computeIfAbsent(prefix(partitionKey, values), k -> new ArrayList<>())
                        .add(renderPart(clustering, values));
            }

            for (Map.Entry<List<Object>, List<String>> entry : expectedOrder.entrySet()) {
                List<String> actualSequence = actualOrder.getOrDefault(entry.getKey(), List.of());
                if (actualSequence.isEmpty() || entry.getValue().equals(actualSequence)) {
                    continue;
                }
                String partition = renderKey(partitionKey, entry.getKey());
                mine.add(Difference.clusteringOrder(table.asCql(), partition,
                        String.join(", ", entry.getValue()), String.join(", ", actualSequence)));
                different.append("    out of clustering order in partition ").append(partition).append('\n')
                        .append("      expected  ").append(String.join(", ", entry.getValue())).append('\n')
                        .append("      but was   ").append(String.join(", ", actualSequence)).append('\n');
            }
        }

        private List<Object> prefix(List<String> columns, Map<String, Object> values) {
            return columns.stream().map(values::get).toList();
        }

        private String renderPart(List<String> columns, Map<String, Object> values) {
            return format.key(columns, columns.stream().map(types::get).toList(),
                    columns.stream().map(values::get).collect(Collectors.toCollection(ArrayList::new)));
        }

        private String renderKey(List<String> columns, List<Object> values) {
            return format.key(columns, columns.stream().map(types::get).toList(), values);
        }

        private List<String> nonKey(List<String> primaryKey, Collection<String> columns) {
            return columns.stream().filter(column -> !primaryKey.contains(column)).toList();
        }

        private void appendReport(StringBuilder report, List<Difference> mine, StringBuilder missing,
                                  StringBuilder unexpected, StringBuilder different) {
            long missingCount = count(mine, Difference.Kind.MISSING_ROW);
            long unexpectedCount = count(mine, Difference.Kind.UNEXPECTED_ROW);
            long valueRows = mine.stream().filter(d -> d.kind() == Difference.Kind.VALUE)
                    .map(Difference::key).distinct().count();

            List<String> summary = new ArrayList<>();
            if (missingCount > 0) {
                summary.add(missingCount + " missing");
            }
            if (unexpectedCount > 0) {
                summary.add(unexpectedCount + " unexpected");
            }
            if (valueRows > 0) {
                summary.add(valueRows + " different");
            }
            if (count(mine, Difference.Kind.CLUSTERING_ORDER) > 0) {
                summary.add(count(mine, Difference.Kind.CLUSTERING_ORDER) + " out of order");
            }

            report.append('\n').append(table.asCql())
                    .append(" - ").append(expected.size()).append(" expected, ")
                    .append(selection.rows().size()).append(" actual: ")
                    .append(String.join(", ", summary)).append('\n')
                    .append("  ").append(selection.cql()).append('\n');

            if (missing.length() > 0) {
                report.append("\n  missing (expected, not found in the database)\n").append(missing);
            }
            if (unexpected.length() > 0) {
                report.append("\n  unexpected (in the database, not in the expected dataset)\n")
                        .append(unexpected);
            }
            if (different.length() > 0) {
                report.append("\n  different\n").append(different);
            }
        }

        private long count(List<Difference> differences, Difference.Kind kind) {
            return differences.stream().filter(d -> d.kind() == kind).count();
        }
    }
}
