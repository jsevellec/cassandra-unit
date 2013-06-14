package org.cassandraunit;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CassandraStartCustomYamlTest extends AbstractCassandraUnit4TestCase {

	public CassandraStartCustomYamlTest() {
		super("another-cassandra.yaml", "localhost:9175");
	}

	@Test
    @Ignore // Does not support the start of two configurations in the same JVM Instance
    public void shouldStartCassandraServer ()  {
        Cluster cluster = HFactory.getOrCreateCluster("anotherCluster", "localhost:9175");
        KeyspaceDefinition keyspaceDefinition = cluster.describeKeyspace("system");
        assertThat(keyspaceDefinition, notNullValue());

    }

	@Override
	public DataSet getDataSet() {
		return new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
	}
	
}
