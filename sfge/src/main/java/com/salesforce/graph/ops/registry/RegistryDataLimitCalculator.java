package com.salesforce.graph.ops.registry;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.UserActionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Helps calculate the limit to enforce on the registry for a given type of {@link Registrable}. */
public class RegistryDataLimitCalculator {
    private static final Logger LOGGER = LogManager.getLogger(RegistryDataLimitCalculator.class);

    /** Size of an average ApexPathExpander instance. Calculated from memory profile information. */
    private static final long APEX_PATH_EXPANDER_AVERAGE_SIZE = 1284328L;
    /** Minimum count of ApexPathExpander instance that we should be able to add to registry. */
    private static final int APEX_PATH_EXPANDER_MINIMUM_ITEM_COUNT_EXPECTED = 100;

    // Allow path expander registry to reach upto 50% of heap
    private static final double CAPACITY_LIMIT = 0.5;

    /** Calculate limit allowed for ApexPathExpander. */
    public static int getApexPathExpanderRegistryLimit() {
        final RegistryDataLimitCalculator calculator = new RegistryDataLimitCalculator();
        return calculator.calculateAllowedLimit(
                APEX_PATH_EXPANDER_AVERAGE_SIZE, APEX_PATH_EXPANDER_MINIMUM_ITEM_COUNT_EXPECTED);
    }

    private final Dependencies dependencies;

    public RegistryDataLimitCalculator() {
        this(new Dependencies());
    }

    RegistryDataLimitCalculator(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Given an item's size, allowed limit is calculated as the number of items that can be
     * accommodated within the allowed capacity of the total heap space allocated for this JVM run.
     */
    int calculateAllowedLimit(long averageItemSize, int minimumItemCountExpected) {
        final long heapMaxSize = dependencies.getHeapMaxSize();

        if (heapMaxSize < minimumItemCountExpected * averageItemSize) {
            throw new UserActionException(
                    String.format(UserFacingMessages.INSUFFICIENT_HEAP_SPACE, heapMaxSize));
        }

        final int allowedLimit = (int) ((heapMaxSize * CAPACITY_LIMIT) / averageItemSize);

        // TODO: Ideally, a log line here would be helpful, but since this method gets invoked
        // before logs are initialized,
        //  this never gets printed.

        return allowedLimit;
    }

    static class Dependencies {
        long getHeapMaxSize() {
            return Runtime.getRuntime().maxMemory();
        }
    }
}
