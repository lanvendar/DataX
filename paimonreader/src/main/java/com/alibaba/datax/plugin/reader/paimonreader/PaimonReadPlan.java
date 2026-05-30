package com.alibaba.datax.plugin.reader.paimonreader;

import org.apache.paimon.predicate.Predicate;

import java.util.Collections;
import java.util.List;

final class PaimonReadPlan {
    
    private final int[] projection;
    
    private final List<Predicate> predicates;
    
    PaimonReadPlan(int[] projection, List<Predicate> predicates) {
        this.projection = projection;
        this.predicates = predicates == null ? Collections.emptyList() : predicates;
    }
    
    int[] getProjection() {
        return projection;
    }
    
    List<Predicate> getPredicates() {
        return predicates;
    }
}
