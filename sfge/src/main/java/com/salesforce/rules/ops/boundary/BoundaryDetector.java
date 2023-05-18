package com.salesforce.rules.ops.boundary;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.rules.ops.visitor.LoopDetectionVisitor;
import java.util.Optional;
import java.util.Stack;
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
    private static final Logger LOGGER = LogManager.getLogger(LoopDetectionVisitor.class);
    private final Stack<T> boundaries;

    protected BoundaryDetector() {
        this.boundaries = new Stack<>();
    }

    private void push(T boundary) {
        this.boundaries.push(boundary);
    }

    private T pop() {
        return this.boundaries.pop();
    }

    public Optional<T> peek() {
        if (boundaries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.boundaries.peek());
    }

    public void pushBoundary(T boundary) {
        LOGGER.debug("Entering loop boundary with boundaryItem=" + boundary.getBoundaryItem());
        push(boundary);
    }

    public void popBoundary(R boundaryItem) {
        LOGGER.debug("Exiting boundary with boundaryItem=" + boundaryItem);

        // Perform checks to ensure correctness of logic
        Optional<T> boundaryOptional = peek();
        if (!boundaryOptional.isPresent()) {
            throw new ProgrammingException(
                    "No boundary has been pushed to get a corresponding pop.");
        }

        R existingBoundaryItem = boundaryOptional.get().getBoundaryItem();
        if (!boundaryItem.equals(existingBoundaryItem)) {
            throw new UnexpectedException(
                    "Boundary vertices don't match. existingBoundaryItem="
                            + existingBoundaryItem
                            + ", afterVisit boundaryItem="
                            + boundaryItem);
        }

        pop();
    }
}
