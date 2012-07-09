package org.cassandraunit.utils;

import me.prettyprint.hector.api.ddl.ComparatorType;

import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;

/**
 * 
 * @author Jeremy Sevellec
 * 
 */
public class TypeExtractor {

	private static String endTypeFunction = ")";
	private static String startTypeFunction = "(";
	private static String[] availableTypeFunctionArray = new String[] { "bytes(", "integer(", "lexicaluuid(", "long(",
			"utf8(", "timeuuid(", "uuid(", "ascii(", "boolean(", "date(", "double(", "float(", "countercolumn("};

	public static GenericType extract(String valueToExtract, ComparatorType defaultValueType) {
		String extractedValue = null;
		GenericType genericType = null;
		if (containFunctions(valueToExtract)) {
			/* there is a type function defined */
			String typeFunction = StringUtils.substringBefore(valueToExtract, startTypeFunction);
			String tmp = StringUtils.substringAfter(valueToExtract, typeFunction + startTypeFunction);
			extractedValue = StringUtils.substringBefore(tmp, endTypeFunction);
			genericType = new GenericType(extractedValue, GenericTypeEnum.fromValue(typeFunction + "type"));
		} else {
			/* there is no type function defined */
			extractedValue = valueToExtract;
			if (defaultValueType == null) {
				genericType = new GenericType(extractedValue, GenericTypeEnum.BYTES_TYPE);
			} else {
				genericType = new GenericType(extractedValue, GenericTypeEnum.fromValue(defaultValueType.getTypeName()));
			}
		}
		return genericType;
	}

	public static boolean containFunctions(String valueToExtract) {
		return StringUtils.startsWithAny(valueToExtract, availableTypeFunctionArray)
				&& StringUtils.endsWith(valueToExtract, endTypeFunction);
	}

	public static GenericType constructGenericType(String rowKeyOrColumnName, ComparatorType type,
			GenericTypeEnum[] typesBelongingCompositeType) {
		GenericType key = null;

		if (type == null) {
			key = new GenericType(rowKeyOrColumnName, GenericTypeEnum.BYTES_TYPE);
		} else if (ComparatorType.COMPOSITETYPE.getTypeName().equals(type.getTypeName())) {
			/* composite type */
			try {
				key = new GenericType(StringUtils.split(rowKeyOrColumnName, ":"), typesBelongingCompositeType);
			} catch (IllegalArgumentException e) {
				throw new ParseException(rowKeyOrColumnName
						+ " doesn't fit with the schema declaration of your composite type");
			}
		} else {
			/* simple type */
			key = new GenericType(rowKeyOrColumnName, GenericTypeEnum.fromValue(type.getTypeName()));
		}
		return key;
	}
}
