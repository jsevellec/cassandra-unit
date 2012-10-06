package org.cassandraunit.model;

import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public class ColumnFamilyModel {

    private String name;
    private ColumnType type = ColumnType.STANDARD;
    private ComparatorType keyType = ComparatorType.BYTESTYPE;
    private String keyTypeAlias = "";
    private ComparatorType comparatorType = ComparatorType.BYTESTYPE;
    private String comparatorTypeAlias = "";
    private ComparatorType subComparatorType = null;
    private ComparatorType defaultColumnValueType = null;
    private String comment = "";
    private String compactionStrategy = null;
    private List<CompactionStrategyOptionModel> compactionStrategyOptions = null;
    private Integer gcGraceSeconds = null;
    private Integer maxCompactionThreshold = null;
    private Integer minCompactionThreshold = null;
    private Double readRepairChance = null;
    private Boolean replicationOnWrite = null;

    private List<ColumnMetadataModel> columnsMetadata = new ArrayList<ColumnMetadataModel>();

    private List<RowModel> rows = new ArrayList<RowModel>();

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

    public void setKeyType(ComparatorType keyType) {
        this.keyType = keyType;
    }

    public ComparatorType getKeyType() {
        return keyType;
    }

    public void setComparatorType(ComparatorType comparatorType) {
        this.comparatorType = comparatorType;
    }

    public ComparatorType getComparatorType() {
        return comparatorType;
    }

    public void setSubComparatorType(ComparatorType subComparatorType) {
        this.subComparatorType = subComparatorType;
    }

    public ComparatorType getSubComparatorType() {
        return subComparatorType;
    }

    public void setRows(List<RowModel> rows) {
        this.rows = rows;
    }

    public List<RowModel> getRows() {
        return rows;
    }

    public void setDefaultColumnValueType(ComparatorType defaultColumnValueType) {
        this.defaultColumnValueType = defaultColumnValueType;
    }

    public ComparatorType getDefaultColumnValueType() {
        return defaultColumnValueType;
    }

    public boolean isCounter() {
        return defaultColumnValueType != null && defaultColumnValueType.equals(ComparatorType.COUNTERTYPE);
    }

    public List<ColumnMetadataModel> getColumnsMetadata() {
        return columnsMetadata;
    }

    public void setColumnsMetadata(List<ColumnMetadataModel> columnsMetadata) {
        this.columnsMetadata = columnsMetadata;
    }

    public void addColumnMetadata(ColumnMetadataModel columnMetadata) {
        columnsMetadata.add(columnMetadata);
    }

    public String getComparatorTypeAlias() {
        return comparatorTypeAlias;
    }

    public void setComparatorTypeAlias(String comparatorTypeAlias) {
        this.comparatorTypeAlias = comparatorTypeAlias;
    }

    public String getKeyTypeAlias() {
        return keyTypeAlias;
    }

    public void setKeyTypeAlias(String keyTypeAlias) {
        this.keyTypeAlias = keyTypeAlias;
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

    public List<CompactionStrategyOptionModel> getCompactionStrategyOptions() {
        return compactionStrategyOptions;
    }

    public void setCompactionStrategyOptions(List<CompactionStrategyOptionModel> compactionStrategyOptions) {
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
