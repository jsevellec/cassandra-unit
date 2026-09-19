package org.cassandraunit.assertion;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.cassandraunit.dataset.rows.TableRows;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a table should hold after a test has run, and the assertion that it does.
 * <p>
 * The other half of a dataset. The load direction turns a file into rows; this turns the same file
 * into an expectation — and it can be <em>literally</em> the same file, because the rules match:
 * a column absent from a row is not asserted, and a column present with {@code null} asserts the
 * column reads back as null.
 *
 * <pre>
 * ExpectedDataSetFactory.fromClassPath("rows/expected-widget.yaml", "mykeyspace")
 *         .ignoringColumns("created")
 *         .verify(session);
 * </pre>
 *
 * Immutable: every option returns a new instance, so one expectation can be shared between tests
 * and narrowed per test.
 * <p>
 * Defaults worth knowing, all argued in {@link DataSetComparator}: rows are matched on the primary
 * key, order across partitions is never compared, a named table must hold exactly the rows listed
 * ({@link MatchMode#STRICT}), and no statement this issues can contain {@code ALLOW FILTERING}.
 *
 * @author Jeremy Sevellec
 */
public final class ExpectedDataSet {

    private final RowsCQLDataSet dataSet;
    private final String keyspaceName;
    private final ExpectedDataSetOptions options;

    ExpectedDataSet(RowsCQLDataSet dataSet, String keyspaceName, ExpectedDataSetOptions options) {
        this.dataSet = dataSet;
        this.keyspaceName = keyspaceName;
        this.options = options;
    }

    private ExpectedDataSet with(ExpectedDataSetOptions newOptions) {
        return new ExpectedDataSet(dataSet, keyspaceName, newOptions);
    }

    /**
     * Columns to leave out of the comparison entirely - they are not even selected. The usual case
     * is a timestamp the code under test sets to {@code now()}.
     * <p>
     * A primary-key column cannot be ignored: it is how a row is matched.
     */
    public ExpectedDataSet ignoringColumns(String... columns) {
        Set<String> ignored = new LinkedHashSet<>(options.ignoredColumns());
        ignored.addAll(List.of(columns));
        return with(new ExpectedDataSetOptions(options.mode(), options.scope(), ignored,
                options.checkClusteringOrder(), options.numericTolerance()));
    }

    /**
     * Allow rows the dataset does not list. Off by default - see {@link MatchMode#STRICT} for why
     * strict is the right default against an upsert-only database.
     */
    public ExpectedDataSet containing() {
        return with(new ExpectedDataSetOptions(MatchMode.CONTAINS, options.scope(),
                options.ignoredColumns(), options.checkClusteringOrder(), options.numericTolerance()));
    }

    /**
     * Assert only the partitions the dataset mentions, leaving the rest of the table alone. Within
     * each named partition, strict still means exactly these rows.
     */
    public ExpectedDataSet withinMentionedPartitions() {
        return with(new ExpectedDataSetOptions(options.mode(), Scope.MENTIONED_PARTITIONS,
                options.ignoredColumns(), options.checkClusteringOrder(), options.numericTolerance()));
    }

    /**
     * Also assert that the order rows are listed in, within a partition, is the order Cassandra
     * returns them. Order across partitions is still not compared, and cannot be.
     */
    public ExpectedDataSet checkingClusteringOrder() {
        return with(new ExpectedDataSetOptions(options.mode(), options.scope(),
                options.ignoredColumns(), true, options.numericTolerance()));
    }

    /**
     * An absolute tolerance for {@code float} and {@code double}. Zero, the default, means exact -
     * which is correct for a value that round-tripped through the same codec. Set one when the
     * value being asserted was computed.
     */
    public ExpectedDataSet withNumericTolerance(double epsilon) {
        return with(new ExpectedDataSetOptions(options.mode(), options.scope(),
                options.ignoredColumns(), options.checkClusteringOrder(), epsilon));
    }

    public ExpectedDataSet withOptions(ExpectedDataSetOptions newOptions) {
        return with(newOptions);
    }

    public ExpectedDataSetOptions options() {
        return options;
    }

    /**
     * Read the tables back and compare.
     *
     * @throws DataSetMismatchError                         if the data does not match
     * @throws org.cassandraunit.dataset.ParseException     if the expectation itself is unusable -
     *                                                      an unknown column, a row missing part of
     *                                                      its primary key
     */
    public void verify(CqlSession session) {
        List<TableRows> tables = new ArrayList<>(dataSet.parse());
        new DataSetComparator(session, options, dataSet.describe()).verify(tables, keyspaceName);
    }
}
