package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

/**
 * Fluent assertions on what a Cassandra actually holds, written in code rather than in a fixture
 * file.
 *
 * <pre>
 * import static org.cassandraunit.assertion.CqlAssertions.assertThat;
 *
 * assertThat(session).keyspace("mykeyspace")
 *         .table("widget")
 *             .hasRowCount(3)
 *             .row("id", widgetId)
 *                 .hasValue("label", "one")
 *                 .hasNull("created");
 * </pre>
 *
 * The companion to {@link ExpectedCassandraDataSet}, not a replacement for it. A dataset file is
 * the right tool for "these are all the rows this table should hold"; this is the right tool for
 * one value, or one row count, where writing a file would be out of proportion.
 * <p>
 * Everything here agrees with the dataset comparison on what equal means - both go through
 * {@link ValueEquality}, so a {@code set} column reading back empty rather than null, or
 * {@code 1.50} against {@code 1.5}, behave identically either way. Expected values accept the same
 * forms a row dataset accepts, so {@code hasValue("quantity", 42)} works against a {@code bigint}
 * and {@code hasValue("id", "1690e8da-…")} against a {@code uuid}.
 * <p>
 * <b>Needs {@code assertj-core} on the test classpath.</b> It is declared {@code optional} here, so
 * it reaches nobody who does not ask for it, and nothing else in this library requires it. Without
 * it, touching this class raises {@code NoClassDefFoundError:
 * org/assertj/core/api/AbstractAssert}. Every assert type below extends AssertJ's
 * {@code AbstractAssert}, so {@code as()}, {@code describedAs()}, {@code satisfies()} and
 * {@code SoftAssertions} all work as usual.
 * <p>
 * The overloads take driver types, so this can be statically imported alongside
 * {@code org.assertj.core.api.Assertions.*} without ambiguity.
 *
 * @author Jeremy Sevellec
 */
public final class CqlAssertions {

    private CqlAssertions() {
    }

    /** Entry point for assertions that run their own queries. */
    public static CqlSessionAssert assertThat(CqlSession session) {
        return new CqlSessionAssert(session);
    }

    /** Assertions on a row you already fetched. */
    public static RowAssert assertThat(Row row) {
        return new RowAssert(row, null);
    }

    /**
     * Assertions on a result set you already fetched.
     * <p>
     * This consumes the result set - {@code ResultSet} is a one-pass cursor, so the rows are read
     * once here and held. Do not iterate it yourself afterwards.
     */
    public static CqlRowsAssert assertThat(ResultSet resultSet) {
        return CqlRowsAssert.of(resultSet);
    }
}
