package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;

import java.util.Locale;

enum PrimaryKeyMode {
    FIELDS,
    PROXY;
    
    static PrimaryKeyMode from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FIELDS;
        }
        try {
            return PrimaryKeyMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw DataXException.asDataXException("primaryKeyMode仅支持FIELDS、PROXY");
        }
    }
}
