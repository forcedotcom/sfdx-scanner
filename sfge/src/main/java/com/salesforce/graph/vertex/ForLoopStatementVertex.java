package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

public class ForLoopStatementVertex extends BaseSFVertex {
    /** Cache the answer to {@link #isSupportedSimpleIncrementingType(String, String)} */
    private final Map<Pair<String, String>, Boolean> supportedSimpleIncrementingType;

    ForLoopStatementVertex(Map<Object, Object> properties) {
        super(properties);
        this.supportedSimpleIncrementingType = new ConcurrentHashMap<>();
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return visitor.visit(this, symbols);
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
        visitor.afterVisit(this, symbols);
    }

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {
        visitor.afterVisit(this);
    }

    @Override
    public boolean startsInnerScope() {
        return true;
    }

    /**
     * TODO: Everything after here is DRAMATICALLY overfitted to the specific use case of
     * identifying when we're iterating over all of the entries in an array. That's fine for now,
     * but we'll almost certainly want to have a more generic "is this variable's value
     * loop-dependent" check in the future.
     *
     * <p>Identifies loops of the form:
     *
     * <ul>
     *   <li>{@code for (int i=0; i<l.size(); i++)}
     *   <li>{@code for (int i=0; i<l.size(); ++i)}
     *   <li>{@code for (int i=0; i<l.size(); i += 1)}
     *   <li>{@code for (int i=0; i<l.size(); i = i+1)}
     * </ul>
     *
     * @return true if this is a for loop that is iterating over all items in arrayName and the
     *     iterator is never reassigned within the loop
     */
    public boolean isSupportedSimpleIncrementingType(String arrayName, String indexName) {
        final Pair<String, String> key = Pair.of(arrayName, indexName);
        return supportedSimpleIncrementingType.computeIfAbsent(
                key, k -> _isSupportedSimpleIncrementingType(arrayName, indexName));
    }

    private boolean _isSupportedSimpleIncrementingType(String arrayName, String indexName) {
        // Verify that the loop's initialization step defines/assigns the index variable to 0.
        BaseSFVertex indexSetup =
                SFVertexFactory.loadSingleOrNull(
                        g(),
                        g().V(getId())
                                .out(Schema.CHILD)
                                // The loop setup must be the first child.
                                .has(Schema.FIRST_CHILD, true)
                                // A VariableDeclaration or AssignmentExpression vertex indicates
                                // that something's being declared or
                                // assigned. Try to find one.
                                .repeat(__.out(Schema.CHILD))
                                .until(
                                        __.hasLabel(
                                                NodeType.VARIABLE_DECLARATION,
                                                NodeType.ASSIGNMENT_EXPRESSION))
                                // One of this vertex's children should be a literal 0, and the
                                // other should be a VariableExpression
                                // for the index variable.
                                .and(
                                        __.out(Schema.CHILD)
                                                .where(
                                                        H.has(
                                                                NodeType.LITERAL_EXPRESSION,
                                                                Schema.VALUE,
                                                                "0")),
                                        __.out(Schema.CHILD)
                                                .where(
                                                        H.has(
                                                                NodeType.VARIABLE_EXPRESSION,
                                                                Schema.NAME,
                                                                indexName))));
        if (indexSetup == null) {
            return false;
        }
        // Verify that the loop's condition is idx < array.size.
        BaseSFVertex conditionVertex =
                SFVertexFactory.loadSingleOrNull(
                        g(),
                        g().V(getId())
                                .out(Schema.CHILD)
                                // The condition step must be the second child.
                                .has(NodeType.STANDARD_CONDITION, Schema.CHILD_INDEX, 1)
                                .out(Schema.CHILD)
                                // We're looking for a boolean expression...
                                .where(H.has(NodeType.BOOLEAN_EXPRESSION, Schema.OPERATOR, "<"))
                                .and(
                                        // ...where the first child is a VariableExpression for the
                                        // index variable.
                                        __.out(Schema.CHILD)
                                                .has(Schema.CHILD_INDEX, 0)
                                                .where(
                                                        H.has(
                                                                NodeType.VARIABLE_EXPRESSION,
                                                                Schema.NAME,
                                                                indexName)),
                                        // ...and the second child is a method call for teh array's
                                        // .size() method.
                                        __.out(Schema.CHILD)
                                                .has(Schema.CHILD_INDEX, 1)
                                                .where(
                                                        H.has(
                                                                NodeType.METHOD_CALL_EXPRESSION,
                                                                Schema.FULL_METHOD_NAME,
                                                                arrayName + ".size"))));
        if (conditionVertex == null) {
            return false;
        }

        // Verify that the iteration step increments the index variable by 1.
        BaseSFVertex indexIncrementer =
                SFVertexFactory.loadSingleOrNull(
                        g(),
                        g().V(getId())
                                .out(Schema.CHILD)
                                // The loop iteration step must be the third child.
                                .has(Schema.CHILD_INDEX, 2)
                                // Any of the following may be true.
                                .or(
                                        // A prefix/postfix operator increments the index variable.
                                        H.has(
                                                        Arrays.asList(
                                                                NodeType.PREFIX_EXPRESSION,
                                                                NodeType.POSTFIX_EXPRESSION),
                                                        Schema.OPERATOR,
                                                        "++")
                                                .out(Schema.CHILD)
                                                .where(
                                                        H.has(
                                                                NodeType.VARIABLE_EXPRESSION,
                                                                Schema.NAME,
                                                                indexName)),
                                        // The index variable is incremented using the `x += 1` or
                                        // `x = x + 1` syntax.
                                        H.hasWithin(
                                                        NodeType.ASSIGNMENT_EXPRESSION,
                                                        Schema.OPERATOR,
                                                        "+=",
                                                        "=")
                                                .and(
                                                        __.out(Schema.CHILD)
                                                                .has(Schema.CHILD_INDEX, 0)
                                                                .where(
                                                                        H.has(
                                                                                NodeType
                                                                                        .VARIABLE_EXPRESSION,
                                                                                Schema.NAME,
                                                                                indexName)),
                                                        __.out(Schema.CHILD)
                                                                .has(Schema.CHILD_INDEX, 1)
                                                                .where(
                                                                        H.has(
                                                                                NodeType
                                                                                        .BINARY_EXPRESSION,
                                                                                Schema.OPERATOR,
                                                                                "+"))
                                                                .and(
                                                                        __.out(Schema.CHILD)
                                                                                .where(
                                                                                        H.has(
                                                                                                NodeType
                                                                                                        .VARIABLE_EXPRESSION,
                                                                                                Schema
                                                                                                        .NAME,
                                                                                                indexName)),
                                                                        __.out(Schema.CHILD)
                                                                                .where(
                                                                                        H.has(
                                                                                                NodeType
                                                                                                        .LITERAL_EXPRESSION,
                                                                                                Schema
                                                                                                        .VALUE,
                                                                                                "1"))))));
        if (indexIncrementer == null) {
            return false;
        }

        // Verify that neither the index variable nor the array are ever re-assigned during the
        // loop.
        List<BaseSFVertex> midLoopReassignments =
                SFVertexFactory.loadVertices(
                        g(),
                        g().V(getId())
                                .out(Schema.CHILD)
                                // The contents of the loop must be the fourth child.
                                .has(NodeType.BLOCK_STATEMENT, Schema.CHILD_INDEX, 3)
                                // Drill down into the children, looking for anything that's a
                                // reassignment.
                                .repeat(__.out(Schema.CHILD))
                                .until(__.hasLabel(NodeType.ASSIGNMENT_EXPRESSION))
                                .out(Schema.CHILD)
                                .has(NodeType.VARIABLE_EXPRESSION, Schema.FIRST_CHILD, true)
                                .where(
                                        H.hasWithin(
                                                NodeType.VARIABLE_EXPRESSION,
                                                Schema.NAME,
                                                arrayName,
                                                indexName)));
        if (!midLoopReassignments.isEmpty()) {
            return false;
        }

        // If we're here, then all of the conditions for a for-loop are satisfied.
        return true;
    }
}
