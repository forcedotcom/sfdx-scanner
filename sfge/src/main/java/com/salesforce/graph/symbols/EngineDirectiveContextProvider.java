package com.salesforce.graph.symbols;

import com.salesforce.graph.ops.directive.EngineDirective;

/** Implemented by classes that accumulate {@link EngineDirective}s while walking a path. */
public interface EngineDirectiveContextProvider {
    EngineDirectiveContext getEngineDirectiveContext();
}
