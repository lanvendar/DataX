package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;

import java.util.Locale;

enum LoadMode {
    APPEND,
    UPSERT,
    OVERWRITE_PARTITION;
    
    static LoadMode from(String value) {
        try {
            return LoadMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw DataXException.asDataXException("loadMode仅支持APPEND、UPSERT、OVERWRITE_PARTITION");
        }
    }
}
