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
import org.apache.commons.lang3.Validate;
import org.apache.paimon.CoreOptions;
import org.apache.paimon.catalog.Catalog;
import org.apache.paimon.catalog.CatalogContext;
import org.apache.paimon.catalog.CatalogFactory;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.fs.Path;
import org.apache.paimon.hive.HiveCatalogOptions;
import org.apache.paimon.options.CatalogOptions;
import org.apache.paimon.options.Options;
import org.apache.paimon.schema.Schema;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    /**
     * 创建 paimon catalog.
     *
     * @return catalog对象.
     */
    static Catalog createPaimonCatalog(Configuration originalConfig) {
        Options options = new Options();
        // warehouse: root path of catalog
        if (null != originalConfig.getString(Key.WAREHOUSE)) {
            String warehouse = originalConfig.getString(Key.WAREHOUSE);
            options.set(CatalogOptions.WAREHOUSE.key(), new Path(warehouse).toUri().toString());
        }
        //metastore: Metastore of paimon catalog, supports filesystem and hive, default is filesystem
        if (null != originalConfig.getString(Key.METASTORE)) {
            options.set(CatalogOptions.METASTORE.key(), originalConfig.getString(Key.METASTORE));
        } else {
            options.set(CatalogOptions.METASTORE.key(), "hive");
        }
        if (null != originalConfig.getString(Key.URI)) {
            options.set(CatalogOptions.URI.key(), originalConfig.getString(Key.URI));
        }
        if (null != originalConfig.getString(Key.TABLE_TYPE)) {
            options.set(CatalogOptions.TABLE_TYPE.key(),
                    originalConfig.getString(Key.TABLE_TYPE));
        }
        if (null != originalConfig.getString(Key.HIVE_CONF_DIR)) {
            options.set(HiveCatalogOptions.HIVE_CONF_DIR.key(),
                    originalConfig.getString(Key.HIVE_CONF_DIR));
        }
        if (null != originalConfig.getString(Key.HADOOP_CONF_DIR)) {
            options.set(HiveCatalogOptions.HADOOP_CONF_DIR.key(),
                    originalConfig.getString(Key.HADOOP_CONF_DIR));
        }
        
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
        String databaseName = originalConfig.getString(Key.DATABASE);
        String tableName = originalConfig.getString(Key.TABLE);
        Identifier identifier = Identifier.create(databaseName, tableName);
        Table table = null;
        try {
            if (!paimonCatalog.tableExists(identifier)) {
                createPaimonTable(paimonCatalog, originalConfig);
            }
            table = paimonCatalog.getTable(identifier);
        } catch (org.apache.paimon.catalog.Catalog.TableNotExistException e) {
            LOG.error("table {} not exist, create it", tableName);
            throw DataXException.asDataXException(
                    String.format("表不存在: '%s'", tableName));
        }
        return table;
    }
    
    /**
     * 获取 paimon 建表语句.
     *
     * @param catalog Paimon Catalog
     */
    static void createPaimonTable(Catalog catalog, Configuration originalConfig) {
        Map<String, String> options = originalConfig.getMap(Key.PAIMON_CONF, String.class);
        if (options == null) {
            options = new HashMap<>(2);
        }
        
        Schema.Builder schemaBuilder = Schema.newBuilder();
        //建表主键
        if (null != originalConfig.getString(Key.PRIMARY_KEY)) {
            schemaBuilder.primaryKey(originalConfig.getString(Key.PRIMARY_KEY));
        }
        //建表分区
        if (null != originalConfig.getString(Key.PARTITION_KEY)) {
            schemaBuilder.partitionKeys(originalConfig.getString(Key.PARTITION_KEY));
            options.put(CoreOptions.METASTORE_PARTITIONED_TABLE.key(), "true");
        }
        //建表字段
        List<Configuration> columns = originalConfig.getListConfiguration(Key.COLUMN);
        columns.forEach(column -> {
            String name = column.getString("name");
            Validate.notNull(name, "column.name can't be null");
            
            String type = column.getString("type");
            Validate.notNull(type, "column.type can't be null");
            
            DataType dataType = typeMapping(type);
            schemaBuilder.column(name, dataType);
        });
        
        //表配置参数
        if (!options.isEmpty()) {
            schemaBuilder.options(options);
        }
        Schema schema = schemaBuilder.build();
        String databaseName = originalConfig.getString(Key.DATABASE);
        String tableName = originalConfig.getString(Key.TABLE);
        Identifier identifier = Identifier.create(databaseName, tableName);
        
        if (!catalog.databaseExists(databaseName)) {
            try {
                catalog.createDatabase(databaseName, true);
            } catch (Catalog.DatabaseAlreadyExistException e) {
                LOG.error("database " + databaseName + " is already exist error: " + e.getMessage());
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
    
    /**
     * 字段类型映射方法.
     *
     * @param columnType 字段类型字符串
     * @return paimon的字段类型.
     * 复杂类型需要支持，如：ARRAY、MAP、STRUCT等,目前暂不支持.
     */
    private static DataType typeMapping(String columnType) {
        columnType = columnType.toUpperCase();
        switch (columnType) {
            case "BOOLEAN":
                return DataTypes.BOOLEAN();
            case "BYTEA":
                return DataTypes.BYTES();
            case "SMALLINT":
                return DataTypes.SMALLINT();
            case "INT":
                return DataTypes.INT();
            case "BIGINT":
                return DataTypes.BIGINT();
            case "FLOAT":
                return DataTypes.FLOAT();
            case "DOUBLE":
                return DataTypes.DOUBLE();
            case "DECIMAL":
                return DataTypes.DECIMAL(38, 8);
            case "TIMESTAMP":
                return DataTypes.TIMESTAMP();
            case "DATE":
                return DataTypes.DATE();
            default:
                return DataTypes.STRING();
        }
    }
}
