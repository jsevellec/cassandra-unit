package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.rows.TableNames.QualifiedTable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Which columns a table has, and which of them make up its primary key.
 * <p>
 * The primary key is what an expected row is matched on, so this has to be right. Matching by key
 * rather than by position is what makes a failure report say "one column differs on this row"
 * instead of "one row missing, one row unexpected".
 *
 * @author Jeremy Sevellec
 */
final class TableSchema {

    private final List<String> partitionKey;
    private final List<String> clusteringColumns;
    private final Set<String> allColumns;

    private TableSchema(List<String> partitionKey, List<String> clusteringColumns, Set<String> allColumns) {
        this.partitionKey = List.copyOf(partitionKey);
        this.clusteringColumns = List.copyOf(clusteringColumns);
        this.allColumns = Set.copyOf(allColumns);
    }

    List<String> partitionKey() {
        return partitionKey;
    }

    List<String> clusteringColumns() {
        return clusteringColumns;
    }

    /** Partition key then clustering columns, in order - the full primary key. */
    List<String> primaryKey() {
        List<String> all = new ArrayList<>(partitionKey);
        all.addAll(clusteringColumns);
        return all;
    }

    boolean hasColumn(String name) {
        return allColumns.contains(name);
    }

    Set<String> columns() {
        return allColumns;
    }

    /**
     * Looks the table up, preferring the driver's schema metadata and falling back to
     * {@code system_schema.columns}.
     * <p>
     * The fallback is not paranoia: a caller-supplied session may have
     * {@code advanced.metadata.schema.enabled = false}, or a {@code refreshed-keyspaces} filter that
     * excludes the keyspace under test. One {@code refreshSchema()} is tried in between, which also
     * covers metadata simply lagging a DDL another session just issued.
     * <p>
     * Note this is a lookup of one named user keyspace, not an enumeration. CONTRIBUTING.md warns
     * against using driver metadata to <em>enumerate</em> keyspaces, because the driver's default
     * {@code refreshed-keyspaces} hides system ones and made an allowlist look redundant. That
     * concern does not apply here.
     */
    static TableSchema of(CqlSession session, QualifiedTable table, String origin) {
        Optional<TableSchema> fromMetadata = fromMetadata(session, table);
        if (fromMetadata.isPresent()) {
            return fromMetadata.get();
        }
        session.refreshSchema();
        return fromMetadata(session, table)
                .orElseGet(() -> fromSystemSchema(session, table, origin));
    }

    private static Optional<TableSchema> fromMetadata(CqlSession session, QualifiedTable table) {
        // The CqlIdentifier overloads, deliberately. The String overloads go through fromCql, which
        // lowercases a bare mixed-case name, while RowBinder builds names with fromInternal. Mixing
        // the two would let a mixed-case table load correctly and then fail to be found here.
        return session.getMetadata()
                .getKeyspace(table.keyspaceId())
                .flatMap(ks -> ks.getTable(table.tableId()))
                .map(TableSchema::from);
    }

    private static TableSchema from(TableMetadata metadata) {
        List<String> partitionKey = metadata.getPartitionKey().stream()
                .map(column -> column.getName().asInternal())
                .toList();
        List<String> clustering = metadata.getClusteringColumns().keySet().stream()
                .map(column -> column.getName().asInternal())
                .toList();
        Set<String> all = new LinkedHashSet<>();
        for (ColumnMetadata column : metadata.getColumns().values()) {
            all.add(column.getName().asInternal());
        }
        return new TableSchema(partitionKey, clustering, all);
    }

    private static TableSchema fromSystemSchema(CqlSession session, QualifiedTable table, String origin) {
        List<Row> rows = session.execute(SimpleStatement.newInstance(
                        "SELECT column_name, kind, position FROM system_schema.columns"
                                + " WHERE keyspace_name = ? AND table_name = ?",
                        table.keyspace(), table.table()))
                .all();

        if (rows.isEmpty()) {
            throw new ParseException(origin + ": no table " + table.asCql()
                    + ". Check the keyspace and table names, and that the schema was loaded before"
                    + " the expectation ran.");
        }

        List<Row> partition = new ArrayList<>();
        List<Row> clustering = new ArrayList<>();
        Set<String> all = new LinkedHashSet<>();
        for (Row row : rows) {
            all.add(row.getString("column_name"));
            switch (String.valueOf(row.getString("kind"))) {
                case "partition_key" -> partition.add(row);
                case "clustering" -> clustering.add(row);
                default -> { }
            }
        }
        return new TableSchema(byPosition(partition), byPosition(clustering), all);
    }

    private static List<String> byPosition(List<Row> rows) {
        return rows.stream()
                .sorted((a, b) -> Integer.compare(a.getInt("position"), b.getInt("position")))
                .map(row -> row.getString("column_name"))
                .toList();
    }
}
