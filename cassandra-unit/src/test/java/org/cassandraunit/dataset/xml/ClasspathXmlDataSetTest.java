package org.cassandraunit.dataset.xml;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.ColumnMetadataModel;
import org.cassandraunit.model.ColumnModel;
import org.cassandraunit.model.RowModel;
import org.cassandraunit.model.StrategyModel;
import org.cassandraunit.model.SuperColumnModel;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

import java.util.List;

import static org.cassandraunit.SampleDataSetChecker.assertThatKeyspaceModelWithCompositeTypeIsOk;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
 */
public class ClasspathXmlDataSetTest {

    @Test
    public void shouldGetAXmlDataSet() {

        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
        assertThat(dataSet, notNullValue());
    }

    @Test
    public void shouldNotGetAXmlDataSetBecauseNull() {
        try {
            DataSet dataSet = new ClassPathXmlDataSet(null);
            fail();
        } catch (ParseException e) {
            /* nothing to do, it what we want */
        }
    }

    @Test
    public void shouldNotGetAXmlDataSetBecauseItNotExist() {
        try {
            DataSet dataSet = new ClassPathXmlDataSet("xml/unknownDataSet.xml");
            fail();
        } catch (ParseException e) {
            /* nothing to do, it what we want */
        }
    }

    @Test
    public void shouldNotGetAXmlDataSetBecauseItIsInvalid() {
        try {
            DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetInvalidDataSet.xml");
            dataSet.getKeyspace();
            fail();
        } catch (ParseException e) {
            /* nothing to do, it what we want */
            assertThat(StringUtils.contains(e.getMessage(),
                    "Invalid content was found starting with element 'columnFamily'"), is(Boolean.TRUE));
        }
    }

    @Test
    public void shouldGetKeyspaceWithDefaultValues() {

        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
        assertThat(dataSet.getKeyspace(), notNullValue());
        assertThat(dataSet.getKeyspace().getName(), notNullValue());
        assertThat(dataSet.getKeyspace().getName(), is("beautifulKeyspaceName"));
        assertThat(dataSet.getKeyspace().getReplicationFactor(), is(1));
        assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

    }

    @Test
    public void shouldGetKeyspaceWithDefinedValues() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefinedValues.xml");
        assertThat(dataSet.getKeyspace(), notNullValue());
        assertThat(dataSet.getKeyspace().getName(), notNullValue());
        assertThat(dataSet.getKeyspace().getName(), is("otherKeyspaceName"));
        assertThat(dataSet.getKeyspace().getReplicationFactor(), is(2));
        assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));
    }

    @Test
    public void shouldGetOneColumnFamilyWithDefaultValues() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
        assertThat(dataSet.getColumnFamilies(), notNullValue());
        assertThat(dataSet.getColumnFamilies().isEmpty(), is(false));

        ColumnFamilyModel actualColumnFamily = dataSet.getColumnFamilies().get(0);
        assertThat(actualColumnFamily, notNullValue());
        assertThat(actualColumnFamily.getName(), is("columnFamily1"));
        assertThat(actualColumnFamily.getType(), is(ColumnType.STANDARD));
        assertThat(actualColumnFamily.getKeyType().getClassName(),is(ComparatorType.BYTESTYPE.getClassName()));
        assertThat(actualColumnFamily.getComparatorType().getClassName(),is(ComparatorType.BYTESTYPE.getClassName()));
        assertThat(actualColumnFamily.getSubComparatorType(), nullValue());
        assertThat(actualColumnFamily.getDefaultColumnValueType(), nullValue());

    }

    @Test
    public void shouldGetColumnFamiliesWithDefinedValues() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefinedValues.xml");
        assertThat(dataSet.getColumnFamilies(), notNullValue());
        assertThat(dataSet.getColumnFamilies().isEmpty(), is(false));

        ColumnFamilyModel beautifulColumnFamily = dataSet.getColumnFamilies().get(0);
        assertThat(beautifulColumnFamily, notNullValue());
        assertThat(beautifulColumnFamily.getName(), is("beautifulColumnFamilyName"));
        assertThat(beautifulColumnFamily.getType(), is(ColumnType.SUPER));
        assertThat(beautifulColumnFamily.getKeyType().getClassName(),is(ComparatorType.TIMEUUIDTYPE.getClassName()));
        assertThat(beautifulColumnFamily.getComparatorType().getClassName(),is(ComparatorType.UTF8TYPE.getClassName()));
        assertThat(beautifulColumnFamily.getSubComparatorType().getClassName(),is(ComparatorType.LONGTYPE.getClassName()));
        assertThat(beautifulColumnFamily.getDefaultColumnValueType().getClassName(),is(ComparatorType.UTF8TYPE.getClassName()));
        assertThat(beautifulColumnFamily.getComment(), is("amazing comment"));
        assertThat(beautifulColumnFamily.getCompactionStrategy(), is("LeveledCompactionStrategy"));
        assertThat(beautifulColumnFamily.getCompactionStrategyOptions().get(0).getName(), is("sstable_size_in_mb"));
        assertThat(beautifulColumnFamily.getCompactionStrategyOptions().get(0).getValue(), is("10"));
        assertThat(beautifulColumnFamily.getGcGraceSeconds(), is(9999));
        assertThat(beautifulColumnFamily.getMaxCompactionThreshold(), is(31));
        assertThat(beautifulColumnFamily.getMinCompactionThreshold(), is(3));
        assertThat(beautifulColumnFamily.getReadRepairChance(), is(0.1d));
        assertThat(beautifulColumnFamily.getReplicationOnWrite(), is(Boolean.FALSE));

        ColumnFamilyModel amazingColumnFamily = dataSet.getColumnFamilies().get(1);
        assertThat(amazingColumnFamily.getName(), is("amazingColumnFamilyName"));
        assertThat(amazingColumnFamily.getType(), is(ColumnType.STANDARD));
        assertThat(amazingColumnFamily.getKeyType().getClassName(),
                is(ComparatorType.UTF8TYPE.getClassName()));
        assertThat(amazingColumnFamily.getComparatorType().getClassName(),
                is(ComparatorType.UTF8TYPE.getClassName()));
    }

    @Test
    public void shouldGetOneStandardColumnFamilyDataWithDefaultValues() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
        List<RowModel> rows = dataSet.getColumnFamilies().get(0).getRows();
        assertThat(rows, notNullValue());
        assertThat(rows.size(), is(3));
        verifyStandardRow(rows.get(0), "10", 2, "11", "11", "12", "12");
        verifyStandardRow(rows.get(1), "20", 3, "21", "21", "22", "22");
        verifyStandardRow(rows.get(2), "30", 2, "31", "31", "32", "32");
    }

    private void verifyStandardRow(RowModel row, String expectedRowkey, int expectedSize,
                                   String expectedFirstColumnName, String expectedFirstColumnValue, String expectedSecondColumnName,
                                   String expectedSecondColumnValue) {
        assertThat(row, notNullValue());
        assertThat(row.getKey().toString(), is(expectedRowkey));
        assertThat(row.getSuperColumns(), notNullValue());
        assertThat(row.getSuperColumns().isEmpty(), is(true));
        assertThat(row.getColumns(), notNullValue());
        assertThat(row.getColumns().size(), is(expectedSize));
        assertThat(row.getColumns().get(0), notNullValue());
        assertThat(row.getColumns().get(0).getName().toString(), is(expectedFirstColumnName));
        assertThat(row.getColumns().get(0).getValue().toString(), is(expectedFirstColumnValue));
        assertThat(row.getColumns().get(0), notNullValue());
        assertThat(row.getColumns().get(1).getName().toString(), is(expectedSecondColumnName));
        assertThat(row.getColumns().get(1).getValue().toString(), is(expectedSecondColumnValue));
    }

    @Test
    public void shouldGetOneSuperColumnFamilyData() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefinedValues.xml");
        assertThat(dataSet.getColumnFamilies().get(0).getRows(), notNullValue());
        assertThat(dataSet.getColumnFamilies().get(0).getRows().size(), is(2));
        RowModel actualrow0 = dataSet.getColumnFamilies().get(0).getRows().get(0);
        assertThat(actualrow0, notNullValue());
        assertThat(actualrow0.getKey().toString(), is("13816710-1dd2-11b2-879a-782bcb80ff6a"));
        assertThat(actualrow0.getColumns(), notNullValue());
        assertThat(actualrow0.getColumns().isEmpty(), is(true));
        assertThat(actualrow0.getSuperColumns(), notNullValue());
        assertThat(actualrow0.getSuperColumns().size(), is(2));

        SuperColumnModel actualSuperColumn = actualrow0.getSuperColumns().get(0);
        assertThat(actualSuperColumn, notNullValue());
        assertThat(actualSuperColumn.getName().toString(), is("name11"));
        assertThat(actualSuperColumn.getColumns(), notNullValue());
        assertThat(actualSuperColumn.getColumns().size(), is(2));

        ColumnModel actualColumn0OfRow0 = actualSuperColumn.getColumns().get(0);
        assertThat(actualColumn0OfRow0, notNullValue());
        assertThat(actualColumn0OfRow0.getName().toString(), is("111"));
        assertThat(actualColumn0OfRow0.getValue().toString(), is("value111"));

        ColumnModel actualColumn1OfRow0 = actualSuperColumn.getColumns().get(1);
        assertThat(actualColumn1OfRow0, notNullValue());
        assertThat(actualColumn1OfRow0.getName().toString(), is("112"));
        assertThat(actualColumn1OfRow0.getValue().toString(), is("value112"));

        SuperColumnModel actualSuperColumn1OfRow0 = actualrow0.getSuperColumns().get(1);
        assertThat(actualSuperColumn1OfRow0.getName().toString(), is("name12"));
        assertThat(actualSuperColumn1OfRow0.getColumns(), notNullValue());
        assertThat(actualSuperColumn1OfRow0.getColumns().size(), is(2));

        ColumnModel actualColumn0OfSuperColumn1OofRow0 = actualSuperColumn1OfRow0.getColumns().get(0);
        assertThat(actualColumn0OfSuperColumn1OofRow0, notNullValue());
        assertThat(actualColumn0OfSuperColumn1OofRow0.getName().toString(), is("121"));
        assertThat(actualColumn0OfSuperColumn1OofRow0.getValue().toString(), is("value121"));

        ColumnModel actualColumn1OfSuperColumn1ofRow0 = actualSuperColumn1OfRow0.getColumns().get(1);
        assertThat(actualColumn1OfSuperColumn1ofRow0, notNullValue());
        assertThat(actualColumn1OfSuperColumn1ofRow0.getName().toString(), is("122"));
        assertThat(actualColumn1OfSuperColumn1ofRow0.getValue().toString(), is("value122"));

        RowModel row1 = dataSet.getColumnFamilies().get(0).getRows().get(1);
        assertThat(row1, notNullValue());
        assertThat(row1.getKey().toString(), is("13818e20-1dd2-11b2-879a-782bcb80ff6a"));
        assertThat(row1.getColumns(), notNullValue());
        assertThat(row1.getColumns().isEmpty(), is(true));
        assertThat(row1.getSuperColumns(), notNullValue());
        assertThat(row1.getSuperColumns().size(), is(3));

        SuperColumnModel actualSuperColumn0OfRow1 = row1.getSuperColumns().get(0);
        assertThat(actualSuperColumn0OfRow1, notNullValue());
        assertThat(actualSuperColumn0OfRow1.getName().toString(), is("name21"));
        assertThat(actualSuperColumn0OfRow1.getColumns(), notNullValue());
        assertThat(actualSuperColumn0OfRow1.getColumns().size(), is(2));

        ColumnModel actualColumn0OfSuperColum0OfRow1 = actualSuperColumn0OfRow1.getColumns().get(0);
        assertThat(actualColumn0OfSuperColum0OfRow1, notNullValue());
        assertThat(actualColumn0OfSuperColum0OfRow1.getName().toString(), is("211"));
        assertThat(actualColumn0OfSuperColum0OfRow1.getValue().toString(), is("value211"));

        ColumnModel actualColumn1OfSuperColum0OfRow1 = actualSuperColumn0OfRow1.getColumns().get(1);
        assertThat(actualColumn1OfSuperColum0OfRow1, notNullValue());
        assertThat(actualColumn1OfSuperColum0OfRow1.getName().toString(), is("212"));
        assertThat(actualColumn1OfSuperColum0OfRow1.getValue().toString(), is("value212"));

        SuperColumnModel actualSuperColumn1OfRow1 = row1.getSuperColumns().get(1);
        assertThat(actualSuperColumn1OfRow1.getName().toString(), is("name22"));
        assertThat(actualSuperColumn1OfRow1.getColumns(), notNullValue());
        assertThat(actualSuperColumn1OfRow1.getColumns().size(), is(2));

        ColumnModel actualColumn0OfSuperCOlumn10OfRow1 = actualSuperColumn1OfRow1.getColumns().get(0);
        assertThat(actualColumn0OfSuperCOlumn10OfRow1, notNullValue());
        assertThat(actualColumn0OfSuperCOlumn10OfRow1.getName().toString(), is("221"));
        assertThat(actualColumn0OfSuperCOlumn10OfRow1.getValue().toString(), is("value221"));

        ColumnModel actualColumn1OfSuperCOlumn10OfRow1 = actualSuperColumn1OfRow1.getColumns().get(1);
        assertThat(actualColumn1OfSuperCOlumn10OfRow1, notNullValue());
        assertThat(actualColumn1OfSuperCOlumn10OfRow1.getName().toString(), is("222"));
        assertThat(actualColumn1OfSuperCOlumn10OfRow1.getValue().toString(), is("value222"));

        SuperColumnModel actualSuperColumnModel2OfRow1 = row1.getSuperColumns().get(2);
        assertThat(actualSuperColumnModel2OfRow1.getName().toString(), is("name23"));
        assertThat(actualSuperColumnModel2OfRow1.getColumns(), notNullValue());
        assertThat(actualSuperColumnModel2OfRow1.getColumns().size(), is(1));

        ColumnModel actualColumn0OfSuperColumn2OfRow1 = actualSuperColumnModel2OfRow1.getColumns().get(0);
        assertThat(actualColumn0OfSuperColumn2OfRow1, notNullValue());
        assertThat(actualColumn0OfSuperColumn2OfRow1.getName().toString(), is("231"));
        assertThat(actualColumn0OfSuperColumn2OfRow1.getValue().toString(), is("value231"));
    }

    @Test
    public void shouldGetDefaultBytesTypeForColumnValue() throws Exception {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetColumnValueTest.xml");

        ColumnFamilyModel actualColumnFamily = dataSet.getColumnFamilies().get(0);
        assertThat(actualColumnFamily, notNullValue());
        assertThat(actualColumnFamily.getName(), is("beautifulColumnFamilyName"));
        assertThat(actualColumnFamily.getRows(), notNullValue());

        RowModel actualRow = actualColumnFamily.getRows().get(0);
        assertThat(actualRow, notNullValue());
        assertThat(actualRow.getColumns(), notNullValue());

        ColumnModel actualColumn = actualRow.getColumns().get(0);
        assertThat(actualColumn, notNullValue());
        assertThat(actualColumn.getValue().toString(), is("11"));
        assertThat(actualColumn.getValue().getType(), is(GenericTypeEnum.BYTES_TYPE));
    }

    @Test
    public void shouldGetDefaultUTF8TypeForColumnValue() throws Exception {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetColumnValueTest.xml");
        ColumnFamilyModel actualColumnFamily = dataSet.getColumnFamilies().get(1);
        assertThat(actualColumnFamily, notNullValue());
        assertThat(actualColumnFamily.getName(), is("beautifulColumnFamilyName2"));
        assertThat(actualColumnFamily.getRows(), notNullValue());
        RowModel actualRow = actualColumnFamily.getRows().get(0);
        assertThat(actualRow, notNullValue());
        assertThat(actualRow.getColumns(), notNullValue());

        ColumnModel actualColumn = actualRow.getColumns().get(0);
        assertThat(actualColumn, notNullValue());
        assertThat(actualColumn.getValue().toString(), is("11"));
        assertThat(actualColumn.getValue().getType(), is(GenericTypeEnum.UTF_8_TYPE));
    }

    @Test
    public void shouldGetDefaultUTF8TypeAndDefinedLongTypeForColumnValue() throws Exception {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetColumnValueTest.xml");
        ColumnFamilyModel actualColumnFamily = dataSet.getColumnFamilies().get(2);
        assertThat(actualColumnFamily, notNullValue());
        assertThat(actualColumnFamily.getName(), is("beautifulColumnFamilyName3"));
        assertThat(actualColumnFamily.getRows(), notNullValue());

        RowModel actualRowOfColumnFamily = actualColumnFamily.getRows().get(0);
        assertThat(actualRowOfColumnFamily, notNullValue());
        assertThat(actualRowOfColumnFamily.getColumns(), notNullValue());
        ColumnModel actualColumn0OfColumnFamily = actualRowOfColumnFamily.getColumns().get(0);
        assertThat(actualColumn0OfColumnFamily, notNullValue());
        assertThat(actualColumn0OfColumnFamily.getValue().toString(), is("1"));
        assertThat(actualColumn0OfColumnFamily.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));

        ColumnModel actualColumn1OfColumnFamily = actualRowOfColumnFamily.getColumns().get(1);
        assertThat(actualColumn1OfColumnFamily, notNullValue());
        assertThat(actualColumn1OfColumnFamily.getValue().toString(), is("value12"));
        assertThat(actualColumn1OfColumnFamily.getValue().getType(), is(GenericTypeEnum.UTF_8_TYPE));
    }

    @Test
    public void shouldGetCounterStandardColumnFamily() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefinedValues.xml");

        ColumnFamilyModel actualColumnFamilyModel2 = dataSet.getColumnFamilies().get(2);
        assertThat(actualColumnFamilyModel2.getName(), is("counterStandardColumnFamilyName"));
        assertThat(actualColumnFamilyModel2.getType(), is(ColumnType.STANDARD));
        assertThat(actualColumnFamilyModel2.getKeyType().getClassName(), is(ComparatorType.LONGTYPE.getClassName()));
        assertThat(actualColumnFamilyModel2.getKeyType().getClassName(), is(ComparatorType.LONGTYPE.getClassName()));
        assertThat(actualColumnFamilyModel2.getComparatorType().getClassName(), is(ComparatorType.UTF8TYPE.getClassName()));
        assertThat(actualColumnFamilyModel2.getDefaultColumnValueType().getClassName(), is(ComparatorType.COUNTERTYPE.getClassName()));
        assertThat(actualColumnFamilyModel2.getRows(), notNullValue());
        assertThat(actualColumnFamilyModel2.getRows().size(), is(1));

        RowModel actualRowOfColumnFamily2 = actualColumnFamilyModel2.getRows().get(0);
        assertThat(actualRowOfColumnFamily2, notNullValue());
        assertThat(actualRowOfColumnFamily2.getColumns(), notNullValue());
        assertThat(actualRowOfColumnFamily2.getColumns().size(), is(2));

        ColumnModel actualColumn0OfRowOfColumnFamily2 = actualRowOfColumnFamily2.getColumns().get(0);
        assertThat(actualColumn0OfRowOfColumnFamily2, notNullValue());
        assertThat(actualColumn0OfRowOfColumnFamily2.getName().getValue(), is("counter11"));
        assertThat(actualColumn0OfRowOfColumnFamily2.getValue().getValue(), is("11"));
        assertThat(actualColumn0OfRowOfColumnFamily2.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));

        ColumnModel actualColumn1OfRowOfColumnFamily2 = actualRowOfColumnFamily2.getColumns().get(1);
        assertThat(actualColumn1OfRowOfColumnFamily2, notNullValue());
        assertThat(actualColumn1OfRowOfColumnFamily2.getName().getValue(), is("counter12"));
        assertThat(actualColumn1OfRowOfColumnFamily2.getValue().getValue(), is("12"));
        assertThat(actualColumn1OfRowOfColumnFamily2.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));
    }

    @Test
    public void shouldGetCounterSuperColumnFamily() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetDefinedValues.xml");
        ColumnFamilyModel actualCounterSupercolumnFamilyModel = dataSet.getColumnFamilies().get(3);
        assertThat(actualCounterSupercolumnFamilyModel.getName(), is("counterSuperColumnFamilyName"));
        assertThat(actualCounterSupercolumnFamilyModel.getType(), is(ColumnType.SUPER));
        assertThat(actualCounterSupercolumnFamilyModel.getKeyType().getClassName(), is(ComparatorType.LONGTYPE.getClassName()));
        assertThat(actualCounterSupercolumnFamilyModel.getComparatorType().getClassName(), is(ComparatorType.UTF8TYPE.getClassName()));
        assertThat(actualCounterSupercolumnFamilyModel.getDefaultColumnValueType().getClassName(), is(ComparatorType.COUNTERTYPE.getClassName()));

        assertThat(actualCounterSupercolumnFamilyModel.getRows(), notNullValue());
        assertThat(actualCounterSupercolumnFamilyModel.getRows().size(), is(1));
        assertThat(actualCounterSupercolumnFamilyModel.getRows().get(0), notNullValue());
        assertThat(actualCounterSupercolumnFamilyModel.getRows().get(0).getSuperColumns(), notNullValue());
        assertThat(actualCounterSupercolumnFamilyModel.getRows().get(0).getSuperColumns().size(), is(1));

        SuperColumnModel actualSuperColumnModel = actualCounterSupercolumnFamilyModel.getRows().get(0).getSuperColumns().get(0);
        assertThat(actualSuperColumnModel, notNullValue());
        assertThat(actualSuperColumnModel.getName().getValue(), is("counter11"));
        assertThat(actualSuperColumnModel.getColumns(), notNullValue());
        assertThat(actualSuperColumnModel.getColumns().size(), is(2));

        ColumnModel actualColumn0 = actualSuperColumnModel.getColumns().get(0);
        assertThat(actualColumn0, notNullValue());
        assertThat(actualColumn0.getName().getValue(), is("counter111"));
        assertThat(actualColumn0.getValue().getValue(), is("111"));
        assertThat(actualColumn0.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));

        ColumnModel actualColumn1 = actualSuperColumnModel.getColumns().get(1);
        assertThat(actualColumn1, notNullValue());
        assertThat(actualColumn1.getName().getValue(), is("counter112"));
        assertThat(actualColumn1.getValue().getValue(), is("112"));
        assertThat(actualColumn1.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));

    }

    @Test(expected = ParseException.class)
    public void shouldNotGetCounterColumnFamilyBecauseThereIsFunctionOverridingDefaultValueType() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetBadCounterColumnFamilyWithFunction.xml");
        dataSet.getKeyspace();
    }

    @Test
    public void shouldGetAColumnFamilyWithSecondaryIndex() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithSecondaryIndex.xml");
        ColumnMetadataModel acutalColumnMetadataModel = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0);
        assertThat(acutalColumnMetadataModel.getColumnName().getValue(), is("columnWithIndexAndUTF8ValidationClass"));
        assertThat(acutalColumnMetadataModel.getColumnIndexType(), is(ColumnIndexType.KEYS));
        assertThat(acutalColumnMetadataModel.getValidationClass(), is(ComparatorType.UTF8TYPE));

        ColumnMetadataModel actualColumnMetadataModel1 = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1);
        assertThat(actualColumnMetadataModel1.getColumnName().getValue(), is("columnWithIndexAndIndexNameAndUTF8ValidationClass"));
        assertThat(actualColumnMetadataModel1.getColumnIndexType(), is(ColumnIndexType.KEYS));
        assertThat(actualColumnMetadataModel1.getValidationClass(), is(ComparatorType.UTF8TYPE));
        assertThat(actualColumnMetadataModel1.getIndexName(), is("indexNameOfTheIndex"));

        ColumnMetadataModel actualColumnMetadataModel2 = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2);
        assertThat(actualColumnMetadataModel2.getColumnName().getValue(), is("columnWithUTF8ValidationClass"));
        assertThat(actualColumnMetadataModel2.getColumnIndexType(), nullValue());
        assertThat(actualColumnMetadataModel2.getValidationClass(), is(ComparatorType.UTF8TYPE));
    }

    @Test
    public void shouldGetAColumnFamilyWithCompositeType() throws Exception {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithCompositeType.xml");
        assertThatKeyspaceModelWithCompositeTypeIsOk(dataSet);
    }




    @Test(expected = ParseException.class)
    public void shouldNotGetAColumnFamilyWithCompositeType() throws Exception {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithBadCompositeType.xml");
        dataSet.getKeyspace();
    }

    @Test
    public void shouldGetAColumnFamilyWithNullColumnValue() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithNullColumnValue.xml");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithNullColumnValue"));
        ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
        assertThat(columnModel.getName().getValue(), is("columnWithNullColumnValue"));
        assertThat(columnModel.getValue(), nullValue());
    }
    @Test
    public void shouldGetAColumnFamilyWithTimestampedColumn() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithTimestamp.xml");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithTimestampedColumn"));
        ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
        assertThat(columnModel.getName().getValue(), is("columnWithTimestamp"));
        assertThat(columnModel.getTimestamp(), is(2020L));
    }

    @Test
    public void shouldGetAColumnFamilyWithMetadataAndFunction() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithMetadataAndFunctions.xml");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithMetadata"));
        List<ColumnModel> columns = columnFamilyModel.getRows().get(0).getColumns();
        ColumnModel column1 = columns.get(0);
        assertThat(column1.getName().getValue(),is("column1"));
        assertThat(column1.getValue().getValue(),is("1"));
        assertThat(column1.getValue().getType(),is(GenericTypeEnum.LONG_TYPE));

        ColumnModel column2 = columns.get(1);
        assertThat(column2.getName().getValue(),is("column2"));
        assertThat(column2.getValue().getValue(),is("2"));
        assertThat(column2.getValue().getType(),is(GenericTypeEnum.LONG_TYPE));

        ColumnModel column3 = columns.get(2);
        assertThat(column3.getName().getValue(),is("column3"));
        assertThat(column3.getValue().getValue(),is("value3"));
        assertThat(column3.getValue().getType(),is(GenericTypeEnum.UTF_8_TYPE));
    }

	@Test
    public void shouldUseComparatorTypeForMetadataColumnName() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithComparatorType.xml");
        ColumnMetadataModel columnMetadata = dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0);
        assertThat(columnMetadata.getColumnName().getType(), is(GenericTypeEnum.TIME_UUID_TYPE));
    }

    @Test
    public void shouldGetAColumnFamilyWithColumnsInReverseOrder() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithReversedComparatorOnSimpleType.xml");

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
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithReversedComparatorOnCompositeTypes.xml");

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

    @Test
    public void shouldGetBinaryData() {
        DataSet dataSet = new ClassPathXmlDataSet("xml/dataSetWithBinaryData.xml");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithBinaryData"));
        List<ColumnModel> columns = columnFamilyModel.getRows().get(0).getColumns();
        ColumnModel column1 = columns.get(0);
        assertThat(column1.getName().getValue(), is("a"));
        assertThat(column1.getValue().getValue(), is("aGVsbG8gd29ybGQh"));
        assertThat(column1.getValue().getType(), is(GenericTypeEnum.BYTES_TYPE));
    }
}
