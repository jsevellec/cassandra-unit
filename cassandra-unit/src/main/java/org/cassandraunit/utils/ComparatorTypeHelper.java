package org.cassandraunit.utils;

import me.prettyprint.hector.api.ddl.ComparatorType;

import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.commons.ParsedDataType;
import org.cassandraunit.type.GenericTypeEnum;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
 */
public class ComparatorTypeHelper {

	private static final String COMPOSITE_TYPE = "CompositeType";

	public static ComparatorType verifyAndExtract(String comparatorType) {
		if (isCompositeType(comparatorType)) {
			return parseCompositeComparatorType(comparatorType);
		} else {
			return parseSimpleComparatorType(comparatorType);
		}
	}

	private static boolean isCompositeType(String comparatorType) {
		return StringUtils.startsWith(comparatorType, COMPOSITE_TYPE);
	}

	private static ComparatorType parseCompositeComparatorType(String comparatorType) {
		String compositeType = removePrependingCompositeType(comparatorType);

		if (!hasParenthesisForComponentTypes(compositeType)) {
			throw new ParseException("[" + comparatorType + "] must contain types wrapped within parenthesis, like: CompositeType(<type>,...,<type>).");
		}

		String[] types = extractComponentTypes(compositeType);

		if (areTypesNullOrEmpty(types)) {
			throw new ParseException("[" + comparatorType + "] must contain non-empty types, like: CompositeType(<type>,...,<type>).");
		}

		for (String type : types) {
			parseComponentType(comparatorType, type);
		}

		return ComparatorType.COMPOSITETYPE;
	}

	private static String removePrependingCompositeType(String comparatorType) {
		return StringUtils.removeStart(comparatorType, COMPOSITE_TYPE);
	}

	private static boolean hasParenthesisForComponentTypes(String types) {
		return StringUtils.startsWith(types, "(") && StringUtils.endsWith(types, ")");
	}

	private static String[] extractComponentTypes(String compositeType) {
		String compositeTypeWithoutParenthesis = removeWrappingParenthesis(compositeType);
		return StringUtils.split(compositeTypeWithoutParenthesis, ",");
	}

	private static String removeWrappingParenthesis(String aliasType) {
		return StringUtils.removeStart(StringUtils.removeEnd(aliasType, ")"), "(");
	}

	private static boolean areTypesNullOrEmpty(String[] types) {
		if (isNullOrEmpty(types))
			return true;

		for (String type : types)
			if (isNullOrEmpty(type))
				return true;

		return false;
	}

	private static boolean isNullOrEmpty(String[] types) {
		return (types == null) || (types.length == 0);
	}

	private static boolean isNullOrEmpty(String type) {
		return (type == null) || (type.length() == 0);
	}

	private static void parseComponentType(String comparatorType, String type) {
		try {
			// Component types may take values like "IntegerType" or
			// "TimeUUID(reversed=true)" or even "LongType(reversed=false)".
			String cleanType = removeReversedQualifierIfPresent(type);
			ParsedDataType.valueOf(cleanType);
		} catch (IllegalArgumentException e) {
			throw new ParseException("[" + comparatorType + "] contains an invalid 'component' type: [" + type + "].");
		}
	}

	private static String removeReversedQualifierIfPresent(String type) {
		return StringUtils.removeEndIgnoreCase(StringUtils.removeEndIgnoreCase(type, "(reversed=true)"), "(reversed=false)");
	}

	private static ComparatorType parseSimpleComparatorType(String type) {
		try {
			// ComparatorTypes may take values like "IntegerType" or
			// "TimeUUID(reversed=true)" or even "LongType(reversed=false)".
			String cleanType = removeReversedQualifierIfPresent(type);
			ParsedDataType.valueOf(cleanType);
			return ComparatorType.getByClassName(cleanType);
		} catch (IllegalArgumentException e) {
			throw new ParseException("ComparatorType [" + type + "] is invalid.");
		}
	}

	public static GenericTypeEnum[] extractGenericTypesFromTypeAlias(String comparatorType) {
		String[] types = extractComponentTypes(comparatorType);
		GenericTypeEnum[] genericTypesEnum = new GenericTypeEnum[types.length];

		for (int i = 0; i < types.length; i++) {
			String cleanType = removeReversedQualifierIfPresent(types[i]);
			genericTypesEnum[i] = GenericTypeEnum.fromValue(cleanType);
		}

		return genericTypesEnum;
	}
}
