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

package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.catalog.Catalog;
import org.apache.paimon.catalog.CatalogContext;
import org.apache.paimon.catalog.CatalogFactory;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.fs.Path;
import org.apache.paimon.hive.HiveCatalogOptions;
import org.apache.paimon.options.CatalogOptions;
import org.apache.paimon.options.Options;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.BigIntType;
import org.apache.paimon.types.BooleanType;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.DoubleType;
import org.apache.paimon.types.FloatType;
import org.apache.paimon.types.IntType;
import org.apache.paimon.types.RowType;
import org.apache.paimon.types.SmallIntType;
import org.apache.paimon.types.TimestampType;
import org.apache.paimon.types.VarCharType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            table = paimonCatalog.getTable(identifier);
        } catch (Catalog.TableNotExistException e) {
            LOG.error("table {} not exist, create it", tableName);
            throw DataXException.asDataXException(
                    String.format("表不存在: '%s'", tableName));
        }
        return table;
    }
    
    static Record convertRecord(RecordSender recordSender, InternalRow r, RowType rowType, int[] projection) {
        Record record = recordSender.createRecord();
        
        int fieldCount = projection == null ? rowType.getFieldCount() : projection.length;
        
        for (int i = 0; i < fieldCount; i++) {
            DataType dataType = projection == null ? rowType.getTypeAt(i) : rowType.getTypeAt(projection[i]);
            
            if (dataType instanceof BooleanType) {
                record.addColumn(new BoolColumn(r.isNullAt(i) ? null : r.getBoolean(i)));
            } else if (dataType instanceof IntType) {
                record.addColumn(new LongColumn(r.isNullAt(i) ? null : r.getInt(i)));
            } else if (dataType instanceof BigIntType) {
                record.addColumn(new LongColumn(r.isNullAt(i) ? null : r.getLong(i)));
            } else if (dataType instanceof SmallIntType) {
                record.addColumn(new LongColumn(r.isNullAt(i) ? null : r.getShort(i) + ""));
            } else if (dataType instanceof FloatType) {
                record.addColumn(new DoubleColumn(r.isNullAt(i) ? null : r.getFloat(i)));
            } else if (dataType instanceof DoubleType) {
                record.addColumn(new DoubleColumn(r.isNullAt(i) ? null : r.getDouble(i)));
            } else if (dataType instanceof VarCharType) {
                record.addColumn(new StringColumn(r.isNullAt(i) ? null : r.getString(i).toString()));
            } else if (dataType instanceof DecimalType) {
                record.addColumn(new DoubleColumn(r.isNullAt(i) ? null : r.getDouble(i)));
            } else if (dataType instanceof TimestampType) {
                record.addColumn(new DateColumn(r.isNullAt(i) ? null : r.getTimestamp(i, 0).toSQLTimestamp()));
            } else {
                record.addColumn(new StringColumn(r.isNullAt(i) ? null : new String(r.getBinary(i))));
            }
        }
        
        return record;
    }
}
