package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.paimon.data.BinaryString;
import org.apache.paimon.data.Decimal;
import org.apache.paimon.data.GenericArray;
import org.apache.paimon.data.GenericMap;
import org.apache.paimon.data.GenericRow;
import org.apache.paimon.data.Timestamp;
import org.apache.paimon.types.ArrayType;
import org.apache.paimon.types.DataField;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DataTypeRoot;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.MapType;
import org.apache.paimon.types.RowKind;
import org.apache.paimon.types.RowType;

import java.math.BigDecimal;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class PaimonRecordConverter {
    
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    
    private final List<PaimonColumn> columns;
    
    private final RowType rowType;
    
    private final int[] fieldIndexes;
    
    private final DataType[] fieldTypes;
    
    private final RowKind rowKind;
    
    private final Map<String, String> defaultValues;
    
    PaimonRecordConverter(List<PaimonColumn> columns, RowType rowType, RowKind rowKind) {
        this(columns, rowType, rowKind, null);
    }
    
    PaimonRecordConverter(List<PaimonColumn> columns, RowType rowType, RowKind rowKind, Map<String, String> defaultValues) {
        this.columns = columns;
        this.rowType = rowType;
        this.fieldIndexes = new int[columns.size()];
        this.fieldTypes = new DataType[columns.size()];
        this.rowKind = rowKind;
        this.defaultValues = defaultValues;
        validateAndBuildMapping();
    }
    
    GenericRow convert(Record record) {
        if (record.getColumnNumber() != columns.size()) {
            throw DataXException.asDataXException(String.format(
                    "列配置信息有错误. 源头读取字段数:%s 与 writer.column 配置字段数:%s 不相等.",
                    record.getColumnNumber(), columns.size()));
        }
        
        GenericRow writeRecord = new GenericRow(rowKind, rowType.getFieldCount());
        fillDefaultValues(writeRecord);
        for (int i = 0; i < columns.size(); i++) {
            Column column = record.getColumn(i);
            writeRecord.setField(fieldIndexes[i], parseValue(column, fieldTypes[i]));
        }
        return writeRecord;
    }
    
    private void fillDefaultValues(GenericRow writeRecord) {
        if (defaultValues == null || defaultValues.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
            int index = rowType.getFieldIndex(entry.getKey());
            if (index >= 0) {
                writeRecord.setField(index, convertValue(entry.getValue(), null, rowType.getTypeAt(index)));
            }
        }
    }
    
    private void validateAndBuildMapping() {
        Map<String, Integer> tableFieldIndexes = new HashMap<>();
        List<String> tableFields = rowType.getFieldNames();
        for (int i = 0; i < tableFields.size(); i++) {
            tableFieldIndexes.put(tableFields.get(i), i);
        }
        
        for (int i = 0; i < columns.size(); i++) {
            PaimonColumn column = columns.get(i);
            Integer tableIndex = tableFieldIndexes.get(column.getName());
            if (tableIndex == null) {
                throw DataXException.asDataXException("column字段不存在于Paimon表: " + column.getName());
            }
            
            DataType declaredType = StarRocksTypeParser.parse(column.getType());
            DataType tableType = rowType.getTypeAt(tableIndex);
            if (!isCompatibleType(declaredType, tableType)) {
                throw DataXException.asDataXException(String.format(
                        "column字段类型与Paimon表不兼容. 字段:%s, 配置类型:%s, 表类型:%s",
                        column.getName(), column.getType(), tableType.asSQLString()));
            }
            fieldIndexes[i] = tableIndex;
            fieldTypes[i] = tableType;
        }
    }
    
    private static boolean isCompatibleType(DataType declaredType, DataType tableType) {
        if (isStringType(declaredType.getTypeRoot()) && isStringType(tableType.getTypeRoot())) {
            return true;
        }
        if (isBinaryType(declaredType.getTypeRoot()) && isBinaryType(tableType.getTypeRoot())) {
            return true;
        }
        if (declaredType.getTypeRoot() == DataTypeRoot.TIMESTAMP_WITHOUT_TIME_ZONE
                && tableType.getTypeRoot() == DataTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
            return true;
        }
        if (declaredType.getTypeRoot() != tableType.getTypeRoot()) {
            return false;
        }
        switch (declaredType.getTypeRoot()) {
            case ARRAY:
                return isCompatibleType(((ArrayType) declaredType).getElementType(), ((ArrayType) tableType).getElementType());
            case MAP:
                return isCompatibleType(((MapType) declaredType).getKeyType(), ((MapType) tableType).getKeyType())
                        && isCompatibleType(((MapType) declaredType).getValueType(), ((MapType) tableType).getValueType());
            case ROW:
                return isCompatibleRow((RowType) declaredType, (RowType) tableType);
            case DECIMAL:
                DecimalType declaredDecimal = (DecimalType) declaredType;
                DecimalType tableDecimal = (DecimalType) tableType;
                return declaredDecimal.getPrecision() == tableDecimal.getPrecision()
                        && declaredDecimal.getScale() == tableDecimal.getScale();
            default:
                return true;
        }
    }
    
    private static boolean isStringType(DataTypeRoot typeRoot) {
        return typeRoot == DataTypeRoot.CHAR || typeRoot == DataTypeRoot.VARCHAR;
    }
    
    private static boolean isBinaryType(DataTypeRoot typeRoot) {
        return typeRoot == DataTypeRoot.BINARY || typeRoot == DataTypeRoot.VARBINARY;
    }
    
    private static boolean isCompatibleRow(RowType declaredType, RowType tableType) {
        if (declaredType.getFieldCount() != tableType.getFieldCount()) {
            return false;
        }
        for (int i = 0; i < declaredType.getFieldCount(); i++) {
            DataField declaredField = declaredType.getFields().get(i);
            DataField tableField = tableType.getFields().get(i);
            if (!declaredField.name().equals(tableField.name())
                    || !isCompatibleType(declaredField.type(), tableField.type())) {
                return false;
            }
        }
        return true;
    }
    
    static Object parseValue(Column column, DataType dataType) {
        if (column == null || column.getRawData() == null) {
            return null;
        }
        return convertValue(column.getRawData(), column, dataType);
    }
    
    private static Object convertValue(Object rawValue, Column column, DataType dataType) {
        if (rawValue == null) {
            return null;
        }
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                return column == null ? toBoolean(rawValue) : column.asBoolean();
            case TINYINT:
                return column == null ? toBigDecimal(rawValue).byteValue() : column.asLong().byteValue();
            case SMALLINT:
                return column == null ? toBigDecimal(rawValue).shortValue() : column.asLong().shortValue();
            case INTEGER:
                return column == null ? toBigDecimal(rawValue).intValue() : column.asBigInteger().intValue();
            case BIGINT:
                return column == null ? toBigDecimal(rawValue).longValue() : column.asLong();
            case FLOAT:
                return column == null ? toBigDecimal(rawValue).floatValue() : column.asBigDecimal().floatValue();
            case DOUBLE:
                return column == null ? toBigDecimal(rawValue).doubleValue() : column.asBigDecimal().doubleValue();
            case DECIMAL:
                DecimalType decimalType = (DecimalType) dataType;
                BigDecimal decimal = column == null ? toBigDecimal(rawValue) : column.asBigDecimal();
                return Decimal.fromBigDecimal(decimal, decimalType.getPrecision(), decimalType.getScale());
            case DATE:
                return toDateEpochDay(rawValue, column);
            case TIME_WITHOUT_TIME_ZONE:
                return toTimeMillis(rawValue, column);
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return toTimestamp(rawValue, column);
            case CHAR:
            case VARCHAR:
                return BinaryString.fromString(toStringValue(rawValue));
            case BINARY:
            case VARBINARY:
                return column == null ? toStringValue(rawValue).getBytes(java.nio.charset.StandardCharsets.UTF_8) : column.asBytes();
            case ARRAY:
                return toArray(rawValue, (ArrayType) dataType);
            case MAP:
                return toMap(rawValue, (MapType) dataType);
            case ROW:
                return toRow(rawValue, (RowType) dataType);
            default:
                throw DataXException.asDataXException("暂不支持写入Paimon类型: " + dataType.asSQLString());
        }
    }
    
    private static GenericArray toArray(Object rawValue, ArrayType arrayType) {
        Object parsed = parseJsonIfNecessary(rawValue);
        if (!(parsed instanceof List)) {
            throw DataXException.asDataXException("ARRAY字段必须是JSON数组: " + rawValue);
        }
        List<?> values = (List<?>) parsed;
        Object[] array = new Object[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = convertValue(values.get(i), null, arrayType.getElementType());
        }
        return new GenericArray(array);
    }
    
    private static GenericMap toMap(Object rawValue, MapType mapType) {
        Object parsed = parseJsonIfNecessary(rawValue);
        if (!(parsed instanceof Map)) {
            throw DataXException.asDataXException("MAP字段必须是JSON对象: " + rawValue);
        }
        Map<?, ?> rawMap = (Map<?, ?>) parsed;
        Map<Object, Object> map = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object key = convertValue(entry.getKey(), null, mapType.getKeyType());
            Object value = convertValue(entry.getValue(), null, mapType.getValueType());
            map.put(key, value);
        }
        return new GenericMap(map);
    }
    
    private static GenericRow toRow(Object rawValue, RowType rowType) {
        Object parsed = parseJsonIfNecessary(rawValue);
        GenericRow row = new GenericRow(rowType.getFieldCount());
        if (parsed instanceof JSONObject || parsed instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parsed;
            for (int i = 0; i < rowType.getFieldCount(); i++) {
                DataField field = rowType.getFields().get(i);
                row.setField(i, convertValue(map.get(field.name()), null, field.type()));
            }
            return row;
        }
        if (parsed instanceof JSONArray || parsed instanceof List) {
            List<?> list = (List<?>) parsed;
            if (list.size() != rowType.getFieldCount()) {
                throw DataXException.asDataXException(String.format(
                        "ROW字段数组长度:%s 与表字段数:%s 不一致", list.size(), rowType.getFieldCount()));
            }
            for (int i = 0; i < rowType.getFieldCount(); i++) {
                row.setField(i, convertValue(list.get(i), null, rowType.getTypeAt(i)));
            }
            return row;
        }
        throw DataXException.asDataXException("ROW字段必须是JSON对象或数组: " + rawValue);
    }
    
    private static Object parseJsonIfNecessary(Object rawValue) {
        if (rawValue instanceof JSONObject || rawValue instanceof JSONArray || rawValue instanceof Map || rawValue instanceof List) {
            return rawValue;
        }
        return JSON.parse(toStringValue(rawValue));
    }
    
    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(toStringValue(value));
    }
    
    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(toStringValue(value));
    }
    
    private static int toDateEpochDay(Object value, Column column) {
        Date date = column == null ? toDate(value, DATE_PATTERN) : column.asDate();
        return (int) TimeUnit.MILLISECONDS.toDays(date.getTime());
    }
    
    private static int toTimeMillis(Object value, Column column) {
        if (column != null && column.getRawData() instanceof Date) {
            Date date = column.asDate();
            return (int) (date.getTime() % TimeUnit.DAYS.toMillis(1));
        }
        String text = toStringValue(value);
        try {
            return (int) Time.valueOf(LocalTime.parse(text)).getTime();
        } catch (Exception ignore) {
            return (int) Time.valueOf(text).getTime();
        }
    }
    
    private static Timestamp toTimestamp(Object value, Column column) {
        Date date = column == null ? toDate(value, null) : column.asDate();
        return Timestamp.fromEpochMillis(date.getTime());
    }
    
    private static Date toDate(Object value, String pattern) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        String text = toStringValue(value);
        if (DATE_PATTERN.equals(pattern)) {
            LocalDate localDate = LocalDate.parse(text);
            return java.sql.Date.valueOf(localDate);
        }
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"};
        for (String candidate : patterns) {
            try {
                return new SimpleDateFormat(candidate, Locale.ROOT).parse(text);
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        throw DataXException.asDataXException("日期时间格式无法解析: " + text);
    }
    
    private static String toStringValue(Object value) {
        return String.valueOf(value);
    }
}
