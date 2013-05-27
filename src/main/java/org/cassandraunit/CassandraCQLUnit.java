package org.cassandraunit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Marcin Szymaniuk
 */
public class CassandraCQLUnit extends BaseCassandraUnit {
    public String cqlFile;
    public static String keyspaceName = "testkeyspace";

    private static final Logger log = LoggerFactory.getLogger(CassandraCQLUnit.class);
    private String hostIp = "127.0.0.1";
    private int port = 9031;

    public CassandraCQLUnit(String cqlFile, String keyspaceName) {
        this.keyspaceName = keyspaceName;
        this.cqlFile = cqlFile;
    }


    public CassandraCQLUnit(String cqlFile, String keyspaceName, String hostIp, int port) {
        this.keyspaceName = keyspaceName;
        this.cqlFile = cqlFile;
        this.hostIp=hostIp;
        this.port=port;
    }

    protected void load() {
        Session session = null;
        try {
            CQLDataLoader dataLoader = new CQLDataLoader(keyspaceName);
            session = createSession();
            recreateKeyspace(session);

            session.execute("use " + keyspaceName);
            dataLoader.load(cqlFile, session);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.shutdown();
            }
        }
    }

    private void recreateKeyspace(Session session) {

        ResultSet keyspaceQueryResult =
                session.execute("SELECT keyspace_name from system.schema_keyspaces where keyspace_name='" + keyspaceName + "'");
        if(keyspaceQueryResult.iterator().hasNext()){
            session.execute("DROP KEYSPACE "+keyspaceName);
        }
        session.execute("CREATE KEYSPACE " + keyspaceName + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}");
    }

    public Session createSession() {
        com.datastax.driver.core.Cluster cluster =
                new com.datastax.driver.core.Cluster.Builder().addContactPoints(hostIp).withPort(port).build();
        Session session = cluster.connect();
        return session;
    }
}
