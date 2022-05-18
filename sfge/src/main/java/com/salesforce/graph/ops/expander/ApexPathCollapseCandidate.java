package com.salesforce.graph.ops.expander;

import com.salesforce.graph.symbols.apex.ApexValue;
import java.util.Objects;
import java.util.Optional;

/** Contains information about a path that might be collapsed */
final class ApexPathCollapseCandidate {
    /** Path's index from the original PathForkedException */
    private final ApexPathExpander apexPathExpander;

    private final Optional<ApexValue<?>> returnValue;

    ApexPathCollapseCandidate(
            ApexPathExpander apexPathExpander, Optional<ApexValue<?>> returnValue) {
        this.apexPathExpander = apexPathExpander;
        this.returnValue = returnValue;
    }

    ApexPathExpander getApexPathExpander() {
        return apexPathExpander;
    }

    /** The value that the path returned if available. */
    public Optional<ApexValue<?>> getReturnValue() {
        return returnValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexPathCollapseCandidate that = (ApexPathCollapseCandidate) o;
        return Objects.equals(apexPathExpander, that.apexPathExpander)
                && Objects.equals(returnValue, that.returnValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apexPathExpander, returnValue);
    }
}
