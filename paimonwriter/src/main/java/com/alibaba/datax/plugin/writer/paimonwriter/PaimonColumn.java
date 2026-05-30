package com.alibaba.datax.plugin.writer.paimonwriter;

final class PaimonColumn {
    
    private final String name;
    
    private final String type;
    
    PaimonColumn(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    String getName() {
        return name;
    }
    
    String getType() {
        return type;
    }
}
