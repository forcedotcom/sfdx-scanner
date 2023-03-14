package com.salesforce.graph.vertex;

/**
 * Interface to mark vertices that can have null-access check performed on them. At some point in
 * the analysis, they can have {@link com.salesforce.graph.ops.expander.NullValueAccessedException}
 * thrown if the value is known to be null. These null access checks are surfaced through {@link
 * com.salesforce.rules.ApexNullPointerExceptionRule}.
 */
public interface NullAccessCheckedVertex {
    /**
     * @return Display name to use while sharing information if vertex is affected by null-access.
     */
    String getDisplayName();

    /**
     * @return Id to represent the vertex in a collection.
     */
    Long getId();
}
