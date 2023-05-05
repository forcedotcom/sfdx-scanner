package com.salesforce.graph.source.supplier;

import com.salesforce.graph.Schema;

/** Supplier for non-test methods with a {@code @NamespaceAccessible} annotation. */
public class AnnotationNamespaceAccessibleSupplier extends AbstractAnnotationSourceSupplier {
    @Override
    protected String getAnnotation() {
        return Schema.NAMESPACE_ACCESSIBLE;
    }
}
