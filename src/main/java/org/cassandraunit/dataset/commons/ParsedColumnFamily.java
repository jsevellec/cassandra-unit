package org.cassandraunit.dataset.commons;

import me.prettyprint.hector.api.ddl.ColumnType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class ParsedColumnFamily {

    private String name;
    private ColumnType type = ColumnType.STANDARD;
    private String keyType = "BytesType";
    private String comparatorType = "BytesType";
    private ParsedDataType subComparatorType = null;
    private ParsedDataType defaultColumnValueType = ParsedDataType.BytesType;
    private String comment = "";
    private String compactionStrategy = null;
    private List<ParsedCompactionStrategyOption> compactionStrategyOptions = null;
    private Integer gcGraceSeconds = null;
    private Integer maxCompactionThreshold = null;
    private Integer minCompactionThreshold = null;
    private Double readRepairChance = null;
    private Boolean replicationOnWrite = null;
    private List<ParsedColumnMetadata> columnsMetadata = new ArrayList<ParsedColumnMetadata>();
    private List<ParsedRow> rows = new ArrayList<ParsedRow>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ColumnType getType() {
        return type;
    }

    public void setType(ColumnType type) {
        this.type = type;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setComparatorType(String comparatorType) {
        this.comparatorType = comparatorType;
    }

    public String getComparatorType() {
        return comparatorType;
    }

    public void setSubComparatorType(ParsedDataType subComparatorType) {
        this.subComparatorType = subComparatorType;
    }

    public ParsedDataType getSubComparatorType() {
        return subComparatorType;
    }

    public void setDefaultColumnValueType(ParsedDataType defaultColumnValueType) {
        this.defaultColumnValueType = defaultColumnValueType;
    }

    public ParsedDataType getDefaultColumnValueType() {
        return defaultColumnValueType;
    }

    public void setRows(List<ParsedRow> rows) {
        this.rows = rows;
    }

    public List<ParsedRow> getRows() {
        return rows;
    }

    public List<ParsedColumnMetadata> getColumnsMetadata() {
        return columnsMetadata;
    }

    public void setColumnsMetadata(List<ParsedColumnMetadata> columnsMetadata) {
        this.columnsMetadata = columnsMetadata;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCompactionStrategy() {
        return compactionStrategy;
    }

    public void setCompactionStrategy(String compactionStrategy) {
        this.compactionStrategy = compactionStrategy;
    }

    public List<ParsedCompactionStrategyOption> getCompactionStrategyOptions() {
        return compactionStrategyOptions;
    }

    public void setCompactionStrategyOptions(List<ParsedCompactionStrategyOption> compactionStrategyOptions) {
        this.compactionStrategyOptions = compactionStrategyOptions;
    }

    public Integer getGcGraceSeconds() {
        return gcGraceSeconds;
    }

    public void setGcGraceSeconds(Integer gcGraceSeconds) {
        this.gcGraceSeconds = gcGraceSeconds;
    }

    public Integer getMaxCompactionThreshold() {
        return maxCompactionThreshold;
    }

    public void setMaxCompactionThreshold(Integer maxCompactionThreshold) {
        this.maxCompactionThreshold = maxCompactionThreshold;
    }

    public Integer getMinCompactionThreshold() {
        return minCompactionThreshold;
    }

    public void setMinCompactionThreshold(Integer minCompactionThreshold) {
        this.minCompactionThreshold = minCompactionThreshold;
    }

    public Double getReadRepairChance() {
        return readRepairChance;
    }

    public void setReadRepairChance(Double readRepairChance) {
        this.readRepairChance = readRepairChance;
    }

    public Boolean getReplicationOnWrite() {
        return replicationOnWrite;
    }

    public void setReplicationOnWrite(Boolean replicationOnWrite) {
        this.replicationOnWrite = replicationOnWrite;
    }
}
