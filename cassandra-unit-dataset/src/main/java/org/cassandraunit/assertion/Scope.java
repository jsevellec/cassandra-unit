package org.cassandraunit.assertion;

/**
 * How much of a table an expected dataset reads back.
 * <p>
 * Neither option can produce {@code ALLOW FILTERING}: one restricts nothing, the other restricts by
 * a complete partition key. There is deliberately no user-supplied {@code WHERE} in this version,
 * because that is what would open the door to it.
 *
 * @author Jeremy Sevellec
 */
public enum Scope {

    /**
     * One unrestricted {@code SELECT} per table. The default, and the right one for the small
     * tables a test fixture builds.
     */
    TABLE,

    /**
     * One {@code SELECT} per distinct partition key appearing in the expected rows, each restricted
     * by the complete partition key. Rows in partitions the dataset does not mention are ignored
     * even under {@link MatchMode#STRICT}.
     * <p>
     * For asserting one partition of a table that holds more than the test put there.
     */
    MENTIONED_PARTITIONS
}
