package org.cassandraunit.assertion;

import java.util.Set;

/**
 * The knobs on an expected dataset, in one value so the fluent API and the annotation share a
 * representation rather than drifting apart.
 *
 * @param ignoredColumns       columns neither compared nor selected. A primary-key column cannot be
 *                             ignored - it is how rows are matched.
 * @param checkClusteringOrder whether the order rows are listed in, within one partition, must
 *                             match the order Cassandra returns them. Off by default; order across
 *                             partitions is never checked, because it is partition-token order and
 *                             means nothing to a test author.
 * @param numericTolerance     absolute tolerance for {@code float} and {@code double}. Zero means
 *                             exact, which is right for a value that round-tripped through the same
 *                             codec; a tolerance is for values the code under test computed.
 * @author Jeremy Sevellec
 */
public record ExpectedDataSetOptions(MatchMode mode, Scope scope, Set<String> ignoredColumns,
                                     boolean checkClusteringOrder, double numericTolerance) {

    public ExpectedDataSetOptions {
        ignoredColumns = Set.copyOf(ignoredColumns);
        if (numericTolerance < 0) {
            throw new IllegalArgumentException("numericTolerance must not be negative");
        }
    }

    public static ExpectedDataSetOptions defaults() {
        return new ExpectedDataSetOptions(MatchMode.STRICT, Scope.TABLE, Set.of(), false, 0d);
    }
}
