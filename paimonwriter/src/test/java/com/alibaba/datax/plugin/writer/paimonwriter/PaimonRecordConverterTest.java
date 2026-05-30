package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.core.transport.record.DefaultRecord;
import org.apache.paimon.data.Decimal;
import org.apache.paimon.data.GenericArray;
import org.apache.paimon.data.GenericMap;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.types.DataTypes;
import org.apache.paimon.types.RowKind;
import org.apache.paimon.types.RowType;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaimonRecordConverterTest {
    
    @Test
    public void testPartialColumnWithOverwritePartitionDefault() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.STRING(),
                        DataTypes.INT(),
                        DataTypes.STRING()
                },
                new String[]{"name", "age", "dt"});
        List<PaimonColumn> columns = Arrays.asList(
                new PaimonColumn("name", "string"),
                new PaimonColumn("age", "int"));
        Map<String, String> defaults = new HashMap<>();
        defaults.put("dt", "20260530");
        PaimonRecordConverter converter = new PaimonRecordConverter(columns, rowType, RowKind.INSERT, defaults);
        
        DefaultRecord record = new DefaultRecord();
        record.setColumn(0, new StringColumn("alice"));
        record.setColumn(1, new LongColumn(18));
        
        GenericRow row = converter.convert(record);
        Assert.assertEquals("alice", row.getString(0).toString());
        Assert.assertEquals(18, row.getInt(1));
        Assert.assertEquals("20260530", row.getString(2).toString());
    }
    
    @Test
    public void testDecimalUsesTablePrecisionScale() {
        Object value = PaimonRecordConverter.parseValue(
                new StringColumn("12.34"), DataTypes.DECIMAL(18, 2));
        
        Decimal decimal = (Decimal) value;
        Assert.assertEquals(new BigDecimal("12.34"), decimal.toBigDecimal());
        Assert.assertEquals(18, decimal.precision());
        Assert.assertEquals(2, decimal.scale());
    }
    
    @Test
    public void testArrayAndMapJsonConversion() {
        Object array = PaimonRecordConverter.parseValue(
                new StringColumn("[1,2,3]"), DataTypes.ARRAY(DataTypes.INT()));
        Object map = PaimonRecordConverter.parseValue(
                new StringColumn("{\"a\":1,\"b\":2}"), DataTypes.MAP(DataTypes.STRING(), DataTypes.INT()));
        
        Assert.assertEquals(3, ((GenericArray) array).size());
        Assert.assertEquals(2, ((GenericMap) map).size());
    }
    
    @Test
    public void testRowJsonConversion() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{DataTypes.INT(), DataTypes.STRING()},
                new String[]{"id", "name"});
        
        Object value = PaimonRecordConverter.parseValue(
                new StringColumn("{\"id\":7,\"name\":\"bob\"}"), rowType);
        
        GenericRow row = (GenericRow) value;
        Assert.assertEquals(7, row.getInt(0));
        Assert.assertEquals("bob", row.getString(1).toString());
    }
}
