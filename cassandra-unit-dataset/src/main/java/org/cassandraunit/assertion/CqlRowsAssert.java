package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;

import java.util.List;

/**
 * Assertions on a set of rows.
 *
 * @author Jeremy Sevellec
 */
public class CqlRowsAssert extends AbstractAssert<CqlRowsAssert, List<Row>> {

    private final String description;

    CqlRowsAssert(List<Row> actual, String description) {
        super(actual, CqlRowsAssert.class);
        this.description = description;
    }

    static CqlRowsAssert of(ResultSet resultSet) {
        return new CqlRowsAssert(resultSet == null ? null : resultSet.all(), "the result set");
    }

    public CqlRowsAssert hasSize(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            failWithMessage("%nExpected %s to hold%n  %s rows%nbut it holds%n  %s",
                    description, expected, actual.size());
        }
        return this;
    }

    public CqlRowsAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("%nExpected %s to be empty%nbut it holds %s rows",
                    description, actual.size());
        }
        return this;
    }

    public CqlRowsAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("%nExpected %s to hold rows%nbut it is empty", description);
        }
        return this;
    }

    /** The first row, in whatever order the server returned them. */
    public RowAssert first() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("%nExpected %s to hold at least one row%nbut it is empty", description);
        }
        return new RowAssert(actual.get(0), description);
    }

    /** The only row. Fails if there is not exactly one. */
    public RowAssert singleRow() {
        isNotNull();
        if (actual.size() != 1) {
            failWithMessage("%nExpected %s to hold exactly one row%nbut it holds %s",
                    description, actual.size());
        }
        return new RowAssert(actual.get(0), description);
    }

    /**
     * Hands one column's values to AssertJ, so the whole of its collection vocabulary -
     * {@code containsExactlyInAnyOrder}, {@code allMatch}, and the rest - applies:
     *
     * <pre>
     * assertThat(session).keyspace("mykeyspace").table("widget").rows()
     *         .extracting("label")
     *         .containsExactlyInAnyOrder("one", "two", "three");
     * </pre>
     *
     * Values come back as the driver decoded them, not coerced, because there is no single
     * expected value here to coerce against.
     */
    public ListAssert<Object> extracting(String column) {
        isNotNull();
        return Assertions.assertThat(actual.stream()
                .map(row -> row.getObject(column))
                .toList());
    }
}
