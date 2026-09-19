package org.cassandraunit.assertion;

import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.rows.RowsCQLDataSet;

/**
 * Builds an {@link ExpectedDataSet} from a file, mirroring {@link CQLDataSetFactory} so the two
 * directions read the same way.
 * <p>
 * The format comes from the extension, exactly as for loading: {@code .yaml}, {@code .yml},
 * {@code .json}, {@code .xml}, {@code .csv}. A {@code .cql} script is rejected — it is a list of
 * statements, and an expectation has to describe rows in order to compare them.
 * <p>
 * Note that CSV cannot express an explicit null (an empty field means "not asserted"), so a CSV
 * expectation can never assert that a column reads back as null.
 *
 * @author Jeremy Sevellec
 */
public final class ExpectedDataSetFactory {

    private ExpectedDataSetFactory() {
    }

    public static ExpectedDataSet fromClassPath(String location) {
        return fromClassPath(location, null);
    }

    public static ExpectedDataSet fromClassPath(String location, String keyspaceName) {
        return of(CQLDataSetFactory.rowsFromClassPath(location, keyspaceName), keyspaceName);
    }

    public static ExpectedDataSet fromFile(String location) {
        return fromFile(location, null);
    }

    public static ExpectedDataSet fromFile(String location, String keyspaceName) {
        return of(CQLDataSetFactory.rowsFromFile(location, keyspaceName), keyspaceName);
    }

    /**
     * Wraps a row dataset you already have — including, deliberately, the very one a test loaded
     * from, so the same file can state the setup and the expectation.
     */
    public static ExpectedDataSet of(RowsCQLDataSet dataSet, String keyspaceName) {
        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet must not be null");
        }
        return new ExpectedDataSet(dataSet, keyspaceName, ExpectedDataSetOptions.defaults());
    }
}
