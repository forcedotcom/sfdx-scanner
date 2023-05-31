package com.salesforce.rules.ops.boundary;

import com.salesforce.graph.vertex.SFVertex;
import java.util.Optional;

/** Implementation of boundary detection for loops. */
public class LoopBoundaryDetector extends BoundaryDetector<LoopBoundary, SFVertex> {
    public Optional<? extends SFVertex> isInsideLoop() {
        final Optional<LoopBoundary> loopBoundaryOpt = peek();
        if (loopBoundaryOpt.isPresent()) {
            // Check if we are inside a loop exclusion.
            if (loopBoundaryOpt.get() instanceof LoopExclusionBoundary) {
                // This is exclusion zone. We consider this not a loop.
                return Optional.empty();
            }

            // Inside a loop.
            return Optional.of(loopBoundaryOpt.get().getBoundaryItem());
        }

        // Not inside a loop
        return Optional.empty();
    }
}
