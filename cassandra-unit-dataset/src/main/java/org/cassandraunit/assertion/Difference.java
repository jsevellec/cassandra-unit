package org.cassandraunit.assertion;

/**
 * One way in which the database did not match the expected dataset.
 * <p>
 * Available from {@link DataSetMismatchError#getDifferences()} for anyone wanting to react to a
 * mismatch programmatically rather than read the message.
 *
 * @param key      the row's primary key, rendered as CQL, or null for a difference about a whole
 *                 partition rather than a row
 * @param column   the column, for {@link Kind#VALUE}; null otherwise
 * @param expected the expected value as a CQL literal, or the expected clustering order
 * @param actual   what was there instead
 * @author Jeremy Sevellec
 */
public record Difference(Kind kind, String table, String key, String column,
                         String expected, String actual) {

    public enum Kind {
        /** The dataset lists this row; the database does not have it. */
        MISSING_ROW,
        /** The database has this row; the dataset does not list it. Only under {@link MatchMode#STRICT}. */
        UNEXPECTED_ROW,
        /** Both have the row, and one column differs. */
        VALUE,
        /** The rows of one partition came back in a different clustering order than listed. */
        CLUSTERING_ORDER
    }

    static Difference missingRow(String table, String key) {
        return new Difference(Kind.MISSING_ROW, table, key, null, null, null);
    }

    static Difference unexpectedRow(String table, String key) {
        return new Difference(Kind.UNEXPECTED_ROW, table, key, null, null, null);
    }

    static Difference value(String table, String key, String column, String expected, String actual) {
        return new Difference(Kind.VALUE, table, key, column, expected, actual);
    }

    static Difference clusteringOrder(String table, String partition, String expected, String actual) {
        return new Difference(Kind.CLUSTERING_ORDER, table, partition, null, expected, actual);
    }
}
