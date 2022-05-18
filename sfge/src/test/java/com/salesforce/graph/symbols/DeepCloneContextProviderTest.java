package com.salesforce.graph.symbols;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TODO: {@link DeepCloneContextProvider} is difficult to test without expanding the path and
 * walking the paths. Create a better way to test this while only expanding the paths.
 */
public class DeepCloneContextProviderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Verify that an ApexCustomValue passed as a method parameter to a method that forks correctly
     * maintains its state. This test fails if the {@link DeepCloneContextProvider} is disabled.
     * This is because the fork caused by the #addNamespace method results in each scope creating
     * its own clone of the MySettings__c instance created on line 3. Cloning the instance without
     * the context results in each scope obtaining a new unrelated instance of a MySettings__c
     * object.
     */
    @Test
    public void testCollapseMiddleFork() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MySettings__c settings = MySettings__c.getOrgDefaults();\n"
                    + "       configSettings1(settings);\n"
                    + "       System.debug(settings.MyField1__c);\n"
                    + "       configSettings2(settings);\n"
                    + "       System.debug(settings.MyField1__c);\n"
                    + "   }\n"
                    + "   public static void configSettings1(MySettings__c settings) {\n"
                    + "       addNamespace(settings, 'Foo');\n"
                    + "   }\n"
                    + "   public static void configSettings2(MySettings__c settings) {\n"
                    + "       if (settings.MyField1__c == null) {\n"
                    + "          settings.MyField1__c = 'Impossible';\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public static void addNamespace(MySettings__c settings, String str) {\n"
                    + "        if (str == '') {\n"
                    + "           settings.MyField1__c = 'Bar';\n"
                    + "        } else {\n"
                    + "           settings.MyField1__c = str;\n"
                    + "        }\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        for (Optional<ApexValue<?>> value : visitor.getAllResults()) {
            MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Foo"));
        }
    }
}
