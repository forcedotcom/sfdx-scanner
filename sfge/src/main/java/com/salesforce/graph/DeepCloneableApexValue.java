package com.salesforce.graph;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.InvocableVertex;
import javax.annotation.Nullable;

public interface DeepCloneableApexValue<T extends ApexValue<?>> {
    /**
     * Clone an ApexValue and modifying the information about where it was returned from. Used by
     * ApexValues that return another ApexValue from a method. We want all of the original ApexValue
     * information, but want to indicate that it was returned from another method than the original
     * ApexValue.
     */
    T deepCloneForReturn(@Nullable ApexValue<?> returnedFrom, @Nullable InvocableVertex invocable);
}
