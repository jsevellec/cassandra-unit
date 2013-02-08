package org.cassandraunit.dataset.yaml;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.ColumnModel;
import org.cassandraunit.model.StrategyModel;
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
public class ClasspathYamlDataSetTest {

	@Test
	public void shouldGetAYamlDataSetStructure() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetDefaultValues.yaml");
		assertThat(dataSet, notNullValue());
		assertThat(dataSet.getKeyspace(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), is("beautifulKeyspaceName"));
		assertThat(dataSet.getKeyspace().getReplicationFactor(), is(1));
		assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

		assertThat(dataSet.getColumnFamilies(), notNullValue());
		assertThat(dataSet.getColumnFamilies().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getName(), is("columnFamily1"));
		assertThat(dataSet.getColumnFamilies().get(0).getType(), is(ColumnType.STANDARD));
		assertThat(dataSet.getColumnFamilies().get(0).getKeyType().getTypeName(),
				is(ComparatorType.BYTESTYPE.getTypeName()));
		assertThat(dataSet.getColumnFamilies().get(0).getComparatorType().getTypeName(),
				is(ComparatorType.BYTESTYPE.getTypeName()));
		assertThat(dataSet.getColumnFamilies().get(0).getSubComparatorType(), nullValue());
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfNull() {
		DataSet dataSet = new ClassPathYamlDataSet(null);
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfDataSetNotExist() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/unknown.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfDataSetEmpty() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadMissingKeyspaceName.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfMissingKeyspaceName() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadMissingKeyspaceName.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidStrategy() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadUnknownStrategy.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfMissingColumnFamilyName() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadMissingColumnFamilyName.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidColumnFamilyType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadInvalidColumnFamilyType.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidRowKeyType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadInvalidKeyType.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidComparatorType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadInvalidComparatorType.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidSubComparatorType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadInvalidSubComparatorType.yaml");
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfInvalidDefaultColumnValueType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadInvalidDefaultColumnValueType.yaml");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAYamlDataSetWithData() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetDefaultValues.yaml");
		assertThat(dataSet, notNullValue());
		assertThat(dataSet.getKeyspace(), notNullValue());

		assertThat(dataSet.getColumnFamilies(), notNullValue());
		assertThat(dataSet.getColumnFamilies().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getName(), is("columnFamily1"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getKey().getValue(), is("01"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.BYTES_TYPE));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getName().getValue(),
				is("02"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getName().getType(),
				is(GenericTypeEnum.BYTES_TYPE));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getValue().getValue(),
				is("03"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.BYTES_TYPE));

	}

	@Test
	public void shouldGetAYamlDataSetWithDefinedValuesAndData() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetDefinedValues.yaml");
		assertThat(dataSet, notNullValue());
		assertThat(dataSet.getKeyspace(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), is("beautifulDefinedKeyspaceName"));
		assertThat(dataSet.getKeyspace().getReplicationFactor(), is(1));
		assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

		assertThat(dataSet.getColumnFamilies(), notNullValue());
		assertThat(dataSet.getColumnFamilies().size(), is(4));

        ColumnFamilyModel columnFamily1 = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamily1, notNullValue());
		assertThat(columnFamily1.getName(), is("columnFamily1"));
		assertThat(columnFamily1.getKeyType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(columnFamily1.getComparatorType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(columnFamily1.getSubComparatorType(), is(nullValue()));
        assertThat(columnFamily1.getComment(),is("amazing comment"));
        assertThat(columnFamily1.getCompactionStrategy(),is("LeveledCompactionStrategy"));
        assertThat(columnFamily1.getCompactionStrategyOptions().get(0).getName(),is("sstable_size_in_mb"));
        assertThat(columnFamily1.getCompactionStrategyOptions().get(0).getValue(),is("10"));
        assertThat(columnFamily1.getGcGraceSeconds(),is(9999));
        assertThat(columnFamily1.getMaxCompactionThreshold(),is(31));
        assertThat(columnFamily1.getMinCompactionThreshold(),is(3));
        assertThat(columnFamily1.getReadRepairChance(),is(0.1d));
        assertThat(columnFamily1.getReplicationOnWrite(),is(Boolean.FALSE));

		assertThat(columnFamily1.getRows(), notNullValue());
		assertThat(columnFamily1.getRows().size(), is(1));
		assertThat(columnFamily1.getRows().get(0), notNullValue());
		assertThat(columnFamily1.getRows().get(0).getKey().getValue(), is("key01"));
		assertThat(columnFamily1.getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(columnFamily1.getRows().get(0).getColumns(), notNullValue());
		assertThat(columnFamily1.getRows().get(0).getColumns().size(), is(1));
		assertThat(columnFamily1.getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(columnFamily1.getRows().get(0).getColumns().get(0).getName().getValue(),
				is("columnName1"));
		assertThat(columnFamily1.getRows().get(0).getColumns().get(0).getName().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(columnFamily1.getRows().get(0).getColumns().get(0).getValue().getValue(),
				is("columnValue1"));
		assertThat(columnFamily1.getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));

		assertThat(dataSet.getColumnFamilies().get(1), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getName(), is("columnFamily2"));
		assertThat(dataSet.getColumnFamilies().get(1).getKeyType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(dataSet.getColumnFamilies().get(1).getComparatorType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(dataSet.getColumnFamilies().get(1).getSubComparatorType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));

		assertThat(dataSet.getColumnFamilies().get(1).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getKey().getValue(), is("key02"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns().isEmpty(), is(true));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getName().getValue(),
				is("superColumnName2"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getName().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns(),
				notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().size(),
				is(1));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().get(0),
				notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getName().getValue(), is("columnName2"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getName().getType(), is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getValue(), is("2"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));

	}

	@Test
	public void shouldGetAStandardCounterColumnFamily() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetDefinedValues.yaml");
		assertThat(dataSet.getColumnFamilies().get(2).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getKey().getValue(), is("key10"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0).getName().getValue(),
				is("columnName11"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0).getValue().getValue(),
				is("11"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.COUNTER_TYPE));
	}

	@Test
	public void shouldGetASuperCounterColumnFamily() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetDefinedValues.yaml");
		assertThat(dataSet.getColumnFamilies().get(3), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getName(), is("superCounterColumnFamily"));
		assertThat(dataSet.getColumnFamilies().get(3).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getKey().getValue(), is("key10"));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().size(), is(1));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getName().getValue(),
				is("superColumnName11"));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns(),
				notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns().size(),
				is(1));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns().get(0),
				notNullValue());
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getName().getValue(), is("columnName111"));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getValue(), is("111"));
		assertThat(dataSet.getColumnFamilies().get(3).getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getType(), is(GenericTypeEnum.COUNTER_TYPE));
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetCounterColumnFamilyBecauseThereIsFunctionOverridingDefaultValueType() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetBadCounterColumnFamilyWithFunction.yaml");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAColumnFamilyWithSecondaryIndex() {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithSecondaryIndex.yaml");

		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getColumnName().getValue(),
				is("columnWithIndexAndUTF8ValidationClass"));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getColumnIndexType(),
				is(ColumnIndexType.KEYS));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getValidationClass(),
				is(ComparatorType.UTF8TYPE));

        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getColumnName().getValue(),
                is("columnWithIndexAndIndexNameAndUTF8ValidationClass"));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getColumnIndexType(),
                is(ColumnIndexType.KEYS));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getValidationClass(),
                is(ComparatorType.UTF8TYPE));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getIndexName(),is("indexNameOfTheIndex"));

		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getColumnName().getValue(),
				is("columnWithUTF8ValidationClass"));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getColumnIndexType(), nullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getValidationClass(),
				is(ComparatorType.UTF8TYPE));
	}

	@Test
	public void shouldGetAColumnFamilyWithCompositeType() throws Exception {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithCompositeType.yaml");
		assertThatKeyspaceModelWithCompositeTypeIsOk(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAColumnFamilyWithCompositeType() throws Exception {
		DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithBadCompositeType.yaml");
		dataSet.getKeyspace();
	}

    @Test
    public void shouldGetAColumnFamilyWithNullColumnValue() {
        DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithNullColumnValue.yaml");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithNullColumnValue"));
        ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
        assertThat(columnModel.getName().getValue(), is("columnWithNullColumnValue"));
        assertThat(columnModel.getValue(), nullValue());
    }
   @Test
   public void shouldGetAColumnFamilyWithTimestampedColumn() {
       DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithTimestamp.yaml");
       ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
       assertThat(columnFamilyModel.getName(), is("columnFamilyWithTimestampedColumn"));
       ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
       assertThat(columnModel.getName().getValue(), is("columnWithTimestamp"));
       assertThat(columnModel.getTimestamp(), is(2020L));
   }

    @Test
    public void shouldGetAColumnFamilyWithMetadataAndFunction() {
        DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithMetadataAndFunctions.yaml");
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
    public void shouldGetAColumnFamilyWithColumnsInReverseOrder() {
        DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithReversedComparatorOnSimpleType.yaml");

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
        DataSet dataSet = new ClassPathYamlDataSet("yaml/dataSetWithReversedComparatorOnCompositeTypes.yaml");

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
