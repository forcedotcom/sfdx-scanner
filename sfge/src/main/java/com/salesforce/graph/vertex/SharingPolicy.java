package com.salesforce.graph.vertex;

// public enum SharingPolicy {
//    WITH_SHARING("with sharing"),
//    WITHOUT_SHARING("without sharing"),
//    INHERITED_SHARING("inherited sharing"),
//    OMITTED_DECLARATION("");
//
//    private static final Map<String, SharingPolicy> MODIFIER_TO_SHARING_POLICY =
//            EnumUtil.getEnumMap(SharingPolicy.class, a -> a.modifier);
//
//    public static Optional<SharingPolicy> fromString(String modifier) {
//        return Optional.ofNullable(MODIFIER_TO_SHARING_POLICY.get(modifier));
//    }
//
//    private final String modifier;
//
//    private SharingPolicy(String modifier) {
//        this.modifier = modifier;
//    }
//
//    /**
//     * get the string representation of this SharingPolicy, which are the keywords/modifiers. i.e.
//     * WITH_SHARING becomes <code>with sharing</code>
//     */
//    @Override
//    public String toString() {
//        return this.modifier;
//    }
// }
