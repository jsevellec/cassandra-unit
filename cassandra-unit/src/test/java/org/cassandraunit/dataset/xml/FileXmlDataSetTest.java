package org.cassandraunit.dataset.xml;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.cassandraunit.dataset.AbstractFileDataSetTest;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class FileXmlDataSetTest extends AbstractFileDataSetTest {

	@Override
	public String getDataSetClasspathRessource() {
		return "/cql/simple.cql";
	}

	@Test
	public void shouldGetAXmlDataSet() {

		DataSet dataSet = new FileXmlDataSet(super.targetDataSetPathFileName);
		assertThat(dataSet, notNullValue());
	}

	@Test
	public void shouldNotGetAXmlDataSetBecauseNull() {
		try {
			DataSet dataSet = new FileXmlDataSet(null);
			fail();
		} catch (ParseException e) {
			/* nothing to do, it what we want */
		}
	}

	@Test
	public void shouldNotGetAXmlDataSetBecauseOfFileNotFound() {
		try {
			DataSet dataSet = new FileXmlDataSet("/notfound.json");
			fail();
		} catch (ParseException e) {
			/* nothing to do, it what we want */
		}
	}

}
