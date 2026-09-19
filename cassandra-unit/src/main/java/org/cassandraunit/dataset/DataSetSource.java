package org.cassandraunit.dataset;

import java.io.InputStream;

/**
 * Where a dataset's bytes come from, independent of the format those bytes are in.
 * <p>
 * Source (classpath, filesystem) and format (cql, yaml, json, xml, csv) are independent axes. Kept
 * as separate types they compose; folded into a class hierarchy they would multiply, and every new
 * format would mean a new class per source.
 *
 * @author Jeremy Sevellec
 */
public interface DataSetSource {

    /**
     * Open the dataset, or return {@code null} if it does not exist. Callers close it.
     */
    InputStream open();

    /**
     * How to name this source in an error message, e.g. {@code classpath:cql/simple.cql}. Every
     * {@link ParseException} raised while reading or parsing a dataset is prefixed with this, so a
     * malformed fixture says which file it was.
     */
    String describe();
}
