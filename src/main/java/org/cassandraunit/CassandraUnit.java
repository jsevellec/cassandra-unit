package org.cassandraunit;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.cassandraunit.dataset.DataSet;

public class CassandraUnit extends BaseCassandraUnit  {
    public Cluster cluster;
    public Keyspace keyspace;
    private DataSet dataSet;

    public static String clusterName = "TestCluster";
    public static String host = "localhost:9171";

    public CassandraUnit(DataSet dataSet) {
        this.dataSet = dataSet;
    }
    public CassandraUnit(DataSet dataSet, String configurationFileName) {
    	this(dataSet);
    	this.configurationFileName = configurationFileName;
    }

    public CassandraUnit(DataSet dataSet, String configurationFileName, String host) {
        this(dataSet, configurationFileName);
        this.host = host;
    }

    @Override
    protected void load() {
        DataLoader dataLoader = new DataLoader(clusterName, host);
        dataLoader.load(dataSet);

        /* get hector client object to query data in your test */
        cluster = HFactory.getOrCreateCluster(clusterName, host);
        keyspace = HFactory.createKeyspace(dataSet.getKeyspace().getName(), cluster);
    }

}
