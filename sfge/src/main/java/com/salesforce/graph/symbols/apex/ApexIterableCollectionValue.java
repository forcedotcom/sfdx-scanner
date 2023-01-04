package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.vertex.Typeable;
import java.util.Collection;
import java.util.Optional;

/** Represents ApexValues that hold an iterable collection of values. */
public interface ApexIterableCollectionValue {
    /**
     * @return values that the collection holds
     */
    Collection<ApexValue<?>> getValues();

    /**
     * @return SubType of the collection value. For example, List<String> would return a String
     *     Typeable
     */
    Optional<Typeable> getSubType();
}
