package com.salesforce.rules.fls.apex.operations;

import com.salesforce.graph.symbols.apex.ApexValue;

/**
 * This class helps with matching apex values when using them as keys on collections. The equals()
 * and hashcode() are open to further fine tuning based on the data we find during real life
 * testing.
 */
public class FlsApexValueVertexWrapper {
    private final ApexValue<?> apexValue;

    public FlsApexValueVertexWrapper(ApexValue<?> apexValue) {
        this.apexValue = apexValue;
    }

    public ApexValue<?> getApexValue() {
        return apexValue;
    }

    // Note that equals and hashcode methods have been customized to compare
    // the value vertex of the apex values rather than the values directly.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlsApexValueVertexWrapper)) return false;

        FlsApexValueVertexWrapper that = (FlsApexValueVertexWrapper) o;

        return apexValue.getValueVertex().equals(that.apexValue.getValueVertex());
    }

    @Override
    public int hashCode() {
        return apexValue.getValueVertex().hashCode();
    }
}
