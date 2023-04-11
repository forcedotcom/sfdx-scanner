package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ApexPath;

/**
 * Classes that implement this interface can be provided to {@link
 * ApexPathExpanderConfig.Builder#withPathExpansionObserver}, and their hooks will be called at the
 * appropriate points in path expansion.<br>
 * Useful for {@link com.salesforce.rules.AbstractPathBasedRule} implementations that need to know
 * what happens during path expansion.
 */
public interface PathExpansionObserver {

    /** Hook invoked every time an {@link ApexPath} instance is visited. */
    void onPathVisit(ApexPath path);
}
