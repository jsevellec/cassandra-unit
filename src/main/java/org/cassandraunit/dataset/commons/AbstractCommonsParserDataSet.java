package org.cassandraunit.dataset.commons;

import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.model.*;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;
import org.cassandraunit.utils.ComparatorTypeHelper;
import org.cassandraunit.utils.TypeExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremy Sevellec
 */
public abstract class AbstractCommonsParserDataSet implements DataSet {

    protected KeyspaceModel keyspace = null;

    protected abstract ParsedKeyspace getParsedKeyspace();

    @Override
    public KeyspaceModel getKeyspace() {
        if (keyspace == null) {
            mapParsedKeyspaceToModel(getParsedKeyspace());
        }
        return keyspace;
    }

    @Override
    public List<ColumnFamilyModel> getColumnFamilies() {
        if (keyspace == null) {
            mapParsedKeyspaceToModel(getParsedKeyspace());
        }
        return keyspace.getColumnFamilies();
    }

    protected void mapParsedKeyspaceToModel(ParsedKeyspace parsedKeyspace) {
        if (parsedKeyspace == null) {
            throw new ParseException("dataSet is empty");
        }
        /* keyspace */
        keyspace = new KeyspaceModel();
        if (parsedKeyspace.getName() == null) {
            throw new ParseException("Keyspace name is mandatory");
        }
        keyspace.setName(parsedKeyspace.getName());

        /* optional conf */
        if (parsedKeyspace.getReplicationFactor() != 0) {
            keyspace.setReplicationFactor(parsedKeyspace.getReplicationFactor());
        }

        if (parsedKeyspace.getStrategy() != null) {
            try {
                keyspace.setStrategy(StrategyModel.fromValue(parsedKeyspace.getStrategy()));
            } catch (IllegalArgumentException e) {
                throw new ParseException("Invalid keyspace Strategy");
            }
        }

        mapsParsedColumnFamiliesToColumnFamiliesModel(parsedKeyspace);

    }

    private void mapsParsedColumnFamiliesToColumnFamiliesModel(ParsedKeyspace parsedKeyspace) {
        if (parsedKeyspace.getColumnFamilies() != null) {
            /* there is column families to integrate */
            for (ParsedColumnFamily parsedColumnFamily : parsedKeyspace.getColumnFamilies()) {
                keyspace.getColumnFamilies().add(mapParsedColumnFamilyToColumnFamilyModel(parsedColumnFamily));
            }
        }

    }

    private ColumnFamilyModel mapParsedColumnFamilyToColumnFamilyModel(ParsedColumnFamily parsedColumnFamily) {

        ColumnFamilyModel columnFamily = new ColumnFamilyModel();

        /* structure information */
        if (parsedColumnFamily == null || parsedColumnFamily.getName() == null) {
            throw new ParseException("Column Family Name is missing");
        }
        columnFamily.setName(parsedColumnFamily.getName());
        if (parsedColumnFamily.getType() != null) {
            columnFamily.setType(ColumnType.valueOf(parsedColumnFamily.getType().toString()));
        }

        columnFamily.setComment(parsedColumnFamily.getComment());

        if (parsedColumnFamily.getCompactionStrategy() != null) {
            columnFamily.setCompactionStrategy(parsedColumnFamily.getCompactionStrategy());
        }

        if (parsedColumnFamily.getCompactionStrategyOptions() != null && !parsedColumnFamily.getCompactionStrategyOptions().isEmpty()) {
            List<CompactionStrategyOptionModel> compactionStrategyOptionModels = new ArrayList<CompactionStrategyOptionModel>();
            for (ParsedCompactionStrategyOption parsedCompactionStrategyOption : parsedColumnFamily.getCompactionStrategyOptions()) {
                compactionStrategyOptionModels.add(new CompactionStrategyOptionModel(parsedCompactionStrategyOption.getName(), parsedCompactionStrategyOption.getValue()));
            }
            columnFamily.setCompactionStrategyOptions(compactionStrategyOptionModels);
        }

        if (parsedColumnFamily.getGcGraceSeconds() != null) {
            columnFamily.setGcGraceSeconds(parsedColumnFamily.getGcGraceSeconds());
        }

        if (parsedColumnFamily.getMaxCompactionThreshold() != null) {
            columnFamily.setMaxCompactionThreshold(parsedColumnFamily.getMaxCompactionThreshold());
        }

        if (parsedColumnFamily.getMinCompactionThreshold() != null) {
            columnFamily.setMinCompactionThreshold(parsedColumnFamily.getMinCompactionThreshold());
        }

        if (parsedColumnFamily.getReadRepairChance() != null) {
            columnFamily.setReadRepairChance(parsedColumnFamily.getReadRepairChance());
        }

        if (parsedColumnFamily.getReplicationOnWrite() != null) {
            columnFamily.setReplicationOnWrite(parsedColumnFamily.getReplicationOnWrite());
        }

        /* keyType */
        GenericTypeEnum[] typesBelongingCompositeTypeForKeyType = null;
        if (parsedColumnFamily.getKeyType() != null) {
            ComparatorType keyType = ComparatorTypeHelper.verifyAndExtract(parsedColumnFamily.getKeyType());
            columnFamily.setKeyType(keyType);
            if (ComparatorType.COMPOSITETYPE.getTypeName().equals(keyType.getTypeName())) {
                String keyTypeAlias = StringUtils.removeStart(parsedColumnFamily.getKeyType(),
                        ComparatorType.COMPOSITETYPE.getTypeName());
                columnFamily.setKeyTypeAlias(keyTypeAlias);
                typesBelongingCompositeTypeForKeyType = ComparatorTypeHelper
                        .extractGenericTypesFromTypeAlias(keyTypeAlias);
            }
        }

        /* comparatorType */
        GenericTypeEnum[] typesBelongingCompositeTypeForComparatorType = null;
        if (parsedColumnFamily.getComparatorType() != null) {
            ComparatorType comparatorType = ComparatorTypeHelper.verifyAndExtract(parsedColumnFamily
                    .getComparatorType());
            columnFamily.setComparatorType(comparatorType);
            if (ComparatorType.COMPOSITETYPE.getTypeName().equals(comparatorType.getTypeName())) {
                String comparatorTypeAlias = StringUtils.removeStart(parsedColumnFamily.getComparatorType(),
                        ComparatorType.COMPOSITETYPE.getTypeName());
                columnFamily.setComparatorTypeAlias(comparatorTypeAlias);
                typesBelongingCompositeTypeForComparatorType = ComparatorTypeHelper
                        .extractGenericTypesFromTypeAlias(comparatorTypeAlias);
            }
        }

        /* subComparatorType */
        if (parsedColumnFamily.getSubComparatorType() != null) {
            columnFamily.setSubComparatorType(ComparatorType.getByClassName(parsedColumnFamily.getSubComparatorType()
                    .name()));
        }

        if (parsedColumnFamily.getDefaultColumnValueType() != null) {
            columnFamily.setDefaultColumnValueType(ComparatorType.getByClassName(parsedColumnFamily
                    .getDefaultColumnValueType().name()));
        }

        columnFamily.setColumnsMetadata(mapParsedColumsMetadataToColumnsMetadata(parsedColumnFamily
                .getColumnsMetadata()));

        /* data information */
        columnFamily.setRows(mapParsedRowsToRowsModel(parsedColumnFamily, columnFamily.getKeyType(),
                typesBelongingCompositeTypeForKeyType, columnFamily.getComparatorType(),
                typesBelongingCompositeTypeForComparatorType, columnFamily.getSubComparatorType(),
                columnFamily.getDefaultColumnValueType()));

        return columnFamily;
    }

    private List<ColumnMetadataModel> mapParsedColumsMetadataToColumnsMetadata(
            List<ParsedColumnMetadata> parsedColumnsMetadata) {
        List<ColumnMetadataModel> columnMetadatas = new ArrayList<ColumnMetadataModel>();
        for (ParsedColumnMetadata parsedColumnMetadata : parsedColumnsMetadata) {
            columnMetadatas.add(mapParsedColumMetadataToColumnMetadata(parsedColumnMetadata));
        }
        return columnMetadatas;
    }

    private ColumnMetadataModel mapParsedColumMetadataToColumnMetadata(ParsedColumnMetadata parsedColumnMetadata) {
        if (parsedColumnMetadata.getName() == null) {
            throw new ParseException("column metadata name can't be empty");
        }

        if (parsedColumnMetadata.getValidationClass() == null) {
            throw new ParseException("column metadata validation class can't be empty");
        }

        ColumnMetadataModel columnMetadata = new ColumnMetadataModel();
        columnMetadata.setColumnName(parsedColumnMetadata.getName());
        columnMetadata.setValidationClass(ComparatorType.getByClassName(parsedColumnMetadata.getValidationClass()
                .name()));
        if (parsedColumnMetadata.getIndexType() != null) {
            columnMetadata.setColumnIndexType(ColumnIndexType.valueOf(parsedColumnMetadata.getIndexType().name()));
        }

        if (parsedColumnMetadata.getIndexName() != null) {
            columnMetadata.setIndexName(parsedColumnMetadata.getIndexName());
        }

        return columnMetadata;
    }

    private List<RowModel> mapParsedRowsToRowsModel(ParsedColumnFamily parsedColumnFamily, ComparatorType keyType,
                                                    GenericTypeEnum[] typesBelongingCompositeTypeForKeyType, ComparatorType comparatorType,
                                                    GenericTypeEnum[] typesBelongingCompositeTypeForComparatorType, ComparatorType subComparatorType,
                                                    ComparatorType defaultColumnValueType) {
        List<RowModel> rowsModel = new ArrayList<RowModel>();
        for (ParsedRow jsonRow : parsedColumnFamily.getRows()) {
            rowsModel.add(mapsParsedRowToRowModel(parsedColumnFamily.getColumnsMetadata(), jsonRow, keyType, typesBelongingCompositeTypeForKeyType,
                    comparatorType, typesBelongingCompositeTypeForComparatorType, subComparatorType,
                    defaultColumnValueType));
        }
        return rowsModel;
    }

    private RowModel mapsParsedRowToRowModel(List<ParsedColumnMetadata> metaData, ParsedRow parsedRow, ComparatorType keyType,
                                             GenericTypeEnum[] typesBelongingCompositeTypeForKeyType, ComparatorType comparatorType,
                                             GenericTypeEnum[] typesBelongingCompositeTypeForComparatorType, ComparatorType subComparatorType,
                                             ComparatorType defaultColumnValueType) {
        RowModel row = new RowModel();

        row.setKey(TypeExtractor.constructGenericType(parsedRow.getKey(), keyType,
                typesBelongingCompositeTypeForKeyType));

        row.setColumns(mapParsedColumnsToColumnsModel(metaData, parsedRow.getColumns(), comparatorType,
                typesBelongingCompositeTypeForComparatorType, defaultColumnValueType));
        row.setSuperColumns(mapParsedSuperColumnsToSuperColumnsModel(metaData, parsedRow.getSuperColumns(), comparatorType,
                subComparatorType, defaultColumnValueType));
        return row;
    }

    private List<SuperColumnModel> mapParsedSuperColumnsToSuperColumnsModel(List<ParsedColumnMetadata> metaData, List<ParsedSuperColumn> parsedSuperColumns,
                                                                            ComparatorType comparatorType, ComparatorType subComparatorType, ComparatorType defaultColumnValueType) {
        List<SuperColumnModel> columnsModel = new ArrayList<SuperColumnModel>();
        for (ParsedSuperColumn parsedSuperColumn : parsedSuperColumns) {
            columnsModel.add(mapParsedSuperColumnToSuperColumnModel(metaData, parsedSuperColumn, comparatorType,
                    subComparatorType, defaultColumnValueType));
        }

        return columnsModel;
    }

    private SuperColumnModel mapParsedSuperColumnToSuperColumnModel(List<ParsedColumnMetadata> metaData, ParsedSuperColumn parsedSuperColumn,
                                                                    ComparatorType comparatorType, ComparatorType subComparatorType, ComparatorType defaultColumnValueType) {
        SuperColumnModel superColumnModel = new SuperColumnModel();

        superColumnModel.setName(new GenericType(parsedSuperColumn.getName(), GenericTypeEnum.fromValue(comparatorType
                .getTypeName())));

        superColumnModel.setColumns(mapParsedColumnsToColumnsModel(metaData, parsedSuperColumn.getColumns(), subComparatorType,
                null, defaultColumnValueType));
        return superColumnModel;
    }

    private List<ColumnModel> mapParsedColumnsToColumnsModel(List<ParsedColumnMetadata> metaData, List<ParsedColumn> parsedColumns,
                                                             ComparatorType comparatorType, GenericTypeEnum[] typesBelongingCompositeTypeForComparatorType,
                                                             ComparatorType defaultColumnValueType) {
        List<ColumnModel> columnsModel = new ArrayList<ColumnModel>();
        for (ParsedColumn jsonColumn : parsedColumns) {
            ParsedColumnMetadata columnMetaData = null;
            for (ParsedColumnMetadata tmpMetaData : metaData) {
                if (tmpMetaData.getName().equals(jsonColumn.getName())) {
                    columnMetaData = tmpMetaData;
                    break;
                }
            }
            columnsModel.add(mapParsedColumnToColumnModel(columnMetaData, jsonColumn, comparatorType,
                    typesBelongingCompositeTypeForComparatorType, defaultColumnValueType));
        }
        return columnsModel;
    }

    private ColumnModel mapParsedColumnToColumnModel(ParsedColumnMetadata metaData, ParsedColumn parsedColumn, ComparatorType comparatorType,
                                                     GenericTypeEnum[] typesBelongingCompositeTypeForComparatorType, ComparatorType defaultColumnValueType) {
        ColumnModel columnModel = new ColumnModel();

        columnModel.setName(TypeExtractor.constructGenericType(parsedColumn.getName(), comparatorType,
                typesBelongingCompositeTypeForComparatorType));

        if (ComparatorType.COUNTERTYPE.getClassName().equals(defaultColumnValueType.getClassName())
                && TypeExtractor.containFunctions(parsedColumn.getValue())) {
            throw new ParseException("Impossible to override Column value into a Counter column family");
        }

        GenericType columnValue = null;
        if (metaData != null) {
            GenericTypeEnum genTypeEnum = GenericTypeEnum.fromValue(metaData.getValidationClass().name());
            columnValue = new GenericType(parsedColumn.getValue(), genTypeEnum);
        } else {
            columnValue = TypeExtractor.extract(parsedColumn.getValue(), defaultColumnValueType);
        }
        columnModel.setValue(columnValue);
        String timestamp = parsedColumn.getTimestamp();
        if(timestamp != null) {
            columnModel.setTimestamp(Long.valueOf(timestamp));
        } else {
            columnModel.setTimestamp(null);
        }
        return columnModel;
    }

}
