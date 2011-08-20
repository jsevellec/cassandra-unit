package org.cassandraunit.type;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public enum GenericTypeEnum {

	BYTES_TYPE("BytesType"), INTEGER_TYPE("IntegerType"), LEXICAL_UUID_TYPE("LexicalUUIDType"), LONG_TYPE("LongType"), TIME_UUID_TYPE(
			"TimeUUIDType"), UTF_8_TYPE("UTF8Type"), UUID_TYPE("UUIDType"), COUNTER_TYPE("CounterColumnType");

	private final String value;

	GenericTypeEnum(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static GenericTypeEnum fromValue(String v) {
		for (GenericTypeEnum c : GenericTypeEnum.values()) {
			if (c.value.equalsIgnoreCase(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

	@Override
	public String toString() {
		return value;
	}

}
