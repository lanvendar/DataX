package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.core.transport.record.DefaultRecord;
import org.apache.paimon.data.Decimal;
import org.apache.paimon.data.GenericArray;
import org.apache.paimon.data.GenericMap;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.types.DataTypes;
import org.apache.paimon.types.RowType;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PaimonRecordConverterTest {
    
    @Test
    public void testProjectionAndDecimalConversion() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.STRING(),
                        DataTypes.DECIMAL(18, 2),
                        DataTypes.INT()
                },
                new String[]{"name", "amount", "age"});
        GenericRow row = new GenericRow(2);
        row.setField(0, org.apache.paimon.data.BinaryString.fromString("alice"));
        row.setField(1, Decimal.fromBigDecimal(new BigDecimal("12.34"), 18, 2));
        
        PaimonRecordConverter converter = new PaimonRecordConverter(rowType, new int[]{0, 1});
        Record record = converter.convert(new MockRecordSender(), row);
        
        Assert.assertEquals(2, record.getColumnNumber());
        Assert.assertEquals("alice", record.getColumn(0).asString());
        Assert.assertEquals("12.34", record.getColumn(1).asString());
    }
    
    @Test
    public void testComplexTypesToJson() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.ARRAY(DataTypes.INT()),
                        DataTypes.MAP(DataTypes.STRING(), DataTypes.INT())
                },
                new String[]{"ids", "attrs"});
        Map<Object, Object> attrs = new HashMap<>();
        attrs.put(org.apache.paimon.data.BinaryString.fromString("a"), 1);
        GenericRow row = new GenericRow(2);
        row.setField(0, new GenericArray(new Object[]{1, 2}));
        row.setField(1, new GenericMap(attrs));
        
        PaimonRecordConverter converter = new PaimonRecordConverter(rowType, null);
        Record record = converter.convert(new MockRecordSender(), row);
        
        Assert.assertEquals("[1,2]", record.getColumn(0).asString());
        Assert.assertEquals("{\"a\":1}", record.getColumn(1).asString());
    }
    
    @Test
    public void testDateAndTimeConversion() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.DATE(),
                        DataTypes.TIME()
                },
                new String[]{"dt", "tm"});
        GenericRow row = new GenericRow(2);
        row.setField(0, (int) java.time.LocalDate.of(2026, 5, 30).toEpochDay());
        row.setField(1, 3_723_000);
        
        PaimonRecordConverter converter = new PaimonRecordConverter(rowType, null);
        Record record = converter.convert(new MockRecordSender(), row);
        
        Assert.assertEquals(
                java.util.concurrent.TimeUnit.DAYS.toMillis(java.time.LocalDate.of(2026, 5, 30).toEpochDay()),
                record.getColumn(0).asLong().longValue());
        Assert.assertEquals(3_723_000L, record.getColumn(1).asLong().longValue());
    }
    
    private static class MockRecordSender implements RecordSender {
        
        @Override
        public Record createRecord() {
            return new DefaultRecord();
        }
        
        @Override
        public void sendToWriter(Record record) {
        }
        
        @Override
        public void flush() {
        }
        
        @Override
        public void terminate() {
        }
        
        @Override
        public void shutdown() {
        }
    }
}
