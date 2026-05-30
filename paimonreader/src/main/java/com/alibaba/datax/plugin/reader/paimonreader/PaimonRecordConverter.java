package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.element.BoolColumn;
import com.alibaba.datax.common.element.BytesColumn;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.DoubleColumn;
import com.alibaba.datax.common.element.LongColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.fastjson2.JSON;
import org.apache.paimon.data.InternalArray;
import org.apache.paimon.data.InternalMap;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.types.ArrayType;
import org.apache.paimon.types.DataField;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.MapType;
import org.apache.paimon.types.RowType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class PaimonRecordConverter {
    
    private final RowType rowType;
    
    private final int[] projection;
    
    PaimonRecordConverter(RowType rowType, int[] projection) {
        this.rowType = rowType;
        this.projection = projection;
    }
    
    Record convert(RecordSender recordSender, InternalRow row) {
        Record record = recordSender.createRecord();
        int fieldCount = projection == null ? rowType.getFieldCount() : projection.length;
        for (int i = 0; i < fieldCount; i++) {
            int tableIndex = projection == null ? i : projection[i];
            DataType dataType = rowType.getTypeAt(tableIndex);
            Object value = getValue(row, i, dataType);
            addColumn(record, value, dataType);
        }
        return record;
    }
    
    private static void addColumn(Record record, Object value, DataType dataType) {
        if (value == null) {
            record.addColumn(new StringColumn(null));
            return;
        }
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                record.addColumn(new BoolColumn((Boolean) value));
                break;
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case DATE:
            case TIME_WITHOUT_TIME_ZONE:
                record.addColumn(new LongColumn(String.valueOf(value)));
                break;
            case FLOAT:
            case DOUBLE:
                record.addColumn(new DoubleColumn(String.valueOf(value)));
                break;
            case DECIMAL:
                record.addColumn(new StringColumn(String.valueOf(value)));
                break;
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                record.addColumn(new DateColumn((java.util.Date) value));
                break;
            case BINARY:
            case VARBINARY:
                record.addColumn(new BytesColumn((byte[]) value));
                break;
            default:
                record.addColumn(new StringColumn(String.valueOf(value)));
                break;
        }
    }
    
    private static Object getValue(InternalRow row, int index, DataType dataType) {
        if (row.isNullAt(index)) {
            return null;
        }
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                return row.getBoolean(index);
            case TINYINT:
                return row.getByte(index);
            case SMALLINT:
                return row.getShort(index);
            case INTEGER:
                return row.getInt(index);
            case BIGINT:
                return row.getLong(index);
            case FLOAT:
                return row.getFloat(index);
            case DOUBLE:
                return row.getDouble(index);
            case DECIMAL:
                DecimalType decimalType = (DecimalType) dataType;
                return row.getDecimal(index, decimalType.getPrecision(), decimalType.getScale()).toBigDecimal();
            case CHAR:
            case VARCHAR:
                return row.getString(index).toString();
            case DATE:
                return TimeUnit.DAYS.toMillis(row.getInt(index));
            case TIME_WITHOUT_TIME_ZONE:
                return row.getInt(index);
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return row.getTimestamp(index, 3).toSQLTimestamp();
            case BINARY:
            case VARBINARY:
                return row.getBinary(index);
            case ARRAY:
                return JSON.toJSONString(toList(row.getArray(index), ((ArrayType) dataType).getElementType()));
            case MAP:
                return JSON.toJSONString(toMap(row.getMap(index), (MapType) dataType));
            case ROW:
                return JSON.toJSONString(toMap(row.getRow(index, ((RowType) dataType).getFieldCount()), (RowType) dataType));
            default:
                return JSON.toJSONString(String.valueOf(row));
        }
    }
    
    private static List<Object> toList(InternalArray array, DataType elementType) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(getArrayValue(array, i, elementType));
        }
        return values;
    }
    
    private static Map<Object, Object> toMap(InternalMap map, MapType mapType) {
        Map<Object, Object> values = new LinkedHashMap<>();
        InternalArray keyArray = map.keyArray();
        InternalArray valueArray = map.valueArray();
        for (int i = 0; i < map.size(); i++) {
            values.put(
                    getArrayValue(keyArray, i, mapType.getKeyType()),
                    getArrayValue(valueArray, i, mapType.getValueType()));
        }
        return values;
    }
    
    private static Map<String, Object> toMap(InternalRow row, RowType rowType) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < rowType.getFieldCount(); i++) {
            DataField field = rowType.getFields().get(i);
            values.put(field.name(), getRowValue(row, i, field.type()));
        }
        return values;
    }
    
    private static Object getArrayValue(InternalArray array, int index, DataType dataType) {
        if (array.isNullAt(index)) {
            return null;
        }
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                return array.getBoolean(index);
            case TINYINT:
                return array.getByte(index);
            case SMALLINT:
                return array.getShort(index);
            case INTEGER:
                return array.getInt(index);
            case BIGINT:
                return array.getLong(index);
            case FLOAT:
                return array.getFloat(index);
            case DOUBLE:
                return array.getDouble(index);
            case DECIMAL:
                DecimalType decimalType = (DecimalType) dataType;
                return array.getDecimal(index, decimalType.getPrecision(), decimalType.getScale()).toBigDecimal();
            case CHAR:
            case VARCHAR:
                return array.getString(index).toString();
            case DATE:
                return java.time.LocalDate.ofEpochDay(array.getInt(index)).toString();
            case TIME_WITHOUT_TIME_ZONE:
                return java.time.LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(array.getInt(index))).toString();
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return array.getTimestamp(index, 3).toSQLTimestamp().toString();
            case BINARY:
            case VARBINARY:
                return array.getBinary(index);
            case ARRAY:
                return toList(array.getArray(index), ((ArrayType) dataType).getElementType());
            case MAP:
                return toMap(array.getMap(index), (MapType) dataType);
            case ROW:
                return toMap(array.getRow(index, ((RowType) dataType).getFieldCount()), (RowType) dataType);
            default:
                return null;
        }
    }
    
    private static Object getRowValue(InternalRow row, int index, DataType dataType) {
        if (row.isNullAt(index)) {
            return null;
        }
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                return row.getBoolean(index);
            case TINYINT:
                return row.getByte(index);
            case SMALLINT:
                return row.getShort(index);
            case INTEGER:
                return row.getInt(index);
            case BIGINT:
                return row.getLong(index);
            case FLOAT:
                return row.getFloat(index);
            case DOUBLE:
                return row.getDouble(index);
            case DECIMAL:
                DecimalType decimalType = (DecimalType) dataType;
                return row.getDecimal(index, decimalType.getPrecision(), decimalType.getScale()).toBigDecimal();
            case CHAR:
            case VARCHAR:
                return row.getString(index).toString();
            case DATE:
                return java.time.LocalDate.ofEpochDay(row.getInt(index)).toString();
            case TIME_WITHOUT_TIME_ZONE:
                return java.time.LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(row.getInt(index))).toString();
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return row.getTimestamp(index, 3).toSQLTimestamp().toString();
            case BINARY:
            case VARBINARY:
                return row.getBinary(index);
            case ARRAY:
                return toList(row.getArray(index), ((ArrayType) dataType).getElementType());
            case MAP:
                return toMap(row.getMap(index), (MapType) dataType);
            case ROW:
                return toMap(row.getRow(index, ((RowType) dataType).getFieldCount()), (RowType) dataType);
            default:
                return null;
        }
    }
}
