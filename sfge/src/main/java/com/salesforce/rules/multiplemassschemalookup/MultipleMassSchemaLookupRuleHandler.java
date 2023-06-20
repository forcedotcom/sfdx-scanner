package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.CloningSymbolProvider;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.MultipleMassSchemaLookupRule;
import java.util.HashSet;
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
                && MmslrUtil.isSchemaExpensiveMethod((MethodCallExpressionVertex) vertex);
    }

    public Set<MultipleMassSchemaLookupInfo> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        final HashSet<MultipleMassSchemaLookupInfo> mmslInfo = new HashSet<>();
        if (!(vertex instanceof MethodCallExpressionVertex)) {
            throw new ProgrammingException(
                    "GetGlobalDescribeViolationRule unexpected invoked on an instance that's not MethodCallExpressionVertex. vertex="
                            + vertex);
        }

        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);
        final DefaultSymbolProviderVertexVisitor symbolVisitor =
                new DefaultSymbolProviderVertexVisitor(g);

        final MultipleMassSchemaLookupVisitor ruleVisitor =
                new MultipleMassSchemaLookupVisitor(
                        sourceVertex, (MethodCallExpressionVertex) vertex);
        final AnotherPathViolationDetector duplicateMethodCallDetector =
                new AnotherPathViolationDetector(
                        symbolVisitor, sourceVertex, (MethodCallExpressionVertex) vertex);

        final SymbolProvider symbols = new CloningSymbolProvider(symbolVisitor.getSymbolProvider());
        ApexPathWalker.walkPath(
                g, path, ruleVisitor, symbolVisitor, duplicateMethodCallDetector, symbols);

        // Once it finishes walking, collect any violations thrown
        mmslInfo.addAll(ruleVisitor.getViolations());
        mmslInfo.addAll(duplicateMethodCallDetector.getViolations());

        return mmslInfo;
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
