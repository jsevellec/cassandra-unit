package org.cassandraunit.assertion;

import java.util.List;

/**
 * The database did not match the expected dataset.
 * <p>
 * An {@link AssertionError}, so every test engine reports it as a <em>failure</em> rather than an
 * error. A mistake in the dataset itself - a column that does not exist, a row missing part of its
 * primary key - throws {@link org.cassandraunit.dataset.ParseException} instead, which is reported
 * as an error. The distinction is deliberate: one means the code under test is wrong, the other
 * means the test is.
 *
 * @author Jeremy Sevellec
 */
public class DataSetMismatchError extends AssertionError {

    private final transient List<Difference> differences;

    DataSetMismatchError(String message, List<Difference> differences) {
        super(message);
        this.differences = List.copyOf(differences);
    }

    /** The differences, for reacting to a mismatch rather than reading the message. */
    public List<Difference> getDifferences() {
        return differences;
    }
}
