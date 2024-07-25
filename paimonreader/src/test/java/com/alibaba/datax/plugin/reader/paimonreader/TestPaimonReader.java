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

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.paimon.data.InternalRow;
import org.apache.paimon.predicate.Predicate;
import org.apache.paimon.predicate.PredicateBuilder;
import org.apache.paimon.reader.RecordReader;
import org.apache.paimon.table.Table;
import org.apache.paimon.table.source.ReadBuilder;
import org.apache.paimon.table.source.Split;
import org.apache.paimon.table.source.TableRead;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Li Zhengkai
 * @version 3.0, 2024/7/17
 * @since 2024/7/17
 */
public class TestPaimonReader {
    private static final Logger LOG = LoggerFactory.getLogger(TestPaimonReader.class);
    
    @Test
    public void testSelect() {
        System.setProperty("HADOOP_USER_NAME", "root");
        Configuration conf = Configuration.from(TestPaimonReader.class.getResourceAsStream("/plugin.json"));
        try {
            Table table = PaimonHelper.getPaimonTable(conf);
            
            PredicateBuilder builder =
                    new PredicateBuilder(table.rowType());
            Predicate notNull = builder.isNotNull(builder.indexOf("name"));
            
            Predicate predicate = builder.equal(builder.indexOf("age"), 16);
            
            int[] projection = new int[] {0, 1};
            ReadBuilder readBuilder = table.newReadBuilder()
                    //.withProjection(projection)
                    .withFilter(predicate);
            
            //readBuilder.withFilter(predicate);
            
            List<Split> splits = readBuilder.newScan().plan().splits();
            
            TableRead read = readBuilder.newRead();
            
            RecordReader<InternalRow> reader = read.createReader(splits);
            
            reader.forEachRemaining(r -> {
                System.out.println(r.getString(0) + ":" + r.getInt(1));
            });
        } catch (Exception e) {
            LOG.error("获取Paimon表失败", e);
        }
    }
    
    @Test
    public void testSqlParser() throws Exception {
        System.setProperty("HADOOP_USER_NAME", "root");
        Configuration conf = Configuration.from(TestPaimonReader.class.getResourceAsStream("/plugin.json"));
        
        String sql = "select a,b,c from table";
        
        List<SQLStatement> list = SQLUtils.toStatementList(sql, JdbcConstants.POSTGRESQL);
        
        SQLStatement sqlStatement = list.get(0);
        
        if (sqlStatement instanceof SQLSelectStatement) {
            SQLSelectStatement selectStatement = (SQLSelectStatement) sqlStatement;
            
            SQLSelect select = selectStatement.getSelect();
            
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) select.getQuery();
            
            List<SQLSelectItem> selectColumnList = queryBlock.getSelectList();
            
            for (SQLSelectItem sqlSelectItem : selectColumnList) {
                SQLExpr expr = sqlSelectItem.getExpr();
                if (expr instanceof SQLPropertyExpr) {
                    SQLPropertyExpr selectColumnExpr = (SQLPropertyExpr) expr;
                    if (selectColumnExpr.getName().equals("*")) {
                        String all = "";
                    } else {
                        if (sqlSelectItem.getAlias() != null) {
                            String no = "";
                        } else {
                            String yes = "";
                        }
                    }
                } else if (expr instanceof SQLIdentifierExpr) {
                    SQLIdentifierExpr selectColumnExpr = (SQLIdentifierExpr) expr;
                    if (sqlSelectItem.getAlias() != null) {
                        String no = "";
                    } else {
                        String yes = selectColumnExpr.getName();
                    }
                } else if (expr instanceof SQLAllColumnExpr) {
                    String all = "";
                } else if (expr instanceof SQLAggregateExpr) {
                    if (sqlSelectItem.getAlias() != null) {
                        String no = "";
                    }
                }
            }
        }
    }
}
