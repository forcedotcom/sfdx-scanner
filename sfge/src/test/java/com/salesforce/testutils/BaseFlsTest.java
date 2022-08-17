package com.salesforce.testutils;

import com.salesforce.TestUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Base test for all FLS tests TODO: more FLS specific contents would be moved here */
public abstract class BaseFlsTest {
    private static final Logger LOGGER = LogManager.getLogger(BaseFlsTest.class);
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

    protected void assertNoVerticesFound(
            AbstractPathBasedRule rule, String[] sourceCode, int line) {
        final ViolationValidator.Builder validatorBuilder =
                getValidatorBuilder(rule, sourceCode, "foo", "MyClass");
        ViolationWrapper violationWrapper =
                ViolationWrapper.MessageBuilder.get(line, "No vertices found").build();
        validatorBuilder.expectViolation(violationWrapper);
        validatorBuilder.build().assertViolations();
    }

    /**
     * @return the maximum line number that contains the <code>validationType</code> in class {@link
     *     #CLASS_NAME}
     */
    protected int getMaximumLine(FlsConstants.FlsValidationType validationType) {
        final List<Integer> dmlLines = getLinesWithDmlStatement(validationType);
        return Collections.max(dmlLines);
    }

    /**
     * @return the minimum line number that contains the <code>validationType</code> in class {@link
     *     #CLASS_NAME}
     */
    protected int getMinimumLine(FlsConstants.FlsValidationType validationType) {
        final List<Integer> dmlLines = getLinesWithDmlStatement(validationType);
        return Collections.min(dmlLines);
    }

    /**
     * @return the line number that contains the <code>validationType</code> in class {@link
     *     #CLASS_NAME}
     */
    protected int getLineWithDmlStatement(FlsConstants.FlsValidationType validationType) {
        return SFVertexFactory.load(
                        g,
                        g.V().hasLabel(validationType.dmlStatementType)
                                .has(Schema.DEFINING_TYPE, CLASS_NAME))
                .getBeginLine();
    }

    /**
     * @return the line numbers that contain <code>validationType</code>s sorted in ascending order
     *     in class {@link #CLASS_NAME}
     */
    protected List<Integer> getLinesWithDmlStatement(
            FlsConstants.FlsValidationType validationType) {
        return SFVertexFactory.loadVertices(
                        g,
                        g.V().hasLabel(validationType.dmlStatementType)
                                .has(Schema.DEFINING_TYPE, CLASS_NAME)
                                .order(Scope.global)
                                .by(Schema.DEFINING_TYPE, Order.asc))
                .stream()
                .map(BaseSFVertex::getBeginLine)
                .collect(Collectors.toList());
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
            ViolationWrapper.FlsViolationBuilder... expectedViolations) {
        assertViolations(rule, new String[] {sourceCode}, expectedViolations);
    }

    protected void assertViolations(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            ViolationWrapper.FlsViolationBuilder... expectedViolations) {
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
            ViolationWrapper.FlsViolationBuilder... expectedViolations) {
        final ViolationValidator.Builder validatorBuilder =
                getValidatorBuilder(rule, sourceCode, definingMethod, definingType);

        for (ViolationWrapper.FlsViolationBuilder expectedViolation : expectedViolations) {
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

    /**
     * Registers the contents of the FLS violation that can be expected by executing the rule
     * against the source code
     *
     * @param line where the violation should be thrown
     * @param validationType type of validation that should be found lacking
     * @param objectName name of object that the violation is thrown for
     * @return
     */
    protected ViolationWrapper.FlsViolationBuilder expect(
            int line, FlsConstants.FlsValidationType validationType, String objectName) {
        return ViolationWrapper.FlsViolationBuilder.get(line, validationType, objectName);
    }

    protected ViolationWrapper.FlsViolationBuilder expectStripInaccWarning(
            int line, FlsConstants.FlsValidationType validationType, String objectName) {
        return ViolationWrapper.FlsViolationBuilder.get(line, validationType, objectName)
                .forViolationType(ViolationWrapper.FlsViolationType.STRIP_INACCESSIBLE_WARNING);
    }

    protected ViolationWrapper.FlsViolationBuilder expectUnresolvedCrudFls(
            int line, FlsConstants.FlsValidationType validationType) {
        return ViolationWrapper.FlsViolationBuilder.get(line, validationType)
                .forViolationType(ViolationWrapper.FlsViolationType.UNRESOLVED_CRUD_FLS);
    }

    protected ViolationWrapper.MessageBuilder expect(int line, String message) {
        return ViolationWrapper.MessageBuilder.get(line, message);
    }

    private ViolationValidator.Builder getValidatorBuilder(
            AbstractPathBasedRule rule,
            String[] sourceCode,
            String definingMethod,
            String definingType) {
        final ViolationValidator.Builder builder =
                ViolationValidator.Builder.get(g, sourceCode, rule, definingMethod, definingType);
        return builder;
    }
}
