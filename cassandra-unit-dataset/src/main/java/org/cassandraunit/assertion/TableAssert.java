package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.assertj.core.api.AbstractAssert;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.rows.TableNames;
import org.cassandraunit.dataset.rows.TableNames.QualifiedTable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assertions on one table, and the way in to a single row.
 *
 * @author Jeremy Sevellec
 */
public class TableAssert extends AbstractAssert<TableAssert, QualifiedTable> {

    private static final String ORIGIN = "CqlAssertions";

    private final CqlSession session;
    private final CodecRegistry codecRegistry;

    TableAssert(CqlSession session, QualifiedTable actual) {
        super(actual, TableAssert.class);
        this.session = session;
        this.codecRegistry = session.getContext().getCodecRegistry();
    }

    public TableAssert hasRowCount(long expected) {
        isNotNull();
        long found = rowCount();
        if (found != expected) {
            failWithMessage("%nExpected %s to hold%n  %s rows%nbut it holds%n  %s",
                    actual.asCql(), expected, found);
        }
        return this;
    }

    public TableAssert isEmpty() {
        isNotNull();
        long found = rowCount();
        if (found != 0) {
            failWithMessage("%nExpected %s to be empty%nbut it holds %s rows",
                    actual.asCql(), found);
        }
        return this;
    }

    public TableAssert isNotEmpty() {
        isNotNull();
        if (rowCount() == 0) {
            failWithMessage("%nExpected %s to hold rows%nbut it is empty", actual.asCql());
        }
        return this;
    }

    /**
     * Narrows to the row with this primary key, for a table whose primary key is a single column.
     * <p>
     * A table with a compound key is rejected here rather than quietly matching whatever the
     * partial key happens to return: use {@link #row(Map)} and give the whole key.
     */
    public RowAssert row(String keyColumn, Object keyValue) {
        isNotNull();
        List<String> primaryKey = schema().primaryKey();
        if (primaryKey.size() != 1 || !primaryKey.get(0).equals(keyColumn)) {
            throw new ParseException(ORIGIN + ": the primary key of " + actual.asCql() + " is ("
                    + String.join(", ", primaryKey) + "), so it cannot be addressed by '"
                    + keyColumn + "' alone. Use row(Map.of(...)) with every key column.");
        }
        Map<String, Object> key = new LinkedHashMap<>();
        key.put(keyColumn, keyValue);
        return row(key);
    }

    /** Narrows to the row with this primary key. Every primary-key column must be given. */
    public RowAssert row(Map<String, Object> key) {
        isNotNull();
        Row found = selectByKey(key);
        if (found == null) {
            failWithMessage("%nExpected %s to hold a row with%n  %s%nbut it holds no such row",
                    actual.asCql(), describe(key));
        }
        return new RowAssert(found, actual.asCql());
    }

    public TableAssert hasNoRow(String keyColumn, Object keyValue) {
        Map<String, Object> key = new LinkedHashMap<>();
        key.put(keyColumn, keyValue);
        return hasNoRow(key);
    }

    public TableAssert hasNoRow(Map<String, Object> key) {
        isNotNull();
        if (selectByKey(key) != null) {
            failWithMessage("%nExpected %s to hold no row with%n  %s%nbut it does",
                    actual.asCql(), describe(key));
        }
        return this;
    }

    /**
     * Every row in the table, for assertions on the set as a whole.
     * <p>
     * Capped at {@link DataSetComparator#MAX_ROWS}, the same ceiling the dataset comparison uses -
     * an assertion pointed at a production-sized table should say so rather than pull it into
     * memory.
     */
    public CqlRowsAssert rows() {
        isNotNull();
        List<Row> rows = session.execute(SimpleStatement.newInstance(
                        "SELECT * FROM " + actual.asCql() + " LIMIT " + (DataSetComparator.MAX_ROWS + 1)))
                .all();
        if (rows.size() > DataSetComparator.MAX_ROWS) {
            throw new ParseException(ORIGIN + ": " + actual.asCql() + " holds more than "
                    + DataSetComparator.MAX_ROWS + " rows, which is more than an assertion should be"
                    + " pulling into memory. Assert on a single row with row(...), or on the count"
                    + " with hasRowCount(...).");
        }
        return new CqlRowsAssert(rows, actual.asCql());
    }

    private long rowCount() {
        // count(*) without a partition key makes the server log "Aggregation query used without
        // partition key". That is expected here and harmless at test-fixture sizes; the alternative
        // is pulling every key back to count them client-side, which is worse.
        Row row = session.execute(
                SimpleStatement.newInstance("SELECT count(*) FROM " + actual.asCql())).one();
        return row == null ? 0 : row.getLong(0);
    }

    private TableSchema schema() {
        return TableSchema.of(session, actual, ORIGIN);
    }

    private Row selectByKey(Map<String, Object> key) {
        TableSchema schema = schema();
        List<String> primaryKey = schema.primaryKey();
        requireWholeKey(primaryKey, key);

        String where = primaryKey.stream()
                .map(column -> TableNames.identifier(column) + " = ?")
                .collect(Collectors.joining(" AND "));
        PreparedStatement statement = session.prepare(
                "SELECT * FROM " + actual.asCql() + " WHERE " + where);

        ColumnDefinitions variables = statement.getVariableDefinitions();
        BoundStatementBuilder bound = statement.boundStatementBuilder();
        for (int i = 0; i < primaryKey.size(); i++) {
            String column = primaryKey.get(i);
            DataType type = variables.get(i).getType();
            bound = bound.set(i,
                    ExpectedValue.coerce(codecRegistry, type, key.get(column), actual.table(), column),
                    codecRegistry.codecFor(type));
        }
        return session.execute(bound.build()).one();
    }

    private void requireWholeKey(List<String> primaryKey, Map<String, Object> key) {
        List<String> missing = primaryKey.stream().filter(column -> !key.containsKey(column)).toList();
        if (!missing.isEmpty()) {
            throw new ParseException(ORIGIN + ": the key given for " + actual.asCql()
                    + " does not set " + String.join(", ", missing)
                    + ". A row is addressed by its whole primary key ("
                    + String.join(", ", primaryKey) + ").");
        }
        List<String> unknown = key.keySet().stream().filter(column -> !primaryKey.contains(column)).toList();
        if (!unknown.isEmpty()) {
            throw new ParseException(ORIGIN + ": " + String.join(", ", unknown)
                    + " is not part of the primary key of " + actual.asCql() + " ("
                    + String.join(", ", primaryKey) + "). Address the row by its key, then assert on"
                    + " the other columns with hasValue(...).");
        }
        // Same rule the expected-dataset path enforces: Cassandra permits no null in a primary-key
        // column, so a key containing one could never match a row, in either direction.
        List<String> nulls = primaryKey.stream().filter(column -> key.get(column) == null).toList();
        if (!nulls.isEmpty()) {
            throw new ParseException(ORIGIN + ": the key given for " + actual.asCql() + " sets "
                    + String.join(", ", nulls) + " to null. No primary-key column can be null in"
                    + " Cassandra, so no row can ever match it.");
        }
    }

    private String describe(Map<String, Object> key) {
        List<String> parts = new ArrayList<>();
        key.forEach((column, value) -> parts.add(column + '=' + value));
        return String.join(", ", parts);
    }
}
