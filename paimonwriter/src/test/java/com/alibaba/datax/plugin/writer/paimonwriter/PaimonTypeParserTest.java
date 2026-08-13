package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.paimon.types.DataTypeRoot;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.RowType;
import org.junit.Assert;
import org.junit.Test;

public class PaimonTypeParserTest {
    
    @Test
    public void testDecimalDefault() {
        DecimalType decimalType = (DecimalType) PaimonTypeParser.parse("decimal");
        
        Assert.assertEquals(10, decimalType.getPrecision());
        Assert.assertEquals(0, decimalType.getScale());
    }
    
    @Test
    public void testTimestampWithLocalTimeZoneMapping() {
        Assert.assertEquals(
                DataTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE,
                PaimonTypeParser.parse("timestamp(6) with local time zone").getTypeRoot());
    }

    @Test
    public void testRejectsNonPaimonTimestampAliases() {
        Assert.assertThrows(DataXException.class, () -> PaimonTypeParser.parse("datetime(6)"));
        Assert.assertThrows(DataXException.class, () -> PaimonTypeParser.parse("timestamp_ltz(6)"));
    }
    
    @Test
    public void testComplexRow() {
        RowType rowType = (RowType) PaimonTypeParser.parse(
                "row<id int, tags array<string>, attrs map<string,decimal(18,2)>>");
        
        Assert.assertEquals(3, rowType.getFieldCount());
        Assert.assertEquals("id", rowType.getFieldNames().get(0));
        Assert.assertEquals(DataTypeRoot.INTEGER, rowType.getTypeAt(0).getTypeRoot());
        Assert.assertEquals(DataTypeRoot.ARRAY, rowType.getTypeAt(1).getTypeRoot());
        Assert.assertEquals(DataTypeRoot.MAP, rowType.getTypeAt(2).getTypeRoot());
    }
}
