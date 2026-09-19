package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;

import java.util.List;
import java.util.Map;

/**
 * Renders values the way {@code cqlsh} would, so a failure message can be pasted into one.
 * <p>
 * Uses the driver's own {@link TypeCodec#format}, which is the exact inverse of {@code parse} and is
 * null-safe by contract, rather than {@code toString} - {@code 'text'} with quotes, {@code 0x0a0b}
 * for a blob, {@code {'a','b'}} for a set.
 *
 * @author Jeremy Sevellec
 */
final class CqlFormat {

    /** Long values are cut here. A failure message is for reading, not for dumping a blob into. */
    private static final int MAX_VALUE_LENGTH = 120;

    private final CodecRegistry codecRegistry;

    CqlFormat(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    String value(DataType type, Object value) {
        String rendered;
        try {
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            rendered = codec.format(value);
        } catch (RuntimeException e) {
            // Never let rendering a value be the reason a test fails with the wrong message.
            rendered = String.valueOf(value);
        }
        if (rendered == null) {
            rendered = "NULL";
        }
        if (rendered.length() > MAX_VALUE_LENGTH) {
            int cut = rendered.length() - MAX_VALUE_LENGTH;
            rendered = rendered.substring(0, MAX_VALUE_LENGTH) + "…(+" + cut + " chars)";
        }
        return rendered;
    }

    /** {@code day='2026-09-19', at='...'} - a row's identity, in a form that reads as a WHERE clause. */
    String key(List<String> columns, List<DataType> types, List<Object> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(columns.get(i)).append('=').append(value(types.get(i), values.get(i)));
        }
        return out.toString();
    }

    /** The non-key columns of a row, one per line, names padded so the values line up. */
    String row(String indent, List<String> columns, Map<String, DataType> types, Map<String, Object> values) {
        int width = columns.stream().mapToInt(String::length).max().orElse(0);
        StringBuilder out = new StringBuilder();
        for (String column : columns) {
            out.append(indent)
                    .append(pad(column, width))
                    .append(" = ")
                    .append(values.containsKey(column)
                            ? value(types.get(column), values.get(column))
                            : "<not asserted>")
                    .append('\n');
        }
        return out.toString();
    }

    static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }
}
