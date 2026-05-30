package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.RowType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PaimonTableValidator {
    
    private PaimonTableValidator() {
    }
    
    static void validate(Table table, PaimonWriteConfig writeConfig) {
        validateLoadMode(table, writeConfig);
        validateRequiredColumns(table, writeConfig);
    }
    
    private static void validateLoadMode(Table table, PaimonWriteConfig writeConfig) {
        LoadMode loadMode = writeConfig.getLoadMode();
        if (loadMode == LoadMode.APPEND && !table.primaryKeys().isEmpty()) {
            throw DataXException.asDataXException("loadMode=APPEND不支持写入主键表，请使用UPSERT");
        }
        if (loadMode == LoadMode.UPSERT && table.primaryKeys().isEmpty()) {
            throw DataXException.asDataXException("loadMode=UPSERT要求Paimon表必须配置primaryKey");
        }
        if (loadMode == LoadMode.OVERWRITE_PARTITION && table.partitionKeys().isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION要求Paimon表必须是分区表");
        }
        if (loadMode == LoadMode.OVERWRITE_PARTITION) {
            validateOverwritePartition(table.partitionKeys(), writeConfig.getOverwritePartition());
        }
    }
    
    private static void validateRequiredColumns(Table table, PaimonWriteConfig writeConfig) {
        Set<String> configuredColumns = new HashSet<>();
        for (PaimonColumn column : writeConfig.getColumns()) {
            configuredColumns.add(column.getName());
        }
        
        for (String primaryKey : table.primaryKeys()) {
            if (!configuredColumns.contains(primaryKey)) {
                throw DataXException.asDataXException("部分列写入必须包含主键字段: " + primaryKey);
            }
        }
        
        for (String partitionKey : table.partitionKeys()) {
            if (!configuredColumns.contains(partitionKey) && !hasOverwritePartitionValue(writeConfig, partitionKey)) {
                throw DataXException.asDataXException("部分列写入必须包含分区字段: " + partitionKey);
            }
        }
        
        RowType rowType = table.rowType();
        for (int i = 0; i < rowType.getFieldCount(); i++) {
            String fieldName = rowType.getFieldNames().get(i);
            if (!rowType.getTypeAt(i).isNullable()
                    && !configuredColumns.contains(fieldName)
                    && !hasOverwritePartitionValue(writeConfig, fieldName)) {
                throw DataXException.asDataXException("部分列写入不能缺失非空字段: " + fieldName);
            }
        }
    }
    
    private static boolean hasOverwritePartitionValue(PaimonWriteConfig writeConfig, String fieldName) {
        return writeConfig.getOverwritePartition() != null && writeConfig.getOverwritePartition().containsKey(fieldName);
    }
    
    private static void validateOverwritePartition(List<String> partitionKeys, Map<String, String> overwritePartition) {
        if (overwritePartition == null || overwritePartition.isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须配置覆盖分区");
        }
        for (String partitionKey : partitionKeys) {
            if (!overwritePartition.containsKey(partitionKey)) {
                throw DataXException.asDataXException("overwritePartition.partition缺少分区字段: " + partitionKey);
            }
        }
        for (String partitionKey : overwritePartition.keySet()) {
            if (!partitionKeys.contains(partitionKey)) {
                throw DataXException.asDataXException("overwritePartition.partition包含非表分区字段: " + partitionKey);
            }
        }
    }
}
