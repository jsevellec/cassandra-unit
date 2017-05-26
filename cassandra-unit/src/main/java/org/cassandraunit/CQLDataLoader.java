package org.cassandraunit;

import com.datastax.driver.core.Session;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.utils.CqlOperations;
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

    public Session getSession() {
        return session;
    }

    private final Session session;

    public CQLDataLoader(Session session) {
        this.session = session;
    }

    public void load(CQLDataSet dataSet) {
        initKeyspaceContext(session, dataSet);

        log.debug("loading data");
        dataSet.getCQLStatements().stream()
                .forEach(execute(session));

        if (dataSet.getKeyspaceName() != null) {
            use(session).accept(dataSet.getKeyspaceName());
        }
    }

    private void initKeyspaceContext(Session session, CQLDataSet dataSet) {
        String keyspaceName = DEFAULT_KEYSPACE_NAME;
        if (dataSet.getKeyspaceName() != null) {
            keyspaceName = dataSet.getKeyspaceName();
        }

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