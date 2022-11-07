package com.salesforce.rules.fls.apex.operations;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import java.util.Optional;
import java.util.TreeSet;

/** Helps determine access checks based on object type */
public final class ObjectBasedCheckUtil {
    private ObjectBasedCheckUtil() {}

    /**
     * Not every object supports the same level of analysis. Given an object name, this method
     * returns the level of analysis appropriate for that object's type.
     */
    private static FlsConstants.AnalysisLevel deriveValidationLevel(String objectName) {
        if (MetadataInfoProvider.get().isCustomSetting(objectName)) {
            // At present, Custom Settings require no validation of any kind. In the future, we may
            // add the ability to
            // specify Custom Settings on which Object-level checks should be performed. If that
            // happens, we'll want to
            // create a Set of those types, and compare the objectName against that Set. But for
            // now, it's fine to return
            // NONE.
            return FlsConstants.AnalysisLevel.NONE;
        } else {
            // Every other object type could potentially require FLS validation, based on the action
            // being taken.
            return FlsConstants.AnalysisLevel.FIELD_LEVEL;
        }
    }

    /**
     * Get {@link com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel} based on the
     * object's expectations and the validation type involved.
     */
    public static FlsConstants.AnalysisLevel getAnalysisLevel(
            ApexValue<?> objectValue, FlsConstants.FlsValidationType validationType) {
        return getAnalysisLevel(getObjectName(objectValue), validationType);
    }

    /**
     * Get {@link com.salesforce.rules.fls.apex.operations.FlsConstants.AnalysisLevel} based on the
     * object's expectations and the validation type involved.
     */
    public static FlsConstants.AnalysisLevel getAnalysisLevel(
            String objectName, FlsConstants.FlsValidationType validationType) {
        final FlsConstants.AnalysisLevel analysisLevelOfObject = deriveValidationLevel(objectName);
        return FlsConstants.AnalysisLevel.getSimplestAnalysisLevel(
                analysisLevelOfObject, validationType.analysisLevel);
    }

    /**
     * @return true if the sobject requires object-level access aka CRUD check; false if any other
     *     check is required.
     */
    public static boolean isCrudCheckExpected(
            DescribeSObjectResult describeSObjectResult,
            FlsConstants.FlsValidationType validationType) {
        final Optional<ApexValue<?>> objectTypeValueOptional =
                describeSObjectResult.getSObjectType().get().getType();

        if (objectTypeValueOptional.isPresent()) {
            // Figure out the maximum validation level associated with this type.
            String objectName = getObjectName(objectTypeValueOptional.get());
            FlsConstants.AnalysisLevel maximumObjectLevel = deriveValidationLevel(objectName);
            if (maximumObjectLevel == FlsConstants.AnalysisLevel.NONE) {
                // If the validation level is NONE, then we can return false, because this outranks
                // everything.
                return false;
            } else if (maximumObjectLevel == FlsConstants.AnalysisLevel.OBJECT_LEVEL) {
                // If object level checks are the maximum allowed, then we can return true.
                return true;
            }
        }

        // If we're here, then either there's no object type given, or the object supports FLS
        // checks. Either way, we need
        // to use the validation type as a fallback. If it's object-level, then we're good to return
        // true. Otherwise, return false.
        return validationType.analysisLevel == FlsConstants.AnalysisLevel.OBJECT_LEVEL;
    }

    /**
     * @return true for System read-only and metadata objects
     */
    public static boolean isSpecialObject(String objectName) {
        return ApexStandardLibraryUtil.isSystemReadOnlyObject(objectName)
                || TypeableUtil.isMetadataObject(objectName);
    }

    private static String getObjectName(ApexValue<?> objectTypeValueOptional) {
        final TreeSet<String> objectNames =
                ApexValueUtil.convertApexValueToString(objectTypeValueOptional);
        if (!objectNames.isEmpty()) {
            if (objectNames.size() > 1) {
                throw new TodoException(
                        "Cannot handle object value with more than one name: " + objectNames);
            } else {
                return objectNames.first();
            }
        }

        // If we are here, we don't know the object name.
        return SoqlParserUtil.UNKNOWN;
    }
}
