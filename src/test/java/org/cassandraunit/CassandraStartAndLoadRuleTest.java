package org.cassandraunit;

import static org.cassandraunit.SampleDataSetChecker.assertDataSetLoaded;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.junit.Rule;
import org.junit.Test;

public class CassandraStartAndLoadRuleTest {

	@Rule
	public CassandraUnit cassandra = new CassandraUnit(new ClassPathXmlDataSet("xml/datasetDefaultValues.xml"));
	
	@Test
	public void shouldStartCassandraServer ()  {
		assertThat(cassandra.cluster, notNullValue());
	}
	
	@Test
	public void shouldCreateStructureAndLoadData() throws Exception {
		assertThat(cassandra.keyspace, notNullValue());
		assertDataSetLoaded(cassandra.keyspace);
	}
}
