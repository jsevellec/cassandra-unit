package org.cassandraunit.utils;

import me.prettyprint.hector.api.ddl.ComparatorType;
import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.commons.ParsedDataType;
import org.cassandraunit.type.GenericTypeEnum;

public class ComparatorTypeHelper {

    public static ComparatorType verifyAndExtract(String comparatorType) {

        final String COMPOSITE_TYPE = "CompositeType";
        if (StringUtils.startsWith(comparatorType, COMPOSITE_TYPE)) {
            boolean error = false;
            /* CompositeType is defined */
            String aliasType = StringUtils.removeStart(comparatorType, COMPOSITE_TYPE);
            if (notStardOrNotEndWithParenthesis(aliasType)) {
                error = true;
            } else {
                String aliasTypeWithoutParenthesis = StringUtils
                        .removeStart(StringUtils.removeEnd(aliasType, ")"), "(");
                String[] types = StringUtils.split(aliasTypeWithoutParenthesis, ",");
                if (typesNotNullAndNotEmpty(types)) {
                    error = true;
                } else {
                    /* has to be a known type */
                    try {
                        for (int i = 0; i < types.length; i++) {
                            ParsedDataType.valueOf(types[i]);
                        }
                    } catch (IllegalArgumentException e) {
                        error = true;
                    }
                }
            }

            if (error) {
                throw new ParseException("CompositeType has to be like that : CompositeType(<type>,...,<type>)");
            } else {
                return ComparatorType.COMPOSITETYPE;
            }
        } else {
            /* standard Type */
            try {
                ParsedDataType.valueOf(comparatorType);
                return ComparatorType.getByClassName(comparatorType);
            } catch (IllegalArgumentException e) {
                throw new ParseException("ComparatorType value is not allowed");
            }
        }

    }

    private static boolean typesNotNullAndNotEmpty(String[] types) {
        return (types == null) || (types.length == 0);
    }

    private static boolean notStardOrNotEndWithParenthesis(String aliasType) {
        return !StringUtils.startsWith(aliasType, "(") || !StringUtils.endsWith(aliasType, ")");
    }

    public static GenericTypeEnum[] extractGenericTypesFromTypeAlias(String comparatorTypeAlias) {
        String aliasTypeWithoutParenthesis = StringUtils.removeStart(StringUtils.removeEnd(comparatorTypeAlias, ")"),
                "(");
        String[] types = StringUtils.split(aliasTypeWithoutParenthesis, ",");

        GenericTypeEnum[] genericTypesEnum = new GenericTypeEnum[types.length];

        for (int i = 0; i < types.length; i++) {
            genericTypesEnum[i] = GenericTypeEnum.fromValue(types[i]);
        }

        return genericTypesEnum;
    }
}
