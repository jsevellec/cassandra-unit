package org.cassandraunit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.OrderedSuperRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.CounterQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.RangeSuperSlicesQuery;
import me.prettyprint.hector.api.query.SliceCounterQuery;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.cassandraunit.utils.MockDataSetHelper;
import org.junit.BeforeClass;
import org.junit.Test;
/**
 * 
 * @author Jeremy Sevellec
 *
 */
public class DataLoaderTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
	}

	@Test
	public void shouldNotBeToConnected() {
		String clusterName = "TestClusterNotConnected";
		String host = "localhost:9172";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		Cluster cluster = dataLoader.getCluster();
		try {
			cluster.describeKeyspaces();
			fail();
		} catch (HectorException e) {
			/* nothing to do it's what we want */
		}
	}

	@Test
	public void shouldBeToConnected() {
		String clusterName = "TestCluster2";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		Cluster cluster = dataLoader.getCluster();
		assertThat(cluster.describeKeyspaces(), notNullValue());
		assertThat(cluster.describeKeyspace("system"), notNullValue());
		assertThat(cluster.describeKeyspace("system").getReplicationFactor(), is(1));
		assertThat(cluster.describeKeyspace("system").getName(), is("system"));
	}

	@Test
	public void shouldCreateKeyspaceWithDefaultValues() {
		String clusterName = "TestCluster3";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName"), notNullValue());
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getReplicationFactor(), is(1));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getStrategyClass(),
				is("org.apache.cassandra.locator.SimpleStrategy"));

	}

	@Test
	public void shouldCreateKeyspaceAndColumnFamiliesWithDefaultValues() {
		String clusterName = "TestCluster4";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName"), notNullValue());
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs(), notNullValue());
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs().size(), is(1));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs().get(0).getName(),
				is("beautifulColumnFamilyName"));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs().get(0).getColumnType(),
				is(ColumnType.STANDARD));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs().get(0).getComparatorType(),
				is(ComparatorType.BYTESTYPE));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getCfDefs().get(0).getKeyValidationClass(),
				is(ComparatorType.BYTESTYPE.getClassName()));
	}

	@Test
	public void shouldCreateKeyspaceAndColumnFamiliesWithDefinedValues() {
		String clusterName = "TestCluster5";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValues());

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		String keyspaceName = "otherKeyspaceName";
		assertThat(cluster.describeKeyspace(keyspaceName), notNullValue());
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs(), notNullValue());
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().size(), is(2));

		String firstColumnFamilyName = "beautifulColumnFamilyName";
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(0).getName(), is(firstColumnFamilyName));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(0).getKeyValidationClass(),
				is(ComparatorType.TIMEUUIDTYPE.getClassName()));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(0).getColumnType(), is(ColumnType.SUPER));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(0).getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(0).getSubComparatorType().getClassName(),
				is(ComparatorType.LONGTYPE.getClassName()));

		String secondColumnFamilyName = "amazingColumnFamilyName";
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(1).getName(), is(secondColumnFamilyName));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(1).getKeyValidationClass(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(1).getColumnType(), is(ColumnType.STANDARD));
		assertThat(cluster.describeKeyspace(keyspaceName).getCfDefs().get(1).getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));

	}

	@Test
	public void shouldLoadDataWithStandardRow() {
		String clusterName = "TestCluster6";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("beautifulKeyspaceName", cluster);
			RangeSlicesQuery<byte[], byte[], byte[]> query = HFactory.createRangeSlicesQuery(keyspace,
					BytesArraySerializer.get(), BytesArraySerializer.get(), BytesArraySerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<byte[], byte[], byte[]>> result = query.execute();
			List<Row<byte[], byte[], byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(3));
			assertThat(rows.get(0).getKey(), is("key30".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("name31".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("value31".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("name32".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("value32".getBytes()));
			assertThat(rows.get(1).getKey(), is("key20".getBytes()));
			assertThat(rows.get(2).getKey(), is("key10".getBytes()));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldLoadDataWithSuperRow() {
		String clusterName = "TestCluster6";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithSuperColumn());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("beautifulKeyspaceName", cluster);
			RangeSuperSlicesQuery<byte[], byte[], byte[], byte[]> query = HFactory.createRangeSuperSlicesQuery(
					keyspace, BytesArraySerializer.get(), BytesArraySerializer.get(), BytesArraySerializer.get(),
					BytesArraySerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedSuperRows<byte[], byte[], byte[], byte[]>> result = query.execute();
			List<SuperRow<byte[], byte[], byte[], byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(2));
			assertThat(rows.get(0).getKey(), is("key1".getBytes()));
			assertThat(rows.get(0).getSuperSlice(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getName(), is("name11".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getName(),
					is("name111".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getValue(),
					is("value111".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getName(),
					is("name112".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getValue(),
					is("value112".getBytes()));

			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getName(), is("name12".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0).getName(),
					is("name121".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0).getValue(),
					is("value121".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1).getName(),
					is("name122".getBytes()));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1).getValue(),
					is("value122".getBytes()));

			assertThat(rows.get(1).getKey(), is("key2".getBytes()));
			assertThat(rows.get(1).getSuperSlice(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().size(), is(1));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getName(), is("name21".getBytes()));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().size(), is(2));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getName(),
					is("name211".getBytes()));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getValue(),
					is("value211".getBytes()));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getName(),
					is("name212".getBytes()));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getValue(),
					is("value212".getBytes()));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldLoadDataWithStandardRowButWithDefinedTypeTimeUUIDTypeAndUTF8Type() {
		String clusterName = "TestCluster6";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			RangeSlicesQuery<UUID, String, String> query = HFactory.createRangeSlicesQuery(keyspace,
					UUIDSerializer.get(), StringSerializer.get(), StringSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<UUID, String, String>> result = query.execute();
			List<Row<UUID, String, String>> rows = result.get().getList();
			assertThat(rows.size(), is(2));
			assertThat(rows.get(0).getKey(), is(UUID.fromString("13818e20-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("name21"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("value21"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("name22"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("value22"));

			assertThat(rows.get(1).getKey(), is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(1).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0).getName(), is("name11"));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0).getValue(), is("value11"));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1).getName(), is("name12"));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1).getValue(), is("value12"));
		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldLoadDataWithStandardRowButWithDefinedTypeLongTypeAndUTF8Type() {
		String clusterName = "TestCluster6";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			RangeSlicesQuery<Long, Integer, String> query = HFactory.createRangeSlicesQuery(keyspace,
					LongSerializer.get(), IntegerSerializer.get(), StringSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName2");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<Long, Integer, String>> result = query.execute();
			List<Row<Long, Integer, String>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is(10L));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(new Integer(11)));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("value11"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is(new Integer(12)));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("value12"));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldLoadDataWithStandardRowButWithDefinedTypeUUIDTypeAndLexicalUUIDType() {
		String clusterName = "TestCluster6";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			RangeSlicesQuery<UUID, UUID, String> query = HFactory.createRangeSlicesQuery(keyspace,
					UUIDSerializer.get(), UUIDSerializer.get(), StringSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName3");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<UUID, UUID, String>> result = query.execute();
			List<Row<UUID, UUID, String>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(),
					is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("value11"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(),
					is(UUID.fromString("13818e20-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("value12"));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldLoadDataWithStandardRowWithDefaultColumnTypeSpecified() {
		String clusterName = "TestCluster7";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			RangeSlicesQuery<byte[], byte[], Long> query = HFactory.createRangeSlicesQuery(keyspace,
					BytesArraySerializer.get(), BytesArraySerializer.get(), LongSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName4");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<byte[], byte[], Long>> result = query.execute();
			List<Row<byte[], byte[], Long>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is("key01".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("name01".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(1L));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("name02".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(19652258L));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void shouldLoadDataWithStandardRowWithCounterColumnTypeSpecified() {
	  String clusterName = "TestCluster7";
	  String host = "localhost:9171";
	  DataLoader dataLoader = new DataLoader(clusterName, host);

	  try {
	    dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

	    /* verify */
	    Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
	    Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
	    SliceCounterQuery<Long, String> query = HFactory.createCounterSliceQuery(keyspace, LongSerializer.get(), StringSerializer.get());
	    query.setColumnFamily("beautifulColumnFamilyName6");
	    query.setKey(10L);
	    query.setRange(null, null, false, 100);
	    QueryResult<CounterSlice<String>> result = query.execute();
	    List<HCounterColumn<String>> columns = result.get().getColumns();
	    assertThat(columns.size(), is(2));
	    assertThat(columns.get(0), notNullValue());
	    assertThat(columns.get(0).getName(), is("counter11"));
	    assertThat(columns.get(0).getValue(), is(11L));
	    assertThat(columns.get(1), notNullValue());
	    assertThat(columns.get(1).getName(), is("counter12"));
      assertThat(columns.get(1).getValue(), is(12L));	

	  } catch (HInvalidRequestException e) {
	    e.printStackTrace();
	    fail();
	  }
	}

	@Test
	public void shouldLoadDataWithStandardRowWithDefaultColumnTypeSpecifiedAndColumnTypeFunction() {
		String clusterName = "TestCluster8";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			RangeSlicesQuery<byte[], byte[], Long> query = HFactory.createRangeSlicesQuery(keyspace,
					BytesArraySerializer.get(), BytesArraySerializer.get(), LongSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName5");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<byte[], byte[], Long>> result = query.execute();
			List<Row<byte[], byte[], Long>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is("key01".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("name01".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(1L));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("name02".getBytes()));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(19652258L));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void shouldCleanKeyspaceBeforeLoadDataBecauseKeyspaceExist() {
		try {
			String clusterName = "TestCluster9";
			String host = "localhost:9171";
			DataLoader dataLoader = new DataLoader(clusterName, host);
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());
		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}
}
