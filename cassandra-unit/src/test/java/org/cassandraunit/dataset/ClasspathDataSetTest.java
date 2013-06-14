package org.cassandraunit.dataset;

import static org.cassandraunit.SampleDataSetChecker.assertDataSetDefaultValues;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.dataset.json.ClassPathJsonDataSet;
import org.cassandraunit.model.StrategyModel;
import org.cassandraunit.type.GenericTypeEnum;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class ClasspathDataSetTest {

	@Test
	public void shouldGetAJsonDataSetStructure() {
		DataSet dataSet = new ClassPathDataSet("json/dataSetDefaultValues.json");
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetADataSetStructureBecauseOfNull() {
		DataSet dataSet = new ClassPathJsonDataSet(null);
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAJsonDataSetStructureBecauseOfDataSetNotExist() {
		DataSet dataSet = new ClassPathJsonDataSet("json/unknown.json");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAXmlDataSetStructure() {
		DataSet dataSet = new ClassPathDataSet("xml/dataSetDefaultValues.xml");
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAXmlDataSetStructureBecauseOfDataSetNotExist() {
		DataSet dataSet = new ClassPathJsonDataSet("xml/unknown.xml");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAYamlDataSetStructure() {
		DataSet dataSet = new ClassPathDataSet("yaml/dataSetDefaultValues.yaml");
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfDataSetNotExist() {
		DataSet dataSet = new ClassPathJsonDataSet("yaml/unknown.yaml");
		dataSet.getKeyspace();
	}

}
