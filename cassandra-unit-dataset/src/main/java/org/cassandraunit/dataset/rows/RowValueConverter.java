package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a parsed value into something a column's codec accepts, given that column's type.
 * <p>
 * This is the part of declarative datasets that makes them worth having. A value arrives as
 * whatever the file said - a string from CSV, an {@code Integer} from unquoted YAML - and has to
 * become a correctly typed value. Rendering CQL text and hoping is how the old XML/JSON/YAML
 * support went wrong: a {@code text} column holding {@code "1"} became {@code VALUES (1)}.
 * <p>
 * The conversion is driven by a {@link DataType} the caller supplies and performed by the driver's
 * {@link CodecRegistry}, so {@code uuid}, {@code timestamp}, {@code blob}, collections and UDTs are
 * handled without this project owning a type system.
 * <p>
 * It takes a {@link CodecRegistry} rather than a {@link CqlSession} because it never needed the
 * session - only the registry. That keeps it usable without a running node, which is how
 * {@code RowValueConverterTest} and {@code QuotedLiteralTypesTest} exercise it, and it means the
 * same ladder serves both directions: the load path sources its {@code DataType} from
 * {@link com.datastax.oss.driver.api.core.cql.PreparedStatement#getVariableDefinitions()}, and a
 * read-back path would source it from
 * {@link com.datastax.oss.driver.api.core.cql.ResultSet#getColumnDefinitions()}. Both are
 * {@code ColumnDefinitions}; the seam between them is just {@code DataType}.
 *
 * @author Jeremy Sevellec
 */
public final class RowValueConverter {

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

    private final CodecRegistry codecRegistry;
    private final String origin;

    /**
     * @param origin how to describe the dataset in an error message, e.g. {@code classpath:rows/widget.yaml}
     */
    public RowValueConverter(CodecRegistry codecRegistry, String origin) {
        this.codecRegistry = codecRegistry;
        this.origin = origin;
    }

    public static RowValueConverter forSession(CqlSession session, String origin) {
        return new RowValueConverter(session.getContext().getCodecRegistry(), origin);
    }

    /**
     * Convert one parsed value to something the column's codec accepts. Collections and UDTs are
     * handled before the scalar ladder because {@link TypeCodec#accepts(Object)} compares raw
     * types: a {@code list<uuid>} codec accepts any {@code ArrayList}, including one still full of
     * strings, which would then fail deep inside serialization.
     *
     * @param type the column's type, from {@code ColumnDefinitions} or {@code ColumnMetadata}
     */
    public Object convert(DataType type, Object value, String table, String column) {
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
     * it. Public so {@code QuotedLiteralTypesTest} can check the answer against the driver's own
     * {@link TypeCodec#format}, rather than trusting a hand-written list.
     */
    public static boolean needsQuoting(DataType type) {
        return QUOTED_LITERAL_TYPES.contains(type);
    }

    /**
     * Wrap in single quotes, doubling any the value contains - the CQL escaping convention, the
     * same one {@code SimpleCQLLexer} reads.
     */
    public static String quote(String value) {
        return '\'' + value.replace("'", "''") + '\'';
    }
}
