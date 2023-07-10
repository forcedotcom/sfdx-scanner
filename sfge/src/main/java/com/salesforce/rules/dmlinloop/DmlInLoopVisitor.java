package com.salesforce.rules.dmlinloop;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.*;
import com.salesforce.rules.Violation;
import com.salesforce.rules.ops.visitor.LoopDetectionVisitor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DmlInLoopVisitor extends LoopDetectionVisitor {

    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Represents the DML statement that is possibly inside a loop */
    private final BaseSFVertex sinkVertex;

    /** Collects violation information */
    private final HashSet<Violation.PathBasedRuleViolation> violations;

    /**
     * Create a DmlInLoopVisitor
     *
     * @param sourceVertex the source of the path containing this vertex
     * @param sinkVertex the problematic sink vertex (in this case a vertex with DML). The sink
     *     vertex must be an instance of an {@link DmlStatementVertex}, {@link
     *     MethodCallExpressionVertex}, or {@link SoqlExpressionVertex}.
     */
    DmlInLoopVisitor(SFVertex sourceVertex, BaseSFVertex sinkVertex) {
        if (!(sinkVertex instanceof DmlStatementVertex
                || sinkVertex instanceof MethodCallExpressionVertex
                || sinkVertex instanceof SoqlExpressionVertex)) {
            throw new ProgrammingException(
                    "Sink vertex must be a DmlStatementVertex, MethodCallExpressionVertex, or SoqlExpressionVertex. Provided sink vertex="
                            + sinkVertex);
        }
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        // from DmlInLoopRuleHandler we already know that vertex is a database operation
        // method, in the format of Database.<something>

        // Perform super method's logic as well to remove exclusion boundary if needed.
        super.afterVisit(vertex, symbols);
    }

    // for all of these DmlStatementVertex implemenetations, we need these
    // so that the afterVisit method will resolve correctly,
    // and not to the parent classes generic afterVisit(BaseSFVertex).
    public void afterVisit(DmlDeleteStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(DmlUndeleteStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(DmlUpdateStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(DmlUpsertStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(DmlMergeStatementVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);
    }

    public void afterVisit(SoqlExpressionVertex vertex, SymbolProvider symbols) {
        createViolationIfSinkInsideLoop(vertex, symbols);

        // Perform super method's logic as well to remove exclusion boundary if needed.
        super.afterVisit(vertex, symbols);
    }

    private void createViolationIfSinkInsideLoop(SFVertex vertex, SymbolProvider symbols) {
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
     *
     * @param loopVertex the vertex at which the violation (loop) was detected
     */
    private void createViolation(SFVertex loopVertex) {

        violations.add(
                new Violation.PathBasedRuleViolation(
                        DmlInLoopUtil.getMessage(loopVertex), sourceVertex, sinkVertex));
    }

    /**
     * @return Violations collected by the rule.
     */
    Set<Violation.PathBasedRuleViolation> getViolations() {
        return violations;
    }
}
