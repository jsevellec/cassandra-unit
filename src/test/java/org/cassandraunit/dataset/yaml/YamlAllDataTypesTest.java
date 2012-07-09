package org.cassandraunit.dataset.yaml;

import org.cassandraunit.dataset.AllDataTypesTest;
import org.cassandraunit.dataset.DataSet;

public class YamlAllDataTypesTest extends AllDataTypesTest {

	@Override
	public DataSet getDataSet() {
		return new ClassPathYamlDataSet("yaml/allDataTypes.yaml");
	}
}
