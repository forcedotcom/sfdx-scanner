package com.salesforce.graph.build;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.Schema;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Utility class for the standardized addition of properties to Synthetic graph vertices.
 *
 * <p>TODO: Parts of {@link StaticBlockUtil} can be refactored into this class.
 *
 * <p>TODO: This class has redundant code that could stand to be cleaned up.
 */
public class VertexSynthesisUtil {

    public static void addBlockStmtProperties(
            GraphTraversalSource g, String definingType, Vertex blockStmt, boolean isStandard) {
        verifyType(blockStmt, NodeType.BLOCK_STATEMENT);

        final Map<String, Object> properties = new HashMap<>();
        if (isStandard) {
            properties.put(Schema.IS_STANDARD, true);
        }
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        // ASSUMPTION: We're only ever adding synthetic block statements to the tail of their
        // parent.
        properties.put(Schema.CHILD_INDEX, 1);
        properties.put(Schema.FIRST_CHILD, false);
        properties.put(Schema.LAST_CHILD, true);

        addProperties(g, blockStmt, properties);
    }

    public static void addExpressionStmtProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex expressionStmt,
            boolean isLastStmt,
            boolean isStandard) {
        verifyType(expressionStmt, NodeType.EXPRESSION_STATEMENT);

        final Map<String, Object> properties = new HashMap<>();
        if (isStandard) {
            properties.put(Schema.IS_STANDARD, true);
        }
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, isLastStmt);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, expressionStmt, properties);
    }

    public static void addSuperMethodCallExpressionProperties(
            GraphTraversalSource g,
            String definingType,
            Vertex superMethodCallExpr,
            boolean isLastStmt,
            boolean isStandard) {
        verifyType(superMethodCallExpr, NodeType.SUPER_METHOD_CALL_EXPRESSION);

        final Map<String, Object> properties = new HashMap<>();
        if (isStandard) {
            properties.put(Schema.IS_STANDARD, true);
        }
        properties.put(Schema.IS_SYNTHETIC, true);
        properties.put(Schema.DEFINING_TYPE, definingType);
        properties.put(Schema.FIRST_CHILD, true);
        properties.put(Schema.LAST_CHILD, isLastStmt);
        properties.put(Schema.CHILD_INDEX, 0);

        addProperties(g, superMethodCallExpr, properties);
    }

    private static void addProperties(
            GraphTraversalSource g, Vertex vertex, Map<String, Object> properties) {
        // TODO: These should probably inherit from the parent vertex, long-term.
        properties.put(Schema.BEGIN_LINE, -1);
        properties.put(Schema.BEGIN_COLUMN, -1);
        properties.put(Schema.END_LINE, -1);
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

    private static void verifyType(Vertex vertex, String expectedType) {
        if (!expectedType.equals(vertex.label())) {
            throw new ProgrammingException("Incorrect vertex type: " + vertex.label());
        }
    }
}
