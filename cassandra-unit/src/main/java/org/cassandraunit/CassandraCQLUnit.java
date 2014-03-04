package org.cassandraunit;

import org.cassandraunit.dataset.CQLDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
/**
 * @author Marcin Szymaniuk
 * @author Jeremy Sevellec
 */
public class CassandraCQLUnit extends BaseCassandraUnit {
    private CQLDataSet dataSet;

    private static final Logger log = LoggerFactory.getLogger(CassandraCQLUnit.class);
    private String hostIp = "127.0.0.1";
    private int port = 9142;
    public Session session;
    public Cluster cluster;


    public CassandraCQLUnit(CQLDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName) {
        this(dataSet);
        this.configurationFileName = configurationFileName;
    }

    public CassandraCQLUnit(CQLDataSet dataSet, String configurationFileName, String hostIp, int port) {
        this(dataSet);
        this.configurationFileName = configurationFileName;
        this.hostIp = hostIp;
        this.port = port;
    }

    protected void load() {
        cluster = new Cluster.Builder().addContactPoints(hostIp).withPort(port).build();
        session = cluster.connect();
        CQLDataLoader dataLoader = new CQLDataLoader(session);
        dataLoader.load(dataSet);
        session = dataLoader.getSession();
    }

}
