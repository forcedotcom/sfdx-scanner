package com.salesforce.rules.dmlinloop;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.CloningSymbolProvider;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.Violation;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class DmlInLoopRuleHandler {

    /** checks if a certain vertex is of interest to this rule */
    public boolean test(BaseSFVertex vertex) {
        /* Three different types of DML statements we need to check for:
           1. insert a;
           2. [SELECT a FROM b WHERE c]
           3. Database.something();
        */
        if (vertex instanceof DmlStatementVertex) {
            return true;
        } else if (vertex instanceof SoqlExpressionVertex) {
            return true;
        } else if (vertex instanceof MethodCallExpressionVertex) {
            return true;
        }
        return false;
    }

    /** within a path, check if there is a violation where DML is in a loop */
    public Set<Violation.PathBasedRuleViolation> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex dmlVertex) {

        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);
        final DefaultSymbolProviderVertexVisitor symbolVisitor =
                new DefaultSymbolProviderVertexVisitor(g);

        // We should only be detecting vertices that have to do with DML
        final DmlInLoopVisitor ruleVisitor;
        if (dmlVertex instanceof DmlStatementVertex) {
            ruleVisitor = new DmlInLoopVisitor(sourceVertex, (DmlStatementVertex) dmlVertex);
        } else if (dmlVertex instanceof MethodCallExpressionVertex) {
            ruleVisitor =
                    new DmlInLoopVisitor(sourceVertex, (MethodCallExpressionVertex) dmlVertex);
        } else if (dmlVertex instanceof SoqlExpressionVertex) {
            ruleVisitor = new DmlInLoopVisitor(sourceVertex, (SoqlExpressionVertex) dmlVertex);
        } else {
            throw new ProgrammingException(
                    "GetGlobalDescribeViolationRule unexpected invoked on an instance "
                            + "that's not DmlStatementVertex, MethodCallExpressionVertex, nor SoqlExpressionVertex. vertex="
                            + dmlVertex);
        }

        final SymbolProvider symbols = new CloningSymbolProvider(symbolVisitor.getSymbolProvider());
        ApexPathWalker.walkPath(g, path, ruleVisitor, symbolVisitor, symbols);

        // Once it finishes walking, collect any violations thrown.
        return ruleVisitor.getViolations();
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final DmlInLoopRuleHandler INSTANCE = new DmlInLoopRuleHandler();
    }

    public static DmlInLoopRuleHandler getInstance() {
        return DmlInLoopRuleHandler.LazyHolder.INSTANCE;
    }
}
