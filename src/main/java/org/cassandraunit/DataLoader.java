package org.cassandraunit;

import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HCounterSuperColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.ddl.ColumnDefinition;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.StringUtils;
import org.cassandraunit.dataset.DataSet;
import org.cassandraunit.model.ColumnFamilyModel;
import org.cassandraunit.model.ColumnMetadataModel;
import org.cassandraunit.model.ColumnModel;
import org.cassandraunit.model.CompactionStrategyOptionModel;
import org.cassandraunit.model.KeyspaceModel;
import org.cassandraunit.model.RowModel;
import org.cassandraunit.model.SuperColumnModel;
import org.cassandraunit.serializer.GenericTypeSerializer;
import org.cassandraunit.type.GenericType;
import org.cassandraunit.type.GenericTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jeremy Sevellec
 * @author Marc Carre (#27)
 */
public class DataLoader {
    Cluster cluster = null;

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    public DataLoader(String clusterName, String host) {
        super();
        cluster = HFactory.getOrCreateCluster(clusterName, host);
    }

    protected Cluster getCluster() {
        return cluster;
    }

    public void load(DataSet dataSet) {
        load(dataSet, new LoadingOption());
    }

    public void load(DataSet dataSet, LoadingOption loadingOption) {
        KeyspaceModel dataSetKeyspace = dataSet.getKeyspace();

        dropKeyspaceIfExist(dataSetKeyspace.getName());

        KeyspaceDefinition keyspaceDefinition = createKeyspaceDefinition(dataSet, loadingOption);

        cluster.addKeyspace(keyspaceDefinition, true);

        log.info("creating keyspace : {}", keyspaceDefinition.getName());
        Keyspace keyspace = HFactory.createKeyspace(dataSet.getKeyspace().getName(), cluster);

        if (!loadingOption.isOnlySchema()) {
            log.info("loading data into keyspace : {}", keyspaceDefinition.getName());
            loadData(dataSet, keyspace);
        }
    }

    private KeyspaceModel overrideKeyspaceValueIfneeded(KeyspaceModel keyspace, LoadingOption loadingOption) {
        if (loadingOption.isOverrideReplicationFactor()) {
            keyspace.setReplicationFactor(loadingOption.getReplicationFactor());
        }

        if (loadingOption.isOverrideStrategy()) {
            keyspace.setStrategy(loadingOption.getStrategy());
        }

        return keyspace;
    }

    private KeyspaceDefinition createKeyspaceDefinition(DataSet dataSet, LoadingOption loadingOption) {
        List<ColumnFamilyDefinition> columnFamilyDefinitions = createColumnFamilyDefinitions(dataSet);

        KeyspaceModel dataSetKeyspace = dataSet.getKeyspace();

        dataSetKeyspace = overrideKeyspaceValueIfneeded(dataSetKeyspace, loadingOption);

        KeyspaceDefinition keyspaceDefinition = HFactory.createKeyspaceDefinition(dataSetKeyspace.getName(),
                dataSetKeyspace.getStrategy().value(), dataSetKeyspace.getReplicationFactor(), columnFamilyDefinitions);
        return keyspaceDefinition;
    }

    private void dropKeyspaceIfExist(String keyspaceName) {
        KeyspaceDefinition existedKeyspace = cluster.describeKeyspace(keyspaceName);
        if (existedKeyspace != null) {
            log.info("dropping existing keyspace : {}", existedKeyspace.getName());
            cluster.dropKeyspace(keyspaceName, true);
        }
    }

    private void loadData(DataSet dataSet, Keyspace keyspace) {
        for (ColumnFamilyModel columnFamily : dataSet.getColumnFamilies()) {
            loadColumnFamilyData(columnFamily, keyspace);
        }

    }

    private void loadColumnFamilyData(ColumnFamilyModel columnFamily, Keyspace keyspace) {
        Mutator<GenericType> mutator = HFactory.createMutator(keyspace, GenericTypeSerializer.get());
        for (RowModel row : columnFamily.getRows()) {
            switch (columnFamily.getType()) {
                case STANDARD:
                    loadStandardColumnFamilyData(columnFamily, mutator, row);
                    break;
                case SUPER:
                    loadSuperColumnFamilyData(columnFamily, mutator, row);
                    break;
                default:
                    break;
            }

        }
        mutator.execute();

    }

    private void loadSuperColumnFamilyData(ColumnFamilyModel columnFamily, Mutator<GenericType> mutator, RowModel row) {
        if (columnFamily.isCounter()) {
            for (SuperColumnModel superColumnModel : row.getSuperColumns()) {
                HCounterSuperColumn<GenericType, GenericType> superCounterColumn = HFactory.createCounterSuperColumn(
                        superColumnModel.getName(), createHCounterColumnList(superColumnModel.getColumns()),
                        GenericTypeSerializer.get(), GenericTypeSerializer.get());
                mutator.addCounter(row.getKey(), columnFamily.getName(), superCounterColumn);
            }
        } else {
            for (SuperColumnModel superColumnModel : row.getSuperColumns()) {
                HSuperColumn<GenericType, GenericType, GenericType> superColumn = HFactory.createSuperColumn(
                        superColumnModel.getName(), createHColumnList(superColumnModel.getColumns()),
                        GenericTypeSerializer.get(), GenericTypeSerializer.get(), GenericTypeSerializer.get());
                mutator.addInsertion(row.getKey(), columnFamily.getName(), superColumn);
            }
        }
    }

    private void loadStandardColumnFamilyData(ColumnFamilyModel columnFamily, Mutator<GenericType> mutator, RowModel row) {
        if (columnFamily.isCounter()) {
            for (HCounterColumn<GenericType> hCounterColumn : createHCounterColumnList(row.getColumns())) {
                mutator.addCounter(row.getKey(), columnFamily.getName(), hCounterColumn);
            }
        } else {
            for (HColumn<GenericType, GenericType> hColumn : createHColumnList(row.getColumns())) {
                mutator.addInsertion(row.getKey(), columnFamily.getName(), hColumn);
            }
        }
    }

    private List<HColumn<GenericType, GenericType>> createHColumnList(List<ColumnModel> columnsModel) {
        List<HColumn<GenericType, GenericType>> hColumns = new ArrayList<HColumn<GenericType, GenericType>>();
        for (ColumnModel columnModel : columnsModel) {
            GenericType columnValue = columnModel.getValue();
            if (columnValue == null) {
                columnValue = new GenericType("", GenericTypeEnum.BYTES_TYPE);
            }
            Long timestamp = columnModel.getTimestamp();
            if(timestamp == null) {
                timestamp = System.currentTimeMillis();
            }
            HColumn<GenericType, GenericType> column = HFactory.createColumn(columnModel.getName(),
                    columnValue, timestamp, GenericTypeSerializer.get(), GenericTypeSerializer.get());
            hColumns.add(column);
        }
        return hColumns;
    }

    private List<HCounterColumn<GenericType>> createHCounterColumnList(List<ColumnModel> columnsModel) {
        List<HCounterColumn<GenericType>> hColumns = new ArrayList<HCounterColumn<GenericType>>();
        for (ColumnModel columnModel : columnsModel) {
            HCounterColumn<GenericType> column = HFactory.createCounterColumn(columnModel.getName(), LongSerializer
                    .get().fromByteBuffer(GenericTypeSerializer.get().toByteBuffer(columnModel.getValue())),
                    GenericTypeSerializer.get());
            hColumns.add(column);
        }
        return hColumns;
    }

    private List<ColumnFamilyDefinition> createColumnFamilyDefinitions(DataSet dataSet) {
        KeyspaceModel dataSetKeyspace = dataSet.getKeyspace();
        List<ColumnFamilyDefinition> columnFamilyDefinitions = new ArrayList<ColumnFamilyDefinition>();
        for (ColumnFamilyModel columnFamily : dataSet.getColumnFamilies()) {
            ColumnFamilyDefinition cfDef = HFactory.createColumnFamilyDefinition(dataSetKeyspace.getName(),
                    columnFamily.getName(),
                    ComparatorType.getByClassName(columnFamily.getComparatorType().getClassName()),
                    createColumnsDefinition(columnFamily.getColumnsMetadata()));
            cfDef.setColumnType(columnFamily.getType());
            cfDef.setComment(columnFamily.getComment());

            if (columnFamily.getCompactionStrategy() != null) {
                cfDef.setCompactionStrategy(columnFamily.getCompactionStrategy());
            }

            if (columnFamily.getCompactionStrategyOptions() != null && !columnFamily.getCompactionStrategyOptions().isEmpty()) {
                Map<String, String> compactionStrategyOptions = new HashMap<String, String>();
                for (CompactionStrategyOptionModel compactionStrategyOption : columnFamily.getCompactionStrategyOptions()) {
                    compactionStrategyOptions.put(compactionStrategyOption.getName(), compactionStrategyOption.getValue());
                }
                cfDef.setCompactionStrategyOptions(compactionStrategyOptions);
            }

            if (columnFamily.getGcGraceSeconds() != null) {
                cfDef.setGcGraceSeconds(columnFamily.getGcGraceSeconds());
            }

            if (columnFamily.getMaxCompactionThreshold() != null) {
                cfDef.setMaxCompactionThreshold(columnFamily.getMaxCompactionThreshold());
            }

            if (columnFamily.getMinCompactionThreshold() != null) {
                cfDef.setMinCompactionThreshold(columnFamily.getMinCompactionThreshold());
            }

            if (columnFamily.getReadRepairChance() != null) {
                cfDef.setReadRepairChance(columnFamily.getReadRepairChance());
            }

            if (columnFamily.getReplicationOnWrite() != null) {
                cfDef.setReplicateOnWrite(columnFamily.getReplicationOnWrite());
            }

            cfDef.setKeyValidationClass(columnFamily.getKeyType().getTypeName() + columnFamily.getKeyTypeAlias());

            if (columnFamily.getDefaultColumnValueType() != null) {
                cfDef.setDefaultValidationClass(columnFamily.getDefaultColumnValueType().getClassName());
            }

            if (columnFamily.getType().equals(ColumnType.SUPER) && columnFamily.getSubComparatorType() != null) {
                cfDef.setSubComparatorType(columnFamily.getSubComparatorType());
            }

            if (ComparatorType.COMPOSITETYPE.equals(columnFamily.getComparatorType())
                    || StringUtils.containsIgnoreCase(columnFamily.getComparatorTypeAlias(), ColumnFamilyModel.REVERSED_QUALIFIER)) {
                cfDef.setComparatorTypeAlias(columnFamily.getComparatorTypeAlias());
            }

            columnFamilyDefinitions.add(cfDef);
        }
        return columnFamilyDefinitions;
    }

    private List<ColumnDefinition> createColumnsDefinition(List<ColumnMetadataModel> columnsMetadata) {
        List<ColumnDefinition> columnsDefinition = new ArrayList<ColumnDefinition>();
        for (ColumnMetadataModel columnMetadata : columnsMetadata) {
            BasicColumnDefinition columnDefinition = new BasicColumnDefinition();

            GenericType columnName = columnMetadata.getColumnName();
            columnDefinition.setName(GenericTypeSerializer.get().toByteBuffer(columnName));

            if (columnMetadata.getColumnIndexType() != null) {
                String indexName = columnMetadata.getIndexName();
                columnDefinition.setIndexName((indexName == null) ? columnName.getValue() : indexName);
                columnDefinition.setIndexType(columnMetadata.getColumnIndexType());
            }

            if (columnMetadata.getValidationClass() != null) {
                columnDefinition.setValidationClass(columnMetadata.getValidationClass().getClassName());
            }
            columnsDefinition.add(columnDefinition);

        }
        return columnsDefinition;
    }
}
