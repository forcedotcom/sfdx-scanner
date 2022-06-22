package com.salesforce.graph.build;

import com.google.common.collect.ImmutableSet;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.MethodUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

		final boolean needsStaticBlockSplHandling = isClintMethod(node) && containsStaticBlock(node);
        Vertex vPreviousSibling = null;
		final List<JorjeNode> children = node.getChildren();

		for (int i = 0; i < children.size(); i++) {
			final JorjeNode child = children.get(i);
			if (needsStaticBlockSplHandling
				&& ASTConstants.NodeType.BLOCK_STATEMENT.equals(child.getLabel())) {
				// todo: add static blocks as new methods
				vPreviousSibling = addStaticBlockAsNewMethod(vNode, vPreviousSibling, child, node, i);
			} else {
				vPreviousSibling = addChildNodeToGraph(vNode, vPreviousSibling, child);
			}
		}
        afterInsert(g, node, vNode);
        if (rootVNode != null) {
            // Only call this for the root node
            afterFileInsert(g, rootVNode);
        }
    }

	private Vertex addChildNodeToGraph(Vertex vNode, Vertex vPreviousSibling, JorjeNode child) {
		Vertex vChild = g.addV(child.getLabel()).next();
		addProperties(g, child, vChild);
		return addParentChildRelationship(vNode, vChild, child, vPreviousSibling);
	}

	private Vertex addParentChildRelationship(Vertex parentVertex, Vertex childVertex, JorjeNode child, Vertex vPreviousSibling) {
		addParentChildRelationship(parentVertex, childVertex);
		if (vPreviousSibling != null) {
			g.addE(Schema.NEXT_SIBLING).from(vPreviousSibling).to(childVertex).iterate();
		}
		vPreviousSibling = childVertex;
		// To save memory in the graph, don't pass the source name into recursive calls.
		buildVertices(child, childVertex, null);
		return vPreviousSibling;
	}

	private void addParentChildRelationship(Vertex parentVertex, Vertex childVertex) {
		// We are currently adding PARENT and CHILD, in theory the same edge could be navigated
		// both ways, however
		// the code looks messy when that is done.
		// TODO: Determine if this causes performance or resource issues. Consider a single edge
		g.addE(Schema.PARENT).from(childVertex).to(parentVertex).iterate();
		g.addE(Schema.CHILD).from(parentVertex).to(childVertex).iterate();
	}

	private Vertex addStaticBlockAsNewMethod(Vertex clintVertex, Vertex vPreviousSibling, JorjeNode child, JorjeNode clintNode, int staticBlockIndex) {
		// Add properties of <clint>() to syntheticMethodVertex,
		// override properties with a new name and an indicator to
		// show this is a synthetic static block method
		final Vertex syntheticMethodVertex = g.addV(ASTConstants.NodeType.METHOD).next();

		final HashMap<String, Object> overrides = new HashMap<>();
		overrides.put(Schema.NAME, String.format(MethodUtil.SYNTHETIC_STATIC_BLOCK_METHOD_NAME, staticBlockIndex));
		addProperties(g, clintNode, syntheticMethodVertex, overrides);

		// TODO: Add <clint> vertex's parent as Synthetic method vertex's parent
		//  how do i get <clint>'s parent vertex?

		final Optional<Vertex> grandParentVertex = GremlinUtil.getParent(g, clintVertex);
		if (grandParentVertex.isEmpty()) {
			throw new UnexpectedException("Did not expect <clint>() to not have a parent vertex. clintVertex=" + clintVertex);
		}

		addParentChildRelationship(grandParentVertex.get(), syntheticMethodVertex);

		// Create a vertex for BlockStatement
		final Vertex vChild = g.addV(child.getLabel()).next();
		addProperties(g, child, vChild);

		// add BlockStatement as child of SyntheticMethodVertex
		return addParentChildRelationship(syntheticMethodVertex, vChild, child, vPreviousSibling);
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
		addProperties(g, node, vNode, new HashMap<>());
	}

	protected void addProperties(GraphTraversalSource g, JorjeNode node, Vertex vNode, HashMap<String, Object> overrides) {
        GraphTraversal<Vertex, Vertex> traversal = g.V(vNode.id());

        TreeSet<String> previouslyInsertedKeys = CollectionUtil.newTreeSet();
        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            value = adjustPropertyValue(node, key, value);
			// Override value for key if requested
			if (overrides.containsKey(key)) {
				value = overrides.get(key);
			}
            addProperty(previouslyInsertedKeys, traversal, key, value);
        }

        for (Map.Entry<String, Object> entry : getAdditionalProperties(node, overrides).entrySet()) {
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
    protected Map<String, Object> getAdditionalProperties(JorjeNode node, HashMap<String, Object> overrides) {
		final HashMap<String, Object> returnMap = new HashMap<>();

		// FIXME: very crude
		if (!overrides.isEmpty()) {
			returnMap.put(Schema.IS_STATIC_BLOCK_METHOD, Boolean.valueOf(true));
		}
		return returnMap;
    }

	/**
	 * @return true if given node represents <clint>()
	 */
	private boolean isClintMethod(JorjeNode node) {
		return ASTConstants.NodeType.METHOD.equals(node.getLabel())
			&& MethodUtil.STATIC_CONSTRUCTOR_CANONICAL_NAME.equals(node.getProperties().get(Schema.NAME));
	}

	/**
	 * @return true if <clint>() node contains any static block definitions
	 */
	private boolean containsStaticBlock(JorjeNode node) {
		for (JorjeNode childNode : node.getChildren()) {
			if (ASTConstants.NodeType.BLOCK_STATEMENT.equals(childNode.getLabel())) {
				return true;
			}
		}
		return false;
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
