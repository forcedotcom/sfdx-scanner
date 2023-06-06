package com.salesforce.graph.source.supplier;

import com.salesforce.graph.Schema;

/** Supplier for non-test methods with a {@code @RemoteAction} annotation. */
public class AnnotationRemoteActionSupplier extends AbstractAnnotationSourceSupplier {
    @Override
    protected String getAnnotation() {
        return Schema.REMOTE_ACTION;
    }
}
