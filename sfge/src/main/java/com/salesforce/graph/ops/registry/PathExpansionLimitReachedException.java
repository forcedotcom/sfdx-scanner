package com.salesforce.graph.ops.registry;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.SfgeRuntimeException;

public class PathExpansionLimitReachedException extends SfgeRuntimeException {
    public PathExpansionLimitReachedException(int limit) {
        super(String.format(UserFacingMessages.PATH_EXPANSION_LIMIT_REACHED, limit));
    }
}
