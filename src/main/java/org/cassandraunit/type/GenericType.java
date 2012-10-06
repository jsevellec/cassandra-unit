package org.cassandraunit.type;

/**
 * @author Jeremy Sevellec
 */
public class GenericType {
    private String value;
    private GenericTypeEnum type;

    private String[] compositeValues;
    private GenericTypeEnum[] typesBelongingCompositeType;

    public GenericType(String value, GenericTypeEnum type) {
        super();
        this.value = value;
        this.type = type;
    }

    /**
     * constructor to use with the compositeType
     *
     * @param compositeValues             the string array of values
     * @param typesBelongingCompositeType the type array belonging the compositeType
     */
    public GenericType(String[] compositeValues, GenericTypeEnum[] typesBelongingCompositeType) {
        super();
        if ((compositeValues == null) || (typesBelongingCompositeType == null)
                || (compositeValues.length != typesBelongingCompositeType.length)) {
            throw new IllegalArgumentException(
                    "the 2 arrays must not be empty and must have the same size to match the value with the type into the compositeType");
        }
        this.compositeValues = compositeValues;
        this.type = GenericTypeEnum.COMPOSITE_TYPE;
        this.typesBelongingCompositeType = typesBelongingCompositeType;

    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public GenericTypeEnum getType() {
        return type;
    }

    public GenericTypeEnum[] getTypesBelongingCompositeType() {
        return typesBelongingCompositeType;
    }

    public String[] getCompositeValues() {
        return compositeValues;
    }

}
