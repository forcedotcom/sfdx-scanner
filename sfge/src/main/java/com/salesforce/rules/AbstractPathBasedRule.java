package com.salesforce.rules;

import com.salesforce.graph.ops.expander.PathExpansionObserver;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.vertex.MethodVertex;
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
    public abstract List<ApexPathSource.Type> getSourceTypes();

    /** Indicates whether this rule considers the specified method as a source of interest. */
    public boolean methodIsPotentialSource(MethodVertex methodVertex) {
        return ApexPathSource.isPotentialSource(methodVertex, getSourceTypes());
    }
}
