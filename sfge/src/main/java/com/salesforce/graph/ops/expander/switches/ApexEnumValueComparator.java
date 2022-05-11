package com.salesforce.graph.ops.expander.switches;

import com.salesforce.apex.ApexEnum;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.vertex.CaseVertex;
import com.salesforce.graph.vertex.IdentifierCaseVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import org.apache.commons.lang3.StringUtils;

/**
 * Compares an ApexEnumValue to the expected value in the {@link IdentifierCaseVertex} or {@link
 * LiteralCaseVertex}
 */
final class ApexEnumValueComparator implements CaseVertexComparator {
    private final ApexEnumValue value;

    /** @param value that was supplied to the switch statement's "switch on" directive */
    ApexEnumValueComparator(ApexEnumValue value) {
        this.value = value;
    }

    @Override
    public boolean valueSatisfiesVertex(CaseVertex vertex) {
        TypedVertexVisitor<Boolean> visitor =
                new TypedVertexVisitor.DefaultThrow<Boolean>() {
                    @Override
                    public Boolean visit(IdentifierCaseVertex vertex) {
                        final ApexEnum.Value enumValue = value.getValue().orElse(null);
                        // The enum value is stored on the vertex as the Identifier. i.e.
                        // DisplayType.ADDRESS is stored as "ADDRESS"
                        return enumValue != null
                                && StringUtils.equalsIgnoreCase(
                                        vertex.getIdentifier(), enumValue.getValueName());
                    }

                    @Override
                    public Boolean visit(LiteralCaseVertex vertex) {
                        if (!(vertex.getLiteralExpression()
                                instanceof LiteralExpressionVertex.Null)) {
                            throw new UnexpectedException(
                                    "LiteralCaseVertex should only be present for a null value. vertex="
                                            + vertex);
                        }
                        return value.isNull();
                    }
                };
        return vertex.accept(visitor);
    }
}
