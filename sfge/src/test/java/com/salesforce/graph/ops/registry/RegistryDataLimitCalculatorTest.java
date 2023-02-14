package com.salesforce.graph.ops.registry;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.UserActionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RegistryDataLimitCalculatorTest {
    @Test
    public void testLimitCalculator() {
        // setup
        final long maxHeapSpaceSize = 20000000000L; // 20G
        final long objectSize = 20000000L;
        final int expectedSize = 500;
        final int minimumItemCountExpected = 100;

        RegistryDataLimitCalculator.Dependencies dependencies =
                Mockito.mock(RegistryDataLimitCalculator.Dependencies.class);
        Mockito.lenient().when(dependencies.getHeapMaxSize()).thenReturn(maxHeapSpaceSize);

        final RegistryDataLimitCalculator calculator =
                new RegistryDataLimitCalculator(dependencies);

        // execute
        final int actualSize =
                calculator.calculateAllowedLimit(objectSize, minimumItemCountExpected);

        // verify
        MatcherAssert.assertThat(actualSize, Matchers.equalTo(expectedSize));
    }

    @Test
    public void testInsufficientMaxHeapSize() {
        // setup
        final long maxHeapSpaceSize = 9000L;
        final int minimumItemCountExpected = 100;
        final long objectSize = 100L;

        RegistryDataLimitCalculator.Dependencies dependencies =
                Mockito.mock(RegistryDataLimitCalculator.Dependencies.class);
        Mockito.lenient().when(dependencies.getHeapMaxSize()).thenReturn(maxHeapSpaceSize);

        final RegistryDataLimitCalculator calculator =
                new RegistryDataLimitCalculator(dependencies);

        // execute & verify
        assertThrows(
                UserActionException.class,
                () -> calculator.calculateAllowedLimit(objectSize, minimumItemCountExpected),
                String.format(UserFacingMessages.INSUFFICIENT_HEAP_SPACE, maxHeapSpaceSize));
    }
}
