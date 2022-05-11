package com.salesforce.rules;

import com.salesforce.graph.vertex.NamedVertex;
import com.salesforce.graph.vertex.SFVertex;
import java.util.Comparator;
import java.util.Objects;

public abstract class Violation implements Comparable<Violation>, RuleThrowable {
    /** The simple violation message that will be conveyed to users of the plugin. */
    protected final String message;

    /**
     * The "source" prefix is to allow for uniform serialization with Violation subclasses that have
     * a "source" and "sink" vertex, e.g. {@link Violation.PathBasedRuleViolation}. For other
     * Violation types, it's a meaningless distinction. The vertex is declared as transient to
     * exclude it from serialization. This avoids StackOverflow errors.
     */
    protected final transient SFVertex sourceVertex;

    protected final String sourceFileName;
    protected final String sourceType;
    /**
     * Some vertices (e.g., MethodVertex) have names. Others don't. For ones that do, this will hold
     * that name. Otherwise, it'll be an empty string.
     */
    protected final String sourceVertexName;

    protected final int sourceLineNumber;
    protected final int sourceColumnNumber;

    protected String ruleName;
    protected int severity;
    protected String description;
    protected String category;

    /**
     * @param message - The value to be inserted into the violation's {@link #message} attribute.
     */
    public Violation(final String message, final SFVertex vertex) {
        this.message = message;
        this.sourceVertex = vertex;
        this.sourceFileName = vertex.getFileName();
        this.sourceType = vertex.getDefiningType();
        this.sourceVertexName =
                vertex instanceof NamedVertex ? ((NamedVertex) vertex).getName() : "";
        this.sourceLineNumber = vertex.getBeginLine();
        this.sourceColumnNumber = vertex.getBeginColumn();
    }

    public String getMessage() {
        return message;
    }

    public SFVertex getSourceVertex() {
        return sourceVertex;
    }

    public int getSourceLineNumber() {
        return sourceLineNumber;
    }

    public String getSourceDefiningType() {
        return sourceType;
    }

    public String getSourceVertexName() {
        return sourceVertexName;
    }

    public String getRuleName() {
        return ruleName;
    }

    public int getSourceColumnNumber() {
        return sourceColumnNumber;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public int getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "Violation{"
                + "message='"
                + message
                + '\''
                + ", sourceLineNumber="
                + sourceLineNumber
                + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Violation violation = (Violation) o;
        return sourceLineNumber == violation.sourceLineNumber
                && sourceColumnNumber == violation.sourceColumnNumber
                && severity == violation.severity
                && Objects.equals(ruleName, violation.ruleName)
                && Objects.equals(message, violation.message)
                && Objects.equals(sourceFileName, violation.sourceFileName)
                && Objects.equals(sourceType, violation.sourceType)
                && Objects.equals(sourceVertexName, violation.sourceVertexName)
                && Objects.equals(description, violation.description)
                && Objects.equals(category, violation.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ruleName,
                message,
                sourceLineNumber,
                sourceColumnNumber,
                sourceFileName,
                sourceType,
                sourceVertexName,
                severity,
                description,
                category);
    }

    @Override
    public int compareTo(Violation o) {
        // First, compare the attributes that are common to all violation types.
        final int baseComparison =
                Comparator.comparing(Violation::getSourceFileName)
                        .thenComparing(Violation::getSourceDefiningType)
                        .thenComparing(Violation::getSourceLineNumber)
                        .thenComparing(Violation::getSourceColumnNumber)
                        .thenComparing(Violation::getSourceVertexName)
                        .thenComparing(Violation::getRuleName)
                        .thenComparing(Violation::getMessage)
                        .compare(this, o);

        // If comparison of the base attributes revealed a definitive ordering, then we should use
        // that.
        if (baseComparison != 0) {
            return baseComparison;
        } else {
            // If we still don't have a definitive ordering, then the only thing we can do here is
            // compare the names
            // of the classes themselves.
            return getClass().getSimpleName().compareTo(o.getClass().getSimpleName());
        }
    }

    /**
     * Abstract class for violations that are associated with specific rules that were not properly
     * satisfied.
     */
    public abstract static class RuleViolation extends Violation {
        public RuleViolation(final String message, final SFVertex vertex) {
            super(message, vertex);
        }

        /**
         * RuleViolations have some rule-related properties that are initially null. This method can
         * be used to set those values, avoiding NPEs during sorting.
         *
         * @param rule - The rule that triggered this violation.
         */
        public void setPropertiesFromRule(final AbstractRule rule) {
            ruleName = rule.getClass().getSimpleName();
            severity = rule.getSeverity();
            description = rule.getDescription();
            category = rule.getCategory();
        }
    }

    /** Violations associated with a {@link StaticRule} */
    public static final class StaticRuleViolation extends RuleViolation {
        public StaticRuleViolation(final String message, final SFVertex sinkVertex) {
            super(message, sinkVertex);
        }
    }

    /** Violations associated with a {@link PathBasedRule} */
    public static final class PathBasedRuleViolation extends RuleViolation {
        /**
         * The "sink" vertex is the vertex at which the violation occurs, as contrasted with the
         * "source" vertex which denotes the start of the path that produces the violation. Declared
         * as transient so it's excluded from serialization, preventing StackOverflow errors.
         */
        private final transient SFVertex sinkVertex;

        private final String sinkFileName;
        private final int sinkLineNumber;
        private final int sinkColumnNumber;

        public PathBasedRuleViolation(
                final String message, final SFVertex sourceVertex, final SFVertex sinkVertex) {
            super(message, sourceVertex);
            this.sinkVertex = sinkVertex;
            this.sinkFileName = sinkVertex.getFileName();
            this.sinkLineNumber = sinkVertex.getBeginLine();
            this.sinkColumnNumber = sinkVertex.getBeginColumn();
        }

        public SFVertex getSinkVertex() {
            return sinkVertex;
        }

        public int getSinkLineNumber() {
            return sinkLineNumber;
        }

        public int getSinkColumnNumber() {
            return sinkColumnNumber;
        }

        public String getSinkFileName() {
            return sinkFileName;
        }

        @Override
        public String toString() {
            return "PathBasedRuleViolation{"
                    + "message='"
                    + message
                    + "'"
                    + ", sourceLineNumber="
                    + sourceLineNumber
                    + ", sinkLineNumber="
                    + sinkLineNumber
                    + "}";
        }

        @Override
        public boolean equals(final Object o) {
            // If the super implementation determines that the objects aren't equal, then this isn't
            // going to change anything.
            if (!super.equals(o)) {
                return false;
            }
            // If the other object isn't a path-based violation, it can't be equal to this one.
            if (!(o instanceof PathBasedRuleViolation)) {
                return false;
            }

            // If we're at this point, then we're looking at two path-based violations with the same
            // source vertex. Use
            // the sink vertex to finish the equality check.
            PathBasedRuleViolation otherViolation = (PathBasedRuleViolation) o;
            return Objects.equals(sinkFileName, otherViolation.sinkFileName)
                    && sinkLineNumber == otherViolation.sinkLineNumber
                    && sinkColumnNumber == otherViolation.sinkColumnNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    ruleName,
                    message,
                    sourceLineNumber,
                    sourceColumnNumber,
                    sourceFileName,
                    sourceType,
                    sourceVertexName,
                    severity,
                    description,
                    category,
                    sinkLineNumber,
                    sinkColumnNumber,
                    sinkFileName);
        }

        @Override
        public int compareTo(Violation otherViolation) {
            final int superResult = super.compareTo(otherViolation);
            // If the super implementation was able to determine an ordering, use that.
            if (superResult != 0) {
                return superResult;
            }

            // Otherwise, we're looking at two PathBasedRuleViolation objects with the same source
            // vertex. Use the sink
            // vertex as the tiebreaker.
            return Comparator.comparing(PathBasedRuleViolation::getSinkFileName)
                    .thenComparing(PathBasedRuleViolation::getSinkLineNumber)
                    .thenComparing(PathBasedRuleViolation::getSinkColumnNumber)
                    .compare(this, (PathBasedRuleViolation) otherViolation);
        }
    }

    /**
     * An artificial "violation" not associated with a rule, but used as a representation of an
     * error that occurred during rule execution.
     */
    public static final class InternalErrorViolation extends Violation {
        /** Used as a hardcoded message for {@link InternalErrorViolation} objects. */
        private static final String SIMPLIFIED_MESSAGE =
                "Internal error. Work in progress. Please ignore.";
        /**
         * A more thorough description of the violation than {@link #message}, not displayed to
         * users of the plugin, but useful for internal debugging and analysis.
         */
        private final String details;

        /**
         * Creates an {@link InternalErrorViolation} object whose {@link #message} is hardcoded to
         * {@link #SIMPLIFIED_MESSAGE}, and whose {@link #details} property is provided as input.
         *
         * @param details - The value to be inserted into the {@link #details} property.
         */
        public InternalErrorViolation(final String details, final SFVertex vertex) {
            super(SIMPLIFIED_MESSAGE, vertex);
            this.details = details;
            // Internal error violations don't have a rule associated with them, so we should set
            // the rule-related properties
            // to non-null values now, to avoid NPEs during sorting.
            this.ruleName = "";
            this.category = "";
            this.description = "";
            this.severity = AbstractRule.SEVERITY.LOW.code;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return "InternalErrorViolation{"
                    + "message='"
                    + message
                    + '\''
                    + ", details="
                    + details
                    + ", sourceLineNumber="
                    + sourceLineNumber
                    + '}';
        }

        @Override
        public boolean equals(final Object o) {
            // If the super implementation determines that the objects aren't equal, then this isn't
            // going to change anything.
            if (!super.equals(o)) {
                return false;
            }
            // If the other object isn't a internal error violation, it can't be equal to this one.
            if (!(o instanceof InternalErrorViolation)) {
                return false;
            }
            // The super implementation compared everything except for the `details` property, so we
            // need to handle that one.
            return Objects.equals(details, ((InternalErrorViolation) o).getDetails());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    ruleName,
                    message,
                    details,
                    sourceLineNumber,
                    sourceColumnNumber,
                    sourceFileName,
                    sourceType,
                    sourceVertexName,
                    severity,
                    description,
                    category);
        }

        @Override
        public int compareTo(final Violation otherViolation) {
            final int superResult = super.compareTo(otherViolation);
            // If the super implementation was able to determine an ordering, we can use that.
            if (superResult != 0) {
                return superResult;
            }
            // If the super comparator wasn't able to determine an ordering, then it means we're
            // looking at two
            // InternalErrorViolations objects whose base properties are all identical.
            // `getDetails()` acts as a tiebreaker.
            return Comparator.comparing(InternalErrorViolation::getDetails)
                    .compare(this, (InternalErrorViolation) otherViolation);
        }
    }

    /**
     * An artificial "violation" not associated with a rule, but used to indicate that a rule could
     * not be evaluated in the allotted time limit.
     */
    public static final class TimeoutViolation extends Violation {
        public TimeoutViolation(final String message, final SFVertex vertex) {
            super(message, vertex);
            // Timeout violations don't have an actual rule associated with them, so we should set
            // the rule-related properties
            // to non-null values now, to avoid NPEs during sorting.
            this.ruleName = "";
            this.category = "";
            this.description = "";
            this.severity = AbstractRule.SEVERITY.LOW.code;
        }
    }
}
