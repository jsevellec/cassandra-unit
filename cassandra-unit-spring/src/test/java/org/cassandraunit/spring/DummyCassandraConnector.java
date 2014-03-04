package org.cassandraunit.spring;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

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

    @PostConstruct
    public void init() {
        cluster = Cluster.builder()
                .addContactPoints("127.0.0.1")
                .withPort(9142)
                .build();
        session = cluster.connect("cassandra_unit_keyspace");
    }

    @PreDestroy
    public void preDestroy() {
        session.close();
        cluster.close();
    }

    public Session getSession() {
        return session;
    }
}
