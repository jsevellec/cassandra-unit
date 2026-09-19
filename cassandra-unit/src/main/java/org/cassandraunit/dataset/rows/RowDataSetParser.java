package org.cassandraunit.dataset.rows;

import java.io.InputStream;
import java.util.List;

/**
 * Turns a dataset's bytes into rows, without knowing anything about Cassandra.
 * <p>
 * Parsers are deliberately type-blind: they report what the file literally said, and
 * {@link RowBinder} converts it against the live schema. A parser that guessed at types would be
 * guessing without the one piece of information that settles the question.
 *
 * @author Jeremy Sevellec
 */
public interface RowDataSetParser {

    /**
     * @param in               the dataset; the caller closes it
     * @param defaultTableName table name to use for a format that cannot name its own table (CSV),
     *                         or for a hierarchical file whose root is a bare list of rows. May be
     *                         {@code null} when the format always names its tables.
     * @param origin           the source description, for error messages
     */
    List<TableRows> parse(InputStream in, String defaultTableName, String origin);
}
