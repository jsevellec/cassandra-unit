package org.cassandraunit.dataset.yaml;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.cassandraunit.dataset.AbstractFileDataSetTest;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.StrategyModel;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class FileYamlDataSetTest extends AbstractFileDataSetTest {

	@Override
	public String getDataSetClasspathRessource() {
		return "/yaml/dataSetDefaultValues.yaml";
	}

	@Test
	public void shouldGetAYamlDataSetStructure() {
		DataSet dataSet = new FileYamlDataSet(super.targetDataSetPathFileName);
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
		DataSet dataSet = new FileYamlDataSet(null);
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfDataSetNotExist() {
		DataSet dataSet = new FileYamlDataSet("/unknown.yaml");
		dataSet.getKeyspace();
	}

}
