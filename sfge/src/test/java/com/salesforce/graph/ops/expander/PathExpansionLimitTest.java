package com.salesforce.graph.ops.expander;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathExpansionLimitTest {
    private GraphTraversalSource g;
    private static final int PATH_EXPANSION_LIMIT_OVERRIDE = 1;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
        SfgeConfigTestProvider.set(
                new TestSfgeConfig() {
                    @Override
                    public int getPathExpansionLimit() {
                        return PATH_EXPANSION_LIMIT_OVERRIDE;
                    }
                });
    }

    @AfterEach
    public void cleanUp() {
        SfgeConfigTestProvider.remove();
    }

    @Test
    public void testPathExpansionLimitExceeded() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void doSomething(boolean b, integer i) {\n"
                        + "       if (b) {\n"
                        + "           method1(i);\n"
                        + "       } else {\n"
                        + "           method2(i);\n"
                        + "       }\n"
                        + "   }\n"
                        + "   void method1(integer i) {\n"
                        + "           if (i > 10) {\n"
                        + "       System.debug('option 1-1');\n"
                        + "           } else {"
                        + "               System.debug('option 1-2');\n"
                        + "   }\n"
                        + "   }\n"
                        + "   void method2() {\n"
                        + "           if (i > 10) {\n"
                        + "       System.debug('option 2-1');\n"
                        + "           } else {"
                        + "               System.debug('option 2-2');\n"
                        + "   }\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.walkPath(g, sourceCode);
    }
}
