package com.salesforce.graph.ops.expander.switches;

import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.symbols.apex.ApexSimpleValue;
import com.salesforce.graph.vertex.CaseVertex;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.IdentifierCaseVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;

/**
 * Excludes switch blocks based on the values present in the child {@link LiteralCaseVertex} {@link
 * IdentifierCaseVertex} vertices. This class handles cases where the switch expression is an enum
 * or simple value such as Integer or Long
 *
 * <p>Comparing an enum value results in a mixture of LiteralCase and IdentifierCase vertices
 *
 * <pre>
 *     DisplayType dt = DisplayType.Address;
 *     switch on dt {
 *         when ADDRESS, CURRENCY {
 *         }
 *         when ANYTYPE {
 *         }
 *         when null {
 *         }
 *         when else {
 *         }
 *     }
 * </pre>
 *
 * <pre>{@code
 * <ValueWhenBlock>
 * 	<IdentifierCase Identifier="ADDRESS"/>
 * 	<IdentifierCase Identifier="CURRENCY"/>
 * 		...
 * </ValueWhenBlock>
 * <ValueWhenBlock>
 * 	<IdentifierCase Identifier="ANYTYPE"/>
 * 		...
 * </ValueWhenBlock>
 * <ValueWhenBlock>
 * <LiteralCase>
 *   <LiteralExpression LiteralType="NULL"/>
 *  </LiteralCase>
 * </ValueWhenBlock>
 * <ElseWhenBlock Label="ElseWhenBlock">
 *    ...
 * </ElseWhenBlock>
 * }</pre>
 *
 * <p>Comparing a simple value results in LiteralCase vertices
 *
 * <pre>
 *     Integer i = 10;
 *     switch on i {
 *         when 1, 2 {
 *         }
 *         when 3 {
 *         }
 *         when null {
 *         }
 *         when else {
 *         }
 *     }
 * </pre>
 *
 * <pre>{@code
 * <ValueWhenBlock>
 * <LiteralCase>
 *   <LiteralExpression LiteralType="Integer" Value="1"/>
 *  </LiteralCase>
 * <LiteralCase>
 *   <LiteralExpression LiteralType="Integer" Value="2"/>
 *  </LiteralCase>
 * 		...
 * </ValueWhenBlock>
 * <ValueWhenBlock>
 * <LiteralCase>
 *   <LiteralExpression LiteralType="Integer" Value="3"/>
 *  </LiteralCase>
 * 		...
 * </ValueWhenBlock>
 * <ValueWhenBlock>
 * <LiteralCase>
 *   <LiteralExpression LiteralType="NULL"/>
 *  </LiteralCase>
 * </ValueWhenBlock>
 * <ElseWhenBlock Label="ElseWhenBlock">
 *    ...
 * </ElseWhenBlock>
 * }</pre>
 *
 * Exceptions that exclude the WhenBlock are thrown in two situations, depending on the type of
 * {@code WhenBlock}.
 *
 * <ol>
 *   <li>{@code ValueWhenBLocks}: If the value passed into the constructor does not match any of the
 *       IdentifierExpression or LiteralExpression values that are children of the ValueWhenBLock.
 *   <li>{@code ElseWhenBLocks}: If any of the ValueWhenBLocks handles the value passed into the
 *       constructor.
 * </ol>
 */
final class CaseVertexExcluder extends TypedVertexVisitor.DefaultThrow<Void> {
    /**
     * Class that implements the method #valueSatisfiesVertex. The implementing subclass depends on
     * the type of ApexValue used as the expression in the switch statement.
     */
    private final CaseVertexComparator caseVertexComparator;

    CaseVertexExcluder(ApexEnumValue value) {
        this.caseVertexComparator = new ApexEnumValueComparator(value);
    }

    CaseVertexExcluder(ApexSimpleValue<?, ?> value) {
        this.caseVertexComparator = new ApexSimpleValueComparator(value);
    }

    /**
     * This is the fallthrough "when else" case. Find all IdentifierCase and LiteralCase vertices in
     * the entire switch statement, see if any of them satisfy the expression condition.
     *
     * @throws ApexPathCaseStatementExcluder.CaseStatementExcludedException if {@link
     *     CaseVertexComparator#valueSatisfiesVertex(CaseVertex)} returns true for any of these
     *     vertices.
     */
    @Override
    public Void visit(ElseWhenBlockVertex vertex) {
        final SwitchStatementVertex switchStatementVertex = vertex.getSwitchStatementVertex();
        if (caseVertexWasHandled(switchStatementVertex.getCaseVertices(), caseVertexComparator)) {
            // The else case should be excluded the value is handled by any of the Case vertices in
            // the entire switch statement
            throw new ApexPathCaseStatementExcluder.CaseStatementExcludedException();
        }
        return null;
    }

    /**
     * Find all IdentifierCase and LiteralCase vertices that are children of {@code vertex}, see if
     * any of them satisfy the expression condition.
     *
     * @throws ApexPathCaseStatementExcluder.CaseStatementExcludedException if {@link
     *     CaseVertexComparator#valueSatisfiesVertex(CaseVertex)} returns false for all of these
     *     vertices.
     */
    @Override
    public Void visit(ValueWhenBlockVertex vertex) {
        if (!caseVertexWasHandled(vertex.getCaseVertices(), caseVertexComparator)) {
            // This case should be excluded if none of the Case vertices handle the expression
            throw new ApexPathCaseStatementExcluder.CaseStatementExcludedException();
        }
        return null;
    }

    /**
     * Return true if {@code caseVertexComparator} returns true for any of the values in {@code
     * identifiers}
     */
    private boolean caseVertexWasHandled(
            List<CaseVertex> caseVertices, CaseVertexComparator caseVertexComparator) {
        for (CaseVertex caseVertex : caseVertices) {
            if (caseVertexComparator.valueSatisfiesVertex(caseVertex)) {
                return true;
            }
        }
        return false;
    }
}
