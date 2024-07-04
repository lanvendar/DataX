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


import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.paimon.data.BinaryRow;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.sink.BatchTableCommit;
import org.apache.paimon.table.sink.BatchTableWrite;
import org.apache.paimon.table.sink.BatchWriteBuilder;
import org.apache.paimon.table.sink.CommitMessage;
import org.apache.paimon.types.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Paimon表的写入插件，暂时仅支持非分区表且bucket=1的表，所以表不宜太大
 * @author Li Zhengkai
 * @version 3.0, 2024/7/3
 * @since 2024/7/3
 */
public class PaimonWriter extends Writer {
    
    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaimonWriter.class);
    
    public static class Job extends Writer.Job {
        
        private Configuration originalConfig = null;
        
        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> splitConfigs = new ArrayList<>();
            splitConfigs.add(originalConfig);
            return splitConfigs;
        }
        
        @Override
        public void init() {
            originalConfig = getPluginJobConf();
        }
        
        @Override
        public void destroy() {
        
        }
        
        @Override
        public void prepare() {
            super.prepare();
            
        }
        
        @Override
        public void post() {
            super.post();
        }
        
        
        public static class Task extends Writer.Task {
            
            private Configuration taskConfig;
            
            private Table table;
            
            private List<Pair<String, String>> columns;
            
            private List<String> columnTypes;
            
            @Override
            public void startWrite(RecordReceiver lineReceiver) {
                try {
                    int columnSize = columns.size();
                    BatchWriteBuilder writeBuilder = table.newBatchWriteBuilder().withOverwrite();
                    BatchTableWrite write = writeBuilder.newWrite();
                    Record record;
                    while ((record = lineReceiver.getFromReader()) != null) {
                        if (record.getColumnNumber() != columnSize) {
                            // 源头读取字段列数与目的表字段写入列数不相等，直接报错
                            throw DataXException
                                    .asDataXException(
                                        String.format(
                                            "列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 目的表要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
                                            record.getColumnNumber(),
                                            columnSize));
                        }
                        GenericRow writeRecord = new GenericRow(columnSize);
                        
                        for (int i = 0; i < columnSize; i++) {
                            Column col = record.getColumn(i);
                            writeRecord.setField(i, parseValue(col, columnTypes.get(i)));
                        }
                        
                        write.write(writeRecord);
                    }
                    
                    try {
                        write.compact(BinaryRow.EMPTY_ROW, 0, true);
                    } catch (Exception e) {
                        LOG.error("compaction error", e);
                        throw e;
                    }
                    
                    List<CommitMessage> messages = null;
                    BatchTableCommit commit = null;
                    try {
                        messages = write.prepareCommit();
                        // 3. Collect all CommitMessages to a global node and commit
                        commit = writeBuilder.newCommit();
                        commit.commit(messages);
                        
                    } catch (Exception e) {
                        LOG.error("commit paimon表失败", e);
                        if (commit != null) {
                            commit.abort(messages);
                        }
                        throw e;
                    } finally {
                        if (commit != null) {
                            commit.close();
                        }
                    }
                } catch (Exception e) {
                    LOG.error("写入Paimon表失败", e);
                    throw DataXException.asDataXException(e.getMessage());
                }
            }
            
            private static Object parseValue(Column col, String type) {
                switch (type) {
                    case "BOOLEAN":
                        return col.asBoolean();
                    case "BYTEA":
                        return col.asBytes();
                    case "SMALLINT":
                        return col.asLong();
                    case "INT":
                        return col.asLong();
                    case "BIGINT":
                        return col.asLong();
                    case "FLOAT":
                        return col.asBigDecimal().doubleValue();
                    case "DOUBLE":
                        return col.asBigDecimal().doubleValue();
                    case "DECIMAL":
                        return col.asBigDecimal().doubleValue();
                    case "TIMESTAMP":
                        return col.asDate();
                    case "DATE":
                        return col.asDate();
                    default:
                        return DataTypes.STRING();
                }
            }
            
            @Override
            public void init() {
                List<Configuration> columnConfigs = taskConfig.getListConfiguration(Key.COLUMN);
                columns = new ArrayList<>();
                columnTypes = new ArrayList<>();
                columnConfigs.forEach(column -> {
                    String name = column.getString("name");
                    String type = column.getString("type");
                    
                    columns.add(Pair.of(name, type));
                    columnTypes.add(type);
                });
            }
            
            @Override
            public void post() {
                super.post();
            }
            
            @Override
            public void prepare() {
                super.prepare();
                table = PaimonHelper.getPaimonTable(taskConfig);
                
            }
            
            @Override
            public void destroy() {
            
            }
        }
    }
}
