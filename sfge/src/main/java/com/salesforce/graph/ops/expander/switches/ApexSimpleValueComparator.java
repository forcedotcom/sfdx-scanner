package com.salesforce.graph.ops.expander.switches;

import com.salesforce.graph.symbols.apex.ApexSimpleValue;
import com.salesforce.graph.vertex.CaseVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.Objects;

/** Compares an ApexSimpleValue to the expected value in the CaseVertex */
final class ApexSimpleValueComparator implements CaseVertexComparator {
    private final ApexSimpleValue<?, ?> value;

    /** @param value that was supplied to the switch statement's "switch on" directive */
    ApexSimpleValueComparator(ApexSimpleValue<?, ?> value) {
        this.value = value;
    }

    @Override
    public boolean valueSatisfiesVertex(CaseVertex vertex) {
        TypedVertexVisitor<Boolean> visitor =
                new TypedVertexVisitor.DefaultThrow<Boolean>() {
                    @Override
                    public Boolean visit(LiteralCaseVertex vertex) {
                        LiteralExpressionVertex literalExpression = vertex.getLiteralExpression();
                        if (literalExpression instanceof LiteralExpressionVertex.Null) {
                            return value.isNull();
                        } else {
                            return Objects.equals(
                                    literalExpression.getLiteral(), value.getValue().orElse(null));
                        }
                    }
                };
        return vertex.accept(visitor);
    }
}
