package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

/**
 * Some objects such as ApexCustomValue are passed to InnerScopes using a MethodInvocationScope.
 * This pattern can be repeated any number of times. Each MethodInvocationScope has its own
 * reference to the same object. The entire Scope stack is cloned When an ApexPath is forked. The
 * single object must be cloned once and each subsequent invocation of {@link
 * DeepCloneable#deepClone()} on other references should return the instance from the first
 * #deepClone invocation, thus maintaining the link across the stack.
 *
 * <p>This context provides a thread local cache that stores the results of previous clone
 * operations for objects that occur between invocations of {@link #establish()} and {@link
 * #release()}. A new clone is created the first time #deepClone is invoked. The results are stored
 * in a thread local map. Subsequent invocations of #deepClone on the same object returns the
 * previously cloned object. This allows a loose coupling of the MethodInvocationScopes.
 *
 * <p>Any class that wants to enforces this clone behavior should implement a #deepClone method like
 * the following example
 *
 * <p>{@code public ApexCustomValue deepClone() { return
 * DeepCloneContextProvider.getOrCreateClone(this, () -> new ApexCustomValue(this)); } }
 *
 * <p>The top level code that initiates a clone should follow the following example
 *
 * <p>{@code DeepCloneContextProvider.establish(); try { scopeStack.deepClone(); } finally {
 * DeepCloneContextProvider.release(); } }
 */
public final class DeepCloneContextProvider {
    /**
     * Map of previously cloned objects that have occurred since {@link #establish()} was called on
     * this thread.
     */
    private static final ThreadLocal<IdentityHashMap<DeepCloneable<?>, DeepCloneable<?>>>
            PREVIOUSLY_CLONED_VALUES = new ThreadLocal<>();

    /** Establish the context at the beginning of a global clone operation. */
    public static void establish() {
        if (PREVIOUSLY_CLONED_VALUES.get() != null) {
            throw new UnexpectedException(
                    "Already established. map=" + PREVIOUSLY_CLONED_VALUES.get());
        }
        PREVIOUSLY_CLONED_VALUES.set(new IdentityHashMap<>());
    }

    /** Release the context of a global clone operation. */
    public static void release() {
        if (PREVIOUSLY_CLONED_VALUES.get() == null) {
            throw new UnexpectedException("Not established.");
        }
        PREVIOUSLY_CLONED_VALUES.remove();
    }

    /**
     * Thread local implementation, that returns the previous clone of {@code existingValue} if the
     * object has been cloned since {@link #establish()} was called. This context will use an
     * existing context if already established, or temporarily establishes a new one if one doesn't
     * exist.
     *
     * @param existingValue object that needs to be cloned
     * @param cloneSupplier that will create a new clone if one doesn't already exist in this
     *     context
     */
    public static <T extends DeepCloneable<?>> T cloneIfAbsent(
            T existingValue, Supplier<T> cloneSupplier) {
        final boolean needToEstablish = PREVIOUSLY_CLONED_VALUES.get() == null;
        if (needToEstablish) {
            establish();
        }
        try {
            return (T)
                    PREVIOUSLY_CLONED_VALUES
                            .get()
                            .computeIfAbsent(existingValue, k -> cloneSupplier.get());
        } finally {
            if (needToEstablish) {
                release();
            }
        }
    }

    private DeepCloneContextProvider() {}
}
