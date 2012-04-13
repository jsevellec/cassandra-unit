package org.cassandraunit.dataset.commons;

public class ParsedColumnMetadata {

	private String name;
	private ParsedDataType validationClass;
	private ParsedIndexType indexType;
	private String indexName;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParsedDataType getValidationClass() {
		return validationClass;
	}

	public void setValidationClass(ParsedDataType validationClass) {
		this.validationClass = validationClass;
	}

	public ParsedIndexType getIndexType() {
		return indexType;
	}

	public void setIndexType(ParsedIndexType indexType) {
		this.indexType = indexType;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

}
