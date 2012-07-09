package org.cassandraunit.dataset.json;

import org.cassandraunit.dataset.AllDataTypesTest;
import org.cassandraunit.dataset.DataSet;

public class JsonAllDataTypesTest extends AllDataTypesTest {

	@Override
	public DataSet getDataSet() {
		return new ClassPathJsonDataSet("json/allDataTypes.json");
	}
}
