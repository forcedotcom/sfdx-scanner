package com.salesforce.graph.ops.expander;

import static com.salesforce.graph.ops.ApexStandardLibraryUtil.Type;
import static com.salesforce.graph.ops.ApexStandardLibraryUtil.getCanonicalName;

import com.salesforce.graph.Immutable;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Removes any paths where the method has a return type of an ApexValue, but the returned value
 * isn't of the expected type the value isn't present. This assumes that any method that returns an
 * ApexValue will only return an invalid result in the cases where there are if/else paths that
 * result in caching and the path with the invalid return value are taking paths where there should
 * be a cache hit. The end result of this is that all paths will be collapsed into the path that
 * only consists of cache misses.
 */
public final class SyntheticResultReturnValuePathCollapser
        implements ApexReturnValuePathCollapser,
                Immutable<SyntheticResultReturnValuePathCollapser> {
    private static final Logger LOGGER =
            LogManager.getLogger(SyntheticResultReturnValuePathCollapser.class);

    public static SyntheticResultReturnValuePathCollapser getInstance() {
        return SyntheticResultReturnValuePathCollapser.LazyHolder.INSTANCE;
    }

    @Override
    public void checkValid(MethodVertex vertex, Optional<ApexValue<?>> returnValue)
            throws ReturnValueInvalidException {
        if (vertex.isStandardType()) {
            // This will be a synthesized type
            return;
        }
        String returnType = vertex.getReturnType();
        ApexValue<?> apexValue = returnValue.orElse(null);
        if (getCanonicalName(returnType).equalsIgnoreCase(Type.SCHEMA_DESCRIBE_S_OBJECT_RESULT)) {
            DescribeSObjectResult describeSObjectResult =
                    validateType(apexValue, DescribeSObjectResult.class);
            validateIsPresent(describeSObjectResult.getSObjectType(), "SObject Type");
        } else if (getCanonicalName(returnType)
                .equalsIgnoreCase(Type.SCHEMA_DESCRIBE_FIELD_RESULT)) {
            DescribeFieldResult describeFieldResult =
                    validateType(apexValue, DescribeFieldResult.class);
            validateIsPresent(describeFieldResult.getSObjectType(), "SObject Type");
            validateIsPresent(describeFieldResult.getFieldName(), "Field Name");
        } else if (MetadataInfoProvider.get().isCustomSetting(returnType)) {
            validateType(apexValue, ApexCustomValue.class);
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Ignoring returnType=" + returnType);
            }
        }
    }

    /**
     * Validates the type of {@code apexValue} and casts it to the correct type
     *
     * @throws ReturnValueInvalidException if {@code apexValue} does not match the type of {@code
     *     clazz}
     */
    private <T extends ApexValue<?>> T validateType(ApexValue<?> apexValue, Class<T> clazz)
            throws ReturnValueInvalidException {
        if (!clazz.isInstance(apexValue)) {
            throw new ReturnValueInvalidException(
                    "ApexValue is the wrong type. expectedType="
                            + clazz.getCanonicalName()
                            + ", apexValue="
                            + apexValue);
        }
        return (T) apexValue;
    }

    /**
     * @throws ReturnValueInvalidException if {@code optApexValue} is not present
     */
    private <T extends ApexValue<?>> void validateIsPresent(
            Optional<T> optApexValue, String objectName) throws ReturnValueInvalidException {
        if (!optApexValue.isPresent()) {
            throw new ReturnValueInvalidException(objectName + " is not present");
        }
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final SyntheticResultReturnValuePathCollapser INSTANCE =
                new SyntheticResultReturnValuePathCollapser();
    }

    private SyntheticResultReturnValuePathCollapser() {}
}
