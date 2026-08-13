package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.exception.DataXException;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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

    @Test
    public void testTimestampWithoutTimeZonePreservesShanghaiWallClock() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));
            java.sql.Timestamp mysqlDateTime = java.sql.Timestamp.valueOf("2026-08-03 03:07:31");

            org.apache.paimon.data.Timestamp value = (org.apache.paimon.data.Timestamp)
                    PaimonRecordConverter.parseValue(
                            new DateColumn(mysqlDateTime), DataTypes.TIMESTAMP(6));

            Assert.assertEquals(
                    LocalDateTime.of(2026, 8, 3, 3, 7, 31),
                    value.toLocalDateTime());
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    public void testTimestampWithLocalTimeZonePreservesInstant() {
        Instant instant = Instant.parse("2026-08-02T19:07:31Z");

        org.apache.paimon.data.Timestamp value = (org.apache.paimon.data.Timestamp)
                PaimonRecordConverter.parseValue(
                        new DateColumn(instant.toEpochMilli()),
                        DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(6));

        Assert.assertEquals(instant, value.toInstant());
    }

    @Test
    public void testTimestampWithLocalTimeZoneRequiresExplicitOffsetForText() {
        org.apache.paimon.data.Timestamp value = (org.apache.paimon.data.Timestamp)
                PaimonRecordConverter.parseValue(
                        new StringColumn("2026-08-03T03:07:31+08:00"),
                        DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(6));

        Assert.assertEquals(Instant.parse("2026-08-02T19:07:31Z"), value.toInstant());

        Assert.assertThrows(
                DateTimeParseException.class,
                () -> PaimonRecordConverter.parseValue(
                        new StringColumn("2026-08-03 03:07:31"),
                        DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(6)));
    }

    @Test(expected = DataXException.class)
    public void testTimestampAndTimestampLtzAreNotCompatible() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(6)},
                new String[]{"created_at"});

        new PaimonRecordConverter(
                Arrays.asList(new PaimonColumn("created_at", "timestamp(6)")),
                rowType,
                RowKind.INSERT);
    }
    
    @Test
    public void testProxyPrimaryKeyUsesEmptyStringAndConfiguredAlgorithm() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.STRING(),
                        DataTypes.STRING(),
                        DataTypes.INT()
                },
                new String[]{"_id_", "name", "age"});
        List<PaimonColumn> columns = Arrays.asList(
                new PaimonColumn("name", "string"),
                new PaimonColumn("age", "int"));
        ProxyPrimaryKeyConfig proxyConfig = new ProxyPrimaryKeyConfig(
                PrimaryKeyMode.PROXY,
                ProxyPrimaryKeyAlgorithm.SHA_256,
                Arrays.asList("name", "age"));
        PaimonRecordConverter converter = new PaimonRecordConverter(
                columns, rowType, RowKind.UPDATE_AFTER, null, proxyConfig);
        
        DefaultRecord record = new DefaultRecord();
        record.setColumn(0, new StringColumn(""));
        record.setColumn(1, new LongColumn(18));
        
        GenericRow row = converter.convert(record);
        Assert.assertEquals(
                ProxyPrimaryKeyGenerator.generate("_18", ProxyPrimaryKeyAlgorithm.SHA_256),
                row.getString(0).toString());
        Assert.assertEquals("", row.getString(1).toString());
        Assert.assertEquals(18, row.getInt(2));
    }
}
