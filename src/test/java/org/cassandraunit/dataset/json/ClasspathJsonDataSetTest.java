package org.cassandraunit.dataset.json;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.*;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

import java.util.List;

import static org.cassandraunit.SampleDataSetChecker.assertThatKeyspaceModelWithCompositeTypeIsOk;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
 */
public class ClasspathJsonDataSetTest {

    @Test
    public void shouldGetAJsonDataSetStructure() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefaultValues.json");
        assertThat(dataSet, notNullValue());

        KeyspaceModel keyspace = dataSet.getKeyspace();
        assertThat(keyspace, notNullValue());
        assertThat(keyspace.getName(), is("beautifulKeyspaceName"));
        assertThat(keyspace.getReplicationFactor(), is(1));
        assertThat(keyspace.getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

        List<ColumnFamilyModel> actualColumnFamilies = dataSet.getColumnFamilies();
        assertThat(actualColumnFamilies, notNullValue());
        assertThat(actualColumnFamilies.size(), is(1));

        ColumnFamilyModel actualColumnFamily1 = actualColumnFamilies.get(0);
        assertThat(actualColumnFamily1, notNullValue());
        assertThat(actualColumnFamily1.getName(), is("columnFamily1"));
        assertThat(actualColumnFamily1.getType(), is(ColumnType.STANDARD));
        assertThat(actualColumnFamily1.getKeyType().getTypeName(), is(ComparatorType.BYTESTYPE.getTypeName()));
        assertThat(actualColumnFamily1.getComparatorType().getTypeName(), is(ComparatorType.BYTESTYPE.getTypeName()));
        assertThat(actualColumnFamily1.getSubComparatorType(), nullValue());
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfNull() {
        DataSet dataSet = new ClassPathJsonDataSet(null);
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfDataSetNotExist() {
        DataSet dataSet = new ClassPathJsonDataSet("json/unknown.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfMissingKeyspaceName() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadMissingKeyspaceName.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidStrategy() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadUnknownStrategy.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfMissingColumnFamilyName() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadMissingColumnFamilyName.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidColumnFamilyType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadInvalidColumnFamilyType.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidRowKeyType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadInvalidKeyType.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidComparatorType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadInvalidComparatorType.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidSubComparatorType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadInvalidSubComparatorType.json");
        dataSet.getKeyspace();
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAJsonDataSetStructureBecauseOfInvalidDefaultColumnValueType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadInvalidDefaultColumnValueType.json");
        dataSet.getKeyspace();
    }

    @Test
    public void shouldGetAJsonDataSetWithData() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefaultValues.json");
        assertThat(dataSet, notNullValue());
        assertThat(dataSet.getKeyspace(), notNullValue());

        List<ColumnFamilyModel> actualColumnFamilies = dataSet.getColumnFamilies();
        assertThat(actualColumnFamilies, notNullValue());
        assertThat(actualColumnFamilies.size(), is(1));

        ColumnFamilyModel actualColumnFamily1 = actualColumnFamilies.get(0);
        assertThat(actualColumnFamily1, notNullValue());
        assertThat(actualColumnFamily1.getName(), is("columnFamily1"));

        List<RowModel> actualRows = actualColumnFamily1.getRows();
        assertThat(actualRows, notNullValue());
        assertThat(actualRows.size(), is(1));

        RowModel actualRow = actualRows.get(0);
        assertThat(actualRow, notNullValue());
        assertThat(actualRow.getKey().getValue(), is("01"));
        assertThat(actualRow.getKey().getType(), is(GenericTypeEnum.BYTES_TYPE));

        List<ColumnModel> actualColumns = actualRow.getColumns();
        assertThat(actualColumns, notNullValue());
        assertThat(actualColumns.size(), is(1));

        ColumnModel actualColumn = actualColumns.get(0);
        assertThat(actualColumn, notNullValue());
        assertThat(actualColumn.getName().getValue(), is("02"));
        assertThat(actualColumn.getName().getType(), is(GenericTypeEnum.BYTES_TYPE));
        assertThat(actualColumn.getValue().getValue(), is("03"));
        assertThat(actualColumn.getValue().getType(), is(GenericTypeEnum.BYTES_TYPE));
    }

    @Test
    public void shouldGetAJsonDataSetWithDefinedValuesAndData() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
        assertThat(dataSet, notNullValue());

        KeyspaceModel actualKeyspace = dataSet.getKeyspace();
        assertThat(actualKeyspace, notNullValue());
        assertThat(actualKeyspace.getName(), is("beautifulDefinedKeyspaceName"));
        assertThat(actualKeyspace.getReplicationFactor(), is(1));
        assertThat(actualKeyspace.getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

        assertThat(dataSet.getColumnFamilies(), notNullValue());
        assertThat(dataSet.getColumnFamilies().size(), is(4));

        ColumnFamilyModel actualColumnFamily1 = dataSet.getColumnFamilies().get(0);
        assertThat(actualColumnFamily1, notNullValue());
        assertThat(actualColumnFamily1.getName(), is("columnFamily1"));
        assertThat(actualColumnFamily1.getKeyType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));
        assertThat(actualColumnFamily1.getComparatorType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));
        assertThat(actualColumnFamily1.getSubComparatorType(), is(nullValue()));
        assertThat(actualColumnFamily1.getComment(), is("amazing comment"));
        assertThat(actualColumnFamily1.getCompactionStrategy(), is("LeveledCompactionStrategy"));
        assertThat(actualColumnFamily1.getCompactionStrategyOptions().get(0).getName(), is("sstable_size_in_mb"));
        assertThat(actualColumnFamily1.getCompactionStrategyOptions().get(0).getValue(), is("10"));
        assertThat(actualColumnFamily1.getGcGraceSeconds(), is(9999));
        assertThat(actualColumnFamily1.getMaxCompactionThreshold(), is(31));
        assertThat(actualColumnFamily1.getMinCompactionThreshold(), is(3));
        assertThat(actualColumnFamily1.getReadRepairChance(), is(0.1d));
        assertThat(actualColumnFamily1.getReplicationOnWrite(), is(Boolean.FALSE));

        List<RowModel> actualRows1 = actualColumnFamily1.getRows();
        assertThat(actualRows1, notNullValue());
        assertThat(actualRows1.size(), is(1));

        RowModel actualRow11 = actualRows1.get(0);
        assertThat(actualRow11, notNullValue());
        assertThat(actualRow11.getKey().getValue(), is("key01"));
        assertThat(actualRow11.getKey().getType(), is(GenericTypeEnum.UTF_8_TYPE));

        List<ColumnModel> actualColumns11 = actualRow11.getColumns();
        assertThat(actualColumns11, notNullValue());
        assertThat(actualColumns11.size(), is(1));

        ColumnModel actualColumn111 = actualColumns11.get(0);
        assertThat(actualColumn111, notNullValue());
        assertThat(actualColumn111.getName().getValue(), is("columnName1"));
        assertThat(actualColumn111.getName().getType(), is(GenericTypeEnum.UTF_8_TYPE));
        assertThat(actualColumn111.getValue().getValue(), is("columnValue1"));
        assertThat(actualColumn111.getValue().getType(), is(GenericTypeEnum.UTF_8_TYPE));

        ColumnFamilyModel actualColumnFamily2 = dataSet.getColumnFamilies().get(1);
        assertThat(actualColumnFamily2, notNullValue());
        assertThat(actualColumnFamily2.getName(), is("columnFamily2"));
        assertThat(actualColumnFamily2.getKeyType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));
        assertThat(actualColumnFamily2.getComparatorType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));
        assertThat(actualColumnFamily2.getSubComparatorType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));

        List<RowModel> actualRows2 = actualColumnFamily2.getRows();
        assertThat(actualRows2, notNullValue());
        assertThat(actualRows2.size(), is(1));

        RowModel actualRow21 = actualRows2.get(0);
        assertThat(actualRow21, notNullValue());
        assertThat(actualRow21.getKey().getValue(), is("key02"));
        assertThat(actualRow21.getKey().getType(), is(GenericTypeEnum.UTF_8_TYPE));
        assertThat(actualRow21.getColumns(), notNullValue());
        assertThat(actualRow21.getColumns().isEmpty(), is(true));

        List<SuperColumnModel> actualSuperColumns21 = actualRow21.getSuperColumns();
        assertThat(actualSuperColumns21, notNullValue());
        assertThat(actualSuperColumns21.size(), is(1));
        SuperColumnModel actualSuperColumn211 = actualSuperColumns21.get(0);
        assertThat(actualSuperColumn211, notNullValue());
        assertThat(actualSuperColumn211.getName().getValue(), is("superColumnName2"));
        assertThat(actualSuperColumn211.getName().getType(), is(GenericTypeEnum.UTF_8_TYPE));

        List<ColumnModel> actualColumns211 = actualSuperColumn211.getColumns();
        assertThat(actualColumns211, notNullValue());
        assertThat(actualColumns211.size(), is(1));

        ColumnModel actualColumn2111 = actualColumns211.get(0);
        assertThat(actualColumn2111, notNullValue());
        assertThat(actualColumn2111.getName().getValue(), is("columnName2"));
        assertThat(actualColumn2111.getName().getType(), is(GenericTypeEnum.UTF_8_TYPE));
        assertThat(actualColumn2111.getValue().getValue(), is("2"));
        assertThat(actualColumn2111.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));
    }

    @Test
    public void shouldGetAStandardCounterColumnFamily() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
        List<RowModel> actualRows = dataSet.getColumnFamilies().get(2).getRows();
        assertThat(actualRows, notNullValue());
        assertThat(actualRows.size(), is(1));

        RowModel actualRow1 = actualRows.get(0);
        assertThat(actualRow1, notNullValue());
        assertThat(actualRow1.getKey().getValue(), is("key10"));
        assertThat(actualRow1.getKey().getType(), is(GenericTypeEnum.UTF_8_TYPE));

        List<ColumnModel> actualColumns1 = actualRow1.getColumns();
        assertThat(actualColumns1, notNullValue());
        assertThat(actualColumns1.size(), is(1));

        ColumnModel actualColumn11 = actualColumns1.get(0);
        assertThat(actualColumn11, notNullValue());
        assertThat(actualColumn11.getName().getValue(), is("columnName11"));
        assertThat(actualColumn11.getValue().getValue(), is("11"));
        assertThat(actualColumn11.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));
    }

    @Test
    public void shouldGetASuperCounterColumnFamily() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
        assertThat(dataSet.getColumnFamilies().get(3), notNullValue());
        assertThat(dataSet.getColumnFamilies().get(3).getName(), is("superCounterColumnFamily"));

        List<RowModel> actualRows = dataSet.getColumnFamilies().get(3).getRows();
        assertThat(actualRows, notNullValue());
        assertThat(actualRows.size(), is(1));

        RowModel actualRow = actualRows.get(0);
        assertThat(actualRow, notNullValue());
        assertThat(actualRow.getKey().getValue(), is("key10"));
        assertThat(actualRow.getKey().getType(), is(GenericTypeEnum.UTF_8_TYPE));

        List<SuperColumnModel> actualSuperColumns = actualRow.getSuperColumns();
        assertThat(actualSuperColumns, notNullValue());
        assertThat(actualSuperColumns.size(), is(1));

        SuperColumnModel actualSuperColumn = actualSuperColumns.get(0);
        assertThat(actualSuperColumn, notNullValue());
        assertThat(actualSuperColumn.getName().getValue(), is("superColumnName11"));

        List<ColumnModel> actualColumns = actualSuperColumn.getColumns();
        assertThat(actualColumns, notNullValue());
        assertThat(actualColumns.size(), is(1));

        ColumnModel actualColumn = actualColumns.get(0);
        assertThat(actualColumn, notNullValue());
        assertThat(actualColumn.getName().getValue(), is("columnName111"));
        assertThat(actualColumn.getValue().getValue(), is("111"));
        assertThat(actualColumn.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetCounterColumnFamilyBecauseThereIsFunctionOverridingDefaultValueType() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadCounterColumnFamilyWithFunction.json");
        dataSet.getKeyspace();
    }

    @Test
    public void shouldGetAColumnFamilyWithSecondaryIndex() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithSecondaryIndex.json");

        ColumnMetadataModel actualColumnMetadataModel1 = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0);
        assertThat(actualColumnMetadataModel1.getColumnName().getValue(), is("columnWithIndexAndUTF8ValidationClass"));
        assertThat(actualColumnMetadataModel1.getColumnIndexType(), is(ColumnIndexType.KEYS));
        assertThat(actualColumnMetadataModel1.getValidationClass(), is(ComparatorType.UTF8TYPE));

        ColumnMetadataModel actualColumnMetadataModel2 = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1);
        assertThat(actualColumnMetadataModel2.getColumnName().getValue(), is("columnWithIndexAndIndexNameAndUTF8ValidationClass"));
        assertThat(actualColumnMetadataModel2.getColumnIndexType(), is(ColumnIndexType.KEYS));
        assertThat(actualColumnMetadataModel2.getValidationClass(), is(ComparatorType.UTF8TYPE));
        assertThat(actualColumnMetadataModel2.getIndexName(), is("indexNameOfTheIndex"));

        ColumnMetadataModel actualColumnMetadataModel3 = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2);
        assertThat(actualColumnMetadataModel3.getColumnName().getValue(), is("columnWithUTF8ValidationClass"));
        assertThat(actualColumnMetadataModel3.getColumnIndexType(), nullValue());
        assertThat(actualColumnMetadataModel3.getValidationClass(), is(ComparatorType.UTF8TYPE));
    }

    @Test
    public void shouldGetAColumnFamilyWithCompositeType() throws Exception {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithCompositeType.json");
        assertThatKeyspaceModelWithCompositeTypeIsOk(dataSet);
    }

    @Test(expected = ParseException.class)
    public void shouldNotGetAColumnFamilyWithCompositeType() throws Exception {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithBadCompositeType.json");
        dataSet.getKeyspace();
    }

    @Test
    public void shouldGetAColumnFamilyWithNullColumnValue() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithNullColumnValue.json");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithNullColumnValue"));
        ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
        assertThat(columnModel.getName().getValue(), is("columnWithNullColumnValue"));
        assertThat(columnModel.getValue(), nullValue());
    }

    @Test
    public void shouldGetAColumnFamilyWithMetadataAndFunction() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithMetadataAndFunctions.json");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithMetadata"));
        List<ColumnModel> columns = columnFamilyModel.getRows().get(0).getColumns();
        ColumnModel column1 = columns.get(0);
        assertThat(column1.getName().getValue(), is("column1"));
        assertThat(column1.getValue().getValue(), is("1"));
        assertThat(column1.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));

        ColumnModel column2 = columns.get(1);
        assertThat(column2.getName().getValue(), is("column2"));
        assertThat(column2.getValue().getValue(), is("2"));
        assertThat(column2.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));

        ColumnModel column3 = columns.get(2);
        assertThat(column3.getName().getValue(), is("column3"));
        assertThat(column3.getValue().getValue(), is("value3"));
        assertThat(column3.getValue().getType(), is(GenericTypeEnum.UTF_8_TYPE));
    }

    @Test
    public void shouldUseComparatorTypeForMetadataColumnName() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithComparatorType.json");
        ColumnMetadataModel columnMetadata = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0);
        assertThat(columnMetadata.getColumnName().getType(), is(GenericTypeEnum.TIME_UUID_TYPE));
    }

    @Test
    public void shouldGetAColumnFamilyWithColumnsInReverseOrder() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithReversedComparatorOnSimpleType.json");

        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithReversedComparatorOnSimpleType"));
        assertThat(columnFamilyModel.getComparatorType().getTypeName(), is(ComparatorType.UTF8TYPE.getTypeName()));
        assertThat(columnFamilyModel.getComparatorTypeAlias(), is("(reversed=true)"));

        List<ColumnModel> columns = columnFamilyModel.getRows().get(0).getColumns();

        ColumnModel column1 = columns.get(0);
        assertThat(column1.getName().getValue(), is("c"));
        assertThat(column1.getValue().getValue(), is("c"));

        ColumnModel column2 = columns.get(1);
        assertThat(column2.getName().getValue(), is("b"));
        assertThat(column2.getValue().getValue(), is("b"));

        ColumnModel column3 = columns.get(2);
        assertThat(column3.getName().getValue(), is("a"));
        assertThat(column3.getValue().getValue(), is("a"));
    }

    @Test
    public void shouldGetAColumnFamilyWithCompositeColumnsInReverseOrder() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithReversedComparatorOnCompositeTypes.json");

        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithReversedComparatorOnCompositeTypes"));
        assertThat(columnFamilyModel.getComparatorType().getTypeName(), is(ComparatorType.COMPOSITETYPE.getTypeName()));
        assertThat(columnFamilyModel.getComparatorTypeAlias(), is("(LongType(reversed=true),UTF8Type,IntegerType(reversed=true))"));

        GenericTypeEnum[] expecTedTypesBelongingCompositeType = new GenericTypeEnum[] { GenericTypeEnum.LONG_TYPE, GenericTypeEnum.UTF_8_TYPE,
                GenericTypeEnum.INTEGER_TYPE };
        List<ColumnModel> columns = columnFamilyModel.getRows().get(0).getColumns();

        assertThat(columns.get(0).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(0).getName().getCompositeValues(), is(new String[] { "12", "aa", "11" }));
        assertThat(columns.get(0).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(0).getValue().getValue(), is("v6"));

        assertThat(columns.get(1).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(1).getName().getCompositeValues(), is(new String[] { "12", "ab", "12" }));
        assertThat(columns.get(1).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(1).getValue().getValue(), is("v5"));

        assertThat(columns.get(2).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(2).getName().getCompositeValues(), is(new String[] { "12", "ab", "11" }));
        assertThat(columns.get(2).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(2).getValue().getValue(), is("v4"));

        assertThat(columns.get(3).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(3).getName().getCompositeValues(), is(new String[] { "11", "aa", "11" }));
        assertThat(columns.get(3).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(3).getValue().getValue(), is("v3"));

        assertThat(columns.get(4).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(4).getName().getCompositeValues(), is(new String[] { "11", "ab", "12" }));
        assertThat(columns.get(4).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(4).getValue().getValue(), is("v2"));

        assertThat(columns.get(5).getName().getType(), is(GenericTypeEnum.COMPOSITE_TYPE));
        assertThat(columns.get(5).getName().getCompositeValues(), is(new String[] { "11", "ab", "11" }));
        assertThat(columns.get(5).getName().getTypesBelongingCompositeType(), is(expecTedTypesBelongingCompositeType));
        assertThat(columns.get(5).getValue().getValue(), is("v1"));
    }
}
