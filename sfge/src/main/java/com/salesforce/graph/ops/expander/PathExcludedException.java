package com.salesforce.graph.ops.expander;

import com.salesforce.graph.vertex.BaseSFVertex;

/**
 * Thrown by {@link ApexPathExcluder}s when a path should be excluded. The members are made
 * available for debugging, but not used for influencing behavior.
 */
public class PathExcludedException extends ApexPathExpanderException {
    private final ApexPathExcluder apexPathExcluder;
    private final BaseSFVertex evaluatedVertex;
    private final BaseSFVertex valueVertex;

    /**
     * @param apexPathExcluder that threw the exception
     * @param evaluatedVertex that was examined by the excluder before throwing the exception
     * @param valueVertex the vertex within the standard condition that was examined
     */
    public PathExcludedException(
            ApexPathExcluder apexPathExcluder,
            BaseSFVertex evaluatedVertex,
            BaseSFVertex valueVertex) {
        this.apexPathExcluder = apexPathExcluder;
        this.evaluatedVertex = evaluatedVertex;
        this.valueVertex = valueVertex;
    }

    @Override
    public String toString() {
        return "PathExcludedException{"
                + ", apexPathExcluder="
                + apexPathExcluder.getClass().getSimpleName()
                + ", evaluatedVertex="
                + evaluatedVertex
                + ", valueVertex="
                + valueVertex
                + "}";
    }
}
