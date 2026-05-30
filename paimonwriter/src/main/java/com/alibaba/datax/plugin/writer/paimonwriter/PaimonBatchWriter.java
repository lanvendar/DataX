package com.alibaba.datax.plugin.writer.paimonwriter;

import org.apache.paimon.data.BinaryRow;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.sink.BatchTableCommit;
import org.apache.paimon.table.sink.BatchTableWrite;
import org.apache.paimon.table.sink.BatchWriteBuilder;
import org.apache.paimon.table.sink.CommitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class PaimonBatchWriter {
    
    private static final Logger LOG = LoggerFactory.getLogger(PaimonBatchWriter.class);
    
    private final Table table;
    
    private final PaimonWriteConfig writeConfig;
    
    private BatchWriteBuilder writeBuilder;
    
    private BatchTableWrite write;
    
    private int batchRecordCount;
    
    private boolean hasRecord;
    
    PaimonBatchWriter(Table table, PaimonWriteConfig writeConfig) {
        this.table = table;
        this.writeConfig = writeConfig;
        resetWriter();
    }
    
    void write(GenericRow row) throws Exception {
        write.write(row);
        hasRecord = true;
        batchRecordCount++;
        if (shouldFlush()) {
            commitCurrentBatch();
            resetWriter();
        }
    }
    
    void finish() throws Exception {
        if (hasRecord && batchRecordCount > 0) {
            commitCurrentBatch();
            write = null;
        }
    }
    
    void close() {
        closeQuietly(write);
    }
    
    private boolean shouldFlush() {
        return writeConfig.getLoadMode() != LoadMode.OVERWRITE_PARTITION
                && batchRecordCount >= writeConfig.getBatchSize();
    }
    
    private void resetWriter() {
        writeBuilder = table.newBatchWriteBuilder();
        if (writeConfig.getLoadMode() == LoadMode.OVERWRITE_PARTITION) {
            writeBuilder = writeBuilder.withOverwrite(writeConfig.getOverwritePartition());
        }
        write = writeBuilder.newWrite();
        batchRecordCount = 0;
    }
    
    private void commitCurrentBatch() throws Exception {
        try {
            write.compact(BinaryRow.EMPTY_ROW, 0, true);
        } catch (Exception e) {
            LOG.warn("compact paimon表失败，继续提交当前批次", e);
        }
        
        List<CommitMessage> messages = null;
        BatchTableCommit commit = null;
        try {
            messages = write.prepareCommit();
            commit = writeBuilder.newCommit();
            commit.commit(messages);
        } catch (Exception e) {
            LOG.error("commit paimon表失败", e);
            if (commit != null) {
                commit.abort(messages);
            }
            throw e;
        } finally {
            closeQuietly(write);
            write = null;
            if (commit != null) {
                commit.close();
            }
        }
    }
    
    private static void closeQuietly(BatchTableWrite write) {
        if (write != null) {
            try {
                write.close();
            } catch (Exception e) {
                LOG.warn("关闭Paimon write失败", e);
            }
        }
    }
}
