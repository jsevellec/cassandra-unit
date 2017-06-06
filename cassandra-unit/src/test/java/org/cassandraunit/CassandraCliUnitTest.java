package org.cassandraunit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.Rule;
import org.junit.Test;


public class CassandraCliUnitTest {

	@Rule
	public CassandraCliUnit cassandraCliUnit = new CassandraCliUnit("src/test/resources/cli/simple.cassandra-cli");

	@Test
	public void testCliFileAreLoaded() {
		Cluster cluster = new Cluster.Builder().addContactPoints("localhost").withPort(9142).withClusterName("Test Cluster").build();
		// keyspace mykeyspace should have been created
		Session session = cluster.connect("mykeyspace");
		// there should be column family named shopping_cart
		session.execute("select * from shopping_cart;");
	}

}
