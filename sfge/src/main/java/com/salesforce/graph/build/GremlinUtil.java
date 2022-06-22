package com.salesforce.graph.build;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Low level gremlin utils that should be used when aiming for efficiency of only dealing with ids
 * and labels.
 */
public final class GremlinUtil {
    public static Long getId(Map<Object, Object> vertexProperties) {
        return (Long) vertexProperties.get(T.id);
    }

    public static String getLabel(Map<Object, Object> vertexProperties) {
        return (String) vertexProperties.get(T.label);
    }

    public static Optional<Vertex> getOnlyChild(
            GraphTraversalSource g, Vertex vertex, String childLabel) {
        List<Vertex> children = g.V(vertex).out(Schema.CHILD).hasLabel(childLabel).toList();

        if (children.isEmpty()) {
            return Optional.empty();
        } else if (children.size() > 1) {
            throw new UnexpectedException("Did not expect more than one child node of type " + childLabel +". Actual count: " + children.size());
        } else {
            return Optional.of(children.get(0));
        }
    }

    public static List<Vertex> getChildren(
            GraphTraversalSource g, Vertex vertex, int expectedSize) {
        final List<Vertex> children = GremlinUtil.getChildren(g, vertex);

        if (children.size() != expectedSize) {
            throw new UnexpectedException(vertex);
        }
        return children;
    }

    public static List<Vertex> getChildren(GraphTraversalSource g, Vertex vertex) {
        return g.V(vertex)
                .out(Schema.CHILD)
                .order(Scope.global)
                .by(Schema.CHILD_INDEX, Order.asc)
                .toList();
    }

	public static List<Vertex> getChildren(GraphTraversalSource g, Vertex vertex, String childLabel) {
		return g.V(vertex).out(Schema.CHILD).hasLabel(childLabel).toList();
	}

    public static Optional<Vertex> getPreviousSibling(GraphTraversalSource g, Vertex vertex) {
        Iterator<Vertex> it = g.V(vertex).in(Schema.NEXT_SIBLING);
        if (it.hasNext()) {
            return Optional.of(it.next());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Vertex> getNextSibling(GraphTraversalSource g, Vertex vertex) {
        Iterator<Vertex> it = g.V(vertex).out(Schema.NEXT_SIBLING);
        if (it.hasNext()) {
            return Optional.of(it.next());
        } else {
            return Optional.empty();
        }
    }

    public static boolean isEmpty(GraphTraversalSource g, Vertex vertex) {
        return !g.V(vertex).out(Schema.CHILD).hasNext();
    }

    public static Optional<Vertex> getFirstChild(GraphTraversalSource g, Vertex vertex) {
        Iterator<Vertex> it = g.V(vertex).out(Schema.CHILD).has(Schema.FIRST_CHILD, true);
        if (it.hasNext()) {
            return Optional.of(it.next());
        } else {
            return Optional.empty();
        }
    }

	public static Optional<Vertex> getParent(GraphTraversalSource g, Vertex vertex) {
		Iterator<Vertex> it = g.V(vertex).out(Schema.PARENT);
		if (it.hasNext()) {
			return Optional.of(it.next());
		} else {
			return Optional.empty();
		}
	}

    private GremlinUtil() {}
}
