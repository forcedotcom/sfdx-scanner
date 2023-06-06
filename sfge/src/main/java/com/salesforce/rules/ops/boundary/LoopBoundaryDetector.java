package com.salesforce.rules.ops.boundary;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.vertex.SFVertex;
import java.util.Optional;
import java.util.Stack;

/** Implementation of boundary detection for loops. */
public class LoopBoundaryDetector extends BoundaryDetector<LoopBoundary, SFVertex> {

    /**
     * Check if the visitor is currently inside a loop. Takes exclusion boundary into account.
     *
     * @return Optional of the last effective boundary vertex. If none exist, return empty to
     *     indicate that exclusion is effective.
     */
    public Optional<? extends SFVertex> isInsideLoop() {
        // TODO: This can be done without the copy stack as well, but it adds more complexity
        //  than I'm willing to handle at the moment. Add more tests if revisiting.
        final Stack<LoopBoundary> copyStack = new Stack<>();
        copyStack.addAll(super.boundaries);
        return this._isInsideLoop(copyStack);
    }

    private Optional<? extends SFVertex> _isInsideLoop(
            Stack<? extends LoopBoundary> boundaryStack) {
        final Optional<? extends LoopBoundary> loopBoundaryOpt = CollectionUtil.peek(boundaryStack);
        if (loopBoundaryOpt.isPresent()) {
            final LoopBoundary loopBoundary = loopBoundaryOpt.get();
            // Check if we are inside a loop exclusion.

            if (loopBoundary instanceof PermanentLoopExclusionBoundary) {
                // All the previous boundaries don't matter
                return Optional.empty();
            } else if (loopBoundary instanceof OverridableLoopExclusionBoundary) {
                // Pop out the OverridableLoopExclusionBoundary.
                boundaryStack.pop();
                // Pop out the corresponding ForEach Loop Boundary.
                boundaryStack.pop();
                return _isInsideLoop(boundaryStack);
            } else {
                // Inside a loop.
                return Optional.of(loopBoundary.getBoundaryItem());
            }
        }

        // Not inside a loop
        return Optional.empty();
    }
}
