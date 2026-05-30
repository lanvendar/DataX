package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.paimon.types.DataField;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DataTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StarRocksTypeParser {
    
    private final String input;
    
    private int pos;
    
    private int fieldId;
    
    private StarRocksTypeParser(String input) {
        this.input = input;
    }
    
    static DataType parse(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw DataXException.asDataXException("column.type不能为空");
        }
        StarRocksTypeParser parser = new StarRocksTypeParser(type.trim());
        DataType dataType = parser.parseType();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw DataXException.asDataXException("不支持的StarRocks字段类型: " + type);
        }
        return dataType;
    }
    
    private DataType parseType() {
        skipWhitespace();
        String name = readIdentifier().toUpperCase(Locale.ROOT);
        if (name.length() == 0) {
            throw DataXException.asDataXException("字段类型不能为空: " + input);
        }
        
        if ("ARRAY".equals(name)) {
            expect('<');
            DataType elementType = parseType();
            expect('>');
            return DataTypes.ARRAY(elementType);
        }
        if ("MAP".equals(name)) {
            expect('<');
            DataType keyType = parseType();
            expect(',');
            DataType valueType = parseType();
            expect('>');
            return DataTypes.MAP(keyType, valueType);
        }
        if ("STRUCT".equals(name) || "ROW".equals(name)) {
            return parseRowType();
        }
        
        Integer firstArg = null;
        Integer secondArg = null;
        skipWhitespace();
        if (peek('(')) {
            expect('(');
            firstArg = readInteger();
            skipWhitespace();
            if (peek(',')) {
                expect(',');
                secondArg = readInteger();
            }
            expect(')');
        }
        
        return parsePrimitiveType(name, firstArg, secondArg);
    }
    
    private DataType parseRowType() {
        expect('<');
        List<DataField> fields = new ArrayList<>();
        do {
            skipWhitespace();
            String fieldName = readIdentifier();
            if (fieldName.length() == 0) {
                throw DataXException.asDataXException("STRUCT字段名不能为空: " + input);
            }
            skipWhitespace();
            if (peek(':')) {
                expect(':');
            }
            DataType fieldType = parseType();
            fields.add(DataTypes.FIELD(fieldId++, fieldName, fieldType));
            skipWhitespace();
            if (!peek(',')) {
                break;
            }
            expect(',');
        } while (true);
        expect('>');
        return DataTypes.ROW(fields.toArray(new DataField[0]));
    }
    
    private DataType parsePrimitiveType(String name, Integer firstArg, Integer secondArg) {
        if ("BOOLEAN".equals(name) || "BOOL".equals(name)) {
            return DataTypes.BOOLEAN();
        }
        if ("TINYINT".equals(name)) {
            return DataTypes.TINYINT();
        }
        if ("SMALLINT".equals(name)) {
            return DataTypes.SMALLINT();
        }
        if ("INT".equals(name) || "INTEGER".equals(name)) {
            return DataTypes.INT();
        }
        if ("BIGINT".equals(name)) {
            return DataTypes.BIGINT();
        }
        if ("LARGEINT".equals(name)) {
            return DataTypes.DECIMAL(38, 0);
        }
        if ("FLOAT".equals(name)) {
            return DataTypes.FLOAT();
        }
        if ("DOUBLE".equals(name)) {
            return DataTypes.DOUBLE();
        }
        if ("DECIMAL".equals(name) || "DECIMALV2".equals(name)
                || "DECIMAL32".equals(name) || "DECIMAL64".equals(name) || "DECIMAL128".equals(name)) {
            return DataTypes.DECIMAL(firstArg == null ? 10 : firstArg, secondArg == null ? 0 : secondArg);
        }
        if ("DATE".equals(name)) {
            return DataTypes.DATE();
        }
        if ("DATETIME".equals(name) || "TIMESTAMP".equals(name)) {
            return firstArg == null ? DataTypes.TIMESTAMP() : DataTypes.TIMESTAMP(firstArg);
        }
        if ("TIME".equals(name)) {
            return firstArg == null ? DataTypes.TIME() : DataTypes.TIME(firstArg);
        }
        if ("CHAR".equals(name)) {
            return DataTypes.CHAR(firstArg == null ? 1 : firstArg);
        }
        if ("VARCHAR".equals(name)) {
            return firstArg == null ? DataTypes.STRING() : DataTypes.VARCHAR(firstArg);
        }
        if ("STRING".equals(name) || "JSON".equals(name)) {
            return DataTypes.STRING();
        }
        if ("BINARY".equals(name) || "BYTEA".equals(name)) {
            return firstArg == null ? DataTypes.BYTES() : DataTypes.BINARY(firstArg);
        }
        if ("VARBINARY".equals(name)) {
            return firstArg == null ? DataTypes.BYTES() : DataTypes.VARBINARY(firstArg);
        }
        if ("BITMAP".equals(name) || "HLL".equals(name) || "PERCENTILE".equals(name)) {
            throw DataXException.asDataXException("PaimonWriter暂不支持StarRocks特殊类型: " + name);
        }
        throw DataXException.asDataXException("不支持的StarRocks字段类型: " + name);
    }
    
    private String readIdentifier() {
        skipWhitespace();
        int start = pos;
        while (!isEnd()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '`') {
                pos++;
            } else {
                break;
            }
        }
        String identifier = input.substring(start, pos).trim();
        if (identifier.startsWith("`") && identifier.endsWith("`") && identifier.length() > 1) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
    }
    
    private int readInteger() {
        skipWhitespace();
        int start = pos;
        while (!isEnd() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        if (start == pos) {
            throw DataXException.asDataXException("字段类型参数必须是整数: " + input);
        }
        return Integer.parseInt(input.substring(start, pos));
    }
    
    private void expect(char expected) {
        skipWhitespace();
        if (isEnd() || input.charAt(pos) != expected) {
            throw DataXException.asDataXException("字段类型格式错误，期望'" + expected + "': " + input);
        }
        pos++;
    }
    
    private boolean peek(char expected) {
        skipWhitespace();
        return !isEnd() && input.charAt(pos) == expected;
    }
    
    private void skipWhitespace() {
        while (!isEnd() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
    
    private boolean isEnd() {
        return pos >= input.length();
    }
}
