package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.vertex.InvocableVertex;
import java.util.HashMap;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract implementation of {@link
 * com.salesforce.graph.symbols.apex.MethodBasedSanitization.SanitizableValue} to cover the basics
 */
public abstract class AbstractSanitizableValue<T extends AbstractSanitizableValue<?>>
        extends ApexValue<T> implements MethodBasedSanitization.SanitizableValue {
    private static final Logger LOGGER = LogManager.getLogger(AbstractSanitizableValue.class);
    private final HashMap<
                    MethodBasedSanitization.SanitizerMechanism,
                    MethodBasedSanitization.SanitizerDecision>
            sanitized;

    protected AbstractSanitizableValue(ApexValueBuilder builder) {
        super(builder);
        this.sanitized = new HashMap<>();
    }

    public AbstractSanitizableValue(
            AbstractSanitizableValue other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        super(other, returnedFrom, invocable);
        this.sanitized = CloneUtil.cloneHashMap(other.sanitized);
    }

    /**
     * @return true if the value has been sanitized for the access type through the given mechanism
     */
    @Override
    public boolean isSanitized(
            MethodBasedSanitization.SanitizerMechanism sanitizerMechanism,
            MethodBasedSanitization.SanitizerAccessType accessType) {
        if (sanitized.containsKey(sanitizerMechanism)) {
            return accessType.equals(
                    sanitized.get(sanitizerMechanism).getAccessType().orElse(null));
        }
        return false;
    }

    public void markSanitized(
            MethodBasedSanitization.SanitizerMechanism sanitizerMechanism,
            MethodBasedSanitization.SanitizerDecision sanitizerDecision) {
        // Make a clone of sanitizerDecision so that everyone has their own copy.
        // TODO: unable to extend SanitizerDecision from DeepCloneable since compilation fails on
        // 	SObjectAccessDecision, which ends up with two copies of DeepCloneable on its hierarchy.
        //  Revisit to make this nicer.
        final MethodBasedSanitization.SanitizerDecision clonedSanitizerDecision =
                sanitizerDecision instanceof DeepCloneable<?>
                        ? (MethodBasedSanitization.SanitizerDecision)
                                ((DeepCloneable<?>) sanitizerDecision).deepClone()
                        : sanitizerDecision;
        final MethodBasedSanitization.SanitizerDecision existingEntry =
                sanitized.put(sanitizerMechanism, clonedSanitizerDecision);
        if (existingEntry != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Sanitizable value already contains an entry for "
                                + sanitizerMechanism
                                + ". Existing entry: "
                                + existingEntry
                                + ". Replacing it. Full apex value: "
                                + this);
            }
        }
    }

    /** Copies sanitization information from one value to another */
    protected static void copySanitization(
            AbstractSanitizableValue source, AbstractSanitizableValue dest) {
        source.sanitized.forEach(
                (key, value) -> {
                    dest.sanitized.put(
                            key,
                            (value instanceof DeepCloneable)
                                    ? ((DeepCloneable<?>) value).deepClone()
                                    : value);
                });
    }
}
