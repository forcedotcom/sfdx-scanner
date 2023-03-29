package com.salesforce.graph.ops.expander;

import com.salesforce.exception.SfgeRuntimeException;

/** Base class for all runtime exceptions thrown by ApexPathExpander related code */
public abstract class ApexPathExpanderRuntimeException extends SfgeRuntimeException
        implements PathExpansionException {
    protected ApexPathExpanderRuntimeException(String msg) {
        super(msg);
    }
}
