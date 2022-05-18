package com.salesforce.graph.ops.expander.switches;

import com.salesforce.graph.vertex.CaseVertex;

/**
 * Determines if a {@link CaseVertex} found in a {@link
 * com.salesforce.graph.vertex.SwitchStatementVertex} evaluates to true for the expression supplied
 * to the switch statement.
 */
interface CaseVertexComparator {
    /**
     * @return true if the ApexValue supplied to the switch statement would cause {@code vertex } to
     *     execute
     */
    boolean valueSatisfiesVertex(CaseVertex vertex);
}
