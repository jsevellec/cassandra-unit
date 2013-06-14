package org.cassandraunit.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import org.apache.cassandra.db.marshal.UTF8Type;
import org.cassandraunit.DataLoader;
import org.cassandraunit.dataset.yaml.ClassPathYamlDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Before;
import org.junit.Test;

public class ReversedComparatorWithSimpleTypeTest {
	private static final String KEYSPACE_NAME = "reversedKeyspace";
	private static final String ROW_KEY = "row1";

	@Before
	public void before() throws Exception {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		DataLoader dataLoader = new DataLoader("TestCluster", "localhost:9171");
		dataLoader.load(new ClassPathYamlDataSet("integration/emptyDataSetWithReversedComparatorOnSimpleType.yaml"));
	}

	@Test
	public void writeAndReadFromCfCreatedUsingCassandraUnit() {
		final Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9170"));
		final Keyspace keyspace = HFactory.createKeyspace(KEYSPACE_NAME, cluster);

		final String columnFamily = "columnFamilyWithReversedComparatorOnSimpleType";

		writeDataSet(keyspace, columnFamily);
		final QueryResult<OrderedRows<String, String, String>> results = readDataSet(keyspace, columnFamily);
		assertResultsAreSortedAccordingToComparator(results);
	}

	@Test
	public void writeAndReadFromCfCreatedUsingHector() {
		final Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9170"));
		final Keyspace keyspace = HFactory.createKeyspace(KEYSPACE_NAME, cluster);

		final String columnFamily = "manuallyCreated";
		createColumnFamily(cluster, columnFamily);

		writeDataSet(keyspace, columnFamily);
		final QueryResult<OrderedRows<String, String, String>> results = readDataSet(keyspace, columnFamily);
		assertResultsAreSortedAccordingToComparator(results);
	}

	private void createColumnFamily(final Cluster cluster, final String columnFamily) {
		final ColumnFamilyDefinition cfd = HFactory.createColumnFamilyDefinition(KEYSPACE_NAME, columnFamily, ComparatorType.UTF8TYPE);
		cfd.setComparatorTypeAlias("(reversed=true)");
		cfd.setKeyValidationClass(UTF8Type.class.getName());
		cfd.setDefaultValidationClass(UTF8Type.class.getName());
		cluster.addColumnFamily(cfd, true);
	}

	private void writeDataSet(final Keyspace keyspace, final String columnFamily) {
		final Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createStringColumn("a", "v1"));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createStringColumn("b", "v2"));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createStringColumn("c", "v3"));
		mutator.execute();
	}

	private QueryResult<OrderedRows<String, String, String>> readDataSet(final Keyspace keyspace, final String columnFamily) {
		final StringSerializer ss = StringSerializer.get();
		final RangeSlicesQuery<String, String, String> query = HFactory.createRangeSlicesQuery(keyspace, ss, ss, ss);
		query.setColumnFamily(columnFamily);
		query.setRange(null, null, false, Integer.MAX_VALUE);
		QueryResult<OrderedRows<String, String, String>> results = query.execute();
		return results;
	}

	private void assertResultsAreSortedAccordingToComparator(QueryResult<OrderedRows<String, String, String>> results) {
		assertNotNull(results);
		assertNotNull(results.get());
		assertNotNull(results.get().getList());
		assertEquals(1, results.get().getList().size());
		assertNotNull(results.get().getList().get(0));
		assertEquals(ROW_KEY, results.get().getList().get(0).getKey());
		assertNotNull(results.get().getList().get(0).getColumnSlice());

		final List<HColumn<String, String>> columns = results.get().getList().get(0).getColumnSlice().getColumns();
		assertNotNull(columns);

		assertEquals("c", columns.get(0).getName());
		assertEquals("v3", columns.get(0).getValue());

		assertEquals("b", columns.get(1).getName());
		assertEquals("v2", columns.get(1).getValue());

		assertEquals("a", columns.get(2).getName());
		assertEquals("v1", columns.get(2).getValue());
	}
}
