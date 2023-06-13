package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.ops.visitor.LoopDetectionVisitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Visitor detects when more than one invocation of Schema.getGlobalDescribe() or
 * Schema.describeSObjects is made in a path.
 */
class MultipleMassSchemaLookupVisitor extends LoopDetectionVisitor {
    private static final Logger LOGGER = LogManager.getLogger(MultipleMassSchemaLookupVisitor.class);

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
        return false;
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (vertex.equals(sinkVertex)) {
            // Mark sink as visited. From this point on, we don't need to
            // look for anymore loops or additional calls.
            // TODO: A more performant approach would be to stop walking path from this point
            isSinkVisited = true;
            createViolationIfInsideLoop(vertex, symbols);
        } else if (MmslrUtil.isSchemaExpensiveMethod(vertex) && shouldContinue()) {
            createViolation(MmslrUtil.RepetitionType.MULTIPLE, vertex);
        }

        // Perform super method's logic as well to remove exclusion boundary if needed.
        super.afterVisit(vertex, symbols);
    }

    private void createViolationIfMultipleCall(MethodCallExpressionVertex vertex) {
        if (MmslrUtil.isSchemaExpensiveMethod(vertex) && shouldContinue()) {
            createViolation(MmslrUtil.RepetitionType.MULTIPLE, vertex);
        }
    }

    private void createViolationIfInsideLoop(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final Optional<? extends SFVertex> loopedVertexOpt = isInsideLoop();
        if (loopedVertexOpt.isPresent()) {
            // Method has been invoked inside a loop. Create a violation.
            createViolation(MmslrUtil.RepetitionType.LOOP, loopedVertexOpt.get());
        }
    }

    private void createViolation(MmslrUtil.RepetitionType type, SFVertex repetitionVertex) {
        violations.add(MmslrUtil.newViolation(sourceVertex, sinkVertex, type, repetitionVertex));
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
