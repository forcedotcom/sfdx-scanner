package com.salesforce.graph.symbols;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.ApexStringValueFactory;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.Optional;

/**
 * Generates an {@link ApexValue} from a {@link VariableExpressionVertex.Unknown}. Examples are
 * Schema.SObjectType.Account, SObjectType.MyObject__c.Name, and
 * Schema.SObjectType.Account.fields.Name
 *
 * <p>{@link ApexStandardLibraryUtil} contains a list of implementors that are iterated over
 * attempting to find an interface that handles variable expressions. Any {@link ApexValue}s that
 * are generated via variable expressions should implement a corresponding class with the suffix
 * {@code Factory} and expose a static variable named {@code VARIABLE_EXPRESSION_BUILDER_FUNCTION}
 * that implements this interface. For instance {@link
 * ApexStringValueFactory#VARIABLE_EXPRESSION_BUILDER_FUNCTION}
 */
@FunctionalInterface
public interface VariableExpressionApexValueBuilder {
    /**
     * @param vertex that corresponds to the static method
     * @return an {@link ApexValue} if one can be resolved, else Optional.empty
     */
    Optional<ApexValue<?>> apply(VariableExpressionVertex.Unknown vertex);
}
