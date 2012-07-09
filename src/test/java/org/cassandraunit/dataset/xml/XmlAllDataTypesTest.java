package org.cassandraunit.dataset.xml;

import org.cassandraunit.dataset.AllDataTypesTest;
import org.cassandraunit.dataset.DataSet;

public class XmlAllDataTypesTest extends AllDataTypesTest {

	@Override
	public DataSet getDataSet() {
		return new ClassPathXmlDataSet("xml/allDataTypes.xml");
	}
}
