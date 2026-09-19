package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.assertj.core.api.AbstractAssert;
import org.cassandraunit.dataset.ParseException;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Assertions on the columns of one row.
 *
 * @author Jeremy Sevellec
 */
public class RowAssert extends AbstractAssert<RowAssert, Row> {

    private final String table;

    RowAssert(Row actual, String table) {
        super(actual, RowAssert.class);
        this.table = table;
    }

    /**
     * Asserts one column's value.
     * <p>
     * Equality is {@link ValueEquality}, the same comparison an expected dataset makes: a
     * collection column that reads back empty matches an expected null, {@code BigDecimal} compares
     * by value rather than by scale, and a {@code ByteBuffer} is not consumed by being looked at.
     * The expected value is coerced to the column's type first, so it may be written in any of the
     * forms a row dataset accepts.
     */
    public RowAssert hasValue(String column, Object expected) {
        isNotNull();
        ColumnDefinition definition = definition(column);
        DataType type = definition.getType();
        CodecRegistry codecRegistry = actual.codecRegistry();

        Object wanted = ExpectedValue.coerce(codecRegistry, type, expected, tableName(definition), column);
        Object found = actual.getObject(column);

        if (!ValueEquality.equal(type, wanted, found, 0d)) {
            CqlFormat format = new CqlFormat(codecRegistry);
            failWithMessage("%nExpected column %s of %s to be%n  %s%nbut was%n  %s",
                    column, tableName(definition),
                    format.value(type, wanted), format.value(type, found));
        }
        return this;
    }

    /**
     * Several columns at once.
     * <p>
     * Stops at the first column that does not match, and the map decides which one that is - a
     * {@code Map.of(...)} has no defined iteration order, so with two wrong columns the one
     * reported can differ between runs. Pass a {@code LinkedHashMap}, or chain separate
     * {@link #hasValue} calls, when that matters.
     */
    public RowAssert hasValues(Map<String, Object> expected) {
        isNotNull();
        expected.forEach(this::hasValue);
        return this;
    }

    /**
     * Asserts the column reads back null.
     * <p>
     * A collection column never reads back null - the driver's codecs decode absent bytes to an
     * empty collection - so for one of those this asserts it is empty, which is the same statement
     * about the data and matches how an expected dataset reads it.
     */
    public RowAssert hasNull(String column) {
        return hasValue(column, null);
    }

    public RowAssert hasNonNull(String column) {
        isNotNull();
        definition(column);
        if (actual.isNull(column)) {
            failWithMessage("%nExpected column %s of %s to hold a value%nbut it was null",
                    column, tableName(definition(column)));
        }
        return this;
    }

    /**
     * A column the row does not have is a {@link ParseException}, not a failure: the assertion
     * cannot be about data, because there is no such column to hold any. Same rule as the
     * expected-dataset path.
     */
    private ColumnDefinition definition(String column) {
        if (!actual.getColumnDefinitions().contains(column)) {
            throw new ParseException("CqlAssertions: no column '" + column + "' in this row"
                    + (table == null ? "" : " of " + table) + ". It has: " + available() + '.');
        }
        return actual.getColumnDefinitions().get(column);
    }

    private String available() {
        return StreamSupport.stream(actual.getColumnDefinitions().spliterator(), false)
                .map(definition -> definition.getName().asInternal())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String tableName(ColumnDefinition definition) {
        return table != null ? table : definition.getTable().asInternal();
    }
}
