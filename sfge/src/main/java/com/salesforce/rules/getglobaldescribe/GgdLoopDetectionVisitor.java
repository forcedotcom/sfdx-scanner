package com.salesforce.rules.getglobaldescribe;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.rules.Violation;

import java.util.HashSet;
import java.util.Set;

class GgdLoopDetectionVisitor extends LoopDetectionVisitor {
    private static final String VIOLATION_MESSAGE = "%s should not be invoked within a loop. Detected presence in %s loop";
    private final Set<Violation> violations;
    private final MethodCallExpressionVertex targetVertex;

    GgdLoopDetectionVisitor(MethodCallExpressionVertex targetVertex) {
        violations = new HashSet<>();
        this.targetVertex = targetVertex;
    }

    Set<Violation> getViolations() {
        return new HashSet<>(violations);
    }

    @Override
    void execAfterLoopVertexVisit(BaseSFVertex vertex, SymbolProvider symbols) {
        // TODO: source vertex is incorrect
        violations.add(new Violation.PathBasedRuleViolation(getViolationMessage(vertex), vertex, targetVertex));
    }

    private String getViolationMessage(BaseSFVertex vertex) {
        return String.format(VIOLATION_MESSAGE, targetVertex.getFullMethodName(), vertex.getLabel());
    }
}
