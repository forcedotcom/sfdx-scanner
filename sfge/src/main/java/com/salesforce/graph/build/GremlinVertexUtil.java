package com.salesforce.graph.build;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/** Handles common operations performed while creating vertices on Graph */
public final class GremlinVertexUtil {
    private static final Logger LOGGER = LogManager.getLogger(GremlinVertexUtil.class);

    private GremlinVertexUtil() {}

    /** Create parent-child relationship between vertices on graph */
    static void addParentChildRelationship(
            GraphTraversalSource g, Vertex parentVertex, Vertex childVertex) {
        // We are currently adding PARENT and CHILD, in theory the same edge could be navigated
        // both ways, however
        // the code looks messy when that is done.
        // TODO: Determine if this causes performance or resource issues. Consider a single edge
        g.addE(Schema.PARENT).from(childVertex).to(parentVertex).iterate();
        g.addE(Schema.CHILD).from(parentVertex).to(childVertex).iterate();
    }

    /** Make a synthetic vertex a sibling of an existing vertex on graph */
    static void makeSiblings(GraphTraversalSource g, Vertex vertex, Vertex syntheticVertex) {
        // Get parent node of vertex
        final Optional<Vertex> rootVertex = GremlinUtil.getParent(g, vertex);
        if (rootVertex.isEmpty()) {
            throw new UnexpectedException(
                    "Did not expect vertex to not have a parent vertex. vertex=" + vertex);
        }

        addParentChildRelationship(g, rootVertex.get(), syntheticVertex);
    }

    /** Add a property to the traversal, throwing an exception if any keys are duplicated. */
    protected static void addProperty(
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
