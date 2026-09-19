package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.SessionAwareDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cassandraunit.utils.CqlOperations.*;

/**
 * @author Marcin Szymaniuk
 * @author Jeremy Sevellec
 */
public class CQLDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CQLDataLoader.class);
    public static final String DEFAULT_KEYSPACE_NAME = "cassandraunitkeyspace";

    /**
     * How a load clears whatever the previous test left behind.
     * <p>
     * The mode belongs here rather than on the dataset: a {@code .cql} script interleaves DDL and
     * DML, so nothing reading a dataset can tell which of its statements are schema and which are
     * data, and a decorator would have to guess.
     */
    public enum Isolation {

        /**
         * Honour the dataset's own {@code isKeyspaceDeletion} / {@code isKeyspaceCreation} flags:
         * normally drop the keyspace and create it again. The default, and what every release
         * before this one did.
         */
        DATASET,

        /**
         * Keep the keyspace and its schema, and empty every table in it instead. The keyspace is
         * created first if it is not there, so the first test in a run behaves like any other.
         * <p>
         * The dataset's creation and deletion flags are ignored, deliberately - asking for this
         * mode is asking for the keyspace to survive.
         * <p>
         * The dataset must then not re-create the schema it is loading into. Either pair this with
         * {@link #loadIfKeyspaceAbsent} for the schema and keep the per-test dataset to rows, or
         * write {@code CREATE TABLE IF NOT EXISTS} in the script.
         * <p>
         * Measured on the embedded server this is dramatically faster at every keyspace size -
         * see {@code docs/datasets.md} for the numbers and for how to reproduce them. It is not
         * the default because it is not a drop-in, not because of the cost: a dataset that creates
         * its own schema breaks under it.
         * <p>
         * The keyspace it creates when one is missing uses the same {@code SimpleStrategy},
         * replication factor 1 defaults as {@link #DATASET} mode does.
         */
        TRUNCATE,

        /**
         * Clear nothing. For a suite that manages its own state, or a dataset meant to accumulate
         * across tests. The keyspace is selected if it exists, so an unqualified statement still
         * lands where it should.
         */
        NONE
    }

    public CqlSession getSession() {
        return session;
    }

    private final CqlSession session;

    public CQLDataLoader(CqlSession session) {
        this.session = session;
    }

    /** Equivalent to {@code load(dataSet, Isolation.DATASET)}. */
    public void load(CQLDataSet dataSet) {
        load(dataSet, Isolation.DATASET);
    }

    public void load(CQLDataSet dataSet, Isolation isolation) {
        initKeyspaceContext(session, dataSet, isolation);

        log.debug("loading data");
        if (dataSet instanceof SessionAwareDataSet sessionAware) {
            // A row dataset (yaml/json/csv/xml) renders nothing to text: it reads the column types
            // from the schema this session can see and binds prepared statements itself.
            sessionAware.load(session);
        } else {
            dataSet.getCQLStatements().stream()
                    .forEach(execute(session));
        }

        if (dataSet.getKeyspaceName() != null) {
            use(session).accept(dataSet.getKeyspaceName());
        }
    }

    /**
     * Loads a dataset only if its keyspace does not already exist, and reports whether it did.
     * <p>
     * For schema that is expensive to build and identical for every test: create it once, then
     * load only rows per test. The check asks the server ({@code system_schema.keyspaces}) rather
     * than remembering in a field, which is the whole point - several test classes routinely share
     * one JVM and one session, so a field would say "not loaded" for each of them in turn and the
     * schema would be rebuilt anyway.
     *
     * @return true if the dataset was loaded, false if the keyspace was already there
     */
    public boolean loadIfKeyspaceAbsent(CQLDataSet dataSet) {
        String keyspaceName = keyspaceNameOf(dataSet);
        if (keyspaceExists(keyspaceName)) {
            log.debug("keyspace {} already exists, skipping {}", keyspaceName, dataSet);
            use(session).accept(keyspaceName);
            return false;
        }
        load(dataSet);
        return true;
    }

    private boolean keyspaceExists(String keyspaceName) {
        return session.execute(SimpleStatement.newInstance(
                        "SELECT keyspace_name FROM system_schema.keyspaces WHERE keyspace_name = ?",
                        keyspaceName))
                .one() != null;
    }

    private static String keyspaceNameOf(CQLDataSet dataSet) {
        return dataSet.getKeyspaceName() != null ? dataSet.getKeyspaceName() : DEFAULT_KEYSPACE_NAME;
    }

    private void initKeyspaceContext(CqlSession session, CQLDataSet dataSet, Isolation isolation) {
        String keyspaceName = keyspaceNameOf(dataSet);

        log.debug("initKeyspaceContext : isolation={} keyspaceDeletion={} keyspaceCreation={}"
                        + " ;keyspaceName={}", isolation,
                dataSet.isKeyspaceDeletion(), dataSet.isKeyspaceCreation(), keyspaceName);

        switch (isolation) {
            case DATASET -> {
                if (dataSet.isKeyspaceDeletion()) {
                    dropKeyspace(session).accept(keyspaceName);
                }
                if (dataSet.isKeyspaceCreation()) {
                    createKeyspace(session).accept(keyspaceName);
                    use(session).accept(keyspaceName);
                }
            }
            case TRUNCATE -> {
                createKeyspace(session).accept(keyspaceName);
                use(session).accept(keyspaceName);
                truncateKeyspace(session, keyspaceName);
            }
            case NONE -> {
                // Selecting the keyspace is not clearing anything, and without it a .cql script
                // that writes unqualified table names would land wherever the last load left the
                // session pointing.
                if (keyspaceExists(keyspaceName)) {
                    use(session).accept(keyspaceName);
                }
            }
        }
    }
}
