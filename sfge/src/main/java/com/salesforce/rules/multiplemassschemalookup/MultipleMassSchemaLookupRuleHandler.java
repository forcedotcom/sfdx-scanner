package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.MultipleMassSchemaLookupRule;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Executes internals of {@link MultipleMassSchemaLookupRule} */
public class MultipleMassSchemaLookupRuleHandler {

    /**
     * @param vertex to consider for analysis
     * @return true if the vertex parameter requires to be treated as a target vertex for {@link
     *     MultipleMassSchemaLookupRule}.
     */
    public boolean test(BaseSFVertex vertex) {
        return vertex instanceof MethodCallExpressionVertex
                && RuleConstants.isSchemaExpensiveMethod((MethodCallExpressionVertex) vertex);
    }

    public Set<MassSchemaLookupInfo> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        if (!(vertex instanceof MethodCallExpressionVertex)) {
            throw new ProgrammingException(
                    "GetGlobalDescribeViolationRule unexpected invoked on an instance that's not MethodCallExpressionVertex. vertex="
                            + vertex);
        }

        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);

        final MultipleMassSchemaLookupVisitor ruleVisitor =
                new MultipleMassSchemaLookupVisitor(
                        sourceVertex, (MethodCallExpressionVertex) vertex);
        // TODO: I'm expecting to add other visitors depending on the other factors we will analyze
        // to decide if a GGD call is not performant.
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, ruleVisitor, symbols);

        // Once it finishes walking, collect any violations thrown
        return ruleVisitor.getViolation();
    }

    public static MultipleMassSchemaLookupRuleHandler getInstance() {
        return MultipleMassSchemaLookupRuleHandler.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final MultipleMassSchemaLookupRuleHandler INSTANCE =
                new MultipleMassSchemaLookupRuleHandler();
    }
}
