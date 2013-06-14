package org.cassandraunit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.cassandraunit.dataset.xml.ClassPathXmlDataSet;
import org.junit.Rule;
import org.junit.Test;

public class CassandraStartAndLoadWithBytesKeyTest {

	@Rule
	public CassandraUnit cassandraUnit = new CassandraUnit(new ClassPathXmlDataSet("xml/dataSetWithBytesKey.xml"));

	@Test
	public void shouldGetBytesKey() {
		CqlQuery<byte[], String, byte[]> query = new CqlQuery<byte[], String, byte[]>(cassandraUnit.keyspace,
				BytesArraySerializer.get(), StringSerializer.get(), BytesArraySerializer.get());
		query.setQuery("SELECT * FROM MyColumnFamily");
		QueryResult<CqlRows<byte[], String, byte[]>> result = query.execute();
		List<Row<byte[], String, byte[]>> rows = result.get().getList();
		Row row = rows.get(0);
		assertThat(row, notNullValue());
		assertThat((byte[]) row.getKey(), is(decodeHex("369ff963196dc2e5fe174dad2c0c6e9149b1acd9")));
	}

	private byte[] decodeHex(String valueToDecode) {
		try {
			return Hex.decodeHex(valueToDecode.toCharArray());
		} catch (DecoderException e) {
			return null;
		}
	}
}
