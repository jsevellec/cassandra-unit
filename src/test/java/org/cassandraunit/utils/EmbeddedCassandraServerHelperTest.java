package org.cassandraunit.utils;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.Test;

import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class EmbeddedCassandraServerHelperTest {

	@Test
	public void shouldStartAndCleanAnEmbeddedCassandra() throws Exception {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		testIfTheEmbeddedCassandraServerIsUpOnHost("127.0.0.1:9171");
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		testIfTheEmbeddedCassandraServerIsUpOnHost("127.0.0.1:9171");
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
	}

	private void testIfTheEmbeddedCassandraServerIsUpOnHost(String hostAndPort) {
        Random random = new Random();
		Cluster cluster = HFactory.getOrCreateCluster("TestCluster" + random.nextInt(), new CassandraHostConfigurator(hostAndPort));
		assertThat(cluster.getConnectionManager().getActivePools().size(), is(1));
		KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace("system");
		assertThat(keyspaceDefinition, notNullValue());
		assertThat(keyspaceDefinition.getReplicationFactor(), is(1));
	}

}
