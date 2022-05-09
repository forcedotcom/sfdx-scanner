package com.salesforce.graph.symbols.apex;

import java.util.Optional;

/** Holds types related to method-based sanitization on an ApexValue */
public class MethodBasedSanitization {

    /** Tracks the various mechanisms allowed to sanitize an ApexValue */
    public enum SanitizerMechanism {
        STRIP_INACCESSIBLE
    }

    /**
     * Denotes ApexValue types that can be sanitized for security by a mechanism defined in {@link
     * SanitizerMechanism}
     */
    public interface SanitizableValue {

        /** @return true if the ApexValue has been sanitized by the given mechanism */
        boolean isSanitized(SanitizerMechanism sanitizerMechanism, SanitizerAccessType accessType);
    }

    /**
     * Denotes a value that holds outcome of a Sanitizer method. For now, this remains a marker
     * interface.
     */
    public interface SanitizerDecision {
        /** @return the {@link SanitizerAccessType} that this SanitizerDecision guards */
        Optional<SanitizerAccessType> getAccessType();
    }

    /**
     * Denotes access types that can be marked through a Sanitizer method. Used in conjunction with
     * {@link SanitizerDecision}
     */
    public interface SanitizerAccessType {}
}
