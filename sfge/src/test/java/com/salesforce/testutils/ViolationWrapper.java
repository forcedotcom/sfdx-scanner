package com.salesforce.testutils;

import com.google.common.base.Objects;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ops.SoqlParserUtil;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import com.salesforce.rules.fls.apex.operations.FlsStripInaccessibleWarningInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationInfo;
import com.salesforce.rules.fls.apex.operations.FlsViolationMessageUtil;
import com.salesforce.rules.fls.apex.operations.UnresolvedCrudFlsViolationInfo;
import java.util.Arrays;
import java.util.TreeSet;
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

    private ViolationWrapper(FlsViolationBuilder builder) {
        this.line = builder.line;
        // Create new instance of FlsViolationInfo based on the type of violation
        final FlsViolationInfo violationInfo = builder.violationType.createInstance(builder);
        this.violationMsg = FlsViolationMessageUtil.constructMessage(violationInfo);
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

    public static class FlsViolationBuilder {
        private final int line;
        private final FlsConstants.FlsValidationType validationType;
        private final String objectName;
        private final TreeSet<String> fieldNames;
        private boolean allFields;

        private String fileName;
        private int sourceLine;
        private String definingType;
        private String definingMethod;

        private FlsViolationType violationType;

        private FlsViolationBuilder(
                int line, FlsConstants.FlsValidationType validationType, String objectName) {
            this.line = line;
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

        public FlsViolationBuilder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FlsViolationBuilder withSourceLine(int sourceLine) {
            this.sourceLine = sourceLine;
            return this;
        }

        public FlsViolationBuilder withDefiningType(String definingType) {
            this.definingType = definingType;
            return this;
        }

        public FlsViolationBuilder withDefiningMethod(String definingMethod) {
            this.definingMethod = definingMethod;
            return this;
        }

        public FlsViolationBuilder forViolationType(FlsViolationType violationType) {
            this.violationType = violationType;
            return this;
        }

        public ViolationWrapper build() {
            return new ViolationWrapper(this);
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
