package com.salesforce.graph.symbols;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class StaticCodeBlockInvocationTest {
    protected GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSingleStaticBlockFromStaticMethod() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(2));

        // verify that static block is invoked first
        ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
        assertThat(stringValue.getValue().get(), equalTo("static block"));
    }

    @Test
    public void testStaticBlockFromConstructor() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		StaticBlockClass sb = new StaticBlockClass();\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(1));

        // verify that static block is invoked first
        ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
        assertThat(stringValue.getValue().get(), equalTo("static block"));
    }

    @Test
    public void testNoStaticBlock() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n" + " public static String foo = 'hello';\n" + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo);\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(1));

        // verify non-static-block case is handled correctly
        ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
        assertThat(stringValue.getValue().get(), equalTo("hello"));
    }

    @Test
    @Disabled // TODO: static field defined along with static block should not be double-invoked
    public void testSingleStaticBlockAndField() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + " static String myStr = 'hello';\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(2));

        // verify that static block is invoked first
        ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
        assertThat(stringValue.getValue().get(), equalTo("static block"));
    }

    @Test
    @Disabled // TODO: static field defined along with static block should not be double-invoked
    public void testSingleStaticBlockAndFieldInvokingAMethod() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + " static String myStr = foo();\n"
                    + " public static String bar() {\n"
                    + "	return 'bar';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.bar());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(2));

        // verify that static block is invoked first
        ApexStringValue stringValue = (ApexStringValue) allResults.get(0).get();
        assertThat(stringValue.getValue().get(), equalTo("static block"));
    }

    @Test
    public void testMultipleStaticBlocks() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block 1');\n"
                    + "	}\n"
                    + "	static {\n"
                    + "		System.debug('static block 2');\n"
                    + "	}\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        final List<String> resultStrings = getResultStrings(allResults);
        assertTrue(resultStrings.contains("static block 1"));
        assertTrue(resultStrings.contains("static block 2"));
        assertTrue(resultStrings.contains("hello"));
    }

    @Test
    @Disabled // TODO: fix issue where static block 2 is invoked twice instead of once
    public void testEachStaticBlockIsInvokedOnlyOnce() {
        String[] sourceCode = {
            "public class StaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block 1');\n"
                    + "	}\n"
                    + "	static {\n"
                    + "		System.debug('static block 2');\n"
                    + "	}\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(
                allResults,
                hasSize(3)); // TODO: this is currently returning 4 where static block 2 gets
        // invoked twice
    }

    @Test
    public void testSuperClassStaticBlocks() {
        String[] sourceCode = {
            "public class SuperStaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('super static block');\n"
                    + "	}\n"
                    + "}",
            "public class StaticBlockClass extends SuperStaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        final List<String> resultStrings = getResultStrings(allResults);
        assertTrue(resultStrings.contains("super static block"));
        assertTrue(resultStrings.contains("static block"));
        assertTrue(resultStrings.contains("hello"));
    }

    @Test
    @Disabled // TODO: super class's static block should be invoked only once
    public void testSuperClassStaticBlocksInvokedOnceEach() {
        String[] sourceCode = {
            "public class SuperStaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('super static block');\n"
                    + "	}\n"
                    + "}",
            "public class StaticBlockClass extends SuperStaticBlockClass {\n"
                    + "	static {\n"
                    + "		System.debug('static block');\n"
                    + "	}\n"
                    + " public static String foo() {\n"
                    + "	return 'hello';\n"
                    + "}\n"
                    + "}",
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(StaticBlockClass.foo());\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        assertThat(allResults, hasSize(3));
    }

    private List<String> getResultStrings(List<Optional<ApexValue<?>>> allResults) {
        return allResults.stream()
                .map(r -> ((ApexStringValue) r.get()).getValue().get())
                .collect(Collectors.toList());
    }
}
