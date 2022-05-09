package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.ops.expander.ApexValueConstrainer;

/**
 * Represents a constraint that was added to an ApexValue. See {@link ApexValueConstrainer}. All
 * constraints are positive or negative. A negative null constraint guarantees the value is
 * non-null. A positive null constraint guarantees the value is null.
 */
public enum Constraint {
    /** Guarantees about the emptiness of a collection */
    Empty,
    /** Guarantees about the null state of a variable */
    Null
}
