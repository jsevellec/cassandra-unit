package org.cassandraunit.dataset.xml;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.RowModel;
import org.cassandraunit.model.StrategyModel;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class ClasspathXmlDataSetTest {

	@Test
	public void shouldGetAXmlDataSet() {

		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefaultValues.xml");
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
	public void shouldNotGetAXmlDataSet() {
		try {
			DataSet dataSet = new ClassPathXmlDataSet("xml/invalidDataSet.xml");
			fail();
		} catch (ParseException e) {
			/* nothing to do, it what we want */
			assertThat(
					e.getMessage(),
					is("javax.xml.bind.UnmarshalException\n - with linked exception:\n[org.xml.sax.SAXParseException: cvc-complex-type.2.4.a: Invalid content was found starting with element 'columnFamily'. One of '{\"http://xml.dataset.cassandraunit.org\":columnFamilies}' is expected.]"));
		}
	}

	@Test
	public void shouldGetKeyspaceWithDefaultValues() {

		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefaultValues.xml");
		assertThat(dataSet.getKeyspace(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), is("beautifulKeyspaceName"));
		assertThat(dataSet.getKeyspace().getReplicationFactor(), is(1));
		assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.SIMPLE_STRATEGY));

	}

	@Test
	public void shouldGetKeyspaceWithDefinedValues() {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefinedValues.xml");
		assertThat(dataSet.getKeyspace(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), notNullValue());
		assertThat(dataSet.getKeyspace().getName(), is("otherKeyspaceName"));
		assertThat(dataSet.getKeyspace().getReplicationFactor(), is(2));
		assertThat(dataSet.getKeyspace().getStrategy(), is(StrategyModel.LOCAL_STRATEGY));
	}

	@Test
	public void shouldGetOneColumnFamilyWithDefaultValues() {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefaultValues.xml");
		assertThat(dataSet.getColumnFamilies(), notNullValue());
		assertThat(dataSet.getColumnFamilies().isEmpty(), is(false));
		assertThat(dataSet.getColumnFamilies().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getName(), is("beautifulColumnFamilyName"));
		assertThat(dataSet.getColumnFamilies().get(0).getType(), is(ColumnType.STANDARD));
		assertThat(dataSet.getColumnFamilies().get(0).getKeyType().getClassName(),
				is(ComparatorType.BYTESTYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(0).getComparatorType().getClassName(),
				is(ComparatorType.BYTESTYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(0).getSubComparatorType(), nullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getDefaultColumnValueType().getClassName(),
				is(ComparatorType.BYTESTYPE.getClassName()));

	}

	@Test
	public void shouldGetColumnFamiliesWithDefinedValues() {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefinedValues.xml");
		assertThat(dataSet.getColumnFamilies(), notNullValue());
		assertThat(dataSet.getColumnFamilies().isEmpty(), is(false));
		assertThat(dataSet.getColumnFamilies().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getName(), is("beautifulColumnFamilyName"));
		assertThat(dataSet.getColumnFamilies().get(0).getType(), is(ColumnType.SUPER));
		assertThat(dataSet.getColumnFamilies().get(0).getKeyType().getClassName(),
				is(ComparatorType.TIMEUUIDTYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(0).getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(0).getSubComparatorType().getClassName(),
				is(ComparatorType.LONGTYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(0).getDefaultColumnValueType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));

		assertThat(dataSet.getColumnFamilies().get(1).getName(), is("amazingColumnFamilyName"));
		assertThat(dataSet.getColumnFamilies().get(1).getType(), is(ColumnType.STANDARD));
		assertThat(dataSet.getColumnFamilies().get(1).getKeyType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		assertThat(dataSet.getColumnFamilies().get(1).getComparatorType().getClassName(),
				is(ComparatorType.UTF8TYPE.getClassName()));
		
    assertThat(dataSet.getColumnFamilies().get(2).getName(), is("counterColumnFamilyName"));
    assertThat(dataSet.getColumnFamilies().get(2).getType(), is(ColumnType.STANDARD));
    assertThat(dataSet.getColumnFamilies().get(2).getKeyType().getClassName(),
        is(ComparatorType.LONGTYPE.getClassName()));
    assertThat(dataSet.getColumnFamilies().get(2).getComparatorType().getClassName(),
        is(ComparatorType.UTF8TYPE.getClassName()));
    assertThat(dataSet.getColumnFamilies().get(2).getDefaultColumnValueType().getClassName(),
        is(ComparatorType.COUNTERTYPE.getClassName()));
	}

	@Test
	public void shouldGetOneStandardColumnFamilyDataWithDefaultValues() {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefaultValues.xml");
		assertThat(dataSet.getColumnFamilies().get(0).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().size(), is(3));
		verifyStandardRow(dataSet.getColumnFamilies().get(0).getRows().get(0), "key10", 2, "name11", "value11",
				"name12", "value12");
		verifyStandardRow(dataSet.getColumnFamilies().get(0).getRows().get(1), "key20", 3, "name21", "value21",
				"name22", "value22");
		verifyStandardRow(dataSet.getColumnFamilies().get(0).getRows().get(2), "key30", 2, "name31", "value31",
				"name32", "value32");
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
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetDefinedValues.xml");
		assertThat(dataSet.getColumnFamilies().get(0).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().size(), is(2));
		RowModel row1 = dataSet.getColumnFamilies().get(0).getRows().get(0);
		assertThat(row1, notNullValue());
		assertThat(row1.getKey().toString(), is("13816710-1dd2-11b2-879a-782bcb80ff6a"));
		assertThat(row1.getColumns(), notNullValue());
		assertThat(row1.getColumns().isEmpty(), is(true));

		assertThat(row1.getSuperColumns(), notNullValue());
		assertThat(row1.getSuperColumns().size(), is(2));
		assertThat(row1.getSuperColumns().get(0), notNullValue());
		assertThat(row1.getSuperColumns().get(0).getName().toString(), is("name11"));
		assertThat(row1.getSuperColumns().get(0).getColumns(), notNullValue());
		assertThat(row1.getSuperColumns().get(0).getColumns().size(), is(2));
		assertThat(row1.getSuperColumns().get(0).getColumns().get(0), notNullValue());
		assertThat(row1.getSuperColumns().get(0).getColumns().get(0).getName().toString(), is("111"));
		assertThat(row1.getSuperColumns().get(0).getColumns().get(0).getValue().toString(), is("value111"));
		assertThat(row1.getSuperColumns().get(0).getColumns().get(1), notNullValue());
		assertThat(row1.getSuperColumns().get(0).getColumns().get(1).getName().toString(), is("112"));
		assertThat(row1.getSuperColumns().get(0).getColumns().get(1).getValue().toString(), is("value112"));

		assertThat(row1.getSuperColumns().get(1).getName().toString(), is("name12"));
		assertThat(row1.getSuperColumns().get(1).getColumns(), notNullValue());
		assertThat(row1.getSuperColumns().get(1).getColumns().size(), is(2));
		assertThat(row1.getSuperColumns().get(1).getColumns().get(0), notNullValue());
		assertThat(row1.getSuperColumns().get(1).getColumns().get(0).getName().toString(), is("121"));
		assertThat(row1.getSuperColumns().get(1).getColumns().get(0).getValue().toString(), is("value121"));
		assertThat(row1.getSuperColumns().get(1).getColumns().get(1), notNullValue());
		assertThat(row1.getSuperColumns().get(1).getColumns().get(1).getName().toString(), is("122"));
		assertThat(row1.getSuperColumns().get(1).getColumns().get(1).getValue().toString(), is("value122"));

		RowModel row2 = dataSet.getColumnFamilies().get(0).getRows().get(1);
		assertThat(row2, notNullValue());
		assertThat(row2.getKey().toString(), is("13818e20-1dd2-11b2-879a-782bcb80ff6a"));
		assertThat(row2.getColumns(), notNullValue());
		assertThat(row2.getColumns().isEmpty(), is(true));

		assertThat(row2.getSuperColumns(), notNullValue());
		assertThat(row2.getSuperColumns().size(), is(3));
		assertThat(row2.getSuperColumns().get(0), notNullValue());
		assertThat(row2.getSuperColumns().get(0).getName().toString(), is("name21"));
		assertThat(row2.getSuperColumns().get(0).getColumns(), notNullValue());
		assertThat(row2.getSuperColumns().get(0).getColumns().size(), is(2));
		assertThat(row2.getSuperColumns().get(0).getColumns().get(0), notNullValue());
		assertThat(row2.getSuperColumns().get(0).getColumns().get(0).getName().toString(), is("211"));
		assertThat(row2.getSuperColumns().get(0).getColumns().get(0).getValue().toString(), is("value211"));
		assertThat(row2.getSuperColumns().get(0).getColumns().get(1), notNullValue());
		assertThat(row2.getSuperColumns().get(0).getColumns().get(1).getName().toString(), is("212"));
		assertThat(row2.getSuperColumns().get(0).getColumns().get(1).getValue().toString(), is("value212"));

		assertThat(row2.getSuperColumns().get(1).getName().toString(), is("name22"));
		assertThat(row2.getSuperColumns().get(1).getColumns(), notNullValue());
		assertThat(row2.getSuperColumns().get(1).getColumns().size(), is(2));
		assertThat(row2.getSuperColumns().get(1).getColumns().get(0), notNullValue());
		assertThat(row2.getSuperColumns().get(1).getColumns().get(0).getName().toString(), is("221"));
		assertThat(row2.getSuperColumns().get(1).getColumns().get(0).getValue().toString(), is("value221"));
		assertThat(row2.getSuperColumns().get(1).getColumns().get(1), notNullValue());
		assertThat(row2.getSuperColumns().get(1).getColumns().get(1).getName().toString(), is("222"));
		assertThat(row2.getSuperColumns().get(1).getColumns().get(1).getValue().toString(), is("value222"));

		assertThat(row2.getSuperColumns().get(2).getName().toString(), is("name23"));
		assertThat(row2.getSuperColumns().get(2).getColumns(), notNullValue());
		assertThat(row2.getSuperColumns().get(2).getColumns().size(), is(1));
		assertThat(row2.getSuperColumns().get(2).getColumns().get(0), notNullValue());
		assertThat(row2.getSuperColumns().get(2).getColumns().get(0).getName().toString(), is("231"));
		assertThat(row2.getSuperColumns().get(2).getColumns().get(0).getValue().toString(), is("value231"));
	}

	@Test
	public void shouldGetDefaultBytesTypeForColumnValue() throws Exception {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetColumnValueTest.xml");
		assertThat(dataSet.getColumnFamilies().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getName(), is("beautifulColumnFamilyName"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getValue().toString(),
				is("value11"));
		assertThat(dataSet.getColumnFamilies().get(0).getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.BYTES_TYPE));

	}

	@Test
	public void shouldGetDefaultUTF8TypeForColumnValue() throws Exception {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetColumnValueTest.xml");
		assertThat(dataSet.getColumnFamilies().get(1), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getName(), is("beautifulColumnFamilyName2"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns().get(0).getValue().toString(),
				is("value11"));
		assertThat(dataSet.getColumnFamilies().get(1).getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
	}

	@Test
	public void shouldGetDefaultUTF8TypeAndDefinedLongTypeForColumnValue() throws Exception {
		DataSet dataSet = new ClassPathXmlDataSet("xml/datasetColumnValueTest.xml");
		assertThat(dataSet.getColumnFamilies().get(2), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getName(), is("beautifulColumnFamilyName3"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0).getValue().toString(),
				is("1"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(0).getValue().getType(),
				is(GenericTypeEnum.LONG_TYPE));

		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns(), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(1), notNullValue());
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(1).getValue().toString(),
				is("value12"));
		assertThat(dataSet.getColumnFamilies().get(2).getRows().get(0).getColumns().get(1).getValue().getType(),
				is(GenericTypeEnum.UTF_8_TYPE));
	}

}
