package com.salesforce.testutils;

import com.salesforce.TestUtil;
import com.salesforce.rules.AbstractPathBasedRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BasePathBasedRuleTest {
    private static final Logger LOGGER = LogManager.getLogger(BasePathBasedRuleTest.class);
    private static final String CLASS_NAME = "MyClass";
    protected GraphTraversalSource g;

    @RegisterExtension
    public BeforeEachCallback watcher =
            context -> LOGGER.info("Starting test: " + context.getTestMethod().get().getName());

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Assert that no violations are thrown when the given rule is executed against on the source
     * code
     *
     * @param sourceCode to execute rule against
     */
    protected void assertNoViolation(AbstractPathBasedRule rule, String sourceCode) {
        assertNoViolation(rule, new String[] {sourceCode});
    }

    protected void assertNoViolation(AbstractPathBasedRule rule, String[] sourceCode) {
        assertNoViolation(rule, sourceCode, "foo", "MyClass");
    }

    protected void assertNoViolation(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            String definingMethod,
            String definingType) {
        getValidatorBuilder(rule, sourceCode, definingMethod, definingType)
                .build()
                .assertNoViolation();
    }

    /**
     * Assert that the expected violations are thrown when the given rule is executed against the
     * source code
     *
     * @param sourceCode to execute the rule against
     * @param expectedViolations violations that are expected
     */
    protected void assertViolations(
            AbstractPathBasedRule rule,
            String sourceCode,
            ViolationWrapper.ViolationBuilder... expectedViolations) {
        assertViolations(rule, new String[] {sourceCode}, expectedViolations);
    }

    protected void assertViolations(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            ViolationWrapper.ViolationBuilder... expectedViolations) {
        assertViolations(
                rule, sourceCode, "foo", "MyClass", TestUtil.FIRST_FILE, 2, expectedViolations);
    }

    protected void assertViolations(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            String definingMethod,
            String definingType,
            String fileName,
            int sourceLine,
            ViolationWrapper.ViolationBuilder... expectedViolations) {
        final ViolationValidator.Builder validatorBuilder =
                getValidatorBuilder(rule, sourceCode, definingMethod, definingType);

        for (ViolationWrapper.ViolationBuilder expectedViolation : expectedViolations) {
            final ViolationWrapper violationWrapper =
                    expectedViolation
                            .withFileName(fileName)
                            .withSourceLine(sourceLine)
                            .withDefiningType(definingType)
                            .withDefiningMethod(definingMethod)
                            .build();
            validatorBuilder.expectViolation(violationWrapper);
        }
        validatorBuilder.build().assertViolations();
    }

    protected ViolationValidator.Builder getValidatorBuilder(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            String definingMethod,
            String definingType) {
        return ViolationValidator.Builder.get(g, sourceCode, rule, definingMethod, definingType);
    }
}
