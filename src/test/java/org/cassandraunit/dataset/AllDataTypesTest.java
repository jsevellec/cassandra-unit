package org.cassandraunit.dataset;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;
import me.prettyprint.cassandra.model.thrift.ThriftSliceQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.FloatSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.cassandraunit.AbstractCassandraUnit4TestCase;
import org.junit.Before;
import org.junit.Test;

public abstract class AllDataTypesTest extends AbstractCassandraUnit4TestCase {	
	
	public static final String COLUMN_FAMILY_NAME = "AllDataTypes";
	
	private class SollData {
		public Object value;
		public Serializer<?> serializer;
		
		public SollData(Object value, Serializer<?> serializer) {
			this.value = value;
			this.serializer = serializer;
		}
	}
	
	protected Map<String, SollData> COLUMNS = new HashMap<String, SollData>();

	@Before
	public void init() {
		COLUMNS.put("ascii", new SollData("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz|", StringSerializer.get()));
		COLUMNS.put("boolean", new SollData(true, BooleanSerializer.get()));
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
		Date sollDate = new Date();
		try {
			sollDate = dateFormat.parse("20010704 120856");
		} catch (ParseException e) {}
		COLUMNS.put("date", new SollData(sollDate, DateSerializer.get()));
		COLUMNS.put("double", new SollData(Double.MAX_VALUE, DoubleSerializer.get()));
		COLUMNS.put("float", new SollData(Float.MAX_VALUE, FloatSerializer.get()));
		COLUMNS.put("integer", new SollData(123456789, IntegerSerializer.get()));
		COLUMNS.put("lexicalUUID", new SollData(UUID.fromString("de80a290-dd71-11e0-8f9d-f81edfd6bd91"), UUIDSerializer.get()));
		COLUMNS.put("long", new SollData(987654321l, LongSerializer.get()));		
		COLUMNS.put("timeUUID", new SollData(UUID.fromString("a4a70900-24e1-11df-8924-001ff3591711"), UUIDSerializer.get()));
		COLUMNS.put("utf8", new SollData("Utf8String", StringSerializer.get()));
		COLUMNS.put("uuid", new SollData(UUID.fromString("c0178282-5c68-4fd3-9d9b-cae4f95fcd69"), UUIDSerializer.get()));		
	}
	
/*
	@Override
	public DataSet getDataSet() {
		return new ClassPathYamlDataSet("json/allDataTypes.json");
	}
*/
	@Test
	public void testCheckAllDataTypes() {
		SliceQuery<String, String, String> sliceQuery = 
			HFactory.createSliceQuery(getKeyspace(), StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
		sliceQuery.setColumnFamily(COLUMN_FAMILY_NAME)
	 		  .setKey("allDataTypes");
		ThriftSliceQuery<String, String, String> thriftSliceQuery = (ThriftSliceQuery<String, String, String>) sliceQuery;
		thriftSliceQuery.setColumnNames(COLUMNS.keySet());
		QueryResult<ColumnSlice<String, String>> resQuery = sliceQuery.execute();
		for (HColumn<String, String> column : resQuery.get().getColumns()) {
			SollData sollData = COLUMNS.get(column.getName());
			Assert.assertEquals(sollData.value.getClass().getName(),
					            sollData.value, sollData.serializer.fromByteBuffer(column.getValueBytes()));
		}
	}
}
