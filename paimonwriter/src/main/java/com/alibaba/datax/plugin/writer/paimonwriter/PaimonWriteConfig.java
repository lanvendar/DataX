package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
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
    
    private final ProxyPrimaryKeyConfig proxyPrimaryKeyConfig;
    
    private PaimonWriteConfig(List<PaimonColumn> columns, LoadMode loadMode, int batchSize,
                              Map<String, String> overwritePartition, ProxyPrimaryKeyConfig proxyPrimaryKeyConfig) {
        this.columns = columns;
        this.loadMode = loadMode;
        this.batchSize = batchSize;
        this.overwritePartition = overwritePartition;
        this.proxyPrimaryKeyConfig = proxyPrimaryKeyConfig;
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
            columns.add(new PaimonColumn(normalizedName, type.trim(), trimToNull(column.getString(ConfigKey.COMMENT))));
        }
        
        LoadMode loadMode = LoadMode.from(taskConfig.getString(ConfigKey.LOAD_MODE, LoadMode.APPEND.name()));
        int batchSize = taskConfig.getInt(ConfigKey.BATCH_SIZE, 1000);
        if (batchSize <= 0) {
            throw DataXException.asDataXException("batchSize必须大于0");
        }
        
        ProxyPrimaryKeyConfig proxyPrimaryKeyConfig = parseProxyPrimaryKeyConfig(taskConfig, columnNames);
        if (loadMode == LoadMode.APPEND && proxyPrimaryKeyConfig.isProxy()) {
            throw DataXException.asDataXException("primaryKeyMode=PROXY会生成主键表，loadMode不能为APPEND，请使用UPSERT");
        }
        return new PaimonWriteConfig(
                columns, loadMode, batchSize, parseOverwritePartition(taskConfig, loadMode), proxyPrimaryKeyConfig);
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
    
    ProxyPrimaryKeyConfig getProxyPrimaryKeyConfig() {
        return proxyPrimaryKeyConfig;
    }
    
    private static Map<String, String> parseOverwritePartition(Configuration taskConfig, LoadMode mode) {
        if (mode != LoadMode.OVERWRITE_PARTITION) {
            return null;
        }
        
        Configuration overwriteConfig = taskConfig.getConfiguration(ConfigKey.OVERWRITE_PARTITION);
        if (overwriteConfig == null || !overwriteConfig.getBool("enabled", false)) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须开启overwritePartition.enabled");
        }
        
        Map<String, Object> rawPartition = overwriteConfig.getMap("partition");
        if (rawPartition == null || rawPartition.isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须配置overwritePartition.partition");
        }
        
        Map<String, String> partition = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawPartition.entrySet()) {
            if (entry.getValue() != null) {
                partition.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (partition.isEmpty()) {
            throw DataXException.asDataXException("loadMode=OVERWRITE_PARTITION时必须配置非空overwritePartition.partition");
        }
        return partition;
    }
    
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    private static String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
    
    private static ProxyPrimaryKeyConfig parseProxyPrimaryKeyConfig(Configuration taskConfig, Set<String> columnNames) {
        PrimaryKeyMode mode = PrimaryKeyMode.from(taskConfig.getString(ConfigKey.PRIMARY_KEY_MODE));
        if (mode == PrimaryKeyMode.FIELDS) {
            return new ProxyPrimaryKeyConfig(mode, ProxyPrimaryKeyAlgorithm.UUID, null);
        }
        
        List<String> sourceFields = splitKeys(taskConfig.getString(ConfigKey.PRIMARY_KEY));
        if (sourceFields.isEmpty()) {
            throw DataXException.asDataXException("primaryKeyMode=PROXY时primaryKey不能为空");
        }
        for (String sourceField : sourceFields) {
            if (ProxyPrimaryKeyConfig.PROXY_PRIMARY_KEY_NAME.equals(sourceField)) {
                throw DataXException.asDataXException("primaryKeyMode=PROXY时primaryKey不能包含代理主键字段_id_");
            }
            if (!columnNames.contains(sourceField)) {
                throw DataXException.asDataXException(
                        "primaryKeyMode=PROXY时primaryKey源字段必须存在于column配置: " + sourceField);
            }
        }
        if (columnNames.contains(ProxyPrimaryKeyConfig.PROXY_PRIMARY_KEY_NAME)) {
            throw DataXException.asDataXException("primaryKeyMode=PROXY时column不需要配置代理主键字段_id_");
        }
        return new ProxyPrimaryKeyConfig(
                mode,
                ProxyPrimaryKeyAlgorithm.from(taskConfig.getString(ConfigKey.PRIMARY_KEY_ALGORITHM)),
                sourceFields);
    }
    
    static List<String> splitKeys(String keys) {
        if (isBlank(keys)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String key : Arrays.asList(keys.split(","))) {
            String normalized = trimToNull(key);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }
}
