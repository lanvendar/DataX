package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.util.Configuration;
import org.junit.Assert;
import org.junit.Test;

public class PaimonWriteConfigTest {
    
    @Test
    public void testOverwriteTableDoesNotRequireOverwritePartition() {
        Configuration configuration = Configuration.from("{"
                + "\"loadMode\":\"OVERWRITE_TABLE\","
                + "\"column\":[{\"name\":\"name\",\"type\":\"varchar\"}]"
                + "}");
        
        PaimonWriteConfig writeConfig = PaimonWriteConfig.from(configuration);
        
        Assert.assertEquals(LoadMode.OVERWRITE_TABLE, writeConfig.getLoadMode());
        Assert.assertTrue(writeConfig.getLoadMode().isOverwrite());
        Assert.assertNull(writeConfig.getOverwritePartition());
    }
    
    @Test
    public void testInvalidLoadModeMessageContainsOverwriteTable() {
        try {
            LoadMode.from("TRUNCATE");
            Assert.fail("invalid loadMode should throw exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("OVERWRITE_TABLE"));
        }
    }
}
