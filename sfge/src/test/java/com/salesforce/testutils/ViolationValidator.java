package com.salesforce.testutils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.Violation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;

/**
 * Validates violation by executing a source code and comparing the contents of violations
 * encountered against the expected set of violations.
 */
public class ViolationValidator {
    private final GraphTraversalSource g;
    private final String[] sourceCode;
    private final AbstractPathBasedRule pathBasedRule;
    private final @Nullable Boolean renderXml;
    private final String definingType;
    private final String definingMethod;
    private final Set<ViolationWrapper> expectedViolations;

    private ViolationValidator(Builder builder) {
        this.g = builder.g;
        this.sourceCode = builder.sourceCode;
        this.pathBasedRule = builder.pathBasedRule;
        this.renderXml = builder.renderXml;
        this.definingType = builder.definingType;
        this.definingMethod = builder.definingMethod;
        this.expectedViolations = builder.expectedViolations;
    }

    public void assertViolations() {
        final List<Violation> violationsEncountered = getViolations();

        final Set<ViolationWrapper> actualViolations = getWrappedViolations(violationsEncountered);
        MatcherAssert.assertThat(actualViolations, equalTo(expectedViolations));
    }

    public void assertNoViolation() {
        MatcherAssert.assertThat(
                "Invalid test setup. Do not add expectViolations() when checking for no violations",
                expectedViolations,
                empty());
        final List<Violation> violationsEncountered = getViolations();

        MatcherAssert.assertThat("No violations expected", violationsEncountered, empty());
    }

    private List<Violation> getViolations() {
        return TestUtil.getViolations(
                g, sourceCode, pathBasedRule, definingType, definingMethod, renderXml);
    }

    private Set<ViolationWrapper> getWrappedViolations(List<Violation> violations) {
        final Set<ViolationWrapper> wrappedViolations = new HashSet<>();
        for (Violation violation : violations) {
            // For path-based violations, the violation "occurs" at the sink vertex. All other
            // violations only have a
            // source vertex.
            final int violationLine =
                    violation instanceof Violation.PathBasedRuleViolation
                            ? ((Violation.PathBasedRuleViolation) violation).getSinkLineNumber()
                            : violation.getSourceLineNumber();
            final ViolationWrapper violationWrapper =
                    ViolationWrapper.MessageBuilder.get(violationLine, violation.getMessage())
                            .build();
            wrappedViolations.add(violationWrapper);
        }
        return wrappedViolations;
    }

    /**
     * Use this builder class to instantiate ViolationValidator. This builder takes care of setting
     * default values that can be replaced on a need-basis.
     */
    public static final class Builder {
        private final GraphTraversalSource g;
        private final String[] sourceCode;
        private final AbstractPathBasedRule pathBasedRule;
        /** Rely on the defaulting in TestUtil if this is not set */
        private @Nullable Boolean renderXml;

        private final String definingType;
        private final String definingMethod;
        private final Set<ViolationWrapper> expectedViolations;

        private Builder(
                GraphTraversalSource g,
                String[] sourceCode,
                AbstractPathBasedRule pathBasedRule,
                String definingMethod,
                String definingType) {
            this.g = g;
            this.sourceCode = sourceCode;
            this.pathBasedRule = pathBasedRule;
            this.definingMethod = definingMethod;
            this.definingType = definingType;
            this.expectedViolations = new HashSet<>();
        }

        public static Builder get(
                GraphTraversalSource g,
                String[] sourceCode,
                AbstractPathBasedRule pathBasedRule,
                String definingMethod,
                String definingType) {
            return new Builder(g, sourceCode, pathBasedRule, definingMethod, definingType);
        }

        public ViolationValidator build() {
            return new ViolationValidator(this);
        }

        public Builder withRenderXml(boolean renderXml) {
            this.renderXml = renderXml;
            return this;
        }

        public Builder expectViolation(ViolationWrapper violationWrapper) {
            this.expectedViolations.add(violationWrapper);
            return this;
        }
    }
}
