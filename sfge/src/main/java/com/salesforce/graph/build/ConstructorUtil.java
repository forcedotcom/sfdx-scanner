package com.salesforce.graph.build;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Handles the creation of synthetic vertices to allow graceful expansion of constructors.
 */
public final class ConstructorUtil {
    /** Util classes have private constructors. */
    private ConstructorUtil() {}

    /**
     * Consider this code:
     * <p><code>
     * public class ChildClass extends ParentClass {
     *     public ChildClass() {}
     * }
     * </code>
     *
     * <p>Since {@code ChildClass} extends {@code ParentClass}, and its constructor doesn't explicitly invoke a {@code this}
     * or {@code super} constructor, the default constructor for {@code ParentClass} is implicitly invoked.
     *
     * <p>This implicit invocation isn't represented in the Jorje-generated AST, so we need to add it ourselves.
     *
     * <p>This method allows us to detect when we're in such a scenario.
     * @return True if {@code node} represents the body of a constructor with an implicit {@code super} invocation.
     */
    public static boolean isImplicitlyDelegatedConstructorBody(GraphTraversalSource g, JorjeNode node) {
        // Is this node the body of a constructor?
        return isConstructorBody(node)
            // Does it construct a class that extends some other class?
                && constructsExtensionClass(g, node)
            // Does it include an explicit delegation of some other constructor?
                && lacksConstructorDelegation(node);
    }

    /**
     * Adds a synthetic child to {@code constructorBodyVertex} to directly capture the implicit invocation
     * of a {@code super()} constructor, as described in {@link #isImplicitlyDelegatedConstructorBody}.
     * @param constructorBodyNode A node that returned true for {@link #isImplicitlyDelegatedConstructorBody}
     * @param constructorBodyVertex The graph vertex corresponding to {@code constructorBodyNode}
     * @return The synthetic child added
     */
    public static Vertex synthesizeSuperCall(
            GraphTraversalSource g, JorjeNode constructorBodyNode, Vertex constructorBodyVertex) {
        String definingType = constructorBodyVertex.value(Schema.DEFINING_TYPE);

        Vertex expressionStatement = g.addV(NodeType.EXPRESSION_STATEMENT).next();
        VertexSynthesisUtil.addExpressionStmtProperties(
                g, definingType, expressionStatement, constructorBodyNode.getChildren().isEmpty());

        Vertex superMethodCallExpression = g.addV(NodeType.SUPER_METHOD_CALL_EXPRESSION).next();
        VertexSynthesisUtil.addSuperMethodCallExpressionProperties(
                g, definingType, superMethodCallExpression, true);

        GremlinVertexUtil.addParentChildRelationship(
                g, expressionStatement, superMethodCallExpression);

        GremlinVertexUtil.addParentChildRelationship(g, constructorBodyVertex, expressionStatement);

        return expressionStatement;
    }

    /**
     *
     * @return True if {@code node} is a {@code BlockStatement} representing the body of a constructor.
     */
    private static boolean isConstructorBody(JorjeNode node) {
        Optional<JorjeNode> parentOptional = node.getParent();
        if (!parentOptional.isPresent()) {
            return false;
        }
        JorjeNode parent = parentOptional.get();
        return NodeType.BLOCK_STATEMENT.equals(node.getLabel())
                && NodeType.METHOD.equals(parent.getLabel())
                && Schema.INSTANCE_CONSTRUCTOR_CANONICAL_NAME.equals(
                        parent.getProperties().get(Schema.NAME));
    }

    /**
     * @return True if {@code node} is part of a class that extends some other class.
     */
    private static boolean constructsExtensionClass(GraphTraversalSource g, JorjeNode node) {
        String definingType = node.getDefiningType();
        UserClassVertex userClassVertex =
                SFVertexFactory.loadSingleOrNull(
                        g,
                        g.V()
                                .hasLabel(NodeType.USER_CLASS)
                                .has(Schema.DEFINING_TYPE, definingType));
        return userClassVertex != null && userClassVertex.getSuperClassName().isPresent();
    }

    /**
     *
     * @return True if {@node} has no children representing an invocation of {@code super} or {@code this}.
     */
    private static boolean lacksConstructorDelegation(JorjeNode node) {
        if (node.getChildren().isEmpty()) {
            return true;
        }
        JorjeNode firstChild = node.getChildren().get(0);
        if (!NodeType.EXPRESSION_STATEMENT.equals(firstChild.getLabel())) {
            return true;
        }
        if (firstChild.getChildren().isEmpty()) {
            return false;
        }
        String firstGrandchildLabel = firstChild.getChildren().get(0).getLabel();
        return !(firstGrandchildLabel.equals(NodeType.SUPER_METHOD_CALL_EXPRESSION)
                || firstGrandchildLabel.equals(NodeType.THIS_METHOD_CALL_EXPRESSION));
    }
}
