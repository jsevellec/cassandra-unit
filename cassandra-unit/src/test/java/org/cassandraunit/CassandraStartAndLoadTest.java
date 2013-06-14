package org.cassandraunit;

import static org.cassandraunit.SampleDataSetChecker.assertDataSetLoaded;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class CassandraStartAndLoadTest extends AbstractCassandraUnit4TestCase {

	@Override
	public DataSet getDataSet() {
		return new ClassPathXmlDataSet("xml/dataSetDefaultValues.xml");
	}

	@Test
	public void shouldWork() throws Exception {
		assertThat(getKeyspace(), notNullValue());
		assertDataSetLoaded(getKeyspace());
	}

	@Test
	public void shouldWorkToo() throws Exception {
		assertThat(getKeyspace(), notNullValue());
		assertDataSetLoaded(getKeyspace());
	}

}
