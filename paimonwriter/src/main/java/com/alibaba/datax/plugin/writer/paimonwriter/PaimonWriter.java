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


import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.table.Table;
import org.apache.paimon.types.RowKind;
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
            System.setProperty("HADOOP_USER_NAME", "root");
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
        
    }
        
    public static class Task extends Writer.Task {
        
        private Configuration taskConfig;
        
        private Table table;
        
        private PaimonWriteConfig writeConfig;
        
        private PaimonRecordConverter recordConverter;
        
        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            PaimonBatchWriter writer = null;
            try {
                writer = new PaimonBatchWriter(table, writeConfig);
                Record record;
                TaskPluginCollector collector = super.getTaskPluginCollector();
                while ((record = lineReceiver.getFromReader()) != null) {
                    try {
                        GenericRow writeRecord = recordConverter.convert(record);
                        writer.write(writeRecord);
                    } catch (Exception e) {
                        LOG.warn("PaimonWriter转换脏数据失败: {}", e.getMessage());
                        collector.collectDirtyRecord(record, e, e.getMessage());
                    }
                }
                writer.finish();
                writer = null;
            } catch (Exception e) {
                LOG.error("写入Paimon表失败", e);
                throw DataXException.asDataXException(e.getMessage());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
        
        @Override
        public void init() {
            System.setProperty("HADOOP_USER_NAME", "root");
            this.taskConfig = super.getPluginJobConf();
            writeConfig = PaimonWriteConfig.from(taskConfig);
        }
        
        @Override
        public void post() {
            super.post();
        }
        
        @Override
        public void prepare() {
            super.prepare();
            try {
                table = PaimonHelper.getPaimonTable(taskConfig);
                PaimonTableValidator.validate(table, writeConfig);
                recordConverter = new PaimonRecordConverter(
                        writeConfig.getColumns(), table.rowType(), rowKind(), writeConfig.getOverwritePartition());
            } catch (Exception e) {
                LOG.error("获取Paimon表失败", e);
                throw e;
            }
            
        }
        
        @Override
        public void destroy() {
        
        }
        
        private RowKind rowKind() {
            return writeConfig.getLoadMode() == LoadMode.UPSERT ? RowKind.UPDATE_AFTER : RowKind.INSERT;
        }
    }
}
