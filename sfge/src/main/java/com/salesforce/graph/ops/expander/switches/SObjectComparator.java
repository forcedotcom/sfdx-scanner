package com.salesforce.graph.ops.expander.switches;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.ApexPropertiesValue;
import com.salesforce.graph.vertex.CaseVertex;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.TypeWhenBlockVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.WhenBlockVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;

/**
 * Compares an ApexPropertiesValue to the expected value in the WhenBlock. This covers SObjects and
 * CustomSettings
 */
final class SObjectComparator {
    private final ApexPropertiesValue<?> value;

    /** @param value that was supplied to the switch statement's "switch on" directive */
    SObjectComparator(ApexPropertiesValue<?> value) {
        this.value = value;
    }

    /**
     * @return true if the ApexValue supplied to the switch statement would cause {@code vertex } to
     *     execute
     */
    boolean valueSatisfiesVertex(WhenBlockVertex vertex) {
        TypedVertexVisitor<Boolean> visitor =
                new TypedVertexVisitor.DefaultThrow<Boolean>() {
                    @Override
                    public Boolean visit(ElseWhenBlockVertex vertex) {
                        final SwitchStatementVertex switchStatementVertex =
                                vertex.getSwitchStatementVertex();
                        final SObjectComparator validator = new SObjectComparator(value);
                        for (WhenBlockVertex whenBlock : switchStatementVertex.getWhenBlocks()) {
                            if (whenBlock == vertex) {
                                continue;
                            }
                            if (validator.valueSatisfiesVertex(whenBlock)) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public Boolean visit(TypeWhenBlockVertex vertex) {
                        final Typeable typeable = value.getMostSpecificType().orElse(null);
                        if (typeable != null) {
                            final String valueCanonicalType = typeable.getCanonicalType();
                            final String vertexCanonicalType =
                                    ApexStandardLibraryUtil.getCanonicalName(vertex.getType());
                            return valueCanonicalType.equalsIgnoreCase(vertexCanonicalType);
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public Boolean visit(ValueWhenBlockVertex vertex) {
                        // This should only be a single "when null" block
                        List<CaseVertex> caseVertices = vertex.getCaseVertices();
                        if (caseVertices.size() != 1) {
                            throw new UnexpectedException(
                                    "Expecting a single null check. vertex=" + vertex);
                        }
                        CaseVertex caseVertex = caseVertices.get(0);
                        if (!(caseVertex instanceof LiteralCaseVertex)) {
                            throw new UnexpectedException(
                                    "Expecting a single null check. vertex=" + vertex);
                        }
                        LiteralCaseVertex literalCaseVertex = (LiteralCaseVertex) caseVertex;
                        LiteralExpressionVertex literalExpression =
                                literalCaseVertex.getLiteralExpression();
                        if (!(literalExpression instanceof LiteralExpressionVertex.Null)) {
                            throw new UnexpectedException(
                                    "Expecting a single null check. vertex=" + vertex);
                        }
                        return value.isNull();
                    }
                };
        return vertex.accept(visitor);
    }
}
