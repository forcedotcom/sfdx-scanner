package com.salesforce.exception;

import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Thrown when an AST related assumption is violated. Some code still uses this where a {@link
 * TodoException} is more appropriate.
 */
public final class UnexpectedException extends SfgeRuntimeException {
    public UnexpectedException(Object obj) {
        super(obj != null ? obj.toString() : "<null>");
    }

    public UnexpectedException(GraphTraversalSource g, Vertex vertex, Throwable cause) {
        super(SFVertexFactory.load(g, vertex).toString(), cause);
    }

    /**
     * Log the properties of the vertex that caused the issue.
     *
     * @param vertex
     */
    public UnexpectedException(Vertex vertex) {
        super(
                StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(
                                        vertex.properties(), Spliterator.ORDERED),
                                false)
                        .map(p -> p.toString())
                        .collect(Collectors.joining(", ")));
    }

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
