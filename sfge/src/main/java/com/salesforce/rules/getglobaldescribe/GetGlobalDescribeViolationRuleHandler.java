package com.salesforce.rules.getglobaldescribe;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.Violation;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Set;

public class GetGlobalDescribeViolationRuleHandler {

    private static final String METHOD_SCHEMA_GET_GLOBAL_DESCRIBE = "Schema.getGlobalDescribe";

    public boolean test(BaseSFVertex vertex) {
        return vertex instanceof MethodCallExpressionVertex && isGetGlobalDescribeMethod((MethodCallExpressionVertex) vertex);
    }

    public Set<Violation> detectViolations(GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
        if (!(vertex instanceof MethodCallExpressionVertex)) {
            throw new ProgrammingException("GetGlobalDescribeViolationRule unexpected invoked on an instance that's not MethodCallExpressionVertex. vertex=" + vertex);
        }
        final GgdLoopDetectionVisitor ruleVisitor = new GgdLoopDetectionVisitor((MethodCallExpressionVertex) vertex);
        // TODO: I'm expecting to add other visitors depending on the other factors we will analyze to decide if a GGD call is not performant.
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, ruleVisitor, symbols);

        return ruleVisitor.getViolations();
    }

    private static boolean isGetGlobalDescribeMethod(MethodCallExpressionVertex vertex) {
        return METHOD_SCHEMA_GET_GLOBAL_DESCRIBE.equalsIgnoreCase(vertex.getFullMethodName());
    }

    public static GetGlobalDescribeViolationRuleHandler getInstance() {
        return GetGlobalDescribeViolationRuleHandler.LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final GetGlobalDescribeViolationRuleHandler INSTANCE = new GetGlobalDescribeViolationRuleHandler();
    }
}
