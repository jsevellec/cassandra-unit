package org.cassandraunit;

import com.datastax.driver.core.Session;
import org.cassandraunit.dataset.CQLDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        for (String query : dataSet.getCQLStatements()) {
            log.debug("executing : " + query);
            session.execute(query);
        }

        if (dataSet.getKeyspaceName() != null) {
            String useQuery = "use " + dataSet.getKeyspaceName();
            session.execute(useQuery);
        }
    }

    private void initKeyspaceContext(Session session, CQLDataSet dataSet) {
        String keyspaceName = DEFAULT_KEYSPACE_NAME;
        if (dataSet.getKeyspaceName() != null) {
            keyspaceName = dataSet.getKeyspaceName();
        }

        log.debug("initKeyspaceContext : " +
                "keyspaceDeletion=" + dataSet.isKeyspaceDeletion() +
                "keyspaceCreation=" + dataSet.isKeyspaceCreation() +
                ";keyspaceName=" + keyspaceName);

        if (dataSet.isKeyspaceDeletion()) {
            String dropQuery = "DROP KEYSPACE IF EXISTS " + keyspaceName;
            log.debug("executing : " + dropQuery);
            session.execute(dropQuery);
        }

        if (dataSet.isKeyspaceCreation()) {
            String createQuery = "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1} AND durable_writes = false";
            log.debug("executing : " + createQuery);
            session.execute(createQuery);
            String useQuery = "USE " + keyspaceName;
            log.debug("executing : " + useQuery);
            session.execute(useQuery);
        }

    }
}