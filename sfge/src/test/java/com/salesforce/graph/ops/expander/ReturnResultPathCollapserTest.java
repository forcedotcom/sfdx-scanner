package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReturnResultPathCollapserTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testNullValueIsNotCollapsed() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething(Boolean b) {\n"
                    + "       List<String> l = getList(b);\n"
                    + "       System.out.println(l);\n"
                    + "    }\n"
                    + "    public List<String> getList(Boolean b) {\n"
                    + "       if (b) {\n"
                    + "       	return new List<String>{'Hello', 'Goodbye'};\n"
                    + "    	} else {\n"
                    + "       	return null;\n"
                    + "    	}\n"
                    + "    }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        return ApexPathExpanderConfig.Builder.get()
                .expandMethodCalls(true)
                .with(ReturnResultPathCollapser.getInstance())
                .build();
    }

    private TestRunner.Result<SystemDebugAccumulator> walkPath(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPath();
    }

    private List<TestRunner.Result<SystemDebugAccumulator>> walkPaths(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPaths();
    }
}
