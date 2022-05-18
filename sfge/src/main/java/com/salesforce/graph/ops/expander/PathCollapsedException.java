package com.salesforce.graph.ops.expander;

final class PathCollapsedException extends ApexPathExpanderException {
    private final ApexPathExpander apexPathExpander;

    PathCollapsedException(ApexPathExpander apexPathExpander) {
        this.apexPathExpander = apexPathExpander;
    }

    ApexPathExpander getApexPathExpander() {
        return apexPathExpander;
    }
}
