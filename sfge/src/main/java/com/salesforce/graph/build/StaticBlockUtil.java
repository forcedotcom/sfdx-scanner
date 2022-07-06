package com.salesforce.graph.build;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.MethodUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Handles creation of synthetic methods and vertices to gracefully invoke static code blocks.
 *
 * <p>Consider this example: <code>
 * class StaticBlockClass {
 *  static {
 *      System.debug("inside static block 1");
 *  }
 *  static {
 *      System.debug("inside static block 2");
 *  }
 * }
 * </code> In Jorje's compilation structure, static blocks are represented like this: <code>
 * class StaticBlockClass {
 *  private static void <clinit>() {
 *      {
 *          System.debug("inside static block 1");
 *      }
 *      {
 *          System.debug("inside static block 2");
 *      }
 *   }
 * }
 * </code>
 *
 * <p>Having multiple block statements inside a method breaks SFGE's normal code flow logic. This
 * makes handling code blocks in <code><clinit>()</code> impossible.
 *
 * <p>As an alternative, we are creating synthetic vertices in the Graph to represent the static
 * blocks as individual methods.
 *
 * <p>We also create one top-level synthetic method ("StaticBlockInvoker") that invokes individual
 * static block methods. While creating static scope for this class, we should invoke the method
 * call expressions inside the top-level synthetic method.
 *
 * <p>New structure looks like this: <code>
 *     class StaticBlockClass {
 *      private static void SyntheticStaticBlock_1() {
 *          System.debug("inside static block 1");
 *     }
 *     private static void SyntheticStaticBlock_2() {
 *          System.debug("inside static block 2");
 *     }
 *     private static void StaticBlockInvoker() {
 *          SyntheticStaticBlock_1();
 *          SyntheticStaticBlock_2();
 *     }
 * }
 * </code>
 */
public final class StaticBlockUtil {
    private static final Logger LOGGER = LogManager.getLogger(StaticBlockUtil.class);

    static final String SYNTHETIC_STATIC_BLOCK_METHOD_NAME = "SyntheticStaticBlock_%d";
    static final String STATIC_BLOCK_INVOKER_METHOD = "StaticBlockInvoker";

    private StaticBlockUtil() {}

    /**
     * Creates a synthetic method vertex to represent a static code block
     *
     * @param g traversal graph
     * @param clinitVertex <clinit>() method's vertex
     * @param staticBlockIndex index to use for name uniqueness - TODO: this index is currently just
     *     the child count index and does not necessarily follow sequence
     * @return new synthetic method vertex with name SyntheticStaticBlock_%d, which will be the
     *     parent for blockStatementVertex, and a sibling of <clinit>()
     */
    public static Vertex createSyntheticStaticBlockMethod(
            GraphTraversalSource g, Vertex clinitVertex, int staticBlockIndex) {
        final Vertex syntheticMethodVertex = g.addV(ASTConstants.NodeType.METHOD).next();
        final String definingType = clinitVertex.value(Schema.DEFINING_TYPE);
        final List<Vertex> siblings =
                GremlinUtil.getChildren(g, GremlinVertexUtil.getParentVertex(g, clinitVertex));
        final int nextSiblingIndex = siblings.size();

        addSyntheticStaticBlockMethodProperties(
                g, definingType, syntheticMethodVertex, staticBlockIndex, nextSiblingIndex);

        final Vertex modifierNodeVertex = g.addV(ASTConstants.NodeType.MODIFIER_NODE).next();
        addStaticModifierProperties(g, definingType, modifierNodeVertex);
        GremlinVertexUtil.addParentChildRelationship(g, syntheticMethodVertex, modifierNodeVertex);

        GremlinVertexUtil.makeSiblings(g, clinitVertex, syntheticMethodVertex);

        return syntheticMethodVertex;
    }

    /**
     * If rootNode contains synthetic static block methods (created through {@link
     * #createSyntheticStaticBlockMethod}), adds a top-level synthetic method that invokes each
     * static block method.
     */
    public static void createSyntheticStaticBlockInvocation(
            GraphTraversalSource g, Vertex rootNode) {
        // Check if root node contains any static block methods
        final List<Vertex> staticBlockMethods = getStaticBlockMethods(g, rootNode);

        // Create static block invocation method
        if (!staticBlockMethods.isEmpty()) {
            final String definingType = rootNode.value(Schema.DEFINING_TYPE);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Creating synthetic invocation method for {} to invoke {} static block(s)",
                        definingType,
                        staticBlockMethods.size());
            }

            final Vertex invokerMethodVertex =
                    createSyntheticInvocationsMethod(g, rootNode, staticBlockMethods, definingType);
            MethodPathBuilderVisitor.apply(g, invokerMethodVertex);
        }
    }

    static boolean isStaticBlockStatement(JorjeNode node, JorjeNode child) {
        return isClinitMethod(node) && containsStaticBlock(node) && isBlockStatement(child);
    }

    private static Vertex createSyntheticInvocationsMethod(
            GraphTraversalSource g,
            Vertex rootNode,
            List<Vertex> staticBlockMethods,
            String definingType) {
        // Create new synthetic method StaticBlockInvoker to invoke each synthetic static block
        // method
        final List<Vertex> siblings = GremlinUtil.getChildren(g, rootNode);
        final Vertex invokerMethodVertex = g.addV(ASTConstants.NodeType.METHOD).next();
        addStaticBlockInvokerProperties(g, definingType, invokerMethodVertex, siblings.size());
        GremlinVertexUtil.addParentChildRelationship(g, rootNode, invokerMethodVertex);

        final Vertex modifierNodeVertex = g.addV(ASTConstants.NodeType.MODIFIER_NODE).next();
        addStaticModifierProperties(g, definingType, modifierNodeVertex);
        GremlinVertexUtil.addParentChildRelationship(g, invokerMethodVertex, modifierNodeVertex);

        // Create synthetic BlockStatement inside StaticBlockInvoker to hold the method invocations
        final Vertex blockStatementInInvoker = g.addV(ASTConstants.NodeType.BLOCK_STATEMENT).next();
        addBlockStatementProperties(g, definingType, blockStatementInInvoker);
        GremlinVertexUtil.addParentChildRelationship(
                g, invokerMethodVertex, blockStatementInInvoker);

        for (int i = 0; i < staticBlockMethods.size(); i++) {
            final Vertex staticBlockMethod = staticBlockMethods.get(i);
            boolean isLastMethod = i == staticBlockMethods.size() - 1;
            // Create a method call invocation to the synthetic static block method
            final Vertex exprStaticBlockMethodCall =
                    createMethodCallExpression(g, definingType, staticBlockMethod, isLastMethod);

            // Add the expression statement containing the method call inside StaticBlockInvoker's
            // block statement
            GremlinVertexUtil.addParentChildRelationship(
                    g, blockStatementInInvoker, exprStaticBlockMethodCall);
        }

        return invokerMethodVertex;
    }

    private static Vertex createMethodCallExpression(
            GraphTraversalSource g,
            String definingType,
            Vertex staticBlockMethod,
            boolean isLastMethod) {
        final Vertex expressionStmt = g.addV(ASTConstants.NodeType.EXPRESSION_STATEMENT).next();
        addExpressionStmtProperties(g, definingType, expressionStmt, isLastMethod);

        // Create new MethodCallExpression for the synthetic static block method
        final Vertex staticBlockMethodCall =
                g.addV(ASTConstants.NodeType.METHOD_CALL_EXPRESSION).next();
        addMethodCallExpressionProperties(
                g, definingType, staticBlockMethod, staticBlockMethodCall);
        GremlinVertexUtil.addParentChildRelationship(g, expressionStmt, staticBlockMethodCall);

        // Create EmptyReferenceExpression inside MethodCallExpression
        final Vertex emptyMethodReference =
                g.addV(ASTConstants.NodeType.EMPTY_REFERENCE_EXPRESSION).next();
        addEmptyMethodReferenceProperties(g, definingType, emptyMethodReference);
        GremlinVertexUtil.addParentChildRelationship(
                g, staticBlockMethodCall, emptyMethodReference);
        return expressionStmt;
    }

    private static void addExpressionStmtProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex expressionStmt,
            boolean isLastMethod) {
        verifyType(expressionStmt, ASTConstants.NodeType.EXPRESSION_STATEMENT);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, true);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, expressionStmt, properties);
    }

    private static void addEmptyMethodReferenceProperties(
            GraphTraversalSource g, String definingType, Vertex emptyMethodReference) {
        verifyType(emptyMethodReference, ASTConstants.NodeType.EMPTY_REFERENCE_EXPRESSION);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, true);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, emptyMethodReference, properties);
    }

    private static void addMethodCallExpressionProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex staticBlockMethod,
            Vertex staticBlockMethodCall) {
        verifyType(staticBlockMethodCall, ASTConstants.NodeType.METHOD_CALL_EXPRESSION);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.METHOD_NAME, staticBlockMethod.value(Schema.NAME));
        properties.put(Schema.FULL_METHOD_NAME, staticBlockMethod.value(Schema.NAME));
        properties.put(Schema.IS_STATIC_BLOCK_INVOCATION, true);
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, true);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, staticBlockMethodCall, properties);
    }

    private static void addBlockStatementProperties(
            GraphTraversalSource g, String definingType, Vertex blockStatementInInvoker) {
        verifyType(blockStatementInInvoker, ASTConstants.NodeType.BLOCK_STATEMENT);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.CHILD_INDEX, 1);
        properties.put(Schema.FIRST_CHILD, false);
        properties.put(Schema.LAST_CHILD, true);

        addProperties(g, blockStatementInInvoker, properties);
    }

    private static void addStaticModifierProperties(
            GraphTraversalSource g, String definingType, Vertex modifier) {
        verifyType(modifier, ASTConstants.NodeType.MODIFIER_NODE);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.STATIC, true);
        properties.put(Schema.ABSTRACT, false);
        properties.put(Schema.GLOBAL, false);
        properties.put(Schema.MODIFIERS, 8); // Apparently, static methods have modifiers 8
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, false);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, modifier, properties);
    }

    private static void addStaticBlockInvokerProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex staticBlockInvokerVertex,
            int childIndex) {
        verifyType(staticBlockInvokerVertex, ASTConstants.NodeType.METHOD);
        final Map<String, Object> properties = new HashMap<>();
        properties.put(Schema.NAME, STATIC_BLOCK_INVOKER_METHOD);
        properties.put(Schema.IS_STATIC_BLOCK_INVOKER_METHOD, true);
        properties.put(Schema.CHILD_INDEX, childIndex);
        addCommonSynthMethodProperties(g, definingType, staticBlockInvokerVertex, properties);
    }

    private static void addSyntheticStaticBlockMethodProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex syntheticMethodVertex,
            int staticBlockIndex,
            int childIndex) {
        verifyType(syntheticMethodVertex, ASTConstants.NodeType.METHOD);
        final Map<String, Object> properties = new HashMap<>();
        properties.put(
                Schema.NAME, String.format(SYNTHETIC_STATIC_BLOCK_METHOD_NAME, staticBlockIndex));
        properties.put(Schema.IS_STATIC_BLOCK_METHOD, true);
        properties.put(Schema.CHILD_INDEX, childIndex);

        addCommonSynthMethodProperties(g, definingType, syntheticMethodVertex, properties);
    }

    private static void addCommonSynthMethodProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex staticBlockInvokerVertex,
            Map<String, Object> properties) {
        properties.put(Schema.ARITY, 0);
        properties.put(Schema.CONSTRUCTOR, false);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.RETURN_TYPE, ASTConstants.TYPE_VOID);

        addProperties(g, staticBlockInvokerVertex, properties);
    }

    private static void addProperties(
            GraphTraversalSource g, Vertex vertex, Map<String, Object> properties) {
        final TreeSet<String> previouslyInsertedKeys = CollectionUtil.newTreeSet();
        final GraphTraversal<Vertex, Vertex> traversal = g.V(vertex.id());

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            GremlinVertexUtil.addProperty(
                    previouslyInsertedKeys, traversal, entry.getKey(), entry.getValue());
            previouslyInsertedKeys.add(entry.getKey());
        }

        // Commit the changes.
        traversal.next();
    }

    /** @return true if given node represents <clinit>() */
    private static boolean isClinitMethod(JorjeNode node) {
        return ASTConstants.NodeType.METHOD.equals(node.getLabel())
                && MethodUtil.STATIC_CONSTRUCTOR_CANONICAL_NAME.equals(
                        node.getProperties().get(Schema.NAME));
    }

    /** @return true if <clinit>() node contains any static block definitions */
    private static boolean containsStaticBlock(JorjeNode node) {
        for (JorjeNode childNode : node.getChildren()) {
            if (ASTConstants.NodeType.BLOCK_STATEMENT.equals(childNode.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private static List<Vertex> getStaticBlockMethods(GraphTraversalSource g, Vertex root) {
        return g.V(root)
                .out(Schema.CHILD)
                .hasLabel(ASTConstants.NodeType.METHOD)
                .has(Schema.IS_STATIC_BLOCK_METHOD, true)
                .toList();
    }

    private static boolean isBlockStatement(JorjeNode child) {
        return ASTConstants.NodeType.BLOCK_STATEMENT.equals(child.getLabel());
    }

    private static void verifyType(Vertex vertex, String expectedType) {
        if (!expectedType.equals(vertex.label())) {
            throw new ProgrammingException("Incorrect vertex type: " + vertex.label());
        }
    }
}
