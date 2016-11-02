package org.cassandraunit.spring;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

/**
 * @author Gaëtan Le Brun
 */
public class DummyCassandraConnector {

    private static int instancesCounter;
    private Session session;
    private Cluster cluster;

    public DummyCassandraConnector() {
        instancesCounter++;
    }

    public static void resetInstancesCounter() {
        instancesCounter = 0;
    }

    public static int getInstancesCounter() {
        return instancesCounter;
    }


    public Session getSession() {
        return EmbeddedCassandraServerHelper.getSession();
    }
}
