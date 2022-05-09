package com.salesforce.graph.ops.expander.switches;

import com.salesforce.graph.symbols.apex.ApexPropertiesValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.TypeWhenBlockVertex;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.WhenBlockVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Excludes switch blocks based on the values present in the child {@link LiteralCaseVertex} and
 * {@link TypeWhenBlockVertex} vertices. This class handles cases where the switch expression is an
 * SObject.
 *
 * <p>Comparing an SObject value results in a mixture of TypeWhenBlocks and a ValueWhenBlock for the
 * null comparison case
 *
 * <pre>
 *     SObject obj = new Account();
 *     switch on obj {
 *         when Account a {
 *         }
 *         when Contact c {
 *         }
 *         when null {
 *         }
 *         when else {
 *         }
 *     }
 * </pre>
 *
 * <pre>{@code
 * <TypeWhenBlock Name="a" Type="Account">
 * 		...
 * </TypeWhenBlock>
 * <TypeWhenBlock Name="c" Type="Contact">
 * 		...
 * </TypeWhenBlock>
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
 * Exceptions that exclude the WhenBlock are thrown in three situations, depending on the type of
 * {@code WhenBlock}.
 *
 * <ol>
 *   <li>{@code TypeWhenBLock}: If the value passed into the constructor does not match the type
 *       declared in the TypeWhenBlock
 *   <li>{@code ValueWhenBlock}: If the value passed into the constructor returns false for {@link
 *       ApexValue#isNull()}
 *   <li>{@code ElseWhenBLocks}: If any of the TypeWhenBLock or ValueWhenBLocks handles the value
 *       passed into the constructor.
 * </ol>
 */
final class SObjectWhenBlockExcluder extends TypedVertexVisitor.DefaultThrow<Void> {
    private final SObjectComparator sObjectComparator;

    SObjectWhenBlockExcluder(ApexPropertiesValue<?> value) {
        this.sObjectComparator = new SObjectComparator(value);
    }

    @Override
    public Void visit(ElseWhenBlockVertex vertex) {
        final SwitchStatementVertex switchStatementVertex = vertex.getSwitchStatementVertex();
        // Find all when blocks except this ElseWhenBlockVertex, throw an exception if any of them
        // handle the apex value
        final List<WhenBlockVertex> whenBlockVertices =
                switchStatementVertex.getWhenBlocks().stream()
                        .filter(v -> v != vertex)
                        .collect(Collectors.toList());
        for (WhenBlockVertex whenBlockVertex : whenBlockVertices) {
            if (sObjectComparator.valueSatisfiesVertex(whenBlockVertex)) {
                // The else case should be excluded the value is handled by any of the TypeWhenBlock
                // or "when null"
                throw new ApexPathCaseStatementExcluder.CaseStatementExcludedException();
            }
        }
        return null;
    }

    @Override
    public Void visit(TypeWhenBlockVertex vertex) {
        if (!sObjectComparator.valueSatisfiesVertex(vertex)) {
            // This case should be excluded if the type does not match the type of the apex value
            // used as the switch expression
            throw new ApexPathCaseStatementExcluder.CaseStatementExcludedException();
        }
        return null;
    }

    @Override
    public Void visit(ValueWhenBlockVertex vertex) {
        if (!sObjectComparator.valueSatisfiesVertex(vertex)) {
            // This case is excluded if the apex values is not null
            throw new ApexPathCaseStatementExcluder.CaseStatementExcludedException();
        }
        return null;
    }
}
