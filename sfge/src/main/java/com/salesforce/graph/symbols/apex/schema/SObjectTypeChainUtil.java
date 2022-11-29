package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.List;
import java.util.Optional;

/**
 * Utility to help checking chained name prefixes in Schema-based invocations. For now, this only
 * handles expansions related to SObjectType. There could be exclusions that resolve to value types
 * that hasn't been handled yet. TODO: Expand this to cover more chained name prefixes to improve
 * readability in Factory classes.
 */
public final class SObjectTypeChainUtil {

    public static final String SCHEMA_STR = ApexStandardLibraryUtil.VariableNames.SCHEMA;
    public static final String S_OBJECT_TYPE_STR =
            ApexStandardLibraryUtil.VariableNames.S_OBJECT_TYPE;

    private SObjectTypeChainUtil() {}

    private static boolean hasSObjectTypePrefix(List<String> chainedNames) {
        if (chainedNames.size() == 1) {
            // SObject.<something>
            return S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(0));
        } else if (chainedNames.size() > 1) {
            // Schema.SObjectType.<something>.<something>...
            return (SCHEMA_STR.equalsIgnoreCase(chainedNames.get(0))
                            && S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(1)))
                    // SObjectType.Account.<something>
                    || S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(0));
        }
        return false;
    }

    private static boolean hasSObjectTypeInChain(List<String> chainedNames) {
        if (chainedNames.size() == 2) {
            // Account.SObjectType.getDescribe()
            return S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(1));
        }
        return false;
    }

    /**
     * Gets SObject name from various Schema-based chained constructs.
     *
     * @param chainedNames prefixes on vertex's name
     * @param vertex that possibly represents the schema item
     * @return SObject name based on the chained constructs if a match was found. Else returns an
     *     empty Optional.
     *     <p>TODO: Consider type checking SObject name against the list of known standard and
     *     custom objects. Not doing this at the moment to avoid missing custom object formats that
     *     we don't handle yet.
     */
    public static Optional<String> getSObjectName(
            List<String> chainedNames, VariableExpressionVertex vertex) {
        if (hasSObjectTypePrefix(chainedNames)) {
            if (chainedNames.size() == 1) {
                // SObjectType.Account
                return Optional.of(vertex.getName());

            } else if (chainedNames.size() == 2) {
                if (S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(1))) {
                    // Schema.SObjectType.Account
                    return Optional.of(vertex.getName());

                } else if (S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(0))) {
                    // SObjectType.Account.Name
                    return Optional.of(chainedNames.get(1));
                }
            } else if (chainedNames.size() == 3) {
                if (S_OBJECT_TYPE_STR.equalsIgnoreCase(chainedNames.get(1))) {
                    // Schema.SObjectType.Account.Name
                    return Optional.of(chainedNames.get(2));
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<String> getSObjectName(
            List<String> chainedNames, MethodCallExpressionVertex vertex) {
        if (hasSObjectTypePrefix(chainedNames)) {
            if (chainedNames.size() == 2 || chainedNames.size() == 3) {
                // SObjectType.Account.isDeletable()
                // Schema.SObjectType.Account.isDeletable()
                return Optional.of(chainedNames.get(chainedNames.size() - 1));
            }
        } else if (hasSObjectTypeInChain(chainedNames)) {
            // Account.SObjectType.getDescribe()
            return Optional.of(chainedNames.get(0));
        }

        return Optional.empty();
    }
}
