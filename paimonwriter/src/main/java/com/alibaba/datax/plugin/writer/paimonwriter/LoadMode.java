package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;

import java.util.Locale;

enum LoadMode {
    APPEND,
    UPSERT,
    OVERWRITE_PARTITION,
    OVERWRITE_TABLE;
    
    static LoadMode from(String value) {
        try {
            return LoadMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw DataXException.asDataXException(
                    "loadMode仅支持APPEND、UPSERT、OVERWRITE_PARTITION、OVERWRITE_TABLE");
        }
    }
    
    boolean isOverwrite() {
        return this == OVERWRITE_PARTITION || this == OVERWRITE_TABLE;
    }
}
