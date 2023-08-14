package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import java.io.File;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class GraphUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        // Get a unique graph since this will load the standard apex classes
        this.g = TestUtil.getUniqueGraph();
    }

    @Test
    public void verifyTriggersAddedToGraph(TestInfo testInfo) {
        try {
            TestUtil.compileTestFiles(g, testInfo);
        } catch (GraphUtil.GraphLoadException ex) {
            fail("failed to create graph");
        }

        Map<Object, Object> triggerVertex =
                g.V().hasLabel(ASTConstants.NodeType.USER_TRIGGER).elementMap().next();
        assertEquals("AccountBefore", triggerVertex.get(Schema.NAME));
        assertEquals("AccountBefore", triggerVertex.get(Schema.DEFINING_TYPE));
        assertEquals("Account", triggerVertex.get(Schema.TARGET_NAME));
    }

    /** Verify that a class which is defined in multiple directories results in an exception. */
    @Test
    public void testDuplicateUserClassesThrowsException(TestInfo testInfo) {
        GraphUtil.GraphLoadException ex =
                Assertions.assertThrows(
                        GraphUtil.GraphLoadException.class,
                        () -> TestUtil.compileTestFiles(g, testInfo));
        MatcherAssert.assertThat(
                ex.getMessage(), containsString("MyClass is defined in multiple files."));
        MatcherAssert.assertThat(
                ex.getMessage(), containsString("directory1" + File.separator + "MyClass.cls"));
        MatcherAssert.assertThat(
                ex.getMessage(), containsString("directory2" + File.separator + "MyClass.cls"));
    }

    /** Verify that a trigger which is defined in multiple directories results in an exception. */
    @Test
    public void testDuplicateTriggersThrowsException(TestInfo testInfo) {
        GraphUtil.GraphLoadException ex =
                Assertions.assertThrows(
                        GraphUtil.GraphLoadException.class,
                        () -> TestUtil.compileTestFiles(g, testInfo));
        MatcherAssert.assertThat(
                ex.getMessage(),
                containsString("BeforeInsertAccount is defined in multiple files."));
        MatcherAssert.assertThat(
                ex.getMessage(),
                containsString("directory1" + File.separator + "BeforeInsertAccount.trigger"));
        MatcherAssert.assertThat(
                ex.getMessage(),
                containsString("directory2" + File.separator + "BeforeInsertAccount.trigger"));
    }

    /** Verify that a class and a trigger with the same name does not result in an exception. */
    @Test
    public void testClassAndTriggerSameNameOK(TestInfo testInfo) {
        Assertions.assertDoesNotThrow(() -> TestUtil.compileTestFiles(g, testInfo));
    }

    /** Verify that an enum and a trigger with the same name does not result in an exception. */
    @Test
    public void testEnumAndTriggerSameNameOK(TestInfo testInfo) {
        Assertions.assertDoesNotThrow(() -> TestUtil.compileTestFiles(g, testInfo));
    }

    /**
     * Verify that an interface and a trigger with the same name does not result in an exception.
     */
    @Test
    public void testInterfaceAndTriggerSameNameOK(TestInfo testInfo) {
        Assertions.assertDoesNotThrow(() -> TestUtil.compileTestFiles(g, testInfo));
    }
}
