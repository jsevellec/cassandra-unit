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

    public CqlSession getSession() {
        return session;
    }

    private final CqlSession session;

    public CQLDataLoader(CqlSession session) {
        this.session = session;
    }

    public void load(CQLDataSet dataSet) {
        initKeyspaceContext(session, dataSet);

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

    private void initKeyspaceContext(CqlSession session, CQLDataSet dataSet) {
        String keyspaceName = keyspaceNameOf(dataSet);

        log.debug("initKeyspaceContext : keyspaceDeletion={} keyspaceCreation={} ;keyspaceName={}",
                dataSet.isKeyspaceDeletion(), dataSet.isKeyspaceCreation(), keyspaceName);

        if (dataSet.isKeyspaceDeletion()) {
            dropKeyspace(session).accept(keyspaceName);
        }

        if (dataSet.isKeyspaceCreation()) {
            createKeyspace(session).accept(keyspaceName);
            use(session).accept(keyspaceName);
        }
    }
}
