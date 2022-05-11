package com.salesforce.graph.symbols.apex;

import com.salesforce.collections.CollectionUtil;
import java.util.TreeSet;

/** General constants used across multiple contexts. */
public final class SystemNames {
    public static final String METHOD_EQUALS = "equals";
    public static final String METHOD_GET_NAME = "getName";
    public static final String METHOD_SERIALIZE = "serialize";

    public static final String METHOD_IS_ACCESSIBLE = "isAccessible";
    public static final String METHOD_IS_CREATEABLE = "isCreateable";
    public static final String METHOD_IS_DELETABLE = "isDeletable";
    public static final String METHOD_IS_MERGEABLE = "isMergeable";
    public static final String METHOD_IS_UNDELETABLE = "isUndeletable";
    public static final String METHOD_IS_UPDATEABLE = "isUpdateable";

    public static final TreeSet<String> DML_OBJECT_ACCESS_METHODS =
            CollectionUtil.newTreeSetOf(
                    METHOD_IS_ACCESSIBLE,
                    METHOD_IS_CREATEABLE,
                    METHOD_IS_DELETABLE,
                    METHOD_IS_MERGEABLE,
                    METHOD_IS_UNDELETABLE,
                    METHOD_IS_UPDATEABLE);

    public static final TreeSet<String> DML_FIELD_ACCESS_METHODS =
            CollectionUtil.newTreeSetOf(
                    METHOD_IS_ACCESSIBLE, METHOD_IS_CREATEABLE, METHOD_IS_UPDATEABLE);

    public static final String SUFFIX_CUSTOM_OBJECT = "__c";
    public static final String SUFFIX_RELATION = "__r";

    public static final String VARIABLE_QUALIFIED_API_NAME = "QualifiedApiName";

    private SystemNames() {}
}
