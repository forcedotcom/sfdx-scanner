package com.salesforce.rules;

import com.salesforce.graph.ops.expander.PathExpansionObserver;
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
}
