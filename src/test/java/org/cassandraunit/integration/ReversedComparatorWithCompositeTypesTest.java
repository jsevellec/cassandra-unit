package org.cassandraunit.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
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

public class ReversedComparatorWithCompositeTypesTest {
	private static final String KEYSPACE_NAME = "reversedKeyspace";
	private static final String ROW_KEY = "row1";

	private static final CompositeSerializer cs = CompositeSerializer.get();
	private static final StringSerializer ss = StringSerializer.get();

	@Before
	public void before() throws Exception {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		DataLoader dataLoader = new DataLoader("TestCluster", "localhost:9171");
		dataLoader.load(new ClassPathYamlDataSet("integration/emptyDataSetWithReversedComparatorOnCompositeTypes.yaml"));
	}

	@Test
	public void writeAndReadFromCfCreatedUsingCassandraUnit() {
		final Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9170"));
		final Keyspace keyspace = HFactory.createKeyspace(KEYSPACE_NAME, cluster);

		final String columnFamily = "columnFamilyWithReversedComparatorOnCompTypes";

		writeDataSet(keyspace, columnFamily);
		final QueryResult<OrderedRows<String, Composite, String>> results = readDataSet(keyspace, columnFamily);
		assertResultsAreSortedAccordingToComparator(results);
	}

	@Test
	public void writeAndReadFromCfCreatedUsingHector() {
		final Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator("localhost:9170"));
		final Keyspace keyspace = HFactory.createKeyspace(KEYSPACE_NAME, cluster);

		final String columnFamily = "manuallyCreated";
		createColumnFamily(cluster, columnFamily);

		writeDataSet(keyspace, columnFamily);
		final QueryResult<OrderedRows<String, Composite, String>> results = readDataSet(keyspace, columnFamily);
		assertResultsAreSortedAccordingToComparator(results);
	}

	private void createColumnFamily(final Cluster cluster, final String columnFamily) {
		final ColumnFamilyDefinition cfd = HFactory.createColumnFamilyDefinition(KEYSPACE_NAME, columnFamily, ComparatorType.COMPOSITETYPE);
		cfd.setComparatorTypeAlias("(LongType(reversed=true),IntegerType,UTF8Type(reversed=true))");
		cfd.setKeyValidationClass(UTF8Type.class.getName());
		cfd.setDefaultValidationClass(UTF8Type.class.getName());
		cluster.addColumnFamily(cfd, true);
	}

	private void writeDataSet(final Keyspace keyspace, final String columnFamily) {
		final Mutator<String> mutator = HFactory.createMutator(keyspace, ss);
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 1, "a"), "v1", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 1, "b"), "v2", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 1, "c"), "v3", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 2, "a"), "v4", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 2, "b"), "v5", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(1, 2, "c"), "v6", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(2, 1, "a"), "v7", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(2, 1, "b"), "v8", cs, ss));
		mutator.addInsertion(ROW_KEY, columnFamily, HFactory.createColumn(getName(2, 1, "c"), "v9", cs, ss));
		mutator.execute();
	}

	private Composite getName(final long i, final int j, final String string) {
		final Composite composite = new Composite();
		composite.addComponent(i, LongSerializer.get());
		composite.addComponent(j, IntegerSerializer.get());
		composite.addComponent(string, StringSerializer.get());
		return composite;
	}

	private QueryResult<OrderedRows<String, Composite, String>> readDataSet(final Keyspace keyspace, final String columnFamily) {
		final RangeSlicesQuery<String, Composite, String> query = HFactory.createRangeSlicesQuery(keyspace, ss, cs, ss);
		query.setColumnFamily(columnFamily);
		query.setRange(null, null, false, Integer.MAX_VALUE);
		QueryResult<OrderedRows<String, Composite, String>> results = query.execute();
		return results;
	}

	private void assertResultsAreSortedAccordingToComparator(QueryResult<OrderedRows<String, Composite, String>> results) {
		assertNotNull(results);
		assertNotNull(results.get());
		assertNotNull(results.get().getList());
		assertEquals(1, results.get().getList().size());
		assertNotNull(results.get().getList().get(0));
		assertEquals(ROW_KEY, results.get().getList().get(0).getKey());
		assertNotNull(results.get().getList().get(0).getColumnSlice());

		final List<HColumn<Composite, String>> columns = results.get().getList().get(0).getColumnSlice().getColumns();
		assertNotNull(columns);

		// assertEquals(getName(2, 1, "c"), columns.get(0).getName());
		assertEquals("v9", columns.get(0).getValue());

		// assertEquals(getName(2, 1, "b"), columns.get(1).getName());
		assertEquals("v8", columns.get(1).getValue());

		// assertEquals(getName(2, 1, "a"), columns.get(2).getName());
		assertEquals("v7", columns.get(2).getValue());

		// assertEquals(getName(1, 1, "c"), columns.get(3).getName());
		assertEquals("v3", columns.get(3).getValue());

		// assertEquals(getName(1, 1, "b"), columns.get(4).getName());
		assertEquals("v2", columns.get(4).getValue());

		// assertEquals(getName(1, 1, "a"), columns.get(5).getName());
		assertEquals("v1", columns.get(5).getValue());

		// assertEquals(getName(1, 2, "c"), columns.get(6).getName());
		assertEquals("v6", columns.get(6).getValue());

		// assertEquals(getName(1, 2, "b"), columns.get(7).getName());
		assertEquals("v5", columns.get(7).getValue());

		// assertEquals(getName(1, 2, "a"), columns.get(8).getName());
		assertEquals("v4", columns.get(8).getValue());
	}
}
