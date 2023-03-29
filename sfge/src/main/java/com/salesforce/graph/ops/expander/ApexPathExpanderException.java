package com.salesforce.graph.ops.expander;

import com.salesforce.exception.SfgeException;

/** Base class for all checked exceptions thrown by ApexPathExpander related code */
public abstract class ApexPathExpanderException extends SfgeException
        implements PathExpansionException {
    ApexPathExpanderException() {}

    ApexPathExpanderException(String message) {
        super(message);
    }
}
