package com.salesforce.testutils;

import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;

/** Base test for all FLS tests TODO: more FLS specific contents would be moved here */
public abstract class BaseFlsTest extends BasePathBasedRuleTest {
    private static final String CLASS_NAME = "MyClass";

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
                        g.V()
                                .hasLabel(validationType.dmlStatementType)
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
                        g.V()
                                .hasLabel(validationType.dmlStatementType)
                                .has(Schema.DEFINING_TYPE, CLASS_NAME)
                                .order(Scope.global)
                                .by(Schema.DEFINING_TYPE, Order.asc))
                .stream()
                .map(BaseSFVertex::getBeginLine)
                .collect(Collectors.toList());
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
}
