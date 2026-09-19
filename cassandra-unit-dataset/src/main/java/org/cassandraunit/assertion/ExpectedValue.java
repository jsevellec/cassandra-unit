package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.cassandraunit.dataset.rows.RowValueConverter;

/**
 * Turns a value written in a test into the Java type the column's codec speaks.
 * <p>
 * Without this, {@code hasValue("quantity", 42)} against a {@code bigint} column compares an
 * {@code Integer} to a {@code Long} and fails for a reason that looks like a library bug. The
 * conversion is {@link RowValueConverter}, the same ladder a row dataset uses, so a value written
 * in a fluent assertion and the same value written in a YAML fixture are accepted on identical
 * terms - a {@code uuid} may be given as its string form, a {@code timestamp} as an ISO instant,
 * and so on.
 * <p>
 * A value the codec already accepts is passed through untouched, so handing over a real
 * {@code UUID} or {@code Instant} costs nothing and cannot be mangled.
 *
 * @author Jeremy Sevellec
 */
final class ExpectedValue {

    private ExpectedValue() {
    }

    static Object coerce(CodecRegistry codecRegistry, DataType type, Object value,
                         String table, String column) {
        if (value == null) {
            return null;
        }
        TypeCodec<Object> codec = codecRegistry.codecFor(type);
        if (codec.accepts(value)) {
            return value;
        }
        return new RowValueConverter(codecRegistry, "CqlAssertions").convert(type, value, table, column);
    }
}
