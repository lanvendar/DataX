/*
 * Copyright © 2020-2024 Nimbus Corporation All rights reserved.
 *
 * 使本项目源码前请仔细阅读以下协议内容，如果你同意以下协议才能使用本项目所有的功能,
 * 否则如果你违反了以下协议，有可能陷入法律纠纷和赔偿，作者保留追究法律责任的权利.
 *
 * 1、本代码为商业源代码，只允许已授权内部人员查看使用
 * 2、任何人员无权将代码泄露或者授权给其他未被授权人员使用
 * 3、任何修改请保留原始作者信息，不得擅自删除及修改
 *
 * 请保留以上版权信息，否则作者将保留追究法律责任.
 */

package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
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
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.predicate.Predicate;
import org.apache.paimon.predicate.PredicateBuilder;
import org.apache.paimon.reader.RecordReader;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.source.ReadBuilder;
import org.apache.paimon.table.source.Split;
import org.apache.paimon.table.source.TableRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Paimon表的读取插件
 * @author Li Zhengkai
 * @version 3.0, 2024/7/15
 * @since 2024/7/15
 */
public class PaimonReader extends Reader {
    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaimonReader.class);
    
    public static class Job extends Reader.Job {
        
        private Configuration originalConfig = null;
        
        @Override
        public List<Configuration> split(int adviceNumber) {
            List<Configuration> splitConfigs = new ArrayList<>();
            splitConfigs.add(originalConfig);
            return splitConfigs;
        }
        
        @Override
        public void init() {
            System.setProperty("HADOOP_USER_NAME", "root");
            originalConfig = getPluginJobConf();
        }
        
        @Override
        public void destroy() {
        
        }
    }
    
    public static class Task extends Reader.Task {
        
        private Configuration taskConfig;
        
        private Table table;
        
        @Override
        public void startRead(RecordSender recordSender) throws ParseException {
            ReadBuilder readBuilder = table.newReadBuilder();
            
            String sql = taskConfig.getString(Key.SQL);
            
            int[] projection = null;
            
            if (StringUtils.isNoneBlank(sql)) {
                List<SQLStatement> list = SQLUtils.toStatementList(sql, JdbcConstants.POSTGRESQL);
                
                SQLStatement sqlStatement = list.get(0);
                
                if (sqlStatement instanceof SQLSelectStatement) {
                    PredicateBuilder builder =
                            new PredicateBuilder(table.rowType());
                    
                    SQLSelectStatement selectStatement = (SQLSelectStatement) sqlStatement;
                    
                    SQLSelect select = selectStatement.getSelect();
                    
                    SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock)select.getQuery();
                    
                    List<SQLSelectItem> selectColumnList = queryBlock.getSelectList();
                    
                    List<Integer> projectionList = new ArrayList<>();
                    
                    for (SQLSelectItem sqlSelectItem : selectColumnList) {
                        SQLExpr expr = sqlSelectItem.getExpr();
                        if (expr instanceof SQLAllColumnExpr) {
                            //所有字段
                            projectionList.clear();
                            break;
                        } else if (expr instanceof SQLIdentifierExpr) {
                            SQLIdentifierExpr selectColumnExpr = (SQLIdentifierExpr) expr;
                            
                            projectionList.add(builder.indexOf(selectColumnExpr.getName()));
                        }
                        
                    }
                    
                    if (!projectionList.isEmpty()) {
                        projection = new int[projectionList.size()];
                        for (int i = 0; i < projectionList.size(); i++) {
                            projection[i] = projectionList.get(i);
                        }
                        readBuilder.withProjection(projection);
                    }
                    
                    List<Predicate> predicateList = new ArrayList<>();
                    
                    parseSqlBinaryOpExpr(queryBlock.getWhere(), predicateList, builder);
                    
                    readBuilder.withFilter(predicateList);
                }
            }
            
            List<Split> splits = readBuilder.newScan().plan().splits();
            
            TableRead read = readBuilder.newRead();
            
            try (RecordReader<InternalRow> reader = read.createReader(splits)) {
                final int[] finalProjection = projection;
                reader.forEachRemaining(r -> {
                    recordSender.sendToWriter(PaimonHelper.convertRecord(recordSender, r, table.rowType(), finalProjection));
                });
            } catch (Exception e) {
                LOG.error("读取Paimon表失败", e);
                throw DataXException.asDataXException("读取Paimon表失败");
            }
        }
        
        @Override
        public void init() {
            System.setProperty("HADOOP_USER_NAME", "root");
            this.taskConfig = super.getPluginJobConf();
        }
        
        @Override
        public void destroy() {
        
        }
        
        @Override
        public void prepare() {
            super.prepare();
            try {
                table = PaimonHelper.getPaimonTable(taskConfig);
            } catch (Exception e) {
                LOG.error("获取Paimon表失败", e);
                throw e;
            }
        }
    }
    
    private static void parseSqlBinaryOpExpr(SQLExpr expr, List<Predicate> list, PredicateBuilder builder) {
        if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr conditionBinary = (SQLBinaryOpExpr) expr;
            
            SQLBinaryOperator opExpr = conditionBinary.getOperator();
            SQLExpr conditionExpr = conditionBinary.getLeft();
            SQLExpr conditionValueExpr = conditionBinary.getRight();
            if (conditionExpr instanceof SQLBinaryOpExpr) {
                parseSqlBinaryOpExpr(conditionExpr, list, builder);
            } else {
                if (conditionExpr instanceof SQLIdentifierExpr) {
                    SQLIdentifierExpr conditionColumnExpr = (SQLIdentifierExpr) conditionExpr;
                    Predicate predicate = buildPredicate(conditionColumnExpr, conditionValueExpr, opExpr, builder);
                    
                    list.add(predicate);
                } else if (conditionExpr instanceof SQLInListExpr) {
                    SQLInListExpr sqlInListExpr = (SQLInListExpr) conditionExpr;
                    
                    Predicate predicate = buildPredicate(sqlInListExpr, builder);
                    list.add(predicate);
                } else if (conditionExpr instanceof SQLBetweenExpr) {
                    SQLBetweenExpr sqlBetweenExpr = (SQLBetweenExpr) conditionExpr;
                    
                    Predicate predicate = buildPredicate(sqlBetweenExpr, builder);
                    list.add(predicate);
                }
            }
            if (conditionValueExpr instanceof SQLBinaryOpExpr && conditionBinary.getOperator().equals(SQLBinaryOperator.BooleanAnd)) {
                parseSqlBinaryOpExpr(conditionValueExpr, list, builder);
            } else if (conditionValueExpr instanceof SQLInListExpr) {
                SQLInListExpr sqlInListExpr = (SQLInListExpr) conditionValueExpr;
                
                Predicate predicate = buildPredicate(sqlInListExpr, builder);
                list.add(predicate);
            } else if (conditionValueExpr instanceof SQLBetweenExpr) {
                SQLBetweenExpr sqlBetweenExpr = (SQLBetweenExpr) conditionValueExpr;
                
                Predicate predicate = buildPredicate(sqlBetweenExpr, builder);
                list.add(predicate);
            }
        } else if (expr instanceof SQLInListExpr) {
            SQLInListExpr sqlInListExpr = (SQLInListExpr) expr;
            
            Predicate predicate = buildPredicate(sqlInListExpr, builder);
            list.add(predicate);
        } else if (expr instanceof SQLBetweenExpr) {
            SQLBetweenExpr sqlBetweenExpr = (SQLBetweenExpr) expr;
            
            Predicate predicate = buildPredicate(sqlBetweenExpr, builder);
            list.add(predicate);
        }
    }
    
    private static Predicate buildPredicate(SQLIdentifierExpr conditionColumnExpr, SQLExpr conditionValueExpr,
            SQLBinaryOperator opExpr, PredicateBuilder builder) {
        
        String fileName = conditionColumnExpr.getName();
        Object value = getValue(conditionValueExpr);
        
        switch (opExpr) {
            case Is:
                if (conditionValueExpr instanceof SQLNullExpr) {
                    return builder.isNull(builder.indexOf(fileName));
                } else {
                    LOG.warn("不支持的值类型,{}", conditionValueExpr);
                }
                break;
            case IsNot:
                if (conditionValueExpr instanceof SQLNullExpr) {
                    return builder.isNotNull(builder.indexOf(fileName));
                } else {
                    LOG.warn("不支持的值类型,{}", conditionValueExpr);
                }
                break;
            case Equality:
                return builder.equal(builder.indexOf(fileName), value);
            case GreaterThan:
                return builder.greaterThan(builder.indexOf(fileName), value);
            case GreaterThanOrEqual:
                return builder.greaterOrEqual(builder.indexOf(fileName), value);
            case LessThan:
                return builder.lessThan(builder.indexOf(fileName), value);
            case LessThanOrEqual:
                return builder.lessOrEqual(builder.indexOf(fileName), value);
            case NotEqual:
            case LessThanOrGreater:
                return builder.notEqual(builder.indexOf(fileName), value);
            default:
                return null;
        }
        
        return null;
    }
    
    private static Object getValue(SQLExpr expr) {
        Object value = null;
        
        if (expr instanceof SQLIntegerExpr) {
            value = ((SQLIntegerExpr) expr).getNumber().longValue();
        } else if (expr instanceof SQLCharExpr) {
            value = BinaryString.fromString(((SQLCharExpr) expr).getText());
        } else if (expr instanceof SQLNumberExpr) {
            value = ((SQLNumberExpr) expr).getNumber().doubleValue();
        } else if (expr instanceof SQLBooleanExpr) {
            value = ((SQLBooleanExpr) expr).getValue();
        }
        
        return value;
    }
    
    private static Predicate buildPredicate(SQLInListExpr sqlInListExpr, PredicateBuilder builder) {
        SQLExpr conditionExpr = sqlInListExpr.getExpr();
        if (conditionExpr instanceof SQLIdentifierExpr) {
            String fileName = ((SQLIdentifierExpr) conditionExpr).getName();
            List<Object> values =sqlInListExpr.getTargetList().stream().map(PaimonReader::getValue).collect(
                    Collectors.toList());
            if (sqlInListExpr.isNot()) {
                return builder.notIn(builder.indexOf(fileName), values);
            } else {
                return builder.in(builder.indexOf(fileName), values);
            }
        } else {
            LOG.warn("不支持的表达式,{}", sqlInListExpr);
            return null;
        }
    }
    
    private static Predicate buildPredicate(SQLBetweenExpr sqlBetweenExpr, PredicateBuilder builder) {
        if (sqlBetweenExpr.getTestExpr() instanceof SQLIdentifierExpr) {
            String fileName = ((SQLIdentifierExpr) sqlBetweenExpr.getTestExpr()).getName();
            Object value1 = getValue(sqlBetweenExpr.getBeginExpr());
            Object value2 = getValue(sqlBetweenExpr.getEndExpr());
            
            return builder.between(builder.indexOf(fileName), value1, value2);
        }
        return null;
    }
}
