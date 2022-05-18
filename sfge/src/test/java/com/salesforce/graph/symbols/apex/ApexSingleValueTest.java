package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexSingleValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * Attempting to put a null key causes a NullPointerException when run in an org. This test
     * validates that the invalid path is excluded because a {@link NullValueAccessedException} is
     * thrown when the #put method is called.
     */
    @Test
    public void testNullKeyPathIsExcluded() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + " 	public static void doSomething(String objectName, String value) {\n"
                    + "   	SObject sObj = Schema.getGlobalDescribe().get(objectName).newSObject();\n"
                    + "       String keyName = null;\n"
                    + "       if (unresolvedMethod()) {\n"
                    + "       	keyName = 'not-null';\n"
                    + "    	}\n"
                    + "       sObj.put(keyName, value);\n"
                    + "       System.debug(sObj);\n"
                    + "    }\n"
                    + "}\n"
        };

        final TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        final SystemDebugAccumulator visitor = result.getVisitor();

        final ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        final Map<ApexValue<?>, ApexValue<?>> objectProperties =
                apexSingleValue.getApexValueProperties();
        MatcherAssert.assertThat(objectProperties.entrySet(), hasSize(equalTo(1)));
        final Map.Entry<ApexValue<?>, ApexValue<?>> entry =
                objectProperties.entrySet().stream().collect(Collectors.toList()).get(0);
        final ApexStringValue key = (ApexStringValue) entry.getKey();
        MatcherAssert.assertThat(TestUtil.apexValueToString(key), equalTo("not-null"));
        final ApexStringValue value = (ApexStringValue) entry.getValue();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }
}
