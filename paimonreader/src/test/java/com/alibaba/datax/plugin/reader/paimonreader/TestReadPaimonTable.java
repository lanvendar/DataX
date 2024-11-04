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
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.reader.RecordReader;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.source.ReadBuilder;
import org.apache.paimon.table.source.Split;
import org.apache.paimon.table.source.TableRead;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 读取Paimon Table.
 * @author Li Zhengkai
 * @version 3.0, 2024/11/4
 * @since 2024/11/4
 */
public class TestReadPaimonTable {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestReadPaimonTable.class);
    
    public static void main(String[] args) {
        
        System.setProperty("HADOOP_USER_NAME", "root");
        Configuration conf = Configuration.from(TestPaimonReader.class.getResourceAsStream("/plugin2.json"));
        try {
            Table table = PaimonHelper.getPaimonTable(conf);
            
            ReadBuilder readBuilder = table.newReadBuilder();
            
            List<Split> splits = readBuilder.newScan().plan().splits();
            
            TableRead read = readBuilder.newRead();
            
            try (RecordReader<InternalRow> reader = read.createReader(splits)) {
                //final int[] finalProjection = projection;
                reader.forEachRemaining(r -> {
                    LOG.info("读取到记录:{}", convertRecord(r, table.rowType()));
                    //LOG.info("读取到记录:{}", convertRecord2(r, table.rowType()));
                });
            } catch (Exception e) {
                LOG.error("读取Paimon表失败", e);
                throw DataXException.asDataXException("读取Paimon表失败");
            }
        } catch (Exception e) {
            LOG.error("获取Paimon表失败", e);
        }
    }
    
    static String convertRecord2(InternalRow r, RowType rowType) {
        int fieldCount = rowType.getFieldCount();
        List<Object> record = new ArrayList<>();
        for (int i = 0; i < fieldCount; i++) {
            try {
                DataType dataType = rowType.getTypeAt(i);
                
                if (dataType instanceof BooleanType) {
                    record.add(new BoolColumn(r.getBoolean(i)));
                } else if (dataType instanceof IntType) {
                    record.add(new LongColumn(r.getInt(i)));
                } else if (dataType instanceof BigIntType) {
                    record.add(new LongColumn(r.getLong(i)));
                } else if (dataType instanceof SmallIntType) {
                    record.add(new LongColumn(r.getShort(i) + ""));
                } else if (dataType instanceof FloatType) {
                    record.add(new DoubleColumn(r.getFloat(i)));
                } else if (dataType instanceof DoubleType) {
                    record.add(new DoubleColumn(r.getDouble(i)));
                } else if (dataType instanceof VarCharType) {
                    record.add(new StringColumn(r.getString(i).toString()));
                } else if (dataType instanceof DecimalType) {
                    record.add(new DoubleColumn(r.getDouble(i)));
                } else if (dataType instanceof TimestampType) {
                    record.add(new DateColumn(r.getTimestamp(i, 0).toSQLTimestamp()));
                } else {
                    record.add(new StringColumn(new String(r.getBinary(i))));
                }
            } catch (Exception e) {
                LOG.error("转换数据类型失败", e);
            }
        }
        return record.toString();
    }
    
    static String convertRecord(InternalRow r, RowType rowType) {
        int fieldCount = rowType.getFieldCount();
        List<Object> record = new ArrayList<>();
        for (int i = 0; i < fieldCount; i++) {
            if (r.isNullAt(i)) {
                record.add(null);
                continue;
            }
            DataType dataType = rowType.getTypeAt(i);
            
            if (dataType instanceof BooleanType) {
                record.add(r.getBoolean(i));
            } else if (dataType instanceof IntType) {
                record.add(r.getInt(i));
            } else if (dataType instanceof BigIntType) {
                record.add(r.getLong(i));
            } else if (dataType instanceof SmallIntType) {
                record.add(r.getShort(i));
            } else if (dataType instanceof FloatType) {
                record.add(r.getFloat(i));
            } else if (dataType instanceof DoubleType) {
                record.add(r.getDouble(i));
            } else if (dataType instanceof VarCharType) {
                record.add(r.getString(i).toString());
            } else if (dataType instanceof DecimalType) {
                record.add(r.getDouble(i));
            } else if (dataType instanceof TimestampType) {
                record.add(r.getTimestamp(i, 0).toSQLTimestamp());
            } else {
                record.add(new String(r.getBinary(i)));
            }
        }
        return record.toString();
    }
}


