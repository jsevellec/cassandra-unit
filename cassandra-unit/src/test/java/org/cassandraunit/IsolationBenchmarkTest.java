package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.cassandraunit.utils.CqlOperations;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measures {@link CQLDataLoader.Isolation#DATASET} against
 * {@link CQLDataLoader.Isolation#TRUNCATE} as the number of tables in a keyspace grows.
 * <p>
 * Off unless asked for, because it takes minutes and its output is a table of numbers rather than
 * a pass or a fail:
 *
 * <pre>
 * mvn -pl cassandra-unit -am test -Dtest=IsolationBenchmarkTest \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dcassandraunit.benchmark=true
 * </pre>
 *
 * <b>Both arms replay the schema.</b> DATASET drops the keyspace, so every test re-runs every
 * {@code CREATE TABLE}; TRUNCATE does not. Timing "drop and create a keyspace" against "truncate N
 * tables" without the DDL would leave DATASET looking nearly free at any table count, and the
 * conclusion would be an artifact of the harness. Loading rows is common to both and so is left
 * out of both - but the tables are <b>seeded before every timed cycle</b>, because what TRUNCATE
 * costs depends on there being something to remove. Truncating empty tables measures nothing.
 * <p>
 * The numbers are a <b>lower bound</b> on TRUNCATE's advantage. This is one embedded node, where
 * schema agreement has no peers to wait for - the friendliest case DATASET will ever get.
 * <p>
 * {@code auto_snapshot} is asserted below rather than assumed, because a snapshot per truncate
 * would drown the measurement. It is not, however, a thumb on the scale for either mode: Cassandra
 * gates on it both the snapshot taken when a table is truncated and the one taken when a table is
 * dropped, so with it on, the DATASET arm would pay it too.
 */
@EnabledIfSystemProperty(named = "cassandraunit.benchmark", matches = "true")
class IsolationBenchmarkTest {

    private static final int[] TABLE_COUNTS = {2, 10, 50};
    private static final int ROWS_PER_TABLE = 20;
    private static final int WARMUP = 3;
    private static final int MEASURED = 10;

    private static CqlSession session;

    @BeforeAll
    static void startServer() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        session = EmbeddedCassandraServerHelper.getSession();
    }

    @Test
    void compareIsolationModes() {
        // Not an assumption: with snapshots on, every TRUNCATE writes one and the whole comparison
        // measures the filesystem instead.
        assertThat(DatabaseDescriptor.isAutoSnapshot())
                .as("auto_snapshot must be false or these numbers mean nothing")
                .isFalse();

        System.out.println();
        System.out.printf("%8s  %14s  %14s  %10s%n",
                "tables", "DATASET (ms)", "TRUNCATE (ms)", "speedup");
        System.out.println("  ".repeat(26));

        for (int tables : TABLE_COUNTS) {
            String keyspace = "benchmarkkeyspace" + tables;
            List<String> ddl = ddl(keyspace, tables);
            Runnable create = () -> {
                CqlOperations.createKeyspace(session).accept(keyspace);
                ddl.forEach(CqlOperations.execute(session));
            };

            CqlOperations.dropKeyspace(session).accept(keyspace);
            create.run();

            double dataset = median(() -> seed(keyspace, tables), () -> {
                CqlOperations.dropKeyspace(session).accept(keyspace);
                create.run();
            });

            // Both arms end in the same state - schema present, tables empty - so the next cycle's
            // seeding has somewhere to go either way.
            double truncate = median(() -> seed(keyspace, tables),
                    () -> CqlOperations.truncateKeyspace(session, keyspace));

            System.out.printf("%8d  %14.1f  %14.1f  %9.2fx%n",
                    tables, dataset, truncate, dataset / truncate);

            CqlOperations.dropKeyspace(session).accept(keyspace);
        }
        System.out.println();
    }

    private static List<String> ddl(String keyspace, int tables) {
        List<String> statements = new ArrayList<>(tables);
        for (int i = 0; i < tables; i++) {
            statements.add("CREATE TABLE " + keyspace + ".widget" + i
                    + " (id uuid PRIMARY KEY, label text, quantity bigint)");
        }
        return statements;
    }

    /** One batch per table, so seeding costs table-count round trips rather than row-count. */
    private static void seed(String keyspace, int tables) {
        for (int t = 0; t < tables; t++) {
            PreparedStatement insert = session.prepare("INSERT INTO " + keyspace + ".widget" + t
                    + " (id, label, quantity) VALUES (?, ?, ?)");
            BatchStatementBuilder batch = BatchStatement.builder(BatchType.UNLOGGED);
            for (int r = 0; r < ROWS_PER_TABLE; r++) {
                batch.addStatement(insert.bind(java.util.UUID.randomUUID(), "row" + r, (long) r));
            }
            session.execute(batch.build());
        }
    }

    /**
     * Median rather than mean: one slow compaction or GC pause should not move the answer. Only
     * {@code cycle} is timed; {@code setUp} puts the rows back first.
     */
    private static double median(Runnable setUp, Runnable cycle) {
        for (int i = 0; i < WARMUP; i++) {
            setUp.run();
            cycle.run();
        }
        List<Double> timings = new ArrayList<>(MEASURED);
        for (int i = 0; i < MEASURED; i++) {
            setUp.run();
            long start = System.nanoTime();
            cycle.run();
            timings.add((System.nanoTime() - start) / 1_000_000d);
        }
        Collections.sort(timings);
        int mid = timings.size() / 2;
        return timings.size() % 2 == 0
                ? (timings.get(mid - 1) + timings.get(mid)) / 2
                : timings.get(mid);
    }
}
