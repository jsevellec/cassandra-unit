package org.cassandraunit.model;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ComparatorType;

/**
 * @author Jeremy Sevellec
 */
public class ColumnMetadataModel {

    private String columnName = null;
    private ComparatorType validationClass = null;
    private ColumnIndexType columnIndexType = null;
    private String indexName = null;

    public ColumnMetadataModel() {
        super();
    }

    public ColumnMetadataModel(String columnName, ComparatorType validationClass, ColumnIndexType columnIndexType, String indexName) {
        super();
        this.columnName = columnName;
        this.validationClass = validationClass;
        this.setColumnIndexType(columnIndexType);
        this.indexName = indexName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public ComparatorType getValidationClass() {
        return validationClass;
    }

    public void setValidationClass(ComparatorType validationClass) {
        this.validationClass = validationClass;
    }

    public ColumnIndexType getColumnIndexType() {
        return columnIndexType;
    }

    public void setColumnIndexType(ColumnIndexType columnIndexType) {
        this.columnIndexType = columnIndexType;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

}
