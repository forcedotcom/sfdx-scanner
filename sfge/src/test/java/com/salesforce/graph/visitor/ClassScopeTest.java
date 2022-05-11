package com.salesforce.graph.visitor;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexMapValue;
import com.salesforce.graph.symbols.apex.ApexSetValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.matchers.TestRunnerListMatcher;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// TODO: Test this with mocks, this overlaps with the DefaultSymbolProviderTest, there is a bit of a
// circular dependency
public class ClassScopeTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testInlineAssignmentInstanceLiterals() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final String a = 'Hello';\n"
                        + "    private final String b = 'Goodbye';\n"
                        + "    private String c = 'Congratulations';\n"
                        + "    private String d;\n"
                        + "    private final String e;\n"
                        + "    public void doSomething() {\n"
                        + "       System.debug(a);\n"
                        + "       System.debug(b);\n"
                        + "       System.debug(c);\n"
                        + "       System.debug(d);\n"
                        + "       System.debug(e);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result,
                TestRunnerMatcher.hasValues(
                        "Hello",
                        "Goodbye",
                        "Congratulations",
                        // Class members are set to null by default
                        null,
                        null));
    }

    @Test
    public void testForkedInlineAssignmentInstanceLiterals() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a = MyOtherClass1.getString();\n"
                    + "    private final String b = MyOtherClass1.getString();\n"
                    + "    public void doSomething() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass1 {\n"
                    + "	public static string getString() {\n"
                    + "   	return MyOtherClass2.getString();\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass2 {\n"
                    + "	public static string getString() {\n"
                    + "   	if (UnresolvedClass.getSetting()) {\n"
                    + "			return 'value1';\n"
                    + "		} else {\n"
                    + "			return 'value2';\n"
                    + "   	}\n"
                    + "   }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(4)));
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            String a = TestUtil.apexValueToString(visitor.getResult(0));
            String b = TestUtil.apexValueToString(visitor.getResult(1));
            pairs.add(Pair.of(a, b));
        }
        MatcherAssert.assertThat(
                pairs,
                containsInAnyOrder(
                        Pair.of("value1", "value1"),
                        Pair.of("value1", "value2"),
                        Pair.of("value2", "value1"),
                        Pair.of("value2", "value2")));
    }

    @Test
    public void testNewObjectExpressionIsTreatedAsPathInlineAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a = 'Hello';\n"
                    + "    public void doSomethingElse() {\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.doSomethingElse();\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testNewObjectExpressionIsTreatedAsPathConstructorAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    public MyClass() {\n"
                    + "       a = 'Hello';\n"
                    + "    }\n"
                    + "    public void doSomethingElse() {\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.doSomethingElse();\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testClassScopeWithParameters() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void invocableMethod(String s) {\n"
                        + "       System.debug(LoggingLevel.ERROR, s);\n"
                        + "   }\n"
                        + "\n"
                        + "   public void testedMethod(String s1, Boolean b1, Integer i1) {\n"
                        + "       invocableMethod(s1);\n"
                        + "   }\n"
                        + "}";

        // Build the graph.
        TestUtil.buildGraph(g, sourceCode);

        // Get the vertex corresponding to the method call.
        MethodCallExpressionVertex mcev =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 7);

        // Get all paths backwards into that method call. (There's only one.)
        ApexPath path = ApexPathUtil.getReversePaths(g, mcev).get(0);

        // Define a map where we'll store the parameter vertices we seek.
        Map<String, Typeable> paramVerticesByName = new HashMap<>();

        // Declare an overridden visitor that we can use for our test.
        PathVertexVisitor pvv =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        // We only want to do extra things when this vertex is the targeted vertex.
                        if (vertex.equals(mcev)) {
                            // Get the declaration of each of the parameters.
                            paramVerticesByName.put(
                                    "s1", symbols.getTypedVertex("s1").orElse(null));
                            paramVerticesByName.put(
                                    "b1", symbols.getTypedVertex("b1").orElse(null));
                            paramVerticesByName.put(
                                    "i1", symbols.getTypedVertex("i1").orElse(null));
                        }
                        return super.visit(vertex, symbols);
                    }
                };

        // Declare a symbol provider.
        SymbolProviderVertexVisitor sp = new DefaultSymbolProviderVertexVisitor(g);

        // Walk the path.
        ApexPathWalker.walkPath(g, path, pvv, sp);

        // During the walking of the path, the parameters should have been added to the map. Verify
        // this now.
        assertEquals(3, paramVerticesByName.size());
        assertNotNull(paramVerticesByName.get("s1"));
        MatcherAssert.assertThat(
                paramVerticesByName.get("s1").getCanonicalType(), equalTo("String"));
        assertNotNull(paramVerticesByName.get("b1"));
        MatcherAssert.assertThat(
                paramVerticesByName.get("b1").getCanonicalType(), equalTo("Boolean"));
        assertNotNull(paramVerticesByName.get("i1"));
        MatcherAssert.assertThat(
                paramVerticesByName.get("i1").getCanonicalType(), equalTo("Integer"));
    }

    @Test
    public void testInlineAssignmentStaticApexValues() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private static Map<String, Integer> theMap = new Map<String, Integer>();\n"
                    + "   public static void doSomething() {\n"
                    + "       System.debug(theMap);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexMapValue apexMapValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexMapValue.isDeterminant(), equalTo(true));
    }

    @Test
    public void testInlineAssignmentWithVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final String a = 'Hello';\n"
                        + "    private final String b = 'Goodbye';\n"
                        + "    private String c = 'Congratulations';\n"
                        + "    private String d;\n"
                        + "    private final String e = a;\n"
                        + "    public void doSomething() {\n"
                        + "       System.debug(a);\n"
                        + "       System.debug(b);\n"
                        + "       System.debug(c);\n"
                        + "       System.debug(d);\n"
                        + "       System.debug(e);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result,
                TestRunnerMatcher.hasValues(
                        "Hello",
                        "Goodbye",
                        "Congratulations",
                        // Class members are set to null by default
                        null,
                        "Hello"));
    }

    // public void testConstructorAssignmentWithParams() {
    // public void testConstructorAssignmentWithVariables() {
    // public void testConstructorAssignmentWithCalloutMethods() {
    // public void testConstructorAssignmentWithNewObjects() {

    @Test
    public void testConstructorAssignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final String a;\n"
                        + "    private final String b;\n"
                        + "    private String c;\n"
                        + "    public MyClass() {\n"
                        + "       a = 'Hello';\n"
                        + "       b = 'Goodbye';\n"
                        + "       c = 'Congratulations';\n"
                        + "    }\n"
                        + "    public void doSomething() {\n"
                        + "       System.debug(a);\n"
                        + "       System.debug(b);\n"
                        + "       System.debug(c);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("Hello", "Goodbye", "Congratulations"));
    }

    @Test
    public void testConstructorAssignmentWithMultiplePaths() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    public MyClass(Boolean b) {\n"
                    + "   	if (b) {\n"
                    + "			a = MyOtherClass.getString1();\n"
                    + "		} else {\n"
                    + "			a = MyOtherClass.getString2();\n"
                    + "   	}\n"
                    + "    }\n"
                    + "    public void doSomething() {\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "	public string getString1() {\n"
                    + "   	return 'value1';\n"
                    + "   }\n"
                    + "	public string getString2() {\n"
                    + "   	return 'value2';\n"
                    + "   }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> result =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerListMatcher.hasValuesAnyOrder("value1", "value2"));
    }

    @Test
    public void testForkedConstructorAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    public MyClass() {\n"
                    + "       a = MyOtherClass1.getString();\n"
                    + "    }\n"
                    + "    public void doSomething() {\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass1 {\n"
                    + "	public string getString() {\n"
                    + "   	return MyOtherClass2.getString2();\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass2 {\n"
                    + "	public string getString2() {\n"
                    + "   	if (UnresolvedClass.getSetting()) {\n"
                    + "			return 'value1';\n"
                    + "		} else {\n"
                    + "			return 'value2';\n"
                    + "   	}\n"
                    + "   }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> result =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerListMatcher.hasValuesAnyOrder("value1", "value2"));
    }

    @Test
    public void testSyntheticSetter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   private String a {\n"
                        + "       get;\n"
                        + "       set;\n"
                        + "   }\n"
                        + "   public MyClass() {\n"
                        + "       a = 'Hello';\n"
                        + "   }\n"
                        + "   public void doSomething() {\n"
                        + "       System.debug(a);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInheritedFields() {
        String[] sourceCode = {
            "public class BaseClass {\n"
                    + "    private String a;\n"
                    + "    public MyClass() {\n"
                    + "    }\n"
                    + "}\n",
            "public class ChildClass extends BaseClass {\n"
                    + "    public void doSomethingElse() {\n"
                    + "       a = 'Hello';\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       ChildClass c = new ChildClass();\n"
                    + "       c.doSomethingElse();\n"
                    + "    }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInheritedConstructor() {
        String[] sourceCode = {
            "public class BaseClass {\n"
                    + "    protected final String a;\n"
                    + "    public BaseClass(String a) {\n"
                    + "       this.a = a;\n"
                    + "    }\n"
                    + "}\n",
            "public class ChildClass extends BaseClass {\n"
                    + "    public ChildClass(String a) {\n"
                    + "       super(a);\n"
                    + "    }\n"
                    + "    public void doSomethingElse() {\n"
                    + "       System.debug(a);\n"
                    + "    }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       ChildClass c = new ChildClass('Hello');\n"
                    + "       c.doSomethingElse();\n"
                    + "    }\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testForkedInitializerWithInstantiatedClass() {
        String[] sourceCode = {
            "public abstract class BaseClass {\n"
                    + "    public String a = MySingleton.getString();\n"
                    + "    public boolean b = false;\n"
                    + "}\n",
            "public class ChildClass extends BaseClass {\n"
                    + "    public ChildClass() {\n"
                    + "       super();\n"
                    + "       this.b = true;\n"
                    + "    }\n"
                    + "    public void doSomethingElse() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "    }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       ChildClass c = new ChildClass();\n"
                    + "       c.doSomethingElse();\n"
                    + "    }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static String getString() {\n"
                    + "       if (SomeOtherClass.getSayHello()) {\n"
                    + "           return 'Hello';\n"
                    + "       } else {\n"
                    + "           return 'Goodbye';\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));
        List<String> greetings = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

            greetings.add(TestUtil.apexValueToString(visitor.getAllResults().get(0)));

            ApexBooleanValue booleanValue = (ApexBooleanValue) visitor.getAllResults().get(1).get();
            MatcherAssert.assertThat(booleanValue.getValue().get(), equalTo(true));
        }
        MatcherAssert.assertThat(greetings, containsInAnyOrder("Hello", "Goodbye"));
    }

    @Test
    public void testForkedInitializerWithRootClass() {
        String[] sourceCode = {
            "public abstract class BaseClass {\n"
                    + "    public String a = MySingleton.getString();\n"
                    + "    public boolean b = false;\n"
                    + "}\n",
            "public class ChildClass extends BaseClass {\n"
                    + "    public ChildClass() {\n"
                    + "       super();\n"
                    + "       this.b = true;\n"
                    + "    }\n"
                    + "    public void doSomething() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "    }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static String getString() {\n"
                    + "       if (SomeOtherClass.getSayHello()) {\n"
                    + "           return 'Hello';\n"
                    + "       } else {\n"
                    + "           return 'Goodbye';\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        List<String> greetings = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

            greetings.add(TestUtil.apexValueToString(visitor.getAllResults().get(0)));

            ApexBooleanValue booleanValue = (ApexBooleanValue) visitor.getAllResults().get(1).get();
            MatcherAssert.assertThat(booleanValue.getValue().get(), equalTo(true));
        }
        MatcherAssert.assertThat(greetings, containsInAnyOrder("Hello", "Goodbye"));
    }

    @Test
    public void testConstructorParameterAssignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    private final String b;\n"
                    + "    private String c;\n"
                    + "    public MyClass(String a1, String b1) {\n"
                    + "       a = a1;\n"
                    + "       b = b1;\n"
                    + "       c = 'Congratulations';\n"
                    + "    }\n"
                    + "    public void logSomething() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "       System.debug(c);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       MyClass m1 = new MyClass('Hello', 'Goodbye');\n"
                    + "       m1.logSomething();\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("Hello", "Goodbye", "Congratulations"));
    }

    @Test
    public void testInlineAssignmentWalker() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a = 'Hello';\n"
                    + "    private final String b = 'Goodbye';\n"
                    + "    private String c = 'Congratulations';\n"
                    + "    public void bar() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "       System.debug(c);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       MyClass m1 = new MyClass();\n"
                    + "       m1.bar();\n"
                    + "    }\n"
                    + "}",
        };

        assertABC(sourceCode, 6);
    }

    /** This also tests assignment of the variable separate from declaration */
    @Test
    public void testConstructorParameterAssignmentWalker() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    private final String b;\n"
                    + "    private String c;\n"
                    + "    public MyClass(String a1, String b1) {\n"
                    + "       a = a1;\n"
                    + "       b = b1;\n"
                    + "       c = 'Congratulations';\n"
                    + "    }\n"
                    + "    public void bar() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "       System.debug(c);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       MyClass m1;\n"
                    + "       m1 = new MyClass('Hello', 'Goodbye');\n"
                    + "       m1.bar();\n"
                    + "    }\n"
                    + "}",
        };

        assertABC(sourceCode, 11);
    }

    @Test
    public void testConstructorParameterAssignmentWalkerTwoInstances() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String a;\n"
                    + "    private final String b;\n"
                    + "    private String c;\n"
                    + "    public MyClass(String a1, String b1) {\n"
                    + "       a = a1;\n"
                    + "       b = b1;\n"
                    + "       c = 'Congratulations';\n"
                    + "    }\n"
                    + "    public void bar() {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(b);\n"
                    + "       System.debug(c);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       MyClass m1 = new MyClass('Hello1', 'Goodbye1');\n"
                    + "       m1.bar();\n"
                    + "       MyClass m2 = new MyClass('Hello2', 'Goodbye2');\n"
                    + "       m2.bar();\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result,
                TestRunnerMatcher.hasValues(
                        "Hello1",
                        "Goodbye1",
                        "Congratulations",
                        "Hello2",
                        "Goodbye2",
                        "Congratulations"));
    }

    /** Test that a variable value is retained across method calls. */
    @Test
    public void testStaticVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    static String s;\n"
                    + "    public static void bar1() {\n"
                    + "       s = 'Hello';\n"
                    + "    }\n"
                    + "    public static void bar2() {\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void doSomething() {\n"
                    + "       MyClass.bar1();\n"
                    + "       MyClass.bar2();\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testSingleton() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MySingleton si = MySingleton.getInstance();\n"
                    + "       String s = si.getName();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       singleton = new MySingleton();\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public string getName() {\n"
                    + "       return 'Acme Inc.';\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Acme Inc."));
    }

    @Test
    public void testSingletonChainedCall() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String s = MySingleton.getInstance().getName();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       singleton = new MySingleton();\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public string getName() {\n"
                    + "       return 'Acme Inc.';\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Acme Inc."));
    }

    @Test
    public void testCachedSingleton() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MySingleton os = MySingleton.getInstance();\n"
                    + "       String s = os.getName();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public string getName() {\n"
                    + "       return 'Acme Inc.';\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Acme Inc."));
    }

    @Test
    public void testChainedCachedSingleton() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String s = getName();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "    public static string getName() {\n"
                    + "       return MySingleton.getInstance().getName();\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public string getName() {\n"
                    + "       return 'Acme Inc.';\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Acme Inc."));
    }

    /** Verify that variable information is pushed through the method stack. */
    @Test
    public void testVariableDeclarationIsPerpetuated() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       Boolean b = PermissionSingleton.canDelete(MyObject__c.SObjectType);\n"
                    + "       System.debug(b);\n"
                    + "    }\n"
                    + "}",
            "public class PermissionSingleton {\n"
                    + "    private static Schema.DescribeSObjectResult objectDescribe;\n"
                    + "    public static Boolean canDelete(SObjectType objType) {\n"
                    + "       objectDescribe = DescribeSingleton.getObjectTypeFromMap(objType).getDescribe();\n"
                    + "       return objectDescribe.isDeletable();\n"
                    + "    }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "    private static Map<String, Schema.SObjectType> globalDescribe;\n"
                    + "    public static Schema.SObjectType getObjectTypeFromMap(SObjectType objType) {\n"
                    + "       globalDescribe = Schema.getGlobalDescribe();\n"
                    + "       return globalDescribe.get(objType.getDescribe().getName());\n"
                    + "    }\n"
                    + "}",
        };

        Map<String, String> variableTypes = new HashMap<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        String symbolicName = "objType";
                        if (vertex.getMethodName().equals("getObjectTypeFromMap")) {
                            SObjectType sObjectType =
                                    (SObjectType) symbols.getApexValue(symbolicName).orElse(null);
                            variableTypes.put(
                                    vertex.getDefiningType(),
                                    TestUtil.apexValueToString(sObjectType.getType()));
                        } else if (vertex.getMethodName().equals("get")) {
                            SObjectType sObjectType =
                                    (SObjectType) symbols.getApexValue(symbolicName).orElse(null);
                            variableTypes.put(
                                    vertex.getDefiningType(),
                                    TestUtil.apexValueToString(sObjectType.getType()));
                        }
                        return true;
                    }
                };

        TestRunner.get(g, sourceCode).withPathVertexVisitor(() -> visitor).walkPath();
        MatcherAssert.assertThat(variableTypes.entrySet(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(variableTypes.get("PermissionSingleton"), equalTo("MyObject__c"));
        MatcherAssert.assertThat(variableTypes.get("DescribeSingleton"), equalTo("MyObject__c"));
    }

    /** Verify that invoking static methods on a class retains state across invocations. */
    @Test
    public void testStaticClassRetainsState() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       MyOtherClass.setValue('Hello');\n"
                    + "       MyOtherClass.printValue();\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   private static String aValue;\n"
                    + "   public static void setValue(String value) {\n"
                    + "      aValue = value;\n"
                    + "   }\n"
                    + "   public static void printValue() {\n"
                    + "      System.debug(aValue);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInstanceClassRetainsStaticState() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       MyOtherClass.setValue('Hello');\n"
                    + "       MyOtherClass c = new MyOtherClass();\n"
                    + "       c.instancePrintValue();\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   private static String aValue;\n"
                    + "   public static void setValue(String value) {\n"
                    + "      aValue = value;\n"
                    + "   }\n"
                    + "   public static void printValue() {\n"
                    + "      System.debug(aValue);\n"
                    + "   }\n"
                    + "   public void instancePrintValue() {\n"
                    + "      System.debug(aValue);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    /**
     * Ideally the MySingleton.getInstance() would return the same instance and the call to
     * System.debug would print Hello. The current implementation has a different instance returned
     * from each call.
     */
    @Test
    public void testSingletonRetrievedMultipleTimes() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MySingleton.getInstance().setString('Hello');\n"
                    + "       MySingleton.getInstance().printString();\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private String aString;\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public void setString(string aString) {\n"
                    + "       this.aString = aString;\n"
                    + "    }\n"
                    + "    public string printString() {\n"
                    + "       System.debug(aString);\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInnerClass() {
        String[] sourceCode = {
            "public class OuterClass {\n"
                    + "    public String save() {\n"
                    + "       return new InnerClass(this).save();\n"
                    + "    }\n"
                    + "    public class InnerClass {\n"
                    + "       private OuterClass oc;\n"
                    + "       public InnerClass(OuterClass oc) {\n"
                    + "           this.oc = oc;\n"
                    + "       }\n"
                    + "       public String save() {\n"
                    + "           return 'Hello';\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n",
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String s = new OuterClass().save();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testDefaultStaticInitialization() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String s;\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(null));
    }

    @Test
    public void testBuilderPattern() {
        String[] sourceCode = {
            "public class Builder {\n"
                    + "    private String s1;\n"
                    + "    private String s2;\n"
                    + "    public Builder withS1(String s1) {\n"
                    + "       this.s1 = s1;\n"
                    + "       return this;\n"
                    + "    }\n"
                    + "    public Builder withS2(String s2) {\n"
                    + "       this.s2 = s2;\n"
                    + "       return this;\n"
                    + "    }\n"
                    + "    public void logSomething() {\n"
                    + "       System.debug(s1);\n"
                    + "       System.debug(s2);\n"
                    + "    }\n"
                    + "}\n",
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       new Builder()"
                    + "           .withS1('Hello')"
                    + "           .withS2('Goodbye')"
                    + "           .logSomething();\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("Hello", "Goodbye"));
    }

    @Test
    public void testBuilderPatternWithUnresolvedValue() {
        String[] sourceCode = {
            "public class Builder {\n"
                    + "    private String s1;\n"
                    + "    private String s2;\n"
                    + "    private String s3;\n"
                    + "    public Builder withS1(String s1) {\n"
                    + "       this.s1 = s1;\n"
                    + "       return this;\n"
                    + "    }\n"
                    + "    public Builder withS2(String s2) {\n"
                    + "       this.s2 = s2;\n"
                    + "       return this;\n"
                    + "    }\n"
                    + "    public Builder withS3(String s3) {\n"
                    + "       this.s3 = s3;\n"
                    + "       return this;\n"
                    + "    }\n"
                    + "    public String build() {\n"
                    + "       return String.format('{0}{1}{2}', new String[] {s1, s2, s3});\n"
                    + "    }\n"
                    + "}\n",
            "public class MyClass {\n"
                    + "    public static void doSomething(SObjectType sObjType) {\n"
                    + "       String s = new Builder()"
                    + "           .withS1('Hello')"
                    + "           .withS2('Goodbye')"
                    + "           .withS3(String.valueOf(sObjType))"
                    + "           .build();\n"
                    + "    	System.debug(s);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("HelloGoodbyeSFGE_Unresolved_Argument_2"));
    }

    @Test
    public void testFieldInitialization() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private String s1;\n"
                    + "    private String s2 = 'Hello';\n"
                    + "    private String s3 = MyOtherClass.getString();\n"
                    + "    public void doSomething() {\n"
                    + "       System.debug(s1);\n"
                    + "       System.debug(s2);\n"
                    + "       System.debug(s3);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        ApexStringValue value;

        value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.isNull(), equalTo(true));

        value = visitor.getResult(1);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
        MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Hello"));

        value = visitor.getResult(2);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.isNull(), equalTo(false));
    }

    @Test
    public void testInstanceFinalVariableInitializedInline() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "		MyOtherClass c = new MyOtherClass();\n"
                    + "		c.logValues();\n"
                    + "	}\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "	public final Set<String> values = new Set<String>{'value1', 'value2'};\n"
                    + "	public void logValues() {\n"
                    + "		System.debug(values);\n"
                    + "	}\n"
                    + "}\n",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSetValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValues(), hasSize(Matchers.equalTo(2)));

        List<String> values =
                Arrays.asList(
                        TestUtil.apexValueToString(value.getValues().get(0)),
                        TestUtil.apexValueToString(value.getValues().get(1)));
        // Order is not guaranteed on a set
        MatcherAssert.assertThat(values, containsInAnyOrder("value1", "value2"));
    }

    @Test
    public void testInstanceSelfReferentialPropertyDefault() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public string myString {\n"
                    + "		get {\n"
                    + "			if (myString == null) {\n"
                    + "				myString = 'defaultValue';\n"
                    + "			}\n"
                    + "			return myString;\n"
                    + "		}\n"
                    + "  		set;\n"
                    + "	}\n"
                    + "	public void doSomething() {\n"
                    + "		System.debug(myString);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("defaultValue"));
    }

    /**
     * Tests accessing a self referenced static property from both a static and non-static entry
     * method
     */
    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = {"", "static"})
    public void testStaticSelfReferentialPropertyDefault(String staticQualifier) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static string myString {\n"
                    + "		get {\n"
                    + "			if (myString == null) {\n"
                    + "				myString = 'defaultValue';\n"
                    + "			}\n"
                    + "			return myString;\n"
                    + "		}\n"
                    + "	}\n"
                    + "	public "
                    + staticQualifier
                    + " void doSomething() {\n"
                    + "		logSomething();\n"
                    + "	}\n"
                    + "	public "
                    + staticQualifier
                    + " void logSomething() {\n"
                    + "		System.debug(myString);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("defaultValue"));
    }

    /**
     * Tests accessing a self referenced static property from both a static and non-static entry
     * method
     */
    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = {"", "static"})
    public void testStaticSelfReferentialPropertySet(String staticQualifier) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static string myString {\n"
                    + "		get {\n"
                    + "			if (myString == null) {\n"
                    + "				myString = 'defaultValue';\n"
                    + "			}\n"
                    + "			return myString;\n"
                    + "		}\n"
                    + "		set {\n"
                    + "			myString = value;\n"
                    + "		}\n"
                    + "	}\n"
                    + "	public "
                    + staticQualifier
                    + " void doSomething() {\n"
                    + "		myString = 'Hello';\n"
                    + "		logSomething();\n"
                    + "	}\n"
                    + "	public "
                    + staticQualifier
                    + " void logSomething() {\n"
                    + "		System.debug(myString);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInstanceSelfReferentialPropertySet() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public string myString {\n"
                    + "		get {\n"
                    + "			if (myString == null) {\n"
                    + "				myString = 'defaultValue';\n"
                    + "			}\n"
                    + "			return myString;\n"
                    + "		}\n"
                    + "  		set;\n"
                    + "	}\n"
                    + "	public void doSomething() {\n"
                    + "		myString = 'Hello';\n"
                    + "		logSomething();\n"
                    + "	}\n"
                    + "	public void logSomething() {\n"
                    + "		System.debug(myString);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    private void assertABC(String[] sourceCode, int lineThatContainsFirstDebugOut) {
        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("Hello", "Goodbye", "Congratulations"));
    }
}
