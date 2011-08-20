package org.cassandraunit.serializer;

import java.nio.ByteBuffer;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;

import org.cassandraunit.type.GenericType;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class GenericTypeSerializer extends AbstractSerializer<GenericType> {

	private static final GenericTypeSerializer instance = new GenericTypeSerializer();

	public static GenericTypeSerializer get() {
		return instance;
	}

	@Override
	public GenericType fromByteBuffer(ByteBuffer byteBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ByteBuffer toByteBuffer(GenericType genericType) {
		ByteBuffer byteBuffer = null;

		if (genericType.getType() == null) {
			byteBuffer = BytesArraySerializer.get().toByteBuffer(genericType.getValue().getBytes());
		} else {

			switch (genericType.getType()) {

			case BYTES_TYPE:
				byteBuffer = BytesArraySerializer.get().toByteBuffer(genericType.getValue().getBytes());
				break;
			case INTEGER_TYPE:
				byteBuffer = IntegerSerializer.get().toByteBuffer(Integer.parseInt(genericType.getValue()));
				break;
			case LEXICAL_UUID_TYPE:
				byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericType.getValue()));
				break;
			case LONG_TYPE:
				byteBuffer = LongSerializer.get().toByteBuffer(Long.parseLong(genericType.getValue()));
				break;
			case TIME_UUID_TYPE:
				byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericType.getValue()));
				break;
			case UTF_8_TYPE:
				byteBuffer = StringSerializer.get().toByteBuffer(genericType.getValue());
				break;
			case UUID_TYPE:
				byteBuffer = UUIDSerializer.get().toByteBuffer(UUID.fromString(genericType.getValue()));
				break;
			case COUNTER_TYPE:
			  byteBuffer = LongSerializer.get().toByteBuffer(Long.parseLong(genericType.getValue()));
			  break;
			default:
				byteBuffer = BytesArraySerializer.get().toByteBuffer(genericType.getValue().getBytes());
				break;
			}
		}
		return byteBuffer;
	}
}
