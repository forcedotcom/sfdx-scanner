package com.salesforce.graph.ops.expander;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.StandardConditionVertex;

/**
 * Adds constraints to indeterminant ApexValues that are found in an if/else condition. For example
 * the following if statement on an indeterminant 'x' value will set a Positive Null constraint on
 * the path that takes the first if branch and a Negative Null constraint on the path that takes the
 * first else branch. The {@link BooleanValuePathConditionExcluder} will use this information to
 * constrain the paths to only the if/if and else/else combination, instead of the 4 paths that
 * would normally be possible given two if conditions.
 *
 * <pre>
 *
 * if (x == null) {
 * } else {
 * }
 *
 * if (x == null) {
 * } else {
 * }
 *
 * </pre>
 *
 * Implementations of this interface should only be used in conjunction with {@link
 * ApexPathStandardConditionExcluder} objects. The additional information added by
 * ApexValueConstrainers allows the excluder to exclude paths that are inconsistent. Using
 * ApexValueConstrainers without the excluder could cause the ApexValueConstrainer to set
 * contradictory constraints.
 */
public interface ApexValueConstrainer {
    void constrain(StandardConditionVertex vertex, SymbolProvider symbols);
}
