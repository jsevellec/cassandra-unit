package org.cassandraunit;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

public class CassandraUnit extends BaseCassandraUnit  {
    public Cluster cluster;
    public Keyspace keyspace;
    private DataSet dataSet;

    public static String clusterName;
    public static String host;

    public CassandraUnit(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public CassandraUnit(DataSet dataSet, String configurationFileName) {
        this(dataSet);
        this.configurationFileName = configurationFileName;
    }

    @Override
    protected void load() {
        host = EmbeddedCassandraServerHelper.getHost() + ":" + EmbeddedCassandraServerHelper.getRpcPort();
        clusterName = EmbeddedCassandraServerHelper.getClusterName();

        DataLoader dataLoader = new DataLoader(clusterName, host);
        dataLoader.load(dataSet);

        /* get hector client object to query data in your test */
        cluster = HFactory.getOrCreateCluster(clusterName, host);
        keyspace = HFactory.createKeyspace(dataSet.getKeyspace().getName(), cluster);
    }

}
