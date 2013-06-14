package org.cassandraunit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.cassandraunit.utils.MockDataSetHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataLoaderCompositeTypeTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
	}

	@Test
	public void shouldCreateKeyspaceAndLoadDataWithColumnCompositeType() {
		String clusterName = "TestClusterCompositeType01";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		dataLoader.load(MockDataSetHelper.getMockDataSetWithCompositeType());
		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertThat(cluster.describeKeyspace("compositeKeyspace").getCfDefs().get(0).getName(),
				is("columnFamilyWithCompositeType"));
		assertThat(
				cluster.describeKeyspace("compositeKeyspace").getCfDefs().get(0).getComparatorType().getTypeName(),
				is(ComparatorType
						.getByClassName(
								"CompositeType(org.apache.cassandra.db.marshal.LongType,org.apache.cassandra.db.marshal.UTF8Type,org.apache.cassandra.db.marshal.IntegerType)")
						.getTypeName()));

		Keyspace keyspace = HFactory.createKeyspace("compositeKeyspace", cluster);

		SliceQuery<String, Composite, String> query = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
				new CompositeSerializer(), StringSerializer.get());
		query.setColumnFamily("columnFamilyWithCompositeType");
		query.setKey("row1");

		// Create a composite search range
		Composite start = new Composite();
		start.addComponent(11L, LongSerializer.get());
		start.addComponent("ab", StringSerializer.get());
		start.addComponent(0, IntegerSerializer.get());

		Composite finish = new Composite();
		finish.addComponent(11L, LongSerializer.get());
		finish.addComponent("ab", StringSerializer.get());
		finish.addComponent(Integer.MAX_VALUE, IntegerSerializer.get());
		query.setRange(start, finish, false, 100);

		// Now search.
		ColumnSlice<Composite, String> columnSlice = query.execute().get();
		assertThat(columnSlice.getColumns().size(), is(2));
		assertThat(columnSlice.getColumns().get(0).getValue(), is("v2"));
		assertThat(columnSlice.getColumns().get(1).getValue(), is("v3"));

		SliceQuery<String, Composite, String> query2 = HFactory.createSliceQuery(keyspace, StringSerializer.get(),
				new CompositeSerializer(), StringSerializer.get());
		query2.setColumnFamily("columnFamilyWithCompositeType");
		query2.setKey("row1");

		// Create a composite search range
		Composite start2 = new Composite();
		start2.addComponent(12L, LongSerializer.get());
		start2.addComponent("a", StringSerializer.get());

		Composite finish2 = new Composite();
		finish2.addComponent(12L, LongSerializer.get());
		finish2.addComponent(Character.toString(Character.MAX_VALUE), StringSerializer.get());
		query2.setRange(start2, finish2, false, 100);

		// Now search.
		ColumnSlice<Composite, String> columnSlice2 = query2.execute().get();
		assertThat(columnSlice2.getColumns().size(), is(3));
		assertThat(columnSlice2.getColumns().get(0).getValue(), is("v4"));
		assertThat(columnSlice2.getColumns().get(1).getValue(), is("v5"));
		assertThat(columnSlice2.getColumns().get(2).getValue(), is("v6"));

	}

	@Test
	public void shouldCreateKeyspaceAndLoadDataWithRowKeyCompositeType() throws Exception {
		String clusterName = "TestClusterCompositeType02";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		dataLoader.load(MockDataSetHelper.getMockDataSetWithCompositeType());
		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertThat(cluster.describeKeyspace("compositeKeyspace").getCfDefs().get(1).getName(),
				is("columnFamilyWithRowKeyCompositeType"));
		assertThat(
				cluster.describeKeyspace("compositeKeyspace").getCfDefs().get(1).getKeyValidationClass(),
				is("org.apache.cassandra.db.marshal.CompositeType(org.apache.cassandra.db.marshal.LongType,org.apache.cassandra.db.marshal.UTF8Type)"));

		Keyspace keyspace = HFactory.createKeyspace("compositeKeyspace", cluster);

		SliceQuery<Composite, String, String> query = HFactory.createSliceQuery(keyspace, new CompositeSerializer(),
				StringSerializer.get(), StringSerializer.get());
		Composite key = new Composite();
		key.addComponent(12L, LongSerializer.get());
		key.addComponent("az", StringSerializer.get());
		query.setColumnFamily("columnFamilyWithRowKeyCompositeType").setKey(key);
		query.setRange(null, null, false, 100);
		QueryResult<ColumnSlice<String, String>> result = query.execute();
		assertThat(result.get().getColumns().get(0).getValue(), is("a"));

	}
}
