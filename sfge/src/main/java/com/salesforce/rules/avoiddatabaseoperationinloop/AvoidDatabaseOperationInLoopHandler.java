package com.salesforce.rules.avoiddatabaseoperationinloop;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.symbols.CloningSymbolProvider;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.rules.Violation;
import com.salesforce.rules.ops.DatabaseOperationUtil;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class AvoidDatabaseOperationInLoopHandler {

    /** checks if a certain vertex is of interest to this rule */
    public boolean test(BaseSFVertex vertex) {
        // we're only interested in database operations
        if (!DatabaseOperationUtil.isDatabaseOperation(vertex)) {
            return false;
        }
        // automatically interested in all database operations that aren't method calls
        if (!(vertex instanceof MethodCallExpressionVertex)) {
            return true;
        }
        // any database operations that are method calls are from Database class.
        // must confirm that they are actually violations in a loop.
        return DatabaseOperationUtil.DatabaseOperation.fromString(
                        ((MethodCallExpressionVertex) vertex).getFullMethodName())
                .get() // we know this exists thanks to isDatabaseOperation(vertex)
                .isViolationInLoop();
    }

    /** within a path, check if there is a violation where DML is in a loop */
    public Set<Violation.PathBasedRuleViolation> detectViolations(
            GraphTraversalSource g, ApexPath path, BaseSFVertex dmlVertex) {

        final SFVertex sourceVertex = path.getMethodVertex().orElse(null);
        final DefaultSymbolProviderVertexVisitor symbolVisitor =
                new DefaultSymbolProviderVertexVisitor(g);

        // We should only be detecting vertices that have to do with DML
        final AvoidDatabaseOperationInLoopVisitor ruleVisitor;
        if (dmlVertex instanceof DmlStatementVertex) {
            ruleVisitor =
                    new AvoidDatabaseOperationInLoopVisitor(
                            sourceVertex, (DmlStatementVertex) dmlVertex);
        } else if (dmlVertex instanceof MethodCallExpressionVertex) {
            ruleVisitor =
                    new AvoidDatabaseOperationInLoopVisitor(
                            sourceVertex, (MethodCallExpressionVertex) dmlVertex);
        } else if (dmlVertex instanceof SoqlExpressionVertex) {
            ruleVisitor =
                    new AvoidDatabaseOperationInLoopVisitor(
                            sourceVertex, (SoqlExpressionVertex) dmlVertex);
        } else {
            throw new ProgrammingException(
                    "AvoidDatabaseOperationInLoopHandler unexpected invoked on an instance "
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
        private static final AvoidDatabaseOperationInLoopHandler INSTANCE =
                new AvoidDatabaseOperationInLoopHandler();
    }

    public static AvoidDatabaseOperationInLoopHandler getInstance() {
        return AvoidDatabaseOperationInLoopHandler.LazyHolder.INSTANCE;
    }
}
