package org.cassandraunit;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.rules.ExternalResource;

public class CassandraUnit extends ExternalResource {
	public Cluster cluster;
	public Keyspace keyspace;

	private DataSet dataSet;

	public String clusterName = "TestCluster";
	public String host = "localhost:9171";

	public CassandraUnit(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	@Override
	protected void before() throws Exception {
		/* start an embedded Cassandra */
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();

		/* create structure and load data */
		DataLoader dataLoader = new DataLoader(clusterName, host);
		dataLoader.load(dataSet);

		/* get hector client object to query data in your test */
		cluster = HFactory.getOrCreateCluster(clusterName, host);
		keyspace = HFactory.createKeyspace(dataSet.getKeyspace().getName(), cluster);
	}

	
	
}
