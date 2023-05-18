package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.ops.boundary.LoopBoundary;
import com.salesforce.rules.ops.visitor.LoopDetectionVisitor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Visitor detects when more than one invocation of Schema.getGlobalDescribe() or
 * Schema.describeSObjects is made in a path.
 */
class MultipleMassSchemaLookupVisitor extends LoopDetectionVisitor {
    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Represents the mass schema lookup vertex that we're looking earlier calls for. */
    private final MethodCallExpressionVertex sinkVertex;

    /** Collects violation information */
    private final HashSet<MultipleMassSchemaLookupInfo> violations;

    /** Indicates if the sink vertex has been visited or not. */
    private boolean isSinkVisited = false;

    MultipleMassSchemaLookupVisitor(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    public boolean visit(BlockStatementVertex vertex, SymbolProvider symbols) {
        return true;
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (vertex.equals(sinkVertex)) {
            // Mark sink as visited. From this point on, we don't need to
            // look for anymore loops or additional calls.
            // TODO: A more performant approach would be to stop walking path from this point
            isSinkVisited = true;
            checkIfInsideLoop(vertex, symbols);
        } else if (RuleConstants.isSchemaExpensiveMethod(vertex) && shouldContinue()) {
            violations.add(
                    new MultipleMassSchemaLookupInfo(
                            sourceVertex,
                            sinkVertex,
                            RuleConstants.RepetitionType.MULTIPLE,
                            vertex));
        }
    }

    private void checkIfInsideLoop(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final Optional<LoopBoundary> loopBoundaryOptional = loopBoundaryDetector.peek();
        if (loopBoundaryOptional.isPresent()) {
            // Method has been invoked inside a loop. Create a violation.
            final SFVertex loopVertex = loopBoundaryOptional.get().getBoundaryItem();
            violations.add(
                    new MultipleMassSchemaLookupInfo(
                            sourceVertex,
                            sinkVertex,
                            RuleConstants.RepetitionType.LOOP,
                            loopVertex));
        }
    }

    /**
     * Decides whether the rule should continue collecting violations
     *
     * @return true if the rule visitor should continue.
     */
    private boolean shouldContinue() {
        return !isSinkVisited;
    }

    /**
     * @return Violations collected by the rule.
     */
    Set<MultipleMassSchemaLookupInfo> getViolations() {
        return violations;
    }
}
