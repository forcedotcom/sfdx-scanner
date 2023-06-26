package com.salesforce.rules.dmlinloop;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.OccurrenceInfo;
import com.salesforce.rules.Violation;
import com.salesforce.rules.ops.visitor.LoopDetectionVisitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DmlInLoopVisitor extends LoopDetectionVisitor {

    private static final Logger LOGGER =
        LogManager.getLogger(DmlInLoopVisitor.class);

    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Represents the DML statement that is possibly inside a loop  */
    private final BaseSFVertex sinkVertex;

    /** Collects violation information */
    private final HashSet<Violation.PathBasedRuleViolation> violations;


    DmlInLoopVisitor(SFVertex sourceVertex, DmlStatementVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    // this constructor is necessary to detect Database.<whatever> methods
    DmlInLoopVisitor(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    DmlInLoopVisitor(SFVertex sourceVertex, SoqlExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
        super.afterVisit(vertex, symbols);
    }

    public void afterVisit(DmlStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    private void createViolationIfSinkInsideLoop(
        SFVertex vertex, SymbolProvider symbols
    ) {
        if (vertex != null && vertex.equals(sinkVertex)) {
            final Optional<? extends SFVertex> loopedVertexOpt = isInsideLoop();
            if (loopedVertexOpt.isPresent()) {
                // this is only a violation if we're inside a loop
                createViolation(loopedVertexOpt.get());
            }
        }
    }

    /**
     * Logs a violation
     * @param loopVertex the vertex at which the violation (loop) was detected
     */
    private void createViolation(SFVertex loopVertex) {
        String sinkName;

        if (sinkVertex instanceof MethodCallExpressionVertex) sinkName = ((MethodCallExpressionVertex) sinkVertex).getFullMethodName();

        // TODO check final UI text
        else sinkName = sinkVertex.getLabel();

        violations.add(new Violation.PathBasedRuleViolation(
            DmlInLoopUtil.getMessage(sinkName, loopVertex),
            sourceVertex, sinkVertex));
    }

    /**
     * @return Violations collected by the rule.
     */
    Set<Violation.PathBasedRuleViolation> getViolations() { return violations; }
}
