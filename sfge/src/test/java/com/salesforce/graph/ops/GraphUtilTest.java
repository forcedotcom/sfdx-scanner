package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.containsString;

import com.salesforce.TestUtil;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import java.io.File;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GraphUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        // Get a unique graph since this will load the standard apex classes
        this.g = TestUtil.getUniqueGraph();
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

    /** Verify that parse errors can be ignored */
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testParseErrorIsIgnored(boolean ignoreParseError, TestInfo testInfo)
            throws GraphUtil.GraphLoadException {
        if (ignoreParseError) {
            try {
                SfgeConfigTestProvider.set(
                        new TestSfgeConfig() {
                            @Override
                            public boolean shouldIgnoreParseErrors() {
                                return true;
                            }
                        });
                TestUtil.compileTestFiles(g, testInfo);
            } finally {
                SfgeConfigTestProvider.remove();
            }
        } else {
            GraphUtil.GraphLoadException ex =
                    Assertions.assertThrows(
                            GraphUtil.GraphLoadException.class,
                            () -> TestUtil.compileTestFiles(g, testInfo));
            MatcherAssert.assertThat(ex.getMessage(), containsString("Fix compilation errors"));
            MatcherAssert.assertThat(
                    ex.getMessage(),
                    containsString("testParseErrorIsIgnored" + File.separator + "MyClass.cls"));
        }
    }
}
