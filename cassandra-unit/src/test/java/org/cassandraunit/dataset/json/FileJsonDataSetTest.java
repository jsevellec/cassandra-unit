package org.cassandraunit.dataset.json;

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
public class FileJsonDataSetTest extends AbstractFileDataSetTest {

	@Override
	public String getDataSetClasspathRessource() {
		return "/json/dataSetDefaultValues.json";
	}

	@Test
	public void shouldGetAJsonDataSetStructure() throws Exception {

		DataSet dataSet = new FileJsonDataSet(super.targetDataSetPathFileName);
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
		DataSet dataSet = new FileJsonDataSet(null);
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAJsonDataSetStructureBecauseOfFileNotFound() {
		DataSet dataSet = new FileJsonDataSet("/notfound.json");
		dataSet.getKeyspace();
	}

}
