package org.cassandraunit.assertion;

/**
 * How much of what is in the database an expected dataset has to account for.
 *
 * @author Jeremy Sevellec
 */
public enum MatchMode {

    /**
     * Within the asserted scope, a table named by the expected dataset must contain exactly the
     * rows it lists - no extras. The default.
     * <p>
     * Cassandra is upsert-only and has no unique constraints, so the mistake an integration test
     * most needs to catch is a write landing in the wrong partition or under the wrong clustering
     * key. That produces an extra row and is invisible to {@link #CONTAINS}.
     */
    STRICT,

    /**
     * Every listed row must be there; anything else in the table is ignored. For suites that
     * pre-seed reference data the fixture does not describe.
     */
    CONTAINS
}
