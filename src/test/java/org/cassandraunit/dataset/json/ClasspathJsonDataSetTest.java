package org.cassandraunit.dataset.json;

import static org.cassandraunit.SampleDataSetChecker.assertThatKeyspaceModelWithCompositeTypeIsOk;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.yaml.ClassPathYamlDataSet;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.ColumnModel;
import org.cassandraunit.model.StrategyModel;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class ClasspathJsonDataSetTest {

	@Test
	public void shouldGetAJsonDataSetStructure() {
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefaultValues.json");
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
	public void shouldGetAJsonDataSetWithDefinedValuesAndData() {
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
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

        ColumnFamilyModel columnFamily2 = dataSet.getColumnFamilies().get(1);
        assertThat(columnFamily2, notNullValue());
		assertThat(columnFamily2.getName(), is("columnFamily2"));
		assertThat(columnFamily2.getKeyType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(columnFamily2.getComparatorType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));
		assertThat(columnFamily2.getSubComparatorType().getTypeName(),
				is(ComparatorType.UTF8TYPE.getTypeName()));

		assertThat(columnFamily2.getRows(), notNullValue());
		assertThat(columnFamily2.getRows().size(), is(1));
		assertThat(columnFamily2.getRows().get(0), notNullValue());
		assertThat(columnFamily2.getRows().get(0).getKey().getValue(), is("key02"));
		assertThat(columnFamily2.getRows().get(0).getKey().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(columnFamily2.getRows().get(0).getColumns(), notNullValue());
		assertThat(columnFamily2.getRows().get(0).getColumns().isEmpty(), is(true));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns(), notNullValue());
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().size(), is(1));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0), notNullValue());
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getName().getValue(),
				is("superColumnName2"));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getName().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns(),
				notNullValue());
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().size(),
				is(1));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().get(0),
				notNullValue());
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getName().getValue(), is("columnName2"));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getName().getType(), is(GenericTypeEnum.UTF_8_TYPE));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getValue(), is("2"));
		assertThat(columnFamily2.getRows().get(0).getSuperColumns().get(0).getColumns().get(0)
				.getValue().getType(), is(GenericTypeEnum.LONG_TYPE));

	}

	@Test
	public void shouldGetAStandardCounterColumnFamily() {
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
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
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetDefinedValues.json");
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
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetBadCounterColumnFamilyWithFunction.json");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAColumnFamilyWithSecondaryIndex() {
		DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithSecondaryIndex.json");

		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getColumnName(),
				is("columnWithIndexAndUTF8ValidationClass"));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getColumnIndexType(),
				is(ColumnIndexType.KEYS));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(0).getValidationClass(),
				is(ComparatorType.UTF8TYPE));

        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getColumnName(),
                is("columnWithIndexAndIndexNameAndUTF8ValidationClass"));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getColumnIndexType(),
                is(ColumnIndexType.KEYS));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getValidationClass(),
                is(ComparatorType.UTF8TYPE));
        assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(1).getIndexName(),is("indexNameOfTheIndex"));

		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getColumnName(),
				is("columnWithUTF8ValidationClass"));
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getColumnIndexType(), nullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getColumnsMetadata().get(2).getValidationClass(),
				is(ComparatorType.UTF8TYPE));
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
        assertThat(columnModel.getValue().getValue(), nullValue());
    }
    @Test
    public void shouldGetAColumnFamilyWithTimestampedColumn() {
        DataSet dataSet = new ClassPathJsonDataSet("json/dataSetWithTimestamp.json");
        ColumnFamilyModel columnFamilyModel = dataSet.getColumnFamilies().get(0);
        assertThat(columnFamilyModel.getName(), is("columnFamilyWithTimestampedColumn"));
        ColumnModel columnModel = columnFamilyModel.getRows().get(0).getColumns().get(0);
        assertThat(columnModel.getName().getValue(), is("columnWithTimestamp"));
        assertThat(columnModel.getTimestamp(), is(2020L));
    }

}
