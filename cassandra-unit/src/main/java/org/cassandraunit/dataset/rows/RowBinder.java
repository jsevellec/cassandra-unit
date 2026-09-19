package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.ListType;
import com.datastax.oss.driver.api.core.type.MapType;
import com.datastax.oss.driver.api.core.type.SetType;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.cassandraunit.dataset.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes parsed rows into Cassandra, taking every column's type from the live schema.
 * <p>
 * This is the class that makes declarative datasets worth having. A row arrives as a map of column
 * name to whatever the file said - a string from CSV, an {@code Integer} from unquoted YAML - and
 * has to become a correctly typed value. Rendering CQL text and hoping is how the old XML/JSON/YAML
 * support went wrong: a {@code text} column holding {@code "1"} became {@code VALUES (1)}.
 * <p>
 * Instead the column types come from {@link PreparedStatement#getVariableDefinitions()}, which is
 * the server's own answer, and the conversion is done by the driver's {@link CodecRegistry}. That
 * gets {@code uuid}, {@code timestamp}, {@code blob}, collections and UDTs right without this
 * project owning a type system.
 *
 * @author Jeremy Sevellec
 */
final class RowBinder {

    /**
     * Types whose CQL literal form is quoted, so a raw string from CSV or XML has to be wrapped
     * before {@link TypeCodec#parse} will take it. Everything else - numerics, {@code boolean},
     * {@code uuid}, {@code blob} ({@code 0x..}), {@code duration} ({@code 1h20m}) - is unquoted.
     * <p>
     * {@code ascii} and {@code text} are in the set for completeness even though textual columns
     * never reach the parse path; {@code QuotedLiteralTypesTest} checks this set against what the
     * driver's own {@link TypeCodec#format} produces, so a driver upgrade that changed it would
     * fail loudly rather than silently.
     */
    private static final Set<DataType> QUOTED_LITERAL_TYPES = Set.of(
            DataTypes.ASCII, DataTypes.TEXT, DataTypes.TIMESTAMP, DataTypes.DATE,
            DataTypes.TIME, DataTypes.INET);

    private final CqlSession session;
    private final CodecRegistry codecRegistry;
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
            Object value = convert(type, row.get(column), table, column);
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
            String columnList = columns.stream().map(RowBinder::identifier).collect(Collectors.joining(", "));
            String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
            String cql = "INSERT INTO " + qualify(table) + " (" + columnList + ") VALUES (" + placeholders + ")";
            try {
                return session.prepare(cql);
            } catch (RuntimeException e) {
                throw new ParseException(origin + ": could not prepare " + cql, e);
            }
        });
    }

    /**
     * Qualify a table with its keyspace.
     * <p>
     * An unqualified name resolves against whatever keyspace the session happens to have current,
     * and a row dataset normally loads with {@code keyspaceCreation=false} - which means
     * {@code CQLDataLoader} issues no USE before it, so the current keyspace is whatever the
     * previous load left behind. That is issue #160, and unlike a CQL script a row dataset cannot
     * work around it by writing {@code keyspace.table} in every statement itself. Qualifying here
     * is the fix.
     * <p>
     * A table name in the file may carry its own keyspace, so one dataset can deliberately span
     * keyspaces; that wins over the dataset's.
     */
    private String qualify(String table) {
        int dot = table.indexOf('.');
        if (dot > 0) {
            return identifier(table.substring(0, dot)) + '.' + identifier(table.substring(dot + 1));
        }
        if (keyspace != null) {
            return identifier(keyspace) + '.' + identifier(table);
        }
        if (session.getKeyspace().isEmpty()) {
            throw new ParseException(origin + ": no keyspace. The dataset names none and the session "
                    + "has none selected. Give the dataset a keyspace name, or write the table as "
                    + "keyspace.table.");
        }
        return identifier(table);
    }

    /**
     * Render a table or column name, quoting only when CQL requires it - so an ordinary lowercase
     * name stays unquoted and a mixed-case one still resolves.
     */
    private static String identifier(String name) {
        return CqlIdentifier.fromInternal(name).asCql(true);
    }

    /**
     * Convert one parsed value to something the column's codec accepts. Collections and UDTs are
     * handled before the scalar ladder because {@link TypeCodec#accepts(Object)} compares raw
     * types: a {@code list<uuid>} codec accepts any {@code ArrayList}, including one still full of
     * strings, which would then fail deep inside serialization.
     */
    private Object convert(DataType type, Object value, String table, String column) {
        if (value == null) {
            return null;
        }
        try {
            if (type instanceof ListType listType) {
                List<Object> converted = new ArrayList<>();
                for (Object element : asCollection(value)) {
                    converted.add(convert(listType.getElementType(), element, table, column));
                }
                return converted;
            }
            if (type instanceof SetType setType) {
                Set<Object> converted = new LinkedHashSet<>();
                for (Object element : asCollection(value)) {
                    converted.add(convert(setType.getElementType(), element, table, column));
                }
                return converted;
            }
            if (type instanceof MapType mapType) {
                Map<Object, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : asMap(value).entrySet()) {
                    converted.put(convert(mapType.getKeyType(), entry.getKey(), table, column),
                            convert(mapType.getValueType(), entry.getValue(), table, column));
                }
                return converted;
            }
            if (type instanceof UserDefinedType userType) {
                return convertUdt(userType, value, table, column);
            }
            return convertScalar(type, value);
        } catch (ParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ParseException(origin + ": cannot convert value [" + value + "] for column "
                    + table + '.' + column + " of type " + type.asCql(true, true)
                    + (e.getMessage() == null ? "" : " - " + e.getMessage()), e);
        }
    }

    /**
     * The scalar ladder. Order matters; each rung exists for a case the previous one misses.
     */
    private Object convertScalar(DataType type, Object rawValue) {
        TypeCodec<Object> codec = codecRegistry.codecFor(type);

        // YAML resolves an unquoted 2026-09-19T10:00:00Z to a java.util.Date, which no codec
        // accepts. Normalising it here means an unquoted timestamp works, rather than failing for
        // a reason the fixture author cannot see.
        Object value = rawValue instanceof java.util.Date date ? date.toInstant() : rawValue;

        // 1. The value already has the Java type the column wants. The common case for YAML and
        //    JSON, which produce String, Long, Boolean and Double natively.
        if (codec.accepts(value)) {
            return value;
        }

        boolean textual = codec.accepts(String.class);

        // 2. A string, which is everything CSV and XML produce.
        if (value instanceof String text) {
            // For text/varchar/ascii the string IS the value. This rung is the whole point: it is
            // what stops "1" in a text column being parsed as the integer 1.
            if (textual) {
                return text;
            }
            return codec.parse(needsQuoting(type) ? quote(text) : text);
        }

        // 3. A non-string landing on a textual column - YAML `label: 1` means the text "1".
        //    Without this rung it would fall through to parse(), and a text codec rejects an
        //    unquoted literal, so the fixture would fail for writing exactly what it meant.
        //
        //    Only for values whose decimal form is exact. A float is not: YAML `label: 1.10`
        //    parses to the double 1.1, and String.valueOf gives "1.1", so the column would quietly
        //    hold something the fixture never said. There is no way to recover the intent, so say
        //    so instead of guessing.
        if (textual) {
            if (value instanceof Double || value instanceof Float || value instanceof java.math.BigDecimal) {
                throw new IllegalArgumentException("a decimal number cannot be written to a text "
                        + "column without losing its exact form (" + value + " was written as "
                        + value.getClass().getSimpleName() + "). Quote it in the dataset file.");
            }
            return String.valueOf(value);
        }

        // 4. Anything else: a number that needs widening (Integer for a bigint column), or a value
        //    whose CQL literal form is unquoted. Numeric literals are unquoted, so this is safe.
        return codec.parse(String.valueOf(value));
    }

    private Object convertUdt(UserDefinedType userType, Object value, String table, String column) {
        UdtValue udtValue = userType.newValue();
        for (Map.Entry<?, ?> entry : asMap(value).entrySet()) {
            CqlIdentifier field = CqlIdentifier.fromInternal(String.valueOf(entry.getKey()));
            int index = userType.firstIndexOf(field);
            if (index < 0) {
                throw new ParseException(origin + ": UDT " + userType.getName().asCql(true)
                        + " has no field " + field.asCql(true) + " (column " + table + '.' + column + ')');
            }
            DataType fieldType = userType.getFieldTypes().get(index);
            Object converted = convert(fieldType, entry.getValue(), table, column);
            if (converted == null) {
                udtValue = udtValue.setToNull(index);
            } else {
                TypeCodec<Object> fieldCodec = codecRegistry.codecFor(fieldType);
                udtValue = udtValue.set(index, converted, fieldCodec);
            }
        }
        return udtValue;
    }

    /**
     * A single value where a collection is expected is treated as a collection of one. CSV reaches
     * this whenever a collection column holds one element, since there is then no separator in the
     * field to split on, and it is the forgiving reading in YAML too.
     */
    private static Collection<?> asCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        return List.of(value);
    }

    private static Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalArgumentException("expected a map, got " + value.getClass().getSimpleName());
    }

    /**
     * Whether a raw string for this type has to be quoted before {@link TypeCodec#parse} will take
     * it. Package-private so {@code QuotedLiteralTypesTest} can check the answer against the
     * driver's own {@link TypeCodec#format}, rather than trusting a hand-written list.
     */
    static boolean needsQuoting(DataType type) {
        return QUOTED_LITERAL_TYPES.contains(type);
    }

    /**
     * Wrap in single quotes, doubling any the value contains - the CQL escaping convention, the
     * same one {@code SimpleCQLLexer} reads.
     */
    static String quote(String value) {
        return '\'' + value.replace("'", "''") + '\'';
    }
}
