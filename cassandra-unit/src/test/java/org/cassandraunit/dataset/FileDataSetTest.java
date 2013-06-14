package org.cassandraunit.dataset;

import static org.cassandraunit.SampleDataSetChecker.assertDataSetDefaultValues;

import org.cassandraunit.utils.FileTmpHelper;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class FileDataSetTest {

	private String targetXmlDataSetPathFileName = null;
	private String targetJsonDataSetPathFileName = null;
	private String targetYamlDataSetPathFileName = null;

	@Before
	public void before() throws Exception {
		targetXmlDataSetPathFileName = FileTmpHelper.copyClassPathDataSetToTmpDirectory(this.getClass(),
				"/xml/dataSetDefaultValues.xml");
		targetJsonDataSetPathFileName = FileTmpHelper.copyClassPathDataSetToTmpDirectory(this.getClass(),
				"/json/dataSetDefaultValues.json");
		targetYamlDataSetPathFileName = FileTmpHelper.copyClassPathDataSetToTmpDirectory(this.getClass(),
				"/yaml/dataSetDefaultValues.yaml");
	}

	@Test
	public void shouldGetAJsonDataSetStructure() throws Exception {

		DataSet dataSet = new FileDataSet(targetJsonDataSetPathFileName);
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetADataSetStructureBecauseOfNull() {
		DataSet dataSet = new FileDataSet(null);
		dataSet.getKeyspace();
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAJsonDataSetStructureBecauseOfFileNotFound() {
		DataSet dataSet = new FileDataSet("/notfound.json");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAXmlDataSetStructure() throws Exception {

		DataSet dataSet = new FileDataSet(targetXmlDataSetPathFileName);
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAXmlDataSetStructureBecauseOfFileNotFound() {
		DataSet dataSet = new FileDataSet("/notfound.xml");
		dataSet.getKeyspace();
	}

	@Test
	public void shouldGetAYamlDataSetStructure() throws Exception {

		DataSet dataSet = new FileDataSet(targetYamlDataSetPathFileName);
		assertDataSetDefaultValues(dataSet);
	}

	@Test(expected = ParseException.class)
	public void shouldNotGetAYamlDataSetStructureBecauseOfFileNotFound() {
		DataSet dataSet = new FileDataSet("/notfound.yaml");
		dataSet.getKeyspace();
	}

}
