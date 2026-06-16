package com.alibaba.datax.plugin.writer.paimonwriter;

import java.util.Collections;
import java.util.List;

final class ProxyPrimaryKeyConfig {
    
    static final String PROXY_PRIMARY_KEY_NAME = "_id_";
    
    private final PrimaryKeyMode mode;
    
    private final ProxyPrimaryKeyAlgorithm algorithm;
    
    private final List<String> sourceFields;
    
    ProxyPrimaryKeyConfig(PrimaryKeyMode mode, ProxyPrimaryKeyAlgorithm algorithm, List<String> sourceFields) {
        this.mode = mode;
        this.algorithm = algorithm;
        this.sourceFields = sourceFields == null ? Collections.<String>emptyList() : sourceFields;
    }
    
    PrimaryKeyMode getMode() {
        return mode;
    }
    
    ProxyPrimaryKeyAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    List<String> getSourceFields() {
        return sourceFields;
    }
    
    boolean isProxy() {
        return mode == PrimaryKeyMode.PROXY;
    }
}
