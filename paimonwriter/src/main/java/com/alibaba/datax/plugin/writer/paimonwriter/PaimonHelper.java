/*
 * Copyright © 2020-2024 Nimbus Corporation All rights reserved.
 *
 * 使本项目源码前请仔细阅读以下协议内容，如果你同意以下协议才能使用本项目所有的功能,
 * 否则如果你违反了以下协议，有可能陷入法律纠纷和赔偿，作者保留追究法律责任的权利.
 *
 * 1、本代码为商业源代码，只允许已授权内部人员查看使用
 * 2、任何人员无权将代码泄露或者授权给其他未被授权人员使用
 * 3、任何修改请保留原始作者信息，不得擅自删除及修改
 *
 * 请保留以上版权信息，否则作者将保留追究法律责任.
 */

package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.paimon.CoreOptions;
import org.apache.paimon.catalog.Catalog;
import org.apache.paimon.catalog.CatalogContext;
import org.apache.paimon.catalog.CatalogFactory;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.options.Options;
import org.apache.paimon.schema.Schema;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Li Zhengkai
 * @version 3.0, 2024/7/3
 * @since 2024/7/3
 */
public class PaimonHelper {
    
    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaimonHelper.class);
    
    private static final Set<String> NON_TABLE_OPTION_KEYS = new HashSet<>();
    
    static {
        NON_TABLE_OPTION_KEYS.add("warehouse");
        NON_TABLE_OPTION_KEYS.add("catalogType");
        NON_TABLE_OPTION_KEYS.add("database");
        NON_TABLE_OPTION_KEYS.add("endpoint");
        NON_TABLE_OPTION_KEYS.add("region");
        NON_TABLE_OPTION_KEYS.add("accessKey");
        NON_TABLE_OPTION_KEYS.add("accessKeyRef");
        NON_TABLE_OPTION_KEYS.add("secretKey");
        NON_TABLE_OPTION_KEYS.add("secretKeyRef");
        NON_TABLE_OPTION_KEYS.add("pathStyleAccess");
        NON_TABLE_OPTION_KEYS.add("sslEnabled");
        NON_TABLE_OPTION_KEYS.add("s3.endpoint");
        NON_TABLE_OPTION_KEYS.add("s3.region");
        NON_TABLE_OPTION_KEYS.add("s3.access-key");
        NON_TABLE_OPTION_KEYS.add("s3.secret-key");
        NON_TABLE_OPTION_KEYS.add("s3.path-style-access");
        NON_TABLE_OPTION_KEYS.add("s3.ssl.enabled");
    }
    
    /**
     * 创建 paimon catalog.
     *
     * @return catalog对象.
     */
    static Catalog createPaimonCatalog(Configuration originalConfig) {
        Options options = PaimonConfigUtil.buildCatalogOptions(originalConfig);
        CatalogContext context = CatalogContext.create(options);
        return CatalogFactory.createCatalog(context);
    }
    
    /**
     * 获取 paimon table 对象.
     *
     * @return table对象.
     */
    static Table getPaimonTable(Configuration originalConfig) {
        Catalog paimonCatalog = createPaimonCatalog(originalConfig);
        String databaseName = PaimonConfigUtil.database(originalConfig);
        String tableName = originalConfig.getString(ConfigKey.TABLE);
        
        Identifier identifier = Identifier.create(databaseName, tableName);
        Table table = null;
        try {
            table = paimonCatalog.getTable(identifier);
        } catch (org.apache.paimon.catalog.Catalog.TableNotExistException e) {
            LOG.error("table {} not exist, create it", tableName);
            createPaimonTable(paimonCatalog, originalConfig);
            try {
                table = paimonCatalog.getTable(identifier);
            } catch (Catalog.TableNotExistException ex) {
                throw DataXException.asDataXException(
                        String.format("表不存在: '%s'", tableName));
            }
        }
        return table;
    }
    
    /**
     * 获取 paimon 建表语句.
     *
     * @param catalog Paimon Catalog
     */
    static void createPaimonTable(Catalog catalog, Configuration originalConfig) {
        Map<String, String> options = tableOptions(originalConfig);
        
        Schema.Builder schemaBuilder = Schema.newBuilder();
        //建表主键
        if (StringUtils.isNotBlank(originalConfig.getString(ConfigKey.PRIMARY_KEY))) {
            schemaBuilder.primaryKey(splitKeys(originalConfig.getString(ConfigKey.PRIMARY_KEY)));
        }
        //建表分区
        if (StringUtils.isNotBlank(originalConfig.getString(ConfigKey.PARTITION_KEY))) {
            schemaBuilder.partitionKeys(splitKeys(originalConfig.getString(ConfigKey.PARTITION_KEY)));
            options.put(CoreOptions.METASTORE_PARTITIONED_TABLE.key(), "true");
        }
        //建表字段
        List<Configuration> columns = originalConfig.getListConfiguration(ConfigKey.COLUMN);
        Validate.notEmpty(columns, "column can't be empty");
        columns.forEach(column -> {
            String name = column.getString("name");
            Validate.notBlank(name, "column.name can't be blank");
            
            String type = column.getString("type");
            Validate.notBlank(type, "column.type can't be blank");
            
            DataType dataType = StarRocksTypeParser.parse(type);
            schemaBuilder.column(name, dataType);
        });
        
        //表配置参数
        if (!options.isEmpty()) {
            schemaBuilder.options(options);
        }
        Schema schema = schemaBuilder.build();
        String databaseName = PaimonConfigUtil.database(originalConfig);
        String tableName = originalConfig.getString(ConfigKey.TABLE);
        Identifier identifier = Identifier.create(databaseName, tableName);
        try {
            catalog.getDatabase(databaseName);
        } catch (Catalog.DatabaseNotExistException e) {
            try {
                catalog.createDatabase(databaseName, true);
            } catch (Catalog.DatabaseAlreadyExistException ex) {
                LOG.error("database " + databaseName + " is already exist error: " + ex.getMessage());
                throw DataXException.asDataXException(
                        String.format("database已存在: '%s'", databaseName));
            }
        }
        try {
            catalog.createTable(identifier, schema, true);
        } catch (Catalog.TableAlreadyExistException e) {
            LOG.error("table " + tableName + " is already exist error: " + e.getMessage());
            throw DataXException.asDataXException(
                    String.format("table已经存在: '%s'", tableName));
        } catch (Catalog.DatabaseNotExistException e) {
            LOG.error("no database " + databaseName + " error: " + e.getMessage());
            throw DataXException.asDataXException(
                    String.format("database不存在: '%s'", databaseName));
        }
    }
    
    private static Map<String, String> tableOptions(Configuration originalConfig) {
        Map<String, Object> rawOptions = originalConfig.getMap(ConfigKey.OPTIONS);
        Map<String, String> options = new java.util.HashMap<>(2);
        if (rawOptions == null) {
            return options;
        }
        for (Map.Entry<String, Object> entry : rawOptions.entrySet()) {
            if (entry.getValue() != null && isTableOption(entry.getKey())) {
                options.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return options;
    }
    
    private static boolean isTableOption(String key) {
        return StringUtils.isNotBlank(key)
                && !NON_TABLE_OPTION_KEYS.contains(key)
                && !key.startsWith("hadoop.")
                && !key.startsWith("fs.")
                && !key.startsWith("s3.");
    }
    
    private static String[] splitKeys(String keys) {
        return java.util.Arrays.stream(keys.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }
}
