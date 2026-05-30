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

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.reader.RecordReader;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.source.ReadBuilder;
import org.apache.paimon.table.source.Split;
import org.apache.paimon.table.source.TableRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Paimon表的读取插件
 * @author Li Zhengkai
 * @version 3.0, 2024/7/15
 * @since 2024/7/15
 */
public class PaimonReader extends Reader {
    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaimonReader.class);
    
    public static class Job extends Reader.Job {
        
        private Configuration originalConfig = null;
        
        @Override
        public List<Configuration> split(int adviceNumber) {
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
    }
    
    public static class Task extends Reader.Task {
        
        private Configuration taskConfig;
        
        private Table table;
        
        private PaimonReadConfig readConfig;
        
        @Override
        public void startRead(RecordSender recordSender) {
            ReadBuilder readBuilder = table.newReadBuilder();
            PaimonReadPlan readPlan = new PaimonSqlPlanner(table.rowType()).plan(readConfig.getSql());
            if (readPlan.getProjection() != null) {
                readBuilder.withProjection(readPlan.getProjection());
            }
            if (!readPlan.getPredicates().isEmpty()) {
                readBuilder.withFilter(readPlan.getPredicates());
            }
            
            List<Split> splits = readBuilder.newScan().plan().splits();
            TableRead read = readBuilder.newRead();
            PaimonRecordConverter converter = new PaimonRecordConverter(table.rowType(), readPlan.getProjection());
            
            try (RecordReader<InternalRow> reader = read.createReader(splits)) {
                reader.forEachRemaining(r -> {
                    recordSender.sendToWriter(converter.convert(recordSender, r));
                });
                recordSender.flush();
            } catch (Exception e) {
                LOG.error("读取Paimon表失败", e);
                throw DataXException.asDataXException("读取Paimon表失败: " + e.getMessage());
            }
        }
        
        @Override
        public void init() {
            System.setProperty("HADOOP_USER_NAME", "root");
            this.taskConfig = super.getPluginJobConf();
            this.readConfig = PaimonReadConfig.from(taskConfig);
        }
        
        @Override
        public void destroy() {
        
        }
        
        @Override
        public void prepare() {
            super.prepare();
            try {
                table = PaimonHelper.getPaimonTable(taskConfig);
            } catch (Exception e) {
                LOG.error("获取Paimon表失败", e);
                throw e;
            }
        }
    }
}
