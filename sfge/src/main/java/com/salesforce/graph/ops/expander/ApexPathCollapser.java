package com.salesforce.graph.ops.expander;

import com.salesforce.graph.ops.registry.Indexable;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;
import java.util.Optional;

/**
 * {@link ApexPathExpanderUtil} creates an instance of this class and passes it to the {@link
 * ApexPathExpander} instances. ApexPathExpanderUtil informs this class when paths fork,
 * ApexPathExpander informs this class when a method that caused a fork has returned a value. Any
 * paths with more than one result are may be collapsed depending on the result of invoking {@link
 * ApexDynamicPathCollapser#collapse(MethodVertex, List)}. The list of ApexDynamicPathCollapsers is
 * retrieved from the
 */
interface ApexPathCollapser extends Indexable {
    /**
     * Indicates that an ApexPathExpander has been forked. This will replace the original
     * ApexPathExpander with the new list
     *
     * @param forkEvent event that corresponds to the fork
     * @param originalExpander expander that was executing when the fork occurred
     * @param newExpanders the list of new expanders for the fork
     */
    void pathForked(
            ForkEvent forkEvent,
            ApexPathExpander originalExpander,
            List<ApexPathExpander> newExpanders);

    /**
     * Invoked by ApexPathExpander whenever a result is returned that corresponds to a method that
     * caused a fork. The return value will be compared with all other values returned from the same
     * ForkEvent to see if the paths can be collapsed.
     *
     * @param apexPathExpander currently executing expander
     * @param forkEvent the original fork event when the method was invoked
     * @param apexValue the value that was returned as a result of the method being invoked
     * @throws PathCollapsedException if the currently executing expander is collapsed
     */
    void resultReturned(
            ApexPathExpander apexPathExpander, ForkEvent forkEvent, Optional<?> apexValue)
            throws PathCollapsedException;

    /** Removes the expander from all maps that are tracking it */
    void removeExistingExpander(ApexPathExpander apexPathExpander);

    /**
     * Clear the list of expanders that have been collapsed
     *
     * @return the list before it was cleared
     */
    List<ApexPathExpander> clearCollapsedExpanders();
}
