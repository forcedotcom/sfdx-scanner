package com.salesforce.rules.fls.apex.operations;

import com.google.common.collect.Sets;
import com.salesforce.apex.ApexEnum;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.graph.EnumUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.symbols.apex.MethodBasedSanitization;
import com.salesforce.graph.vertex.DmlStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.rules.DmlUtil.DmlOperation;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Constants reused between FLS violation checks */
public final class FlsConstants {

    // We don't want this class to be initialized ever.
    private FlsConstants() {}

    /** Source of truth for all validations supported so far. */
    public enum FlsValidationType {
        DELETE(
                DmlOperation.DELETE,
                Sets.newHashSet("isdeletable"),
                AnalysisLevel.OBJECT_LEVEL,
                false,
                null),
        INSERT(
                DmlOperation.INSERT,
                Sets.newHashSet("iscreateable"),
                AnalysisLevel.FIELD_LEVEL,
                true,
                StripInaccessibleAccessType.CREATABLE),
        MERGE(
                DmlOperation.MERGE,
                Sets.newHashSet("ismergeable"),
                AnalysisLevel.OBJECT_LEVEL,
                false,
                null,
                2),
        READ(
                DmlOperation.READ,
                Sets.newHashSet("isaccessible"),
                AnalysisLevel.FIELD_LEVEL,
                true,
                StripInaccessibleAccessType.READABLE),
        UNDELETE(
                DmlOperation.UNDELETE,
                Sets.newHashSet("isundeletable"),
                AnalysisLevel.OBJECT_LEVEL,
                false,
                null),
        UPDATE(
                DmlOperation.UPDATE,
                Sets.newHashSet("isupdateable"),
                AnalysisLevel.FIELD_LEVEL,
                true,
                StripInaccessibleAccessType.UPDATABLE),
        UPSERT(
                DmlOperation.UPSERT,
                Sets.newHashSet("iscreateable", "isupdateable"),
                AnalysisLevel.FIELD_LEVEL,
                true,
                StripInaccessibleAccessType.UPSERTABLE),
        ;

        // Create a map of databaseOperationMethod -> FlsValidationType
        private static final TreeMap<String, FlsValidationType> DATABASE_OPERATION_MAP =
                EnumUtil.getEnumTreeMap(
                        FlsValidationType.class, FlsValidationType::getDatabaseOperationMethod);

        // Create a map of dmlStatementType -> FlsValidationType
        private static final TreeMap<String, FlsValidationType> DML_STATEMENT_MAP =
                EnumUtil.getEnumTreeMap(
                        FlsValidationType.class, FlsValidationType::getDmlStatementType);

        /**
         * DML Operation Type enumeration that stores the Statement type (as indicated by AST) and
         * the corresponding DML operation invoked on the Database type. See DmlUtil.DmlOperation.
         */
        public final DmlOperation dmlOperation;


        /** Check method used in standard FLS. Invoked on SObjecDescribe.FieldDescribe */
        public final TreeSet<String> checkMethod;

        /**
         * The level at which operation is performed. For example, while Insert, Update, and Read
         * are performed at Field level, Delete operation is performed at an Object level. This
         * means, the check for validation also needs to happen at a corresponding field or object
         * level.
         */
        public final AnalysisLevel analysisLevel;

        /**
         * Shows if the operation is supported by the Security.isStripInaccessible() method to
         * remove fields from sObject instance that are not accessible.
         */
        public final boolean isStripInaccessibleSupported;

        /**
         * AccessType required to be used with Security.isStripInaccessible() method. If the
         * operation is not supported by Security.isStripInaccessible(), the value is null.
         */
        public final StripInaccessibleAccessType stripInaccessibleAccessType;

        /** The number of parameters that the operation takes. By default, this is 1; */
        public final int parameterCount;

        FlsValidationType(
                DmlOperation dmlOperation,
                Set<String> checkMethod,
                AnalysisLevel analysisLevel,
                boolean isStripInaccessibleSupported,
                StripInaccessibleAccessType stripInaccessibleAccessType) {
            this(
                    dmlOperation,
                    checkMethod,
                    analysisLevel,
                    isStripInaccessibleSupported,
                    stripInaccessibleAccessType,
                    1);
        }

        FlsValidationType(
                DmlOperation dmlOperation,
                Set<String> checkMethod,
                AnalysisLevel analysisLevel,
                boolean isStripInaccessibleSupported,
                StripInaccessibleAccessType stripInaccessibleAccessType,
                int parameterCount) {
            this.dmlOperation = dmlOperation;
            this.checkMethod = CollectionUtil.newTreeSet();
            this.checkMethod.addAll(checkMethod);
            this.analysisLevel = analysisLevel;
            this.isStripInaccessibleSupported = isStripInaccessibleSupported;
            this.stripInaccessibleAccessType = stripInaccessibleAccessType;
            this.parameterCount = parameterCount;
        }

        public static Optional<FlsValidationType> getValidationType(String checkMethodName) {
            for (FlsValidationType type : FlsValidationType.values()) {
                if (type.checkMethod.contains(checkMethodName)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        public static FlsValidationType getValidationType(DmlStatementVertex dmlVertex) {

            final FlsValidationType validationType = DML_STATEMENT_MAP.get(dmlVertex.getLabel());

            if (validationType == null) {
                throw new TodoException(
                        "DmlStatementVertex is not handled in FlsValidationType: " + dmlVertex);
            }
            return validationType;
        }

        public static Optional<FlsValidationType> getValidationType(
                MethodCallExpressionVertex methodCall) {
            final String fullMethodName = methodCall.getFullMethodName();
            return Optional.ofNullable(DATABASE_OPERATION_MAP.get(fullMethodName));
        }

        public boolean isFieldCheckNeeded() {
            return AnalysisLevel.FIELD_LEVEL.equals(this.analysisLevel);
        }

        private DmlOperation getDmlOperation() {
            return dmlOperation;
        }

        /**
         * @return the databse operation method stored in the underlying {@link com.salesforce.rules.DmlUtil}
         */
        public String getDatabaseOperationMethod() {
            return getDmlOperation().getDatabaseOperationMethod();
        }

        /**
         * @return the databse statement type (aka method name) stored in the underlying {@link com.salesforce.rules.DmlUtil}
         */
        public String getDmlStatementType() { return getDmlOperation().getDmlStatementType(); }

        public ProcessFields getProcessFields() {
            if (AnalysisLevel.FIELD_LEVEL.equals(this.analysisLevel)) {
                return ProcessFields.INDIVIDUAL;
            }
            return ProcessFields.NONE;
        }
    }

    /** Level at which validation needs to be performed for a DML operation. */
    public enum AnalysisLevel {
        FIELD_LEVEL(0),
        OBJECT_LEVEL(1),
        NONE(2);

        private final int simplicityRank;

        AnalysisLevel(int simplicityRank) {
            this.simplicityRank = simplicityRank;
        }

        public static AnalysisLevel getSimplestAnalysisLevel(
                AnalysisLevel level, AnalysisLevel... levels) {
            AnalysisLevel simplestLevel = level;
            for (AnalysisLevel l : levels) {
                if (l.simplicityRank > simplestLevel.simplicityRank) {
                    simplestLevel = l;
                }
            }
            return simplestLevel;
        }
    }

    /** Defines AccessTypes that can be used with stripInaccessible() checks */
    public enum StripInaccessibleAccessType implements MethodBasedSanitization.SanitizerAccessType {
        CREATABLE("CREATABLE"),
        READABLE("READABLE"),
        UPDATABLE("UPDATABLE"),
        UPSERTABLE("UPSERTABLE");

        // Create a map of accessType -> StripInaccessibleAccessType
        private static final TreeMap<String, StripInaccessibleAccessType> ACCESS_TYPE_MAP =
                EnumUtil.getEnumTreeMap(
                        StripInaccessibleAccessType.class,
                        StripInaccessibleAccessType::getAccessType);

        final String accessType;

        StripInaccessibleAccessType(String accessType) {
            this.accessType = accessType;
        }

        private String getAccessType() {
            return accessType;
        }

        public static Optional<StripInaccessibleAccessType> getAccessType(String accessType) {
            return Optional.ofNullable(ACCESS_TYPE_MAP.get(accessType));
        }

        public static Optional<StripInaccessibleAccessType> getAccessType(ApexEnumValue enumValue) {
            if (enumValue != null
                    && enumValue.matchesType(ApexStandardLibraryUtil.Type.SYSTEM_ACCESS_TYPE)) {
                final Optional<ApexEnum.Value> value = enumValue.getValue();
                if (value.isPresent()) {
                    return getAccessType(value.get().getValueName());
                }
            }
            return Optional.empty();
        }
    }

    /** Indicates the level at which fields should be handled when validating FLS checks */
    public enum ProcessFields {
        INDIVIDUAL,
        ALL,
        NONE
    }
}
