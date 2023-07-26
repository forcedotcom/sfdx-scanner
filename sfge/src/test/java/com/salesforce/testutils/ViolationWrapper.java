package com.salesforce.testutils;

import com.google.common.base.Objects;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.AvoidMultipleMassSchemaLookups;
import com.salesforce.rules.UseWithSharingOnDatabaseOperation;
import com.salesforce.rules.avoiddatabaseoperationinloop.AvoidDatabaseOperationInLoopUtil;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsStripInaccessibleWarningInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationMessageUtil;
import com.salesforce.rules.fls.apex.operations.UnresolvedCrudFlsViolationInfo;
import com.salesforce.rules.multiplemassschemalookup.MassSchemaLookupInfoUtil;
import com.salesforce.rules.multiplemassschemalookup.MmslrUtil;
import com.salesforce.rules.ops.OccurrenceInfo;
import com.salesforce.rules.usewithsharingondatabaseoperation.SharingPolicyUtil;
import java.util.*;
import java.util.function.Function;

/** Wrapper around Violation to help comparing only the violation message and line numbers */
public class ViolationWrapper {
    public enum FlsViolationType {
        STANDARD(
                (builder) ->
                        new FlsViolationInfo(
                                builder.validationType,
                                builder.objectName,
                                builder.fieldNames,
                                builder.allFields)),
        STRIP_INACCESSIBLE_WARNING(
                (builder) ->
                        new FlsStripInaccessibleWarningInfo(
                                builder.validationType,
                                builder.objectName,
                                builder.fieldNames,
                                builder.allFields)),
        UNRESOLVED_CRUD_FLS(
                (builder) -> new UnresolvedCrudFlsViolationInfo(builder.validationType));

        Function<ViolationWrapper.FlsViolationBuilder, FlsViolationInfo> instanceSupplier;

        FlsViolationType(
                Function<ViolationWrapper.FlsViolationBuilder, FlsViolationInfo> instanceSupplier) {
            this.instanceSupplier = instanceSupplier;
        }

        FlsViolationInfo createInstance(FlsViolationBuilder builder) {
            return this.instanceSupplier.apply(builder);
        }
    }

    final int line;
    final String violationMsg;

    private ViolationWrapper(MessageBuilder builder) {
        this.line = builder.line;
        this.violationMsg = builder.violationMsg;
    }

    private ViolationWrapper(ViolationBuilder builder) {
        this.line = builder.line;
        this.violationMsg = builder.getMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViolationWrapper)) return false;
        ViolationWrapper that = (ViolationWrapper) o;
        return line == that.line
                && Objects.equal(violationMsg.toLowerCase(), that.violationMsg.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(violationMsg.toLowerCase(), line);
    }

    @Override
    public String toString() {
        return "ViolationWrapper{"
                + "violationMsg='"
                + violationMsg
                + '\''
                + ", line="
                + line
                + '}';
    }

    public abstract static class ViolationBuilder {
        private int line;
        private int sourceLine;
        private String fileName;
        private String definingType;
        private String definingMethod;

        private ViolationBuilder(int line) {
            this.line = line;
        }

        public ViolationBuilder withSourceLine(int sourceLine) {
            this.sourceLine = sourceLine;
            return this;
        }

        public ViolationBuilder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ViolationBuilder withDefiningType(String definingType) {
            this.definingType = definingType;
            return this;
        }

        public ViolationBuilder withDefiningMethod(String definingMethod) {
            this.definingMethod = definingMethod;
            return this;
        }

        public ViolationWrapper build() {
            return new ViolationWrapper(this);
        }

        public abstract String getMessage();
    }

    public static class NullPointerViolationBuilder extends ViolationBuilder {
        private final String operation;

        private NullPointerViolationBuilder(int line, String operation) {
            super(line);
            this.operation = operation;
        }

        public static NullPointerViolationBuilder get(int line, String operation) {
            return new NullPointerViolationBuilder(line, operation);
        }

        @Override
        public String getMessage() {
            return String.format(
                    UserFacingMessages.RuleViolationTemplates.APEX_NULL_POINTER_EXCEPTION_RULE,
                    operation);
        }
    }

    public static class FlsViolationBuilder extends ViolationBuilder {
        private final FlsConstants.FlsValidationType validationType;
        private final String objectName;
        private final TreeSet<String> fieldNames;
        private boolean allFields;

        private FlsViolationType violationType;

        private FlsViolationBuilder(
                int line, FlsConstants.FlsValidationType validationType, String objectName) {
            super(line);
            this.validationType = validationType;
            this.objectName = objectName;
            this.fieldNames = CollectionUtil.newTreeSet();
            this.allFields = false;
            this.violationType = FlsViolationType.STANDARD;
        }

        public static FlsViolationBuilder get(
                int line, FlsConstants.FlsValidationType validationType, String objectName) {
            return new FlsViolationBuilder(line, validationType, objectName);
        }

        public static FlsViolationBuilder get(
                int line, FlsConstants.FlsValidationType validationType) {
            return get(line, validationType, SoqlParserUtil.UNKNOWN);
        }

        public FlsViolationBuilder withField(String field) {
            this.fieldNames.add(field);
            return this;
        }

        public FlsViolationBuilder withFields(String... fields) {
            this.fieldNames.addAll(Arrays.asList(fields));
            return this;
        }

        public FlsViolationBuilder withAllFields() {
            this.allFields = true;
            return this;
        }

        public FlsViolationBuilder forViolationType(FlsViolationType violationType) {
            this.violationType = violationType;
            return this;
        }

        @Override
        public String getMessage() {
            // Create new instance of FlsViolationInfo based on the type of violation
            final FlsViolationInfo violationInfo = this.violationType.createInstance(this);
            return FlsViolationMessageUtil.constructMessage(violationInfo);
        }
    }

    /** Message builder to help with testing {@link AvoidMultipleMassSchemaLookups}. */
    public static class MassSchemaLookupInfoBuilder extends ViolationBuilder {
        private final String sinkMethodName;
        private final MmslrUtil.RepetitionType repetitionType;
        private final List<OccurrenceInfo> occurrenceInfoList;

        private MassSchemaLookupInfoBuilder(
                int sinkLine, String sinkMethodName, MmslrUtil.RepetitionType type) {
            super(sinkLine);
            this.sinkMethodName = sinkMethodName;
            this.repetitionType = type;
            this.occurrenceInfoList = new ArrayList<>();
        }

        public static MassSchemaLookupInfoBuilder get(
                int sinkLine, String sinkMethodName, MmslrUtil.RepetitionType type) {
            return new MassSchemaLookupInfoBuilder(sinkLine, sinkMethodName, type);
        }

        /**
         * To include occurrence information in the verification.
         *
         * @param label on the occurrence
         * @param definingType - class in which the occurrence is expected
         * @param line - where the occurrence should show up.
         * @return reference to the builder to continue building the value.
         */
        public MassSchemaLookupInfoBuilder withOccurrence(
                String label, String definingType, int line) {
            this.occurrenceInfoList.add(new OccurrenceInfo(label, definingType, line));
            return this;
        }

        @Override
        public String getMessage() {
            return MassSchemaLookupInfoUtil.getMessage(
                    sinkMethodName, repetitionType, occurrenceInfoList);
        }
    }

    /**
     * Message builder to help with testing {@link
     * com.salesforce.rules.AvoidDatabaseOperationInLoop}.
     */
    public static class AvoidDatabaseInLoopInfoBuilder extends ViolationBuilder {

        private final OccurrenceInfo occurrenceInfo;

        public AvoidDatabaseInLoopInfoBuilder(int line, OccurrenceInfo occurrenceInfo) {
            super(line);
            this.occurrenceInfo = occurrenceInfo;
        }

        public static AvoidDatabaseInLoopInfoBuilder get(
                int sinkLine, OccurrenceInfo occurrenceInfo) {
            return new AvoidDatabaseInLoopInfoBuilder(sinkLine, occurrenceInfo);
        }

        @Override
        public String getMessage() {
            return AvoidDatabaseOperationInLoopUtil.getMessage(occurrenceInfo);
        }
    }

    /** Message builder to help with testing {@link UseWithSharingOnDatabaseOperation}. */
    public static class SharingPolicyViolationBuilder extends ViolationBuilder {
        private final boolean warning;
        private final SharingPolicyUtil.InheritanceType inheritanceType;
        private final String classInheritedFrom;

        public SharingPolicyViolationBuilder(int sinkLine) {
            super(sinkLine);
            this.warning = false;
            this.inheritanceType = null;
            this.classInheritedFrom = null;
        }

        public SharingPolicyViolationBuilder(
                int sinkLine,
                SharingPolicyUtil.InheritanceType inheritanceType,
                String classInheritedFrom) {
            super(sinkLine);
            this.warning = true;
            this.inheritanceType = inheritanceType;
            this.classInheritedFrom = classInheritedFrom;
        }

        public static SharingPolicyViolationBuilder get(int sinkLine) {
            return new SharingPolicyViolationBuilder(sinkLine);
        }

        public static SharingPolicyViolationBuilder getWarning(
                int sinkLine,
                SharingPolicyUtil.InheritanceType inheritanceType,
                String classInheritedFrom) {
            return new SharingPolicyViolationBuilder(sinkLine, inheritanceType, classInheritedFrom);
        }

        @Override
        public String getMessage() {
            if (warning) {
                if (this.inheritanceType == null || this.classInheritedFrom == null) {
                    throw new ProgrammingException(
                            "SharingPolicyInfoBuilder cannot build a warning violation without both an inheritance type and a class inherited from.");
                }

                return String.format(
                        UserFacingMessages.SharingPolicyRuleTemplates.WARNING_TEMPLATE,
                        this.inheritanceType,
                        this.classInheritedFrom);
            } else {
                return UserFacingMessages.SharingPolicyRuleTemplates.MESSAGE_TEMPLATE;
            }
        }
    }

    public static class MessageBuilder {
        private final int line;
        private final String violationMsg;

        private MessageBuilder(int line, String violationMsg) {
            this.line = line;
            this.violationMsg = violationMsg;
        }

        public static MessageBuilder get(int line, String violationMsg) {
            return new MessageBuilder(line, violationMsg);
        }

        public ViolationWrapper build() {
            return new ViolationWrapper(this);
        }
    }
}
