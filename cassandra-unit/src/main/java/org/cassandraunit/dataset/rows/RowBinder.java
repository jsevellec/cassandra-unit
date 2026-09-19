package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.cassandraunit.dataset.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes parsed rows into Cassandra, taking every column's type from the live schema.
 * <p>
 * The column types come from {@link PreparedStatement#getVariableDefinitions()}, which is the
 * server's own answer, and {@link RowValueConverter} turns each parsed value into something that
 * type's codec accepts. That gets {@code uuid}, {@code timestamp}, {@code blob}, collections and
 * UDTs right without this project owning a type system.
 *
 * @author Jeremy Sevellec
 */
final class RowBinder {

    private final CqlSession session;
    private final CodecRegistry codecRegistry;
    private final RowValueConverter converter;
    private final String keyspace;
    private final String origin;

    /**
     * One prepared statement per (table, column set). Rows in the same file routinely differ in
     * which columns they set, and each shape needs its own statement - sharing one across shapes
     * would bind values to the wrong positions. Caching is not only a speed-up: preparing per row
     * would be a round trip per row.
     */
    private final Map<String, PreparedStatement> prepared = new HashMap<>();

    RowBinder(CqlSession session, String keyspace, String origin) {
        this.session = session;
        this.codecRegistry = session.getContext().getCodecRegistry();
        this.converter = new RowValueConverter(this.codecRegistry, origin);
        this.keyspace = keyspace;
        this.origin = origin;
    }

    void insertAll(List<TableRows> tables) {
        for (TableRows table : tables) {
            for (Map<String, Object> row : table.rows()) {
                insert(table.table(), row);
            }
        }
    }

    private void insert(String table, Map<String, Object> row) {
        if (row.isEmpty()) {
            return;
        }
        // Sorted, so two rows with the same columns in a different order share a prepared
        // statement, and so the INSERT's column order always matches the bind order below.
        List<String> columns = row.keySet().stream().sorted().collect(Collectors.toList());
        PreparedStatement statement = prepare(table, columns);

        ColumnDefinitions variables = statement.getVariableDefinitions();
        BoundStatementBuilder builder = statement.boundStatementBuilder();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            DataType type = variables.get(i).getType();
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            Object value = converter.convert(type, row.get(column), table, column);
            if (value == null) {
                builder = builder.setToNull(i);
            } else {
                builder = builder.set(i, value, codec);
            }
        }
        session.execute(builder.build());
    }

    private PreparedStatement prepare(String table, List<String> columns) {
        String key = table + '(' + String.join(",", columns) + ')';
        return prepared.computeIfAbsent(key, ignored -> {
            String columnList = columns.stream().map(TableNames::identifier).collect(Collectors.joining(", "));
            String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
            String cql = "INSERT INTO " + qualify(table) + " (" + columnList + ") VALUES (" + placeholders + ")";
            try {
                return session.prepare(cql);
            } catch (RuntimeException e) {
                throw new ParseException(origin + ": could not prepare " + cql, e);
            }
        });
    }

    private String qualify(String table) {
        CqlIdentifier sessionKeyspace = session.getKeyspace().orElse(null);
        return TableNames.resolve(table, keyspace, sessionKeyspace, origin).asCql();
    }
}
