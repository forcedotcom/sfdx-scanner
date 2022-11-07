package com.salesforce.graph.ops.expander;

/**
 * Optimization to cut down on the number of objects that are kept around while walking all paths. A
 * caller that only cares about paths can discard the rest of the ApexPathExpander objects as soon
 * as the path is discovered
 */
abstract class ResultCollector {
    final void collect(ApexPathExpander pathExpander) {
        // Copy over information to each path which the expander has been accumulating
        // TODO: This is sharing information between the original path and initialization paths. I
        // think this is OK, but
        // we should keep an eye on it.
        pathExpander.getTopMostPath().setApexPathMetaInfo(pathExpander.getApexPathVertexMetaInfo());
        _collect(pathExpander);
    }

    /**
     * Remove the object that corresponds to {@code pathExpander} if it has been previously
     * collected. This is typically because the result has been collapsed and superceded by another
     * result.
     *
     * @return true if it was removed
     */
    abstract boolean remove(ApexPathExpander pathExpander);

    /** Overridden by subclasses to gather the information necessary for their task */
    abstract void _collect(ApexPathExpander pathExpander);

    /**
     * @return the number of objects collected so far
     */
    abstract int size();
}
