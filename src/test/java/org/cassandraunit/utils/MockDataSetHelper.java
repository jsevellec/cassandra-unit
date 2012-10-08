package org.cassandraunit.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.model.*;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class MockDataSetHelper {

	public static DataSet getMockDataSetWithDefaultValues() {
		DataSet mockDataSet = mock(DataSet.class);
		KeyspaceModel keyspace = contructDefaultValuesKeyspace();

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	private static KeyspaceModel contructDefaultValuesKeyspace() {
		/* keyspace */
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("beautifulKeyspaceName");
		List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();

		/* column family */
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("columnFamily1");
		List<RowModel> rows = new ArrayList<RowModel>();

		/* row1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("10", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();

		columns1.add(constructDefaultColumnForMock("11", "11"));
		columns1.add(constructDefaultColumnForMock("12", "12"));
		row1.setColumns(columns1);
		rows.add(row1);

		/* row2 */
		RowModel row2 = new RowModel();
		row2.setKey(new GenericType("20", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns2 = new ArrayList<ColumnModel>();

		columns2.add(constructDefaultColumnForMock("21", "21"));
		columns2.add(constructDefaultColumnForMock("22", "22"));
		columns2.add(constructDefaultColumnForMock("23", "23"));
		row2.setColumns(columns2);
		rows.add(row2);

		/* row3 */
		RowModel row3 = new RowModel();
		row3.setKey(new GenericType("30", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns3 = new ArrayList<ColumnModel>();

		columns3.add(constructDefaultColumnForMock("31", "31"));
		columns3.add(constructDefaultColumnForMock("32", "32"));
		row3.setColumns(columns3);
		rows.add(row3);

		columnFamily.setRows(rows);
		columnFamilies.add(columnFamily);
		keyspace.setColumnFamilies(columnFamilies);
		return keyspace;
	}

	private static ColumnModel constructDefaultColumnForMock(String name, String value) {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName(new GenericType(name, GenericTypeEnum.BYTES_TYPE));
		columnModel.setValue(new GenericType(value, GenericTypeEnum.BYTES_TYPE));
		return columnModel;
	}

	public static DataSet getMockDataSetWithDefinedValues() {
		DataSet mockDataSet = mock(DataSet.class);
		/* keyspace */
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("otherKeyspaceName");
		keyspace.setStrategy(StrategyModel.SIMPLE_STRATEGY);
		keyspace.setReplicationFactor(1);
		List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();

		/* column family 1 */
		ColumnFamilyModel beautifulColumnFamily = new ColumnFamilyModel();
		beautifulColumnFamily.setName("beautifulColumnFamilyName");
		beautifulColumnFamily.setType(ColumnType.SUPER);
		beautifulColumnFamily.setKeyType(ComparatorType.TIMEUUIDTYPE);
		beautifulColumnFamily.setComparatorType(ComparatorType.UTF8TYPE);
		beautifulColumnFamily.setSubComparatorType(ComparatorType.LONGTYPE);
		beautifulColumnFamily.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

        beautifulColumnFamily.setComment("amazing comment");
        beautifulColumnFamily.setCompactionStrategy("LeveledCompactionStrategy");
        List<CompactionStrategyOptionModel> compactionStrategyOptions = new ArrayList<CompactionStrategyOptionModel>();
        compactionStrategyOptions.add(new CompactionStrategyOptionModel("sstable_size_in_mb", "10"));
        beautifulColumnFamily.setCompactionStrategyOptions(compactionStrategyOptions);
        beautifulColumnFamily.setGcGraceSeconds(9999);
        beautifulColumnFamily.setMaxCompactionThreshold(31);
        beautifulColumnFamily.setMinCompactionThreshold(3);
        beautifulColumnFamily.setReadRepairChance(0.1d);
        beautifulColumnFamily.setReplicationOnWrite(Boolean.FALSE);


		List<RowModel> rows = new ArrayList<RowModel>();

		/* row1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("13816710-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.TIME_UUID_TYPE));
		List<SuperColumnModel> superColumns1 = new ArrayList<SuperColumnModel>();
		superColumns1.add(constructDefinedSuperColumnForMock(11));
		superColumns1.add(constructDefinedSuperColumnForMock(12));

		row1.setSuperColumns(superColumns1);
		rows.add(row1);

		/* row2 */
		RowModel row2 = new RowModel();
		row2.setKey(new GenericType("13818e20-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.TIME_UUID_TYPE));
		List<SuperColumnModel> superColumns2 = new ArrayList<SuperColumnModel>();
		superColumns2.add(constructDefinedSuperColumnForMock(21));
		superColumns2.add(constructDefinedSuperColumnForMock(22));
		superColumns2.add(constructDefinedSuperColumnForMock(23));

		row2.setSuperColumns(superColumns2);
		rows.add(row2);

		beautifulColumnFamily.setRows(rows);
		columnFamilies.add(beautifulColumnFamily);

		/* column family 2 */
		ColumnFamilyModel columnFamily2 = new ColumnFamilyModel();
		columnFamily2.setName("amazingColumnFamilyName");
		columnFamily2.setType(ColumnType.STANDARD);
		columnFamily2.setKeyType(ComparatorType.UTF8TYPE);
		columnFamily2.setComparatorType(ComparatorType.UTF8TYPE);
		columnFamily2.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

		columnFamilies.add(columnFamily2);

		/* column family 3 with index */
		ColumnFamilyModel columnFamily3 = new ColumnFamilyModel();
		columnFamily3.setName("columnFamilyWithSecondaryIndex");
		columnFamily3.setType(ColumnType.STANDARD);
		columnFamily3.setKeyType(ComparatorType.UTF8TYPE);
		columnFamily3.setComparatorType(ComparatorType.UTF8TYPE);
		columnFamily3.setDefaultColumnValueType(ComparatorType.UTF8TYPE);
		columnFamily3.addColumnMetadata(new ColumnMetadataModel(
                "columnWithSecondaryIndexAndValidationClassAsLongType", ComparatorType.LONGTYPE, ColumnIndexType.KEYS, null));
		columnFamilies.add(columnFamily3);

        /* column family 4 with index */
        ColumnFamilyModel columnFamily4 = new ColumnFamilyModel();
        columnFamily4.setName("columnFamilyWithSecondaryIndexAndIndexName");
        columnFamily4.setType(ColumnType.STANDARD);
        columnFamily4.setKeyType(ComparatorType.UTF8TYPE);
        columnFamily4.setComparatorType(ComparatorType.UTF8TYPE);
        columnFamily4.setDefaultColumnValueType(ComparatorType.UTF8TYPE);
        columnFamily4.addColumnMetadata(new ColumnMetadataModel(
                "columnWithSecondaryIndexAndValidationClassAsUTF8Type", ComparatorType.UTF8TYPE, ColumnIndexType.KEYS,
                "columnWithSecondaryIndexHaveIndexNameAndValidationClassAsUTF8Type"));

        columnFamilies.add(columnFamily4);

        /* column family 5 with column validation class */
        ColumnFamilyModel columnFamily5 = new ColumnFamilyModel();
        columnFamily5.setName("columnFamilyWithColumnValidationClass");
        columnFamily5.setType(ColumnType.STANDARD);
        columnFamily5.setKeyType(ComparatorType.UTF8TYPE);
        columnFamily5.setComparatorType(ComparatorType.UTF8TYPE);
        columnFamily5.setDefaultColumnValueType(ComparatorType.UTF8TYPE);
        columnFamily5.addColumnMetadata(new ColumnMetadataModel("columnWithValidationClassAsUTF8Type",
                ComparatorType.UTF8TYPE, null, null));

        columnFamilies.add(columnFamily5);

		keyspace.setColumnFamilies(columnFamilies);

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	private static SuperColumnModel constructDefinedSuperColumnForMock(int columnNumber) {
		SuperColumnModel superColumnModel = new SuperColumnModel();
		superColumnModel.setName(new GenericType("name" + columnNumber, GenericTypeEnum.UTF_8_TYPE));
		List<ColumnModel> columns = new ArrayList<ColumnModel>();
		columns.add(constructDefinedColumnForMock(columnNumber + "1", "value" + columnNumber + "1"));
		columns.add(constructDefinedColumnForMock(columnNumber + "2", "value" + columnNumber + "2"));
		superColumnModel.setColumns(columns);
		return superColumnModel;
	}

	private static ColumnModel constructDefinedColumnForMock(String name, String value) {
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName(new GenericType(name, GenericTypeEnum.LONG_TYPE));
		columnModel.setValue(new GenericType(value, GenericTypeEnum.UTF_8_TYPE));
		return columnModel;
	}

	public static DataSet getMockDataSetWithDefinedValuesSimple() {
		DataSet mockDataSet = mock(DataSet.class);
		/* keyspace */
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("otherKeyspaceName");
		keyspace.setStrategy(StrategyModel.SIMPLE_STRATEGY);
		keyspace.setReplicationFactor(1);
		List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();
		columnFamilies.add(constructColumnFamily1ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily2ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily3ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily4ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily5ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily6ForDefinedValueSimple());
		columnFamilies.add(constructColumnFamily7ForDefinedValueSimple());
		keyspace.setColumnFamilies(columnFamilies);

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	private static ColumnFamilyModel constructColumnFamily7ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName7");
		columnFamily.setType(ColumnType.SUPER);
		columnFamily.setKeyType(ComparatorType.LONGTYPE);
		columnFamily.setComparatorType(ComparatorType.UTF8TYPE);
		columnFamily.setSubComparatorType(ComparatorType.UTF8TYPE);
		columnFamily.setDefaultColumnValueType(ComparatorType.COUNTERTYPE);
		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("10", GenericTypeEnum.LONG_TYPE));
		List<SuperColumnModel> superColumns = new ArrayList<SuperColumnModel>();
		row1.setSuperColumns(superColumns);
		SuperColumnModel superColumn = new SuperColumnModel();
		superColumn.setName(new GenericType("superColumnName11", GenericTypeEnum.UTF_8_TYPE));
		superColumns.add(superColumn);

		List<ColumnModel> columns = new ArrayList<ColumnModel>();
		superColumn.setColumns(columns);

		ColumnModel columnModel111 = new ColumnModel();
		columnModel111.setName(new GenericType("counter111", GenericTypeEnum.UTF_8_TYPE));
		columnModel111.setValue(new GenericType("111", GenericTypeEnum.COUNTER_TYPE));
		columns.add(columnModel111);
		ColumnModel columnModel112 = new ColumnModel();
		columnModel112.setName(new GenericType("counter112", GenericTypeEnum.UTF_8_TYPE));
		columnModel112.setValue(new GenericType("112", GenericTypeEnum.COUNTER_TYPE));
		columns.add(columnModel112);

		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily6ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName6");
		columnFamily.setKeyType(ComparatorType.LONGTYPE);
		columnFamily.setComparatorType(ComparatorType.UTF8TYPE);
		columnFamily.setDefaultColumnValueType(ComparatorType.COUNTERTYPE);
		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("10", GenericTypeEnum.LONG_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11.setName(new GenericType("counter11", GenericTypeEnum.UTF_8_TYPE));
		columnModel11.setValue(new GenericType("11", GenericTypeEnum.COUNTER_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12.setName(new GenericType("counter12", GenericTypeEnum.UTF_8_TYPE));
		columnModel12.setValue(new GenericType("12", GenericTypeEnum.COUNTER_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily5ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName5");
		columnFamily.setDefaultColumnValueType(ComparatorType.UTF8TYPE);
		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("01", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11.setName(new GenericType("01", GenericTypeEnum.BYTES_TYPE));
		columnModel11.setValue(new GenericType("1", GenericTypeEnum.LONG_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12.setName(new GenericType("02", GenericTypeEnum.BYTES_TYPE));
		columnModel12.setValue(new GenericType("19652258", GenericTypeEnum.LONG_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily4ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName4");
		columnFamily.setDefaultColumnValueType(ComparatorType.LONGTYPE);
		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("01", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11.setName(new GenericType("01", GenericTypeEnum.BYTES_TYPE));
		columnModel11.setValue(new GenericType("1", GenericTypeEnum.LONG_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12.setName(new GenericType("02", GenericTypeEnum.BYTES_TYPE));
		columnModel12.setValue(new GenericType("19652258", GenericTypeEnum.LONG_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily3ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName3");
		columnFamily.setKeyType(ComparatorType.UUIDTYPE);
		columnFamily.setComparatorType(ComparatorType.LEXICALUUIDTYPE);

		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("13816710-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.UUID_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11
				.setName(new GenericType("13816710-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.LEXICAL_UUID_TYPE));
		columnModel11.setValue(new GenericType("11", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12
				.setName(new GenericType("13818e20-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.LEXICAL_UUID_TYPE));
		columnModel12.setValue(new GenericType("12", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily2ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName2");
		columnFamily.setKeyType(ComparatorType.LONGTYPE);
		columnFamily.setComparatorType(ComparatorType.INTEGERTYPE);

		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("10", GenericTypeEnum.LONG_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11.setName(new GenericType("11", GenericTypeEnum.INTEGER_TYPE));
		columnModel11.setValue(new GenericType("11", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12.setName(new GenericType("12", GenericTypeEnum.INTEGER_TYPE));
		columnModel12.setValue(new GenericType("12", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	private static ColumnFamilyModel constructColumnFamily1ForDefinedValueSimple() {
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName");
		columnFamily.setKeyType(ComparatorType.TIMEUUIDTYPE);
		columnFamily.setComparatorType(ComparatorType.UTF8TYPE);

		List<RowModel> rows = new ArrayList<RowModel>();

		/* row 1 */
		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("13816710-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.TIME_UUID_TYPE));
		List<ColumnModel> columns1 = new ArrayList<ColumnModel>();
		ColumnModel columnModel11 = new ColumnModel();
		columnModel11.setName(new GenericType("name11", GenericTypeEnum.UTF_8_TYPE));
		columnModel11.setValue(new GenericType("11", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel11);
		ColumnModel columnModel12 = new ColumnModel();
		columnModel12.setName(new GenericType("name12", GenericTypeEnum.UTF_8_TYPE));
		columnModel12.setValue(new GenericType("12", GenericTypeEnum.BYTES_TYPE));
		columns1.add(columnModel12);
		row1.setColumns(columns1);
		rows.add(row1);

		/* row 2 */
		RowModel row2 = new RowModel();
		row2.setKey(new GenericType("13818e20-1dd2-11b2-879a-782bcb80ff6a", GenericTypeEnum.TIME_UUID_TYPE));
		List<ColumnModel> columns2 = new ArrayList<ColumnModel>();
		ColumnModel columnModel21 = new ColumnModel();
		columnModel21.setName(new GenericType("name21", GenericTypeEnum.UTF_8_TYPE));
		columnModel21.setValue(new GenericType("21", GenericTypeEnum.BYTES_TYPE));
		columns2.add(columnModel21);
		ColumnModel columnModel22 = new ColumnModel();
		columnModel22.setName(new GenericType("name22", GenericTypeEnum.UTF_8_TYPE));
		columnModel22.setValue(new GenericType("22", GenericTypeEnum.BYTES_TYPE));
		columns2.add(columnModel22);
		row2.setColumns(columns2);
		rows.add(row2);

		columnFamily.setRows(rows);

		return columnFamily;
	}

	public static DataSet getMockDataSetWithSuperColumn() {
		DataSet mockDataSet = mock(DataSet.class);
		/* keyspace */
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("beautifulKeyspaceName");
		List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("beautifulColumnFamilyName");
		columnFamily.setType(ColumnType.SUPER);

		List<RowModel> rows = new ArrayList<RowModel>();

		RowModel row1 = new RowModel();
		row1.setKey(new GenericType("01", GenericTypeEnum.BYTES_TYPE));
		List<SuperColumnModel> superColumns1 = new ArrayList<SuperColumnModel>();

		SuperColumnModel superColumn11 = new SuperColumnModel();
		superColumn11.setName(new GenericType("11", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns11 = new ArrayList<ColumnModel>();
		columns11.add(constructDefaultColumnForMock("1110", "1110"));
		columns11.add(constructDefaultColumnForMock("1120", "1120"));
		superColumn11.setColumns(columns11);
		superColumns1.add(superColumn11);

		SuperColumnModel superColumn12 = new SuperColumnModel();
		superColumn12.setName(new GenericType("12", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns12 = new ArrayList<ColumnModel>();
		columns12.add(constructDefaultColumnForMock("1210", "1210"));
		columns12.add(constructDefaultColumnForMock("1220", "1220"));
		superColumn12.setColumns(columns12);
		superColumns1.add(superColumn12);

		row1.setSuperColumns(superColumns1);
		rows.add(row1);

		RowModel row2 = new RowModel();
		row2.setKey(new GenericType("02", GenericTypeEnum.BYTES_TYPE));
		List<SuperColumnModel> superColumns2 = new ArrayList<SuperColumnModel>();
		SuperColumnModel superColumn21 = new SuperColumnModel();
		superColumn21.setName(new GenericType("21", GenericTypeEnum.BYTES_TYPE));
		List<ColumnModel> columns21 = new ArrayList<ColumnModel>();
		columns21.add(constructDefaultColumnForMock("2110", "2110"));
		columns21.add(constructDefaultColumnForMock("2120", "2120"));
		superColumn21.setColumns(columns21);
		superColumns2.add(superColumn21);
		row2.setSuperColumns(superColumns2);

		rows.add(row2);

		columnFamily.setRows(rows);
		columnFamilies.add(columnFamily);
		keyspace.setColumnFamilies(columnFamilies);

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	public static DataSet getMockDataSetWithSchemaAndDefaultColumnValueValidator() {
		DataSet mockDataSet = mock(DataSet.class);
		/* keyspace */
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("keyspace");
		List<ColumnFamilyModel> columnFamilies = new ArrayList<ColumnFamilyModel>();

		/* column family */
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("columnFamily");
		columnFamily.setDefaultColumnValueType(ComparatorType.LONGTYPE);

		columnFamilies.add(columnFamily);
		keyspace.setColumnFamilies(columnFamilies);

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	public static DataSet getMockDataSetWithDefaultValuesAndReplicationFactor2() {
		DataSet mockDataSet = mock(DataSet.class);
		KeyspaceModel keyspace = contructDefaultValuesKeyspace();
		keyspace.setReplicationFactor(2);

		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	public static DataSet getMockDataSetWithDefaultValuesAndNetworkTopologyStrategy() {
		DataSet mockDataSet = mock(DataSet.class);
		KeyspaceModel keyspace = contructDefaultValuesKeyspace();
		keyspace.setStrategy(StrategyModel.NETWORK_TOPOLOGY_STRATEGY);
		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());
		return mockDataSet;
	}

	public static DataSet getMockDataSetWithCompositeType() {
		DataSet mockDataSet = mock(DataSet.class);
		KeyspaceModel keyspace = new KeyspaceModel();
		keyspace.setName("compositeKeyspace");
		keyspace.getColumnFamilies();

		/* column family */
		ColumnFamilyModel columnFamily = new ColumnFamilyModel();
		columnFamily.setName("columnFamilyWithCompositeType");
		columnFamily.setKeyType(ComparatorType.UTF8TYPE);
		columnFamily.setComparatorType(ComparatorType.COMPOSITETYPE);
		columnFamily.setComparatorTypeAlias("(LongType,UTF8Type,IntegerType)");
		columnFamily.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

		/* row1 */
		RowModel row = new RowModel();
		row.setKey(new GenericType("row1", GenericTypeEnum.UTF_8_TYPE));

		/* column1 */
		ColumnModel column1 = new ColumnModel();
		column1.setName(new GenericType(new String[] { "11", "aa", "11" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column1.setValue(new GenericType("v1", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column1);

		/* column2 */
		ColumnModel column2 = new ColumnModel();
		column2.setName(new GenericType(new String[] { "11", "ab", "11" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column2.setValue(new GenericType("v2", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column2);

		/* column3 */
		ColumnModel column3 = new ColumnModel();
		column3.setName(new GenericType(new String[] { "11", "ab", "12" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column3.setValue(new GenericType("v3", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column3);

		/* column4 */
		ColumnModel column4 = new ColumnModel();
		column4.setName(new GenericType(new String[] { "12", "aa", "11" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column4.setValue(new GenericType("v4", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column4);

		/* column5 */
		ColumnModel column5 = new ColumnModel();
		column5.setName(new GenericType(new String[] { "12", "ab", "11" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column5.setValue(new GenericType("v5", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column5);

		/* column6 */
		ColumnModel column6 = new ColumnModel();
		column6.setName(new GenericType(new String[] { "12", "zz", "12" }, new GenericTypeEnum[] {
				GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE, GenericTypeEnum.INTEGER_TYPE }));
		column6.setValue(new GenericType("v6", GenericTypeEnum.UTF_8_TYPE));
		row.getColumns().add(column6);

		columnFamily.getRows().add(row);
		keyspace.getColumnFamilies().add(columnFamily);

		/* column family 2 with composite Type row key */
		ColumnFamilyModel columnFamily2 = new ColumnFamilyModel();
		columnFamily2.setName("columnFamilyWithRowKeyCompositeType");
		columnFamily2.setKeyType(ComparatorType.COMPOSITETYPE);
		columnFamily2.setKeyTypeAlias("(LongType,UTF8Type)");
		columnFamily2.setComparatorType(ComparatorType.UTF8TYPE);
		columnFamily2.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

		/* row1 */
		RowModel row21 = new RowModel();
		row21.setKey(new GenericType(new String[] { "12", "az" }, new GenericTypeEnum[] { GenericTypeEnum.LONG_TYPE,
				GenericTypeEnum.UTF_8_TYPE }));

		/* column1 */
		ColumnModel column21 = new ColumnModel();
		column21.setName(new GenericType("a", GenericTypeEnum.UTF_8_TYPE));
		column21.setValue(new GenericType("a", GenericTypeEnum.UTF_8_TYPE));
		row21.getColumns().add(column21);
		columnFamily2.getRows().add(row21);

		keyspace.getColumnFamilies().add(columnFamily2);
		when(mockDataSet.getKeyspace()).thenReturn(keyspace);
		when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());

		return mockDataSet;
	}

    public static DataSet getMockDataSetWithNullColumnValue() {
        DataSet mockDataSet = mock(DataSet.class);
        KeyspaceModel keyspace = new KeyspaceModel();
        keyspace.setName("keyspaceWithNullColumnValue");
        keyspace.getColumnFamilies();

        /* column family */
        ColumnFamilyModel columnFamily = new ColumnFamilyModel();
        columnFamily.setName("columnFamilyWithNullColumnValue");
        columnFamily.setKeyType(ComparatorType.UTF8TYPE);
        columnFamily.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

        /* row1 */
        RowModel row = new RowModel();
        row.setKey(new GenericType("rowWithNullColumnValue", GenericTypeEnum.UTF_8_TYPE));

        /* column1 */
        ColumnModel column1 = new ColumnModel();
        column1.setName(new GenericType("columnWithNullValue",GenericTypeEnum.UTF_8_TYPE));
        row.getColumns().add(column1);

        columnFamily.getRows().add(row);
        keyspace.getColumnFamilies().add(columnFamily);

        when(mockDataSet.getKeyspace()).thenReturn(keyspace);
        when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());

        return mockDataSet;
    }

    public static DataSet getMockDataSetWithTimestampedColumn() {
        DataSet mockDataSet = mock(DataSet.class);
        KeyspaceModel keyspace = new KeyspaceModel();
        keyspace.setName("keyspaceWithTimestampedColumn");
        keyspace.getColumnFamilies();

        /* column family */
        ColumnFamilyModel columnFamily = new ColumnFamilyModel();
        columnFamily.setName("columnFamilyWithTimestampedColumn");
        columnFamily.setKeyType(ComparatorType.UTF8TYPE);
        columnFamily.setDefaultColumnValueType(ComparatorType.UTF8TYPE);

        /* row1 */
        RowModel row = new RowModel();
        row.setKey(new GenericType("rowWithTimestampedColumn", GenericTypeEnum.UTF_8_TYPE));

        /* column1 */
        ColumnModel column1 = new ColumnModel();
        column1.setName(new GenericType("columnWithTimestamp",GenericTypeEnum.UTF_8_TYPE));
        column1.setTimestamp(new Long(2020L));
        row.getColumns().add(column1);

        columnFamily.getRows().add(row);
        keyspace.getColumnFamilies().add(columnFamily);

        when(mockDataSet.getKeyspace()).thenReturn(keyspace);
        when(mockDataSet.getColumnFamilies()).thenReturn(keyspace.getColumnFamilies());

        return mockDataSet;
    }

}
