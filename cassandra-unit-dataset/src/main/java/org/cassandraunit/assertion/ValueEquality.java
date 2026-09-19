package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.ListType;
import com.datastax.oss.driver.api.core.type.MapType;
import com.datastax.oss.driver.api.core.type.SetType;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

/**
 * Whether an expected value and an actual one are the same value.
 * <p>
 * Mostly they just are. Both sides go through the same {@code TypeCodec} for the column's type, so
 * they land in the same Java class and {@link Object#equals} is the right comparison - which also
 * gives collections the right semantics for free, since {@code List.equals} is ordered while
 * {@code Set.equals} and {@code Map.equals} are not, exactly matching CQL's {@code list}, {@code set}
 * and {@code map}.
 * <p>
 * What follows are the places where plain {@code equals} would be wrong.
 *
 * @author Jeremy Sevellec
 */
final class ValueEquality {

    private ValueEquality() {
    }

    static boolean equal(DataType type, Object expected, Object actual, double numericTolerance) {
        // A collection column never reads back as null. SetCodec, ListCodec and MapCodec all decode
        // null or empty bytes to an empty collection, so an expected `tags: null` would fail against
        // an actual `{}` forever. Treat the two as the same value in both directions.
        if (isCollection(type)) {
            return collectionsEqual(expected, actual);
        }

        if (expected == null || actual == null) {
            return expected == actual;
        }

        // 1.5 and 1.50 are the same decimal. BigDecimal.equals is scale-sensitive and would say
        // otherwise. This is correctness, not a preference, so it is not configurable.
        if (expected instanceof BigDecimal a && actual instanceof BigDecimal b) {
            return a.compareTo(b) == 0;
        }

        // Exact by default: a fixture value round-trips through the same codec and matches exactly.
        // A tolerance is for a value the code under test computed.
        if (numericTolerance > 0 && isFloatingPoint(type)) {
            return Math.abs(((Number) expected).doubleValue() - ((Number) actual).doubleValue())
                    <= numericTolerance;
        }

        // A ByteBuffer carries a position, and reading one moves it. Compare a duplicate so a
        // consumed buffer on either side cannot produce a spurious mismatch.
        if (expected instanceof ByteBuffer a && actual instanceof ByteBuffer b) {
            return a.duplicate().equals(b.duplicate());
        }

        return expected.equals(actual);
    }

    private static boolean collectionsEqual(Object expected, Object actual) {
        Object left = emptyIfNull(expected);
        Object right = emptyIfNull(actual);
        if (left == null || right == null) {
            return left == right;
        }
        return left.equals(right);
    }

    /**
     * Normalises the null/empty pair so the comparison below sees one shape. Returns null only when
     * the value is neither null nor a collection, which the caller treats as "not equal".
     */
    private static Object emptyIfNull(Object value) {
        if (value == null) {
            return EMPTY;
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty() ? EMPTY : collection;
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty() ? EMPTY : map;
        }
        return value;
    }

    /** A single stand-in for "no elements", whatever collection kind produced it. */
    private static final Object EMPTY = new Object() {
        @Override
        public String toString() {
            return "empty";
        }
    };

    private static boolean isCollection(DataType type) {
        return type instanceof ListType || type instanceof SetType || type instanceof MapType;
    }

    private static boolean isFloatingPoint(DataType type) {
        return DataTypes.DOUBLE.equals(type) || DataTypes.FLOAT.equals(type);
    }
}
