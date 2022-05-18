package com.salesforce.graph.ops;

import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ObjectPropertiesHolder;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.SyntheticTypedVertex;

/**
 * Util to handle repetitive functionalities in implementations of {@link
 * com.salesforce.graph.symbols.ObjectProperties}.
 */
public final class ObjectPropertiesUtil {
    private ObjectPropertiesUtil() {}

    /**
     * @return a default indeterminant value of unknown type. Also, adds the default value to the
     *     ObjectProperties map.
     */
    public static ApexValue<?> getDefaultIndeterminantValue(
            String key, ObjectPropertiesHolder objectPropertiesHolder) {
        final ApexStringValue keyValue = createKeyValue(key);
        final ApexValue<?> apexValue =
                ApexValueBuilder.getWithoutSymbolProvider()
                        // Since we don't know what the value's type is, we'll just use the key's
                        // name
                        // TODO: reevaluate this approach
                        .declarationVertex(SyntheticTypedVertex.get(key))
                        .withStatus(ValueStatus.INDETERMINANT)
                        .buildUnknownType();

        // Add both the key and value back to properties
        objectPropertiesHolder.put(keyValue, apexValue);
        return apexValue;
    }

    /**
     * @return a default null value of unknown type. Also, adds the default value to the
     *     ObjectProperties map.
     */
    public static ApexValue<?> getDefaultNullValue(
            String key, ObjectPropertiesHolder objectPropertiesHolder) {
        final ApexStringValue keyValue = createKeyValue(key);
        final ApexValue<?> apexValue =
                ApexValueBuilder.getWithoutSymbolProvider()
                        // An uninitialized value is Apex is null
                        .withStatus(ValueStatus.UNINITIALIZED)
                        .buildUnknownType();

        // Add both the key and value back to properties
        objectPropertiesHolder.put(keyValue, apexValue);
        return apexValue;
    }

    private static ApexStringValue createKeyValue(String key) {
        return ApexValueBuilder.getWithoutSymbolProvider().buildString(key);
    }
}
