package com.salesforce.graph.vertex;

public interface NullAccessCheckedVertex {
    /**
     * @return Display name to use while sharing information
     * if vertex is affected by null-access.
     */
    String getDisplayName();

    /**
     * @return Id to represent the vertex in a collection.
     */
    Long getId();
}
