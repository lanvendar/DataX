package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.util.Configuration;
import org.apache.paimon.schema.Schema;
import org.junit.Assert;
import org.junit.Test;

public class PaimonHelperTest {
    
    @Test
    public void testBuildSchemaWithChineseComments() {
        Configuration configuration = Configuration.from("{"
                + "\"tableComment\":\"用户信息表\","
                + "\"column\":["
                + "{\"name\":\"name\",\"type\":\"varchar\",\"comment\":\"用户姓名\"},"
                + "{\"name\":\"age\",\"type\":\"int\",\"comment\":\"用户年龄\"}"
                + "]"
                + "}");
        
        Schema schema = PaimonHelper.buildSchema(configuration);
        
        Assert.assertEquals("用户信息表", schema.comment());
        Assert.assertEquals("用户姓名", schema.fields().get(0).description());
        Assert.assertEquals("用户年龄", schema.fields().get(1).description());
    }
    
    @Test
    public void testBuildSchemaWithoutComments() {
        Configuration configuration = Configuration.from("{"
                + "\"column\":["
                + "{\"name\":\"name\",\"type\":\"varchar\"},"
                + "{\"name\":\"age\",\"type\":\"int\",\"comment\":\"\"}"
                + "]"
                + "}");
        
        Schema schema = PaimonHelper.buildSchema(configuration);
        
        Assert.assertTrue(schema.comment() == null || schema.comment().isEmpty());
        Assert.assertTrue(schema.fields().get(0).description() == null
                || schema.fields().get(0).description().isEmpty());
        Assert.assertTrue(schema.fields().get(1).description() == null
                || schema.fields().get(1).description().isEmpty());
    }
    
    @Test
    public void testBuildSchemaWithProxyPrimaryKey() {
        Configuration configuration = Configuration.from("{"
                + "\"primaryKeyMode\":\"PROXY\","
                + "\"loadMode\":\"UPSERT\","
                + "\"primaryKey\":\"name,age\","
                + "\"partitionKey\":\"dt\","
                + "\"options\":{\"bucket-key\":\"name\"},"
                + "\"column\":["
                + "{\"name\":\"name\",\"type\":\"varchar\"},"
                + "{\"name\":\"age\",\"type\":\"int\"},"
                + "{\"name\":\"dt\",\"type\":\"varchar\"}"
                + "]"
                + "}");
        
        Schema schema = PaimonHelper.buildSchema(configuration);
        
        Assert.assertEquals("_id_", schema.fields().get(0).name());
        Assert.assertEquals("_id_", schema.primaryKeys().get(0));
        Assert.assertTrue(schema.primaryKeys().contains("dt"));
        Assert.assertTrue(schema.primaryKeys().contains("name"));
        Assert.assertFalse(schema.primaryKeys().contains("age"));
    }
}
