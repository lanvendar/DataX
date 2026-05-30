package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.paimon.types.DataTypes;
import org.apache.paimon.types.RowType;
import org.junit.Assert;
import org.junit.Test;

public class PaimonSqlPlannerTest {
    
    @Test
    public void testProjectionAndPredicates() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.STRING(),
                        DataTypes.INT(),
                        DataTypes.STRING()
                },
                new String[]{"name", "age", "dt"});
        
        PaimonReadPlan plan = new PaimonSqlPlanner(rowType).plan(
                "select name, age from test_person where dt = '20260530' and age >= 18");
        
        Assert.assertArrayEquals(new int[]{0, 1}, plan.getProjection());
        Assert.assertEquals(2, plan.getPredicates().size());
    }
    
    @Test
    public void testSelectAll() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{DataTypes.STRING()},
                new String[]{"name"});
        
        PaimonReadPlan plan = new PaimonSqlPlanner(rowType).plan("select * from test_person");
        
        Assert.assertNull(plan.getProjection());
        Assert.assertTrue(plan.getPredicates().isEmpty());
    }
    
    @Test(expected = DataXException.class)
    public void testRejectOr() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{DataTypes.STRING(), DataTypes.INT()},
                new String[]{"name", "age"});
        
        new PaimonSqlPlanner(rowType).plan("select name from test_person where name = 'a' or age = 1");
    }
    
    @Test
    public void testTypedPredicates() {
        RowType rowType = RowType.of(
                new org.apache.paimon.types.DataType[]{
                        DataTypes.INT(),
                        DataTypes.DECIMAL(18, 2),
                        DataTypes.DATE(),
                        DataTypes.TIMESTAMP()
                },
                new String[]{"age", "amount", "dt", "created_at"});
        
        PaimonReadPlan plan = new PaimonSqlPlanner(rowType).plan(
                "select age from test_person where age = 18 and amount = 12.34 and dt = '2026-05-30' and created_at = '2026-05-30 12:13:14'");
        
        Assert.assertArrayEquals(new int[]{0}, plan.getProjection());
        Assert.assertEquals(4, plan.getPredicates().size());
    }
}
