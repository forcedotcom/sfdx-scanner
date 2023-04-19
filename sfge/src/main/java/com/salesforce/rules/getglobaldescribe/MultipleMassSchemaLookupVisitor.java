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
    private final MassSchemaLookupViolationInfo violationInfo;

    MultipleMassSchemaLookupVisitor(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.violationInfo = new MassSchemaLookupViolationInfo(sourceVertex, sinkVertex);
    }

    @Override
    void execAfterLoopVertexVisit(BaseSFVertex vertex, SymbolProvider symbols) {
        violationInfo.addOccurrence(MassSchemaLookupViolationInfo.Type.INVOKED_IN_A_LOOP, vertex);
    }

    MassSchemaLookupViolationInfo getViolationInfo() {
        return violationInfo;
    }

}
