package com.salesforce.graph.ops.registry;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.SfgeRuntimeException;

/**
 * Thrown when the number of objects in the registry exceed the limit that can be handled to
 * continue processing without ending in an OutOfMemory error.
 */
public class PathExpansionLimitReachedException extends SfgeRuntimeException {
    public PathExpansionLimitReachedException(int limit) {
        super(
                String.format(
                        UserFacingMessages.PathExpansionTemplates.PATH_EXPANSION_LIMIT_REACHED,
                        limit));
    }
}
