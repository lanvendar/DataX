package com.alibaba.datax.plugin.writer.paimonwriter;

final class PaimonColumn {
    
    private final String name;
    
    private final String type;
    
    private final String comment;
    
    PaimonColumn(String name, String type) {
        this(name, type, null);
    }
    
    PaimonColumn(String name, String type, String comment) {
        this.name = name;
        this.type = type;
        this.comment = comment;
    }
    
    String getName() {
        return name;
    }
    
    String getType() {
        return type;
    }
    
    String getComment() {
        return comment;
    }
}
