package com.salesforce.graph.source.supplier;

import com.salesforce.graph.Schema;

/** Supplier for non-test methods with an {@code @InvocableMethod} annotation. */
public class AnnotationInvocableMethodSupplier extends AbstractAnnotationSourceSupplier {
    @Override
    protected String getAnnotation() {
        return Schema.INVOCABLE_METHOD;
    }
}
