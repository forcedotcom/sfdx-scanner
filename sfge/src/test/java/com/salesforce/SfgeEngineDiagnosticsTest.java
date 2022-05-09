package com.salesforce;

import static org.hamcrest.Matchers.containsString;

import com.salesforce.exception.CircularReferenceException;
import com.salesforce.exception.UnexpectedException;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for bugs in the SFGE engine. These scenarios often require the tests to use Apex that would
 * fail to compile in an org. We can use this invalid Apex to trigger conditions that the SFGE
 * engine sees as an indication of a bug in the sfge engine itself.
 */
public class SfgeEngineDiagnosticsTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * This is invalid Apex, the <code>values</code> variable is final and assigned to twice. We
     * currently throw an exception if a final variable is assigned to twice to ensure that the sfge
     * code doesn't have a bug. This is not done to validate the Apex. This only works for variables
     * initialized inline. It doesn't currently catch situations where the same value is set twice
     * in the constructor.
     */
    @Test
    public void testInstanceFinalVariableInitializedInlineAndInConstructor() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "		MyOtherClass c = new MyOtherClass();\n"
                    + "		c.logValues();\n"
                    + "	}\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    +
                    // First assignment
                    "	public final Set<String> values = new Set<String>{'value1', 'value2'};\n"
                    + "	public MyOtherClass() {\n"
                    +
                    // Second assignment
                    "		this.values = new Set<String>();\n"
                    + "	}\n"
                    + "	public void logValues() {\n"
                    + "		System.debug(values);\n"
                    + "	}\n"
                    + "}\n",
        };

        final UnexpectedException ex =
                Assertions.assertThrows(
                        UnexpectedException.class, () -> TestRunner.walkPath(g, sourceCode));
        MatcherAssert.assertThat(ex.getMessage(), containsString("Variable is final. key=values"));
    }

    public static Stream<Arguments> testCircularApexList() {
        return Stream.of(
                Arguments.of("l.add(l)"),
                Arguments.of("l.add(0, l)"),
                Arguments.of("l.addAll(l)"),
                Arguments.of("l.set(0, l)"),
                Arguments.of("l.addAll(s)"));
    }

    /** Ensure that a list can never be used as a parameter to itself for any mutating methods */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: expression=({0})")
    public void testCircularApexList(String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "		List<String> l = new List<String>();\n"
                    + "		Set<List<String>> s = new Set<List<String>>();\n"
                    + "		s.add(l);\n"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}\n"
        };

        Assertions.assertThrows(
                CircularReferenceException.class, () -> TestRunner.walkPath(g, sourceCode));
    }

    public static Stream<Arguments> testCircularApexMap() {
        return Stream.of(
                Arguments.of("m.put(m, 's')"),
                Arguments.of("m.put('s', m)"),
                Arguments.of("m.remove(m)"));
    }

    /** Ensure that a map can never be used as a parameter to itself for any mutating methods */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: expression=({0})")
    public void testCircularApexMap(String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "		Map<String, String> m = new Map<String, String>();\n"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}\n"
        };

        Assertions.assertThrows(
                CircularReferenceException.class, () -> TestRunner.walkPath(g, sourceCode));
    }

    public static Stream<Arguments> testCircularApexSet() {
        return Stream.of(Arguments.of("s.add(s)"));
    }

    /** Ensure that a set can never be used as a parameter to itself for any mutating methods */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: expression=({0})")
    public void testCircularApexSet(String expression) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "		Set<String> s = new Set<String>();\n"
                    + expression
                    + ";\n"
                    + "	}\n"
                    + "}\n"
        };

        Assertions.assertThrows(
                CircularReferenceException.class, () -> TestRunner.walkPath(g, sourceCode));
    }
}
