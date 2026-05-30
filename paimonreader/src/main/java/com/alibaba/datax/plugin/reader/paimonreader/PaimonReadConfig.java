package com.alibaba.datax.plugin.reader.paimonreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.lang3.StringUtils;

final class PaimonReadConfig {
    
    private final String sql;
    
    private PaimonReadConfig(String sql) {
        this.sql = sql;
    }
    
    static PaimonReadConfig from(Configuration taskConfig) {
        String table = taskConfig.getString(ConfigKey.TABLE);
        if (StringUtils.isBlank(table)) {
            throw DataXException.asDataXException("table不能为空");
        }
        return new PaimonReadConfig(taskConfig.getString(ConfigKey.SQL));
    }
    
    String getSql() {
        return sql;
    }
}
