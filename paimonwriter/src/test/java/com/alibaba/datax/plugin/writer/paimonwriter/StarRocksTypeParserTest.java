package com.alibaba.datax.plugin.writer.paimonwriter;

import org.apache.paimon.types.DataTypeRoot;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.RowType;
import org.junit.Assert;
import org.junit.Test;

public class StarRocksTypeParserTest {
    
    @Test
    public void testDecimalDefault() {
        DecimalType decimalType = (DecimalType) StarRocksTypeParser.parse("decimal");
        
        Assert.assertEquals(10, decimalType.getPrecision());
        Assert.assertEquals(0, decimalType.getScale());
    }
    
    @Test
    public void testLargeIntMapping() {
        DecimalType decimalType = (DecimalType) StarRocksTypeParser.parse("largeint");
        
        Assert.assertEquals(38, decimalType.getPrecision());
        Assert.assertEquals(0, decimalType.getScale());
    }
    
    @Test
    public void testComplexStruct() {
        RowType rowType = (RowType) StarRocksTypeParser.parse(
                "struct<id:int, tags:array<varchar>, attrs:map<varchar,decimal(18,2)>>");
        
        Assert.assertEquals(3, rowType.getFieldCount());
        Assert.assertEquals("id", rowType.getFieldNames().get(0));
        Assert.assertEquals(DataTypeRoot.INTEGER, rowType.getTypeAt(0).getTypeRoot());
        Assert.assertEquals(DataTypeRoot.ARRAY, rowType.getTypeAt(1).getTypeRoot());
        Assert.assertEquals(DataTypeRoot.MAP, rowType.getTypeAt(2).getTypeRoot());
    }
}
