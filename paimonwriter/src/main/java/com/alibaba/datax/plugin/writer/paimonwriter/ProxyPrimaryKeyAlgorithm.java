package com.alibaba.datax.plugin.writer.paimonwriter;

import com.alibaba.datax.common.exception.DataXException;

import java.util.Locale;

enum ProxyPrimaryKeyAlgorithm {
    UUID,
    SHA_256("SHA-256"),
    SHA_512("SHA-512");
    
    private final String configName;
    
    ProxyPrimaryKeyAlgorithm() {
        this.configName = name();
    }
    
    ProxyPrimaryKeyAlgorithm(String configName) {
        this.configName = configName;
    }
    
    String getConfigName() {
        return configName;
    }
    
    static ProxyPrimaryKeyAlgorithm from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UUID;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ProxyPrimaryKeyAlgorithm.valueOf(normalized);
        } catch (Exception e) {
            throw DataXException.asDataXException("primaryKeyAlgorithm仅支持UUID、SHA-256、SHA-512");
        }
    }
}
