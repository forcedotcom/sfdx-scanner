package com.salesforce.graph.build;

import com.google.common.collect.ImmutableSet;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Base class used by subclasses to import Apex Standard Classes and Custom supplied classes into
 * the graph database.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class AbstractApexVertexBuilder {
    private static final Logger LOGGER = LogManager.getLogger(AbstractApexVertexBuilder.class);
    protected static final Set<String> ROOT_VERTICES =
            ImmutableSet.<String>builder()
                    .addAll(Arrays.asList(ASTConstants.NodeType.ROOT_VERTICES))
                    .build();
    protected final GraphTraversalSource g;

    protected AbstractApexVertexBuilder(GraphTraversalSource g) {
        this.g = g;
    }

    void buildVertices(JorjeNode node, String fileName) {
        buildVertices(node, null, fileName);
    }

    private void buildVertices(JorjeNode node, Vertex vNodeParam, String fileName) {
        // Keep track when this is the initial root node for the file. #afterFileInsert is only
        // called
        // for this node.
        final Vertex rootVNode;
        final Vertex vNode;

        // It's possible for this method to be called on vertices that don't exist yet. If that's
        // the case, create a
        // corresponding vertex.
        if (vNodeParam == null) {
            vNode = g.addV(node.getLabel()).next();
            rootVNode = vNode;
        } else {
            rootVNode = null;
            vNode = vNodeParam;
        }

        addProperties(g, node, vNode);

        // If the source name is available, use it to populate the corresponding property.
        if (!StringUtils.isEmpty(fileName)) {
            // The engine assumes that only certain types of nodes can be roots. We need to enforce
            // that assumption and
            // fail noisily if it's violated.
            if (!ROOT_VERTICES.contains(vNode.label())) {
                throw new UnexpectedException("Unexpected root vertex of type " + vNode.label());
            }
            vNode.property(Schema.FILE_NAME, fileName);
        }

        Vertex vPreviousSibling = null;
        for (JorjeNode child : node.getChildren()) {
            Vertex vChild = g.addV(child.getLabel()).next();
            addProperties(g, child, vChild);
            // We are currently adding PARENT and CHILD, in theory the same edge could be navigated
            // both ways, however
            // the code looks messy when that is done.
            // TODO: Determine if this causes performance or resource issues. Consider a single edge
            g.addE(Schema.PARENT).from(vChild).to(vNode).iterate();
            g.addE(Schema.CHILD).from(vNode).to(vChild).iterate();
            if (vPreviousSibling != null) {
                g.addE(Schema.NEXT_SIBLING).from(vPreviousSibling).to(vChild).iterate();
            }
            vPreviousSibling = vChild;
            // To save memory in the graph, don't pass the source name into recursive calls.
            buildVertices(child, vChild, null);
        }
        afterInsert(g, node, vNode);
        if (rootVNode != null) {
            // Only call this for the root node
            afterFileInsert(g, rootVNode);
        }
    }

    /**
     * Adds edges to Method vertices after they are inserted. TODO: Replace this with a listener or
     * visitor pattern for more general purpose solutions
     */
    private final void afterInsert(GraphTraversalSource g, JorjeNode node, Vertex vNode) {
        if (node.getLabel().equals(ASTConstants.NodeType.METHOD)) {
            MethodPathBuilderVisitor.apply(g, vNode);
        }
    }

    /**
     * Invoked after a file is completely processed
     *
     * @param vNode root node that corresponds to the file
     */
    protected void afterFileInsert(GraphTraversalSource g, Vertex vNode) {
        // Intentionally left blank
    }

    protected void addProperties(GraphTraversalSource g, JorjeNode node, Vertex vNode) {
        GraphTraversal<Vertex, Vertex> traversal = g.V(vNode.id());

        TreeSet<String> previouslyInsertedKeys = CollectionUtil.newTreeSet();
        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            value = adjustPropertyValue(node, key, value);
            addProperty(previouslyInsertedKeys, traversal, key, value);
        }

        for (Map.Entry<String, Object> entry : getAdditionalProperties(node).entrySet()) {
            addProperty(previouslyInsertedKeys, traversal, entry.getKey(), entry.getValue());
        }

        // Commit the changes.
        traversal.next();
    }

    /** Allow subclasses to modify the stored value */
    protected Object adjustPropertyValue(JorjeNode node, String key, Object value) {
        return value;
    }

    /** Add additional properties to the node that aren't present in the orginal AST */
    protected Map<String, Object> getAdditionalProperties(JorjeNode node) {
        return new HashMap<>();
    }

    /** Add a property to the traversal, throwing an exception if any keys are duplicated. */
    protected void addProperty(
            TreeSet<String> previouslyInsertedKeys,
            GraphTraversal<Vertex, Vertex> traversal,
            String keyParam,
            Object value) {
        final String key = keyParam.intern();

        if (!previouslyInsertedKeys.add(key)) {
            throw new UnexpectedException(key);
        }

        if (value instanceof List) {
            List list = (List) value;
            // Convert ArrayList to an Array. There seems to be a Tinkerpop bug where a singleton
            // ArrayList
            // isn't properly stored. Remote graphs also have issues with empty lists, so don't add
            // an empty list.
            if (!list.isEmpty()) {
                traversal.property(key, list.toArray());
            } else {
                // return so that we don't store a case insensitive version
                return;
            }
        } else if (value instanceof Boolean
                || value instanceof Double
                || value instanceof Integer
                || value instanceof Long
                || value instanceof String) {
            traversal.property(key, value);
        } else {
            if (value != null) {
                if (!(value instanceof Enum)) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(
                                "Using string for value. type=" + value.getClass().getSimpleName());
                    }
                }
                final String strValue = String.valueOf(value).intern();
                traversal.property(key, strValue);
            }
        }
        CaseSafePropertyUtil.addCaseSafeProperty(traversal, key, value);
    }
}
