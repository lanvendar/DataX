package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.paimon.data.BinaryString;
import org.apache.paimon.data.Decimal;
import org.apache.paimon.data.Timestamp;
import org.apache.paimon.predicate.Predicate;
import org.apache.paimon.predicate.PredicateBuilder;
import org.apache.paimon.types.DataType;
import org.apache.paimon.types.DecimalType;
import org.apache.paimon.types.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class PaimonSqlPlanner {
    
    private static final Logger LOG = LoggerFactory.getLogger(PaimonSqlPlanner.class);
    
    private final RowType rowType;
    
    PaimonSqlPlanner(RowType rowType) {
        this.rowType = rowType;
    }
    
    PaimonReadPlan plan(String sql) {
        if (StringUtils.isBlank(sql)) {
            return new PaimonReadPlan(null, new ArrayList<Predicate>());
        }
        
        List<SQLStatement> statements = SQLUtils.toStatementList(sql, JdbcConstants.POSTGRESQL);
        if (statements.size() != 1 || !(statements.get(0) instanceof SQLSelectStatement)) {
            throw DataXException.asDataXException("sql仅支持单条select语句");
        }
        
        SQLSelectStatement selectStatement = (SQLSelectStatement) statements.get(0);
        SQLSelect select = selectStatement.getSelect();
        if (!(select.getQuery() instanceof SQLSelectQueryBlock)) {
            throw DataXException.asDataXException("sql仅支持简单select查询");
        }
        
        PredicateBuilder builder = new PredicateBuilder(rowType);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) select.getQuery();
        int[] projection = parseProjection(queryBlock.getSelectList(), builder);
        List<Predicate> predicates = new ArrayList<>();
        parseSqlBinaryOpExpr(queryBlock.getWhere(), predicates, builder);
        predicates.removeIf(predicate -> predicate == null);
        return new PaimonReadPlan(projection, predicates);
    }
    
    private int[] parseProjection(List<SQLSelectItem> selectColumnList, PredicateBuilder builder) {
        List<Integer> projectionList = new ArrayList<>();
        for (SQLSelectItem sqlSelectItem : selectColumnList) {
            SQLExpr expr = sqlSelectItem.getExpr();
            if (expr instanceof SQLAllColumnExpr) {
                return null;
            }
            if (!(expr instanceof SQLIdentifierExpr)) {
                throw DataXException.asDataXException("select仅支持字段名或*，不支持表达式: " + expr);
            }
            SQLIdentifierExpr selectColumnExpr = (SQLIdentifierExpr) expr;
            int index = builder.indexOf(selectColumnExpr.getName());
            if (index < 0) {
                throw DataXException.asDataXException("select字段不存在于Paimon表: " + selectColumnExpr.getName());
            }
            projectionList.add(index);
        }
        
        if (projectionList.isEmpty()) {
            return null;
        }
        int[] projection = new int[projectionList.size()];
        for (int i = 0; i < projectionList.size(); i++) {
            projection[i] = projectionList.get(i);
        }
        return projection;
    }
    
    private void parseSqlBinaryOpExpr(SQLExpr expr, List<Predicate> list, PredicateBuilder builder) {
        if (expr == null) {
            return;
        }
        if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr conditionBinary = (SQLBinaryOpExpr) expr;
            SQLBinaryOperator opExpr = conditionBinary.getOperator();
            if (opExpr == SQLBinaryOperator.BooleanOr) {
                throw DataXException.asDataXException("where暂不支持or条件: " + expr);
            }
            
            SQLExpr conditionExpr = conditionBinary.getLeft();
            SQLExpr conditionValueExpr = conditionBinary.getRight();
            if (opExpr == SQLBinaryOperator.BooleanAnd) {
                parseSqlBinaryOpExpr(conditionExpr, list, builder);
                parseSqlBinaryOpExpr(conditionValueExpr, list, builder);
                return;
            }
            
            if (conditionExpr instanceof SQLIdentifierExpr) {
                Predicate predicate = buildPredicate((SQLIdentifierExpr) conditionExpr, conditionValueExpr, opExpr, builder);
                list.add(predicate);
                return;
            }
        }
        if (expr instanceof SQLInListExpr) {
            list.add(buildPredicate((SQLInListExpr) expr, builder));
            return;
        }
        if (expr instanceof SQLBetweenExpr) {
            list.add(buildPredicate((SQLBetweenExpr) expr, builder));
            return;
        }
        LOG.warn("忽略不支持的where表达式: {}", expr);
    }
    
    private Predicate buildPredicate(SQLIdentifierExpr conditionColumnExpr, SQLExpr conditionValueExpr,
                                            SQLBinaryOperator opExpr, PredicateBuilder builder) {
        String fieldName = conditionColumnExpr.getName();
        int index = builder.indexOf(fieldName);
        if (index < 0) {
            throw DataXException.asDataXException("where字段不存在于Paimon表: " + fieldName);
        }
        Object value = getValue(conditionValueExpr, rowType.getTypeAt(index));
        
        switch (opExpr) {
            case Is:
                if (conditionValueExpr instanceof SQLNullExpr) {
                    return builder.isNull(index);
                }
                break;
            case IsNot:
                if (conditionValueExpr instanceof SQLNullExpr) {
                    return builder.isNotNull(index);
                }
                break;
            case Equality:
                return builder.equal(index, value);
            case GreaterThan:
                return builder.greaterThan(index, value);
            case GreaterThanOrEqual:
                return builder.greaterOrEqual(index, value);
            case LessThan:
                return builder.lessThan(index, value);
            case LessThanOrEqual:
                return builder.lessOrEqual(index, value);
            case NotEqual:
            case LessThanOrGreater:
                return builder.notEqual(index, value);
            default:
                break;
        }
        throw DataXException.asDataXException("不支持的where条件: " + conditionColumnExpr + " " + opExpr + " " + conditionValueExpr);
    }
    
    private static Object getValue(SQLExpr expr, DataType dataType) {
        if (expr instanceof SQLNullExpr) {
            return null;
        }
        Object rawValue;
        if (expr instanceof SQLIntegerExpr) {
            rawValue = ((SQLIntegerExpr) expr).getNumber();
        } else if (expr instanceof SQLCharExpr) {
            rawValue = ((SQLCharExpr) expr).getText();
        } else if (expr instanceof SQLNumberExpr) {
            rawValue = ((SQLNumberExpr) expr).getNumber();
        } else if (expr instanceof SQLBooleanExpr) {
            rawValue = ((SQLBooleanExpr) expr).getValue();
        } else {
            throw DataXException.asDataXException("where条件值类型暂不支持: " + expr);
        }
        return convertLiteral(rawValue, dataType);
    }
    
    private static Object convertLiteral(Object rawValue, DataType dataType) {
        switch (dataType.getTypeRoot()) {
            case BOOLEAN:
                return rawValue instanceof Boolean ? rawValue : Boolean.valueOf(String.valueOf(rawValue));
            case TINYINT:
                return toBigDecimal(rawValue).byteValue();
            case SMALLINT:
                return toBigDecimal(rawValue).shortValue();
            case INTEGER:
                return toBigDecimal(rawValue).intValue();
            case BIGINT:
                return toBigDecimal(rawValue).longValue();
            case FLOAT:
                return toBigDecimal(rawValue).floatValue();
            case DOUBLE:
                return toBigDecimal(rawValue).doubleValue();
            case DECIMAL:
                DecimalType decimalType = (DecimalType) dataType;
                return Decimal.fromBigDecimal(toBigDecimal(rawValue), decimalType.getPrecision(), decimalType.getScale());
            case DATE:
                return rawValue instanceof Number
                        ? ((Number) rawValue).intValue()
                        : (int) TimeUnit.MILLISECONDS.toDays(Date.valueOf(String.valueOf(rawValue)).getTime());
            case TIME_WITHOUT_TIME_ZONE:
                return rawValue instanceof Number
                        ? ((Number) rawValue).intValue()
                        : (int) Time.valueOf(String.valueOf(rawValue)).getTime();
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                return Timestamp.fromSQLTimestamp(toSqlTimestamp(rawValue));
            case CHAR:
            case VARCHAR:
                return BinaryString.fromString(String.valueOf(rawValue));
            case BINARY:
            case VARBINARY:
                return String.valueOf(rawValue).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            default:
                throw DataXException.asDataXException("where暂不支持字段类型: " + dataType.asSQLString());
        }
    }
    
    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }
    
    private static java.sql.Timestamp toSqlTimestamp(Object value) {
        if (value instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp) value;
        }
        if (value instanceof Number) {
            return new java.sql.Timestamp(((Number) value).longValue());
        }
        String text = String.valueOf(value);
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                return new java.sql.Timestamp(new SimpleDateFormat(pattern, Locale.ROOT).parse(text).getTime());
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        throw DataXException.asDataXException("where时间条件无法解析: " + text);
    }
    
    private Predicate buildPredicate(SQLInListExpr sqlInListExpr, PredicateBuilder builder) {
        SQLExpr conditionExpr = sqlInListExpr.getExpr();
        if (!(conditionExpr instanceof SQLIdentifierExpr)) {
            throw DataXException.asDataXException("in条件左侧必须是字段名: " + sqlInListExpr);
        }
        String fieldName = ((SQLIdentifierExpr) conditionExpr).getName();
        int index = builder.indexOf(fieldName);
        if (index < 0) {
            throw DataXException.asDataXException("where字段不存在于Paimon表: " + fieldName);
        }
        List<Object> values = sqlInListExpr.getTargetList().stream()
                .map(expr -> getValue(expr, rowType.getTypeAt(index)))
                .collect(Collectors.toList());
        return sqlInListExpr.isNot() ? builder.notIn(index, values) : builder.in(index, values);
    }
    
    private Predicate buildPredicate(SQLBetweenExpr sqlBetweenExpr, PredicateBuilder builder) {
        if (!(sqlBetweenExpr.getTestExpr() instanceof SQLIdentifierExpr)) {
            throw DataXException.asDataXException("between条件左侧必须是字段名: " + sqlBetweenExpr);
        }
        String fieldName = ((SQLIdentifierExpr) sqlBetweenExpr.getTestExpr()).getName();
        int index = builder.indexOf(fieldName);
        if (index < 0) {
            throw DataXException.asDataXException("where字段不存在于Paimon表: " + fieldName);
        }
        DataType dataType = rowType.getTypeAt(index);
        return builder.between(index, getValue(sqlBetweenExpr.getBeginExpr(), dataType), getValue(sqlBetweenExpr.getEndExpr(), dataType));
    }
}
