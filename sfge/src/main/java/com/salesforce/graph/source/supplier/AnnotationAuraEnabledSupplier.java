package com.salesforce.graph.source.supplier;

import com.salesforce.graph.Schema;

/** Supplier for non-test methods with an {@code @AuraEnabled} annotation. */
public class AnnotationAuraEnabledSupplier extends AbstractAnnotationSourceSupplier {
    @Override
    protected String getAnnotation() {
        return Schema.AURA_ENABLED;
    }
}
