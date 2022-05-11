package com.salesforce.graph.vertex;

/** Represents an initialization operation for a Collection in Apex. */
public interface NewCollectionExpressionVertex extends Typeable {
    /** @return Type prefix that defines a collection */
    String getTypePrefix();
}
