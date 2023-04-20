package com.salesforce.rules.getglobaldescribe;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor detects when more than one invocation of Schema.getGlobalDescribe() is made in a path.
 */
class MultipleMassSchemaLookupVisitor extends LoopDetectionVisitor {
    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Represents the mass schema lookup vertex that we're looking earlier calls for. */
    private final MethodCallExpressionVertex sinkVertex;

    /** Collects violation information */
    private final HashSet<MassSchemaLookupInfo> violations;

    /** Indicates if the sink vertex has been visited or not. */
    private boolean isSinkVisited = false;

    MultipleMassSchemaLookupVisitor(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    @Override
    void execAfterLoopVertexVisit(BaseSFVertex vertex, SymbolProvider symbols) {
        if (shouldContinue()) {
            violations.add(
                    new MassSchemaLookupInfo(
                            sourceVertex, sinkVertex, RuleConstants.RepetitionType.LOOP, vertex));
        }
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (vertex.equals(sinkVertex)) {
            // Mark sink as visited. From this point on, we don't need to
            // look for anymore loops or additional calls.
            // TODO: A more performant approach would be to stop walking path from this point
            isSinkVisited = true;
        } else if (RuleConstants.isSchemaExpensiveMethod(vertex) && shouldContinue()) {
            violations.add(
                    new MassSchemaLookupInfo(
                            sourceVertex,
                            sinkVertex,
                            RuleConstants.RepetitionType.MULTIPLE,
                            vertex));
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
    Set<MassSchemaLookupInfo> getViolation() {
        return violations;
    }
}
