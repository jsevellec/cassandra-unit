package org.cassandraunit;

import static org.cassandraunit.SampleDataSetChecker.assertDefaultValuesDataIsEmpty;
import static org.cassandraunit.SampleDataSetChecker.assertDefaultValuesSchemaExist;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.CounterSuperSlice;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HCounterSuperColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.OrderedSuperRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.ddl.*;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.RangeSuperSlicesQuery;
import me.prettyprint.hector.api.query.SliceCounterQuery;
import me.prettyprint.hector.api.query.SliceQuery;
import me.prettyprint.hector.api.query.SuperSliceCounterQuery;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.annotation.Immutable;
import org.cassandraunit.model.StrategyModel;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.cassandraunit.utils.MockDataSetHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
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
	public void shouldCreateKeyspaceWithDefaultColumnValueValidator() {
		String clusterName = "TestCluster30";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithSchemaAndDefaultColumnValueValidator());

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertThat(cluster.describeKeyspace("keyspace"), notNullValue());
		assertThat(cluster.describeKeyspace("keyspace").getCfDefs().get(0).getName(), is("columnFamily"));
		assertThat(cluster.describeKeyspace("keyspace").getCfDefs().get(0).getDefaultValidationClass(),
				is(ComparatorType.LONGTYPE.getClassName()));
	}

	@Test
	public void shouldCreateKeyspaceAndColumnFamiliesWithDefaultValues() {
		String clusterName = "TestCluster4";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues());

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertDefaultValuesSchemaExist(cluster);
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
        List<ColumnFamilyDefinition> columnFamilyDefinitions = cluster.describeKeyspace(keyspaceName).getCfDefs();
        assertThat(columnFamilyDefinitions, notNullValue());
		assertThat(columnFamilyDefinitions.size(), is(5));

        Map<String,ColumnFamilyDefinition> columnFamilyDefinitionsMap = Maps.uniqueIndex(columnFamilyDefinitions, new Function<ColumnFamilyDefinition, String>() {
            @Override
            public String apply(ColumnFamilyDefinition columnFamilyDefinition) {
                return columnFamilyDefinition.getName();
            }
        });

        ColumnFamilyDefinition beautifulColumnFamily = columnFamilyDefinitionsMap.get("beautifulColumnFamilyName");
        assertThat(beautifulColumnFamily.getName(), is("beautifulColumnFamilyName"));
		assertThat(beautifulColumnFamily.getKeyValidationClass(),
                is(ComparatorType.TIMEUUIDTYPE.getClassName()));
		assertThat(beautifulColumnFamily.getColumnType(), is(ColumnType.SUPER));
		assertThat(beautifulColumnFamily.getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(beautifulColumnFamily.getSubComparatorType().getClassName(),
				is(ComparatorType.LONGTYPE.getClassName()));
        assertThat(beautifulColumnFamily.getComment(), is("amazing comment"));
        assertThat(beautifulColumnFamily.getCompactionStrategy(),is("org.apache.cassandra.db.compaction.LeveledCompactionStrategy"));
        assertThat(beautifulColumnFamily.getCompactionStrategyOptions().get("sstable_size_in_mb"),is("10"));
        assertThat(beautifulColumnFamily.getGcGraceSeconds(),is(9999));
        assertThat(beautifulColumnFamily.getMaxCompactionThreshold(),is(31));
        assertThat(beautifulColumnFamily.getMinCompactionThreshold(),is(3));
        assertThat(beautifulColumnFamily.getReadRepairChance(),is(0.1d));
        assertThat(beautifulColumnFamily.isReplicateOnWrite(),is(false));


        ColumnFamilyDefinition amazingColumnFamily = columnFamilyDefinitionsMap.get("amazingColumnFamilyName");
        assertThat(amazingColumnFamily.getName(), is("amazingColumnFamilyName"));
		assertThat(amazingColumnFamily.getKeyValidationClass(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(amazingColumnFamily.getColumnType(), is(ColumnType.STANDARD));
		assertThat(amazingColumnFamily.getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));


        ColumnFamilyDefinition columnFamilyWithSecondaryIndex = columnFamilyDefinitionsMap.get("columnFamilyWithSecondaryIndex");
        assertThat(columnFamilyWithSecondaryIndex.getName(), is("columnFamilyWithSecondaryIndex"));
		assertThat(columnFamilyWithSecondaryIndex.getColumnMetadata(), notNullValue());
        ColumnDefinition columnDefinition = columnFamilyWithSecondaryIndex.getColumnMetadata().get(0);
        assertThat(columnDefinition, notNullValue());
		assertThat(columnDefinition.getName(),
				is(ByteBuffer.wrap("columnWithSecondaryIndexAndValidationClassAsLongType".getBytes(Charsets.UTF_8))));
		assertThat(columnDefinition.getIndexName(),
				is("columnWithSecondaryIndexAndValidationClassAsLongType"));
		assertThat(columnDefinition.getIndexType(),
				is(ColumnIndexType.KEYS));
		assertThat(columnDefinition
				.getValidationClass(), is(ComparatorType.LONGTYPE.getClassName()));


        ColumnFamilyDefinition columnFamilyWithSecondaryIndexAndIndexName = columnFamilyDefinitionsMap.get("columnFamilyWithSecondaryIndexAndIndexName");
        ColumnDefinition columnDefinition1 = columnFamilyWithSecondaryIndexAndIndexName.getColumnMetadata().get(0);
        assertThat(columnDefinition1, notNullValue());
		assertThat(columnDefinition1.getIndexName(),
				is("columnWithSecondaryIndexHaveIndexNameAndValidationClassAsUTF8Type"));
		assertThat(columnDefinition1.getName(),
				is(ByteBuffer.wrap("columnWithSecondaryIndexAndValidationClassAsUTF8Type".getBytes(Charsets.UTF_8))));
		assertThat(columnDefinition1.getIndexType(),
				is(ColumnIndexType.KEYS));
		assertThat(columnDefinition1
				.getValidationClass(), is(ComparatorType.UTF8TYPE.getClassName()));

        ColumnFamilyDefinition columnFamilyWithColumnValidationClass = columnFamilyDefinitionsMap.get("columnFamilyWithColumnValidationClass");
        ColumnDefinition columnDefinition2 = columnFamilyWithColumnValidationClass.getColumnMetadata().get(0);
        assertThat(columnDefinition2, notNullValue());
		assertThat(columnDefinition2.getName(),
				is(ByteBuffer.wrap("columnWithValidationClassAsUTF8Type".getBytes(Charsets.UTF_8))));
		assertThat(columnDefinition2.getIndexName(),
				nullValue());
		assertThat(columnDefinition2.getIndexType(),
				nullValue());
		assertThat(columnDefinition2
				.getValidationClass(), is(ComparatorType.UTF8TYPE.getClassName()));

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
			query.setColumnFamily("columnFamily1");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<byte[], byte[], byte[]>> result = query.execute();
			List<Row<byte[], byte[], byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(3));
			assertThat(rows.get(0).getKey(), is(decodeHex("30")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("31")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("31")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is(decodeHex("32")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(decodeHex("32")));
			assertThat(rows.get(2).getKey(), is(decodeHex("20")));
			assertThat(rows.get(1).getKey(), is(decodeHex("10")));

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
			assertThat(rows.get(0).getKey(), is(decodeHex("01")));
			assertThat(rows.get(0).getSuperSlice(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getName(), is(decodeHex("11")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getName(),
					is(decodeHex("1110")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getValue(),
					is(decodeHex("1110")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getName(),
					is(decodeHex("1120")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getValue(),
					is(decodeHex("1120")));

			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getName(), is(decodeHex("12")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns(), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().size(), is(2));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0).getName(),
					is(decodeHex("1210")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(0).getValue(),
					is(decodeHex("1210")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1).getName(),
					is(decodeHex("1220")));
			assertThat(rows.get(0).getSuperSlice().getSuperColumns().get(1).getColumns().get(1).getValue(),
					is(decodeHex("1220")));

			assertThat(rows.get(1).getKey(), is(decodeHex("02")));
			assertThat(rows.get(1).getSuperSlice(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().size(), is(1));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getName(), is(decodeHex("21")));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns(), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().size(), is(2));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getName(),
					is(decodeHex("2110")));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(0).getValue(),
					is(decodeHex("2110")));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1), notNullValue());
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getName(),
					is(decodeHex("2120")));
			assertThat(rows.get(1).getSuperSlice().getSuperColumns().get(0).getColumns().get(1).getValue(),
					is(decodeHex("2120")));

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
			RangeSlicesQuery<UUID, String, byte[]> query = HFactory.createRangeSlicesQuery(keyspace,
					UUIDSerializer.get(), StringSerializer.get(), BytesArraySerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<UUID, String, byte[]>> result = query.execute();
			List<Row<UUID, String, byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(2));
			assertThat(rows.get(0).getKey(), is(UUID.fromString("13818e20-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("name21"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("21")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("name22"));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(decodeHex("22")));

			assertThat(rows.get(1).getKey(), is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(1).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0).getName(), is("name11"));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("11")));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1).getName(), is("name12"));
			assertThat(rows.get(1).getColumnSlice().getColumns().get(1).getValue(), is(decodeHex("12")));
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
			RangeSlicesQuery<Long, Integer, byte[]> query = HFactory.createRangeSlicesQuery(keyspace,
					LongSerializer.get(), IntegerSerializer.get(), BytesArraySerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName2");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<Long, Integer, byte[]>> result = query.execute();
			List<Row<Long, Integer, byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is(10L));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(new Integer(11)));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("11")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is(new Integer(12)));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(decodeHex("12")));

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
			RangeSlicesQuery<UUID, UUID, byte[]> query = HFactory.createRangeSlicesQuery(keyspace,
					UUIDSerializer.get(), UUIDSerializer.get(), BytesArraySerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName3");
			query.setRange(null, null, false, Integer.MAX_VALUE);
			QueryResult<OrderedRows<UUID, UUID, byte[]>> result = query.execute();
			List<Row<UUID, UUID, byte[]>> rows = result.get().getList();
			assertThat(rows.size(), is(1));
			assertThat(rows.get(0).getKey(), is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(),
					is(UUID.fromString("13816710-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("11")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(),
					is(UUID.fromString("13818e20-1dd2-11b2-879a-782bcb80ff6a")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(decodeHex("12")));

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
			assertThat(rows.get(0).getKey(), is(decodeHex("01")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("01")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(1L));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is(decodeHex("02")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is(19652258L));

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
			assertThat(rows.get(0).getKey(), is(decodeHex("01")));
			assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(2));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("01")));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(1L));
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
			assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is(decodeHex("02")));
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

	@Test
	public void shouldLoadDataWithStandardRowWithCounterColumnTypeSpecified() {
		String clusterName = "TestCluster10";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			SliceCounterQuery<Long, String> query = HFactory.createCounterSliceQuery(keyspace, LongSerializer.get(),
					StringSerializer.get());
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
	public void shouldLoadDataWithSuperRowWithCounterColumnTypeSpecified() {
		String clusterName = "TestCluster11";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);

		try {
			dataLoader.load(MockDataSetHelper.getMockDataSetWithDefinedValuesSimple());

			/* verify */
			Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
			Keyspace keyspace = HFactory.createKeyspace("otherKeyspaceName", cluster);
			SuperSliceCounterQuery<Long, String, String> query = HFactory.createSuperSliceCounterQuery(keyspace,
					LongSerializer.get(), StringSerializer.get(), StringSerializer.get());
			query.setColumnFamily("beautifulColumnFamilyName7").setKey(10L);
			query.setRange(null, null, false, 100);
			QueryResult<CounterSuperSlice<String, String>> result = query.execute();
			List<HCounterSuperColumn<String, String>> superColumns = result.get().getSuperColumns();
			assertThat(superColumns, notNullValue());
			assertThat(superColumns.size(), is(1));
			HCounterSuperColumn<String, String> columns = superColumns.get(0);

			assertThat(columns.getColumns(), notNullValue());
			assertThat(columns.getColumns().size(), is(2));
			assertThat(columns.getColumns().get(0), notNullValue());
			assertThat(columns.getColumns().get(0).getName(), is("counter111"));
			assertThat(columns.getColumns().get(0).getValue(), is(111L));

			assertThat(columns.getColumns().get(1), notNullValue());
			assertThat(columns.getColumns().get(1).getName(), is("counter112"));
			assertThat(columns.getColumns().get(1).getValue(), is(112L));

		} catch (HInvalidRequestException e) {
			e.printStackTrace();
			fail();
		}
	}

	private byte[] decodeHex(String valueToDecode) {
		try {
			return Hex.decodeHex(valueToDecode.toCharArray());
		} catch (DecoderException e) {
			return null;
		}
	}

	@Test
	public void shouldLoadDataSetButOnlySchema() throws Exception {
		String clusterName = "TestCluster12";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		LoadingOption loadingOption = new LoadingOption();
		loadingOption.setOnlySchema(true);
		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValues(), loadingOption);

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertDefaultValuesSchemaExist(cluster);

		assertDefaultValuesDataIsEmpty(cluster);
	}

	@Test
	public void shouldLoadDataSetButOverrideReplicationFactor() throws Exception {
		String clusterName = "TestCluster13";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		LoadingOption loadingOption = new LoadingOption();
		loadingOption.setReplicationFactor(1);
		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValuesAndReplicationFactor2(), loadingOption);

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertDefaultValuesSchemaExist(cluster);
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getReplicationFactor(), not(2));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getReplicationFactor(), is(1));
	}

	@Test
	public void shouldLoadDataSetButOverrideStrategy() throws Exception {
		String clusterName = "TestCluster14";
		String host = "localhost:9171";
		DataLoader dataLoader = new DataLoader(clusterName, host);
		LoadingOption loadingOption = new LoadingOption();
		loadingOption.setStrategy(StrategyModel.SIMPLE_STRATEGY);

		dataLoader.load(MockDataSetHelper.getMockDataSetWithDefaultValuesAndNetworkTopologyStrategy(), loadingOption);

		/* test */
		Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
		assertDefaultValuesSchemaExist(cluster);
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getStrategyClass(),
				not("org.apache.cassandra.locator.NetworkTopologyStrategy"));
		assertThat(cluster.describeKeyspace("beautifulKeyspaceName").getStrategyClass(),
				is("org.apache.cassandra.locator.SimpleStrategy"));
	}

    @Test
    public void shouldLoadDataWithReversedComparatorOnSimpleType() {
        String clusterName = "TestCluster6";
        String host = "localhost:9171";
        DataLoader dataLoader = new DataLoader(clusterName, host);

        try {
            dataLoader.load(MockDataSetHelper.getMockDataSetWithReversedComparatorOnSimpleType());

			/* verify */
            Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
            Keyspace keyspace = HFactory.createKeyspace("reversedKeyspace", cluster);
            RangeSlicesQuery<String, String, String> query = HFactory.createRangeSlicesQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
                    StringSerializer.get());
            query.setColumnFamily("columnFamilyWithReversedComparatorOnSimpleType");
            query.setRange(null, null, false, Integer.MAX_VALUE);
            QueryResult<OrderedRows<String, String, String>> result = query.execute();
            List<Row<String, String, String>> rows = result.get().getList();

            assertThat(rows.size(), is(1));
            assertThat(rows.get(0).getKey(), is("row1"));
            assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(3));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is("c"));
            assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("c"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getName(), is("b"));
            assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("b"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(2), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(2).getName(), is("a"));
            assertThat(rows.get(0).getColumnSlice().getColumns().get(2).getValue(), is("a"));
        } catch (HInvalidRequestException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void shouldLoadDataWithReversedComparatorOnCompositeTypes() {
        String clusterName = "TestCluster6";
        String host = "localhost:9171";
        DataLoader dataLoader = new DataLoader(clusterName, host);

        try {
            dataLoader.load(MockDataSetHelper.getMockDataSetWithReversedComparatorOnCompositeTypes());

			/* verify */
            Cluster cluster = HFactory.getOrCreateCluster(clusterName, host);
            Keyspace keyspace = HFactory.createKeyspace("reversedKeyspace", cluster);
            RangeSlicesQuery<String, Composite, String> query = HFactory.createRangeSlicesQuery(keyspace, StringSerializer.get(), CompositeSerializer.get(),
                    StringSerializer.get());
            query.setColumnFamily("columnFamilyWithReversedCompOnCompositeTypes");
            query.setRange(null, null, false, Integer.MAX_VALUE);
            QueryResult<OrderedRows<String, Composite, String>> result = query.execute();
            List<Row<String, Composite, String>> rows = result.get().getList();

            assertThat(rows.size(), is(1));
            assertThat(rows.get(0).getKey(), is("row1"));
            assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(6));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is("v6"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(1), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(1).getValue(), is("v5"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(2), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(2).getValue(), is("v4"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(3), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(3).getValue(), is("v3"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(4), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(4).getValue(), is("v2"));

            assertThat(rows.get(0).getColumnSlice().getColumns().get(5), notNullValue());
            assertThat(rows.get(0).getColumnSlice().getColumns().get(5).getValue(), is("v1"));
        } catch (HInvalidRequestException e) {
            e.printStackTrace();
            fail();
        }
    }
}
