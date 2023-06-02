package com.salesforce.rules.ops.boundary;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.UnexpectedException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tracks the beginning and end of boundaries. Each implementation of a {@link BoundaryDetector}
 * works with a specific type of {@link Boundary}. Use {@link #peek()} to know if a boundary is
 * currently active.
 *
 * @param <T> Boundary type associated with the implementation.
 */
public abstract class BoundaryDetector<T extends Boundary<R>, R> {
    private static final Logger LOGGER = LogManager.getLogger(BoundaryDetector.class);
    protected final Stack<T> boundaries;

    protected BoundaryDetector() {
        this.boundaries = new Stack<>();
    }

    public Optional<T> peek() {
        return CollectionUtil.peek(boundaries);
    }

    /**
     * Start extent of a new boundary
     *
     * @param boundary item that governs this extent.
     */
    public void pushBoundary(T boundary) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entering loop boundary with boundaryItem=" + boundary.getBoundaryItem());
        }
        this.boundaries.push(boundary);
    }

    /**
     * End extent of a boundary. Performs additional checks to confirm validity.
     *
     * @param boundaryItem that is expected to govern the current boundary that's to be ended.
     */
    public void popBoundary(R boundaryItem) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exiting boundary with boundaryItem=" + boundaryItem);
        }

        // Check if a boundary is actually in place
        Optional<T> boundaryOptional = peek();
        if (!boundaryOptional.isPresent()) {
            throw new ProgrammingException(
                    "No boundary has been pushed to get a corresponding pop.");
        }

        // Check if the existing boundary has the same boundary item as the end item
        R existingBoundaryItem = boundaryOptional.get().getBoundaryItem();
        if (!boundaryItem.equals(existingBoundaryItem)) {
            throw new UnexpectedException(
                    "Boundary vertices don't match. existingBoundaryItem="
                            + existingBoundaryItem
                            + ", afterVisit boundaryItem="
                            + boundaryItem);
        }

        // Remove the boundary
        this.boundaries.pop();
    }

    /**
     * @return items in the boundary stack. Items are ordered TODO ?
     */
    public List<T> getBoundaryItems() {
        return boundaries.stream().collect(Collectors.toList());
    }
}
