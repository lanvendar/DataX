package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PaimonWriteConfig {
    
    private final List<PaimonColumn> columns;
    
    private final LoadMode loadMode;
    
    private final int batchSize;
    
    private final Map<String, String> overwritePartition;
    
    private PaimonWriteConfig(List<PaimonColumn> columns, LoadMode loadMode, int batchSize,
                              Map<String, String> overwritePartition) {
        this.columns = columns;
        this.loadMode = loadMode;
        this.batchSize = batchSize;
        this.overwritePartition = overwritePartition;
    }
    
    static PaimonWriteConfig from(Configuration taskConfig) {
        List<Configuration> columnConfigs = taskConfig.getListConfiguration(ConfigKey.COLUMN);
        if (columnConfigs == null || columnConfigs.isEmpty()) {
            throw DataXException.asDataXException("column不能为空");
        }
        
        List<PaimonColumn> columns = new ArrayList<>();
        Set<String> columnNames = new HashSet<>();
        for (Configuration column : columnConfigs) {
            String name = column.getString("name");
            String type = column.getString("type");
            if (isBlank(name)) {
                throw DataXException.asDataXException("column.name不能为空");
            }
            if (isBlank(type)) {
                throw DataXException.asDataXException("column.type不能为空");
            }
            String normalizedName = name.trim();
            if (!columnNames.add(normalizedName)) {
                throw DataXException.asDataXException("column.name重复: " + normalizedName);
            }
            columns.add(new PaimonColumn(normalizedName, type.trim()));
        }
        
        LoadMode loadMode = LoadMode.from(taskConfig.getString(ConfigKey.LOAD_MODE, LoadMode.APPEND.name()));
        int batchSize = taskConfig.getInt(ConfigKey.WRITE_BATCH_SIZE, 1000);
        if (batchSize <= 0) {
            throw DataXException.asDataXException("write.batchSize必须大于0");
        }
        
        return new PaimonWriteConfig(columns, loadMode, batchSize, parseOverwritePartition(taskConfig, loadMode));
    }
    
    List<PaimonColumn> getColumns() {
        return columns;
    }
    
    LoadMode getLoadMode() {
        return loadMode;
    }
    
    int getBatchSize() {
        return batchSize;
    }
    
    Map<String, String> getOverwritePartition() {
        return overwritePartition;
    }
    
    private static Map<String, String> parseOverwritePartition(Configuration taskConfig, LoadMode mode) {
        if (mode != LoadMode.OVERWRITE_PARTITION) {
            return null;
        }
        
        Configuration overwriteConfig = taskConfig.getConfiguration(ConfigKey.WRITE_OVERWRITE_PARTITION);
        if (overwriteConfig == null || !overwriteConfig.getBool("enabled", false)) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须开启write.overwritePartition.enabled");
        }
        
        Map<String, Object> rawPartition = overwriteConfig.getMap("partition");
        if (rawPartition == null || rawPartition.isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须配置write.overwritePartition.partition");
        }
        
        Map<String, String> partition = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawPartition.entrySet()) {
            if (entry.getValue() != null) {
                partition.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (partition.isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须配置非空write.overwritePartition.partition");
        }
        return partition;
    }
    
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
