package org.cassandraunit.dataset.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import me.prettyprint.hector.api.ddl.ColumnType;

import org.cassandraunit.dataset.commons.ParsedColumn;
import org.cassandraunit.dataset.commons.ParsedColumnFamily;
import org.cassandraunit.dataset.commons.ParsedDataType;
import org.cassandraunit.dataset.commons.ParsedKeyspace;
import org.cassandraunit.dataset.commons.ParsedRow;
import org.cassandraunit.dataset.commons.ParsedSuperColumn;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.schema.JsonSchema;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class JsonDataSetExampleTest {

	private Logger log = LoggerFactory.getLogger(JsonDataSetExampleTest.class);

	@Test
	public void shouldGenerateAJsonDataSetDocument() throws JAXBException {

		ParsedKeyspace keyspace = new ParsedKeyspace();
		keyspace.setName("beautifulKeyspaceName");
		keyspace.setReplicationFactor(1);
		List<ParsedColumnFamily> columnFamilies = keyspace.getColumnFamilies();

		ParsedColumnFamily columnFamily1 = new ParsedColumnFamily();
		columnFamily1.setName("columnFamily1");
		columnFamily1.setType(ColumnType.STANDARD);
		columnFamily1.setKeyType("UTF8Type");
		columnFamily1.setComparatorType("UTF8Type");
		columnFamily1.setDefaultColumnValueType(ParsedDataType.UTF8Type);
		List<ParsedRow> rows1 = new ArrayList<ParsedRow>();
		ParsedRow row1 = new ParsedRow();
		row1.setKey("key01");
		List<ParsedColumn> columns1 = row1.getColumns();
		ParsedColumn column1 = new ParsedColumn();
		column1.setName("columnName1");
		column1.setValue("columnValue1");
		columns1.add(column1);
		rows1.add(row1);
		columnFamily1.setRows(rows1);

		columnFamilies.add(columnFamily1);

		ParsedColumnFamily columnFamily2 = new ParsedColumnFamily();
		columnFamily2.setName("columnFamily1");
		columnFamily2.setType(ColumnType.SUPER);
		columnFamily2.setKeyType("UTF8Type");
		columnFamily2.setComparatorType("UTF8Type");
		columnFamily2.setDefaultColumnValueType(ParsedDataType.UTF8Type);
		List<ParsedRow> rows2 = new ArrayList<ParsedRow>();
		ParsedRow row2 = new ParsedRow();
		row2.setKey("key02");
		List<ParsedSuperColumn> superColumns = row2.getSuperColumns();
		ParsedSuperColumn superColumn2 = new ParsedSuperColumn();
		superColumn2.setName("superColumnName2");
		List<ParsedColumn> columns2 = superColumn2.getColumns();
		ParsedColumn column2 = new ParsedColumn();
		column2.setName("columnName2");
		column2.setValue("columnValue2");
		columns2.add(column2);
		superColumns.add(superColumn2);
		rows2.add(row1);
		columnFamily2.setRows(rows2);
		columnFamilies.add(columnFamily2);

		ObjectMapper jSONMapper = new ObjectMapper();
		try {
			JsonSchema schema = jSONMapper.generateJsonSchema(ParsedKeyspace.class);
			log.debug(schema.toString());
			log.debug(jSONMapper.writeValueAsString(keyspace));
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
