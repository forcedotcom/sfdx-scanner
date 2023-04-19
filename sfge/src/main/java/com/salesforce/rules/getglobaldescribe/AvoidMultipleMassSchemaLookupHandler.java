package com.salesforce.rules.getglobaldescribe;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.AvoidMultipleMassSchemaLookup;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Executes internals of {@link AvoidMultipleMassSchemaLookup}
 */
public class AvoidMultipleMassSchemaLookupHandler {

    private static final String METHOD_SCHEMA_GET_GLOBAL_DESCRIBE = "Schema.getGlobalDescribe";

    /**
     * @param vertex to consider for analysis
     * @return true if the vertex parameter requires to be treated as a target vertex for {@link AvoidMultipleMassSchemaLookup}.
     */
    public boolean test(BaseSFVertex vertex) {
        return vertex instanceof MethodCallExpressionVertex && isGetGlobalDescribeMethod((MethodCallExpressionVertex) vertex);
    }

    public MassSchemaLookupViolationInfo detectViolations(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        if (!(vertex instanceof MethodCallExpressionVertex)) {
            throw new ProgrammingException("GetGlobalDescribeViolationRule unexpected invoked on an instance that's not MethodCallExpressionVertex. vertex=" + vertex);
        }

        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);

        final MultipleMassSchemaLookupVisitor ruleVisitor = new MultipleMassSchemaLookupVisitor(sourceVertex, (MethodCallExpressionVertex) vertex);
        // TODO: I'm expecting to add other visitors depending on the other factors we will analyze to decide if a GGD call is not performant.
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, ruleVisitor, symbols);

        // Once it finishes walking, collect any violations thrown
        return ruleVisitor.getViolationInfo();
    }

    private static boolean isGetGlobalDescribeMethod(MethodCallExpressionVertex vertex) {
        return METHOD_SCHEMA_GET_GLOBAL_DESCRIBE.equalsIgnoreCase(vertex.getFullMethodName());
    }

    public static AvoidMultipleMassSchemaLookupHandler getInstance() {
        return AvoidMultipleMassSchemaLookupHandler.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final AvoidMultipleMassSchemaLookupHandler INSTANCE = new AvoidMultipleMassSchemaLookupHandler();
    }
}
