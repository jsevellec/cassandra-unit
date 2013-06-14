package org.cassandraunit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
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

  public CQLDataLoader(String hostIp, int port) {
    this.session = createSession(hostIp, port);
  }

  private Session createSession(String hostIp, int port) {
    Cluster cluster =
        new Cluster.Builder().addContactPoints(hostIp).withPort(port).build();
    Session session = cluster.connect();
    return session;
  }

  public void load(CQLDataSet dataSet) {
    initKeyspaceContext(session, dataSet);

    log.debug("loading data");
    for (String query : dataSet.getCQLStatements()) {
      log.debug("executing : " + query );
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
      String selectQuery = "SELECT keyspace_name FROM system.schema_keyspaces where keyspace_name='" + keyspaceName + "'";
      ResultSet keyspaceQueryResult = session.execute(selectQuery);
      if (keyspaceQueryResult.iterator().hasNext()) {
        String dropQuery = "DROP KEYSPACE " + keyspaceName;
        log.debug("executing : " + dropQuery);
        session.execute(dropQuery);
      }
    }

    if (dataSet.isKeyspaceCreation()) {
      String createQuery = "CREATE KEYSPACE " + keyspaceName + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}";
      log.debug("executing : " + createQuery);
      session.execute(createQuery);
    }

    String useQuery = "USE " + keyspaceName;
    log.debug("executing : " + useQuery);
    session.execute(useQuery);
  }
}