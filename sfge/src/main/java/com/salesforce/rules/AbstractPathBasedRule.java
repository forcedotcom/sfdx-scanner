package com.salesforce.rules;

import com.google.common.collect.ImmutableSet;
import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.MethodVertex;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract parent class for rules whose execution requires the construction and/or traversal of
 * {@link com.salesforce.graph.ApexPath} instances.
 */
public abstract class AbstractPathBasedRule extends AbstractRule {

    /**
     * Allows rules to provide a {@link PathExpansionObserver} to gather data during path expansion.
     */
    public Optional<PathExpansionObserver> getPathExpansionObserver() {
        return Optional.empty();
    }

    /** Allows rules to indicate what types of path sources they are interested in. */
    public abstract ImmutableSet<ApexPathSource.Type> getSourceTypes();

    /** Indicates whether this rule considers the specified method as a source of interest. */
    public boolean methodIsPotentialSource(MethodVertex methodVertex) {
        return ApexPathSource.isPotentialSource(methodVertex, getSourceTypes());
    }

    /**
     * This method will be invoked after every entrypoint has been fully evaluated, allowing for rules to create
     * violations based on the run as a whole, rather than against individual paths.
     */
    public List<Violation> postProcess(GraphTraversalSource g) {
        return Collections.emptyList();
    }
}
