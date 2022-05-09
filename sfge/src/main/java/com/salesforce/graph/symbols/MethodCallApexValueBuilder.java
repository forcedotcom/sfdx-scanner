package com.salesforce.graph.symbols;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.ApexStringValueFactory;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Generates an {@link ApexValue} from a {@link MethodCallExpressionVertex}. Examples are
 * String.join, String.format, and Schema.SObjectType.Account.isDeletable()
 *
 * <p>{@link ApexStandardLibraryUtil} contains a list of implementors that are iterated over
 * attempting to find an interface that handles static methods. Any {@link ApexValue}s that are
 * generated via static methods should implement a corresponding class with the suffix {@code
 * Factory } and expose a static variable named {@code METHOD_CALL_BUILDER_FUNCTIONS} that
 * implements this interface. For instance {@link
 * ApexStringValueFactory#METHOD_CALL_BUILDER_FUNCTION}
 *
 * <p>Sometimes there is not a 1:1 mapping of ApexValue to Factory. This is typically in cases where
 * there are a group of methods that are similar enough that they are grouped in a single factory.
 * For instance {@link com.salesforce.graph.symbols.apex.ApexSimpleValueFactory} handles the
 * #valueOf method, and and {@link JSONDeserializeFactory} handles JSON#deserialize for multiple
 * object types.
 */
@FunctionalInterface
public interface MethodCallApexValueBuilder {
    /**
     * @param g the graph that contains the vertex
     * @param vertex that corresponds to the static method
     * @param symbols used to resolve method parameters
     * @return an {@link ApexValue} if one can be resolved, else Optional.empty
     */
    Optional<ApexValue<?>> apply(
            GraphTraversalSource g, MethodCallExpressionVertex vertex, SymbolProvider symbols);
}
