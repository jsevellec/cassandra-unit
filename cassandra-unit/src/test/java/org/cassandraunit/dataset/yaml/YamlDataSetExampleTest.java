package org.cassandraunit.dataset.yaml;

import java.util.ArrayList;
import java.util.List;

import me.prettyprint.hector.api.ddl.ColumnType;

import org.cassandraunit.dataset.commons.ParsedColumn;
import org.cassandraunit.dataset.commons.ParsedColumnFamily;
import org.cassandraunit.dataset.commons.ParsedDataType;
import org.cassandraunit.dataset.commons.ParsedKeyspace;
import org.cassandraunit.dataset.commons.ParsedRow;
import org.cassandraunit.dataset.commons.ParsedSuperColumn;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class YamlDataSetExampleTest {

	private Logger log = LoggerFactory.getLogger(YamlDataSetExampleTest.class);

	@Test
	public void shouldGenerateAYamlDataSet() throws Exception {
		Yaml yaml = new Yaml();

		ParsedKeyspace keyspace = new ParsedKeyspace();
		keyspace.setName("beautifulKeyspaceName");
		keyspace.setReplicationFactor(1);

		List<ParsedColumnFamily> columnFamilies = new ArrayList<ParsedColumnFamily>();
		columnFamilies.add(constructSuperColumnFamily());
		keyspace.setColumnFamilies(columnFamilies);

		String result = yaml.dump(keyspace);
		log.debug(result);

	}

	private ParsedColumnFamily constructSuperColumnFamily() {
		ParsedColumnFamily parsedColumnFamily = new ParsedColumnFamily();
		parsedColumnFamily.setType(ColumnType.SUPER);
		parsedColumnFamily.setComparatorType("UTF8Type");
		parsedColumnFamily.setSubComparatorType(ParsedDataType.BytesType);
		parsedColumnFamily.setKeyType("BytesType");
		parsedColumnFamily.setName("SuperColumnFamilyName");

		List<ParsedRow> rows = new ArrayList<ParsedRow>();
		rows.add(constructSuperRow());
		parsedColumnFamily.setRows(rows);

		return parsedColumnFamily;
	}

	private ParsedRow constructSuperRow() {
		ParsedRow parsedRow = new ParsedRow();
		parsedRow.setKey("key02");
		List<ParsedSuperColumn> superColumns = new ArrayList<ParsedSuperColumn>();
		superColumns.add(constructSuperColumn());
		parsedRow.setSuperColumns(superColumns);
		return parsedRow;
	}

	private ParsedSuperColumn constructSuperColumn() {
		ParsedSuperColumn parsedSuperColumn = new ParsedSuperColumn();
		parsedSuperColumn.setName("superColumnName02");
		List<ParsedColumn> columns = new ArrayList<ParsedColumn>();
		columns.add(constructColumn("name021", "value021"));
		columns.add(constructColumn("name022", "value022"));
		columns.add(constructColumn("name023", "value023"));
		parsedSuperColumn.setColumns(columns);
		return parsedSuperColumn;
	}

	private ParsedColumn constructColumn(String name, String value) {
		ParsedColumn parsedColumn = new ParsedColumn();
		parsedColumn.setName(name);
		parsedColumn.setValue(value);
		return parsedColumn;
	}

}
