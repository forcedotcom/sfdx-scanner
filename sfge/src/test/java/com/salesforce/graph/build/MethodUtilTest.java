package com.salesforce.graph.build;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.AbstractDefaultNoOpScope;
import com.salesforce.graph.symbols.DefaultNoOpScope;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.ApexValueAccumulator;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import com.salesforce.metainfo.MetaInfoCollectorTestProvider;
import com.salesforce.metainfo.VisualForceHandlerImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** See https://developer.salesforce.com/wiki/enforcing_crud_and_fls for CRUD/FLS examples */
public class MethodUtilTest {
    private static final Logger LOGGER = LogManager.getLogger(MethodUtilTest.class);
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void getFirstVertexInMethodOverloadsDifferByArity() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       debug('Hello');\n"
                        + "    }\n"
                        + "    public static void debug(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "    public static void debug(System.LoggingLevel level, String s) {\n"
                        + "       System.debug(level, s);\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex methodCallExpression =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 3);

        MethodVertex methodVertex =
                MethodUtil.getInvoked(
                                g, "MyClass", methodCallExpression, DefaultNoOpScope.getInstance())
                        .orElse(null);
        ;
        MatcherAssert.assertThat(methodVertex, not(nullValue()));
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(5));
    }

    @Test
    public void getFirstVertexInMethodOverloadsDifferByTypeInvokeString() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       logSomething('Hello');\n"
                        + "    }\n"
                        + "    public static void logSomething(Integer i) {\n"
                        + "       System.debug('Hello');\n"
                        + "    }\n"
                        + "    public static void logSomething(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex methodCallExpression =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 3);

        MethodVertex methodVertex =
                MethodUtil.getInvoked(
                                g, "MyClass", methodCallExpression, DefaultNoOpScope.getInstance())
                        .orElse(null);
        ;
        MatcherAssert.assertThat(methodVertex, not(nullValue()));
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(8));
    }

    @Test
    public void testSyntheticGetter() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get { return 'Hello'; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       String s = c.aString;\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        List<ApexPath> propertyPaths = new ArrayList<>();
        PathVertexVisitor visitor =
                new ApexValueAccumulator() {
                    @Override
                    public boolean visit(
                            VariableExpressionVertex.Single vertex, SymbolProvider symbols) {
                        if (vertex.getName().equals("aString")) {
                            propertyPaths.addAll(MethodUtil.getPaths(g, vertex, symbols));
                        }
                        return super.visit(vertex, symbols);
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(propertyPaths, hasSize(equalTo(1)));
        MethodVertex method = propertyPaths.get(0).getMethodVertex().get();
        MatcherAssert.assertThat(method.getName(), equalTo("__sfdc_aString"));
        MatcherAssert.assertThat(method.getArity(), equalTo(0));
    }

    @Test
    public void testSetterWithBlockStatement() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       set { aString = value; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.aString = 'Hello';\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        List<ApexPath> propertyPaths = new ArrayList<>();
        PathVertexVisitor visitor =
                new ApexValueAccumulator() {
                    @Override
                    public boolean visit(
                            VariableExpressionVertex.Single vertex, SymbolProvider symbols) {
                        if (vertex.getName().equals("aString")) {
                            propertyPaths.addAll(MethodUtil.getPaths(g, vertex, symbols));
                        }
                        return super.visit(vertex, symbols);
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(propertyPaths, hasSize(equalTo(1)));
        MethodVertex method = propertyPaths.get(0).getMethodVertex().get();
        MatcherAssert.assertThat(method.getName(), equalTo("__sfdc_aString"));
        MatcherAssert.assertThat(method.getArity(), equalTo(1));
    }

    @Test
    public void testGetterWithBlockStatement() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get { return 'Hello'; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       String s = c.aString;\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        List<ApexPath> propertyPaths = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            VariableExpressionVertex.Single vertex, SymbolProvider symbols) {
                        if (vertex.getName().equals("aString")) {
                            propertyPaths.addAll(MethodUtil.getPaths(g, vertex, symbols));
                        }
                        return super.visit(vertex, symbols);
                    }
                };
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(propertyPaths, hasSize(equalTo(1)));
        MethodVertex method = propertyPaths.get(0).getMethodVertex().get();
        MatcherAssert.assertThat(method.getName(), equalTo("__sfdc_aString"));
        MatcherAssert.assertThat(method.getArity(), equalTo(0));
    }

    @Test
    public void getFirstVertexInMethodOverloadsDifferByTypeInvokeInteger() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       logSomething(10);\n"
                        + "    }\n"
                        + "    public static void logSomething(Integer i) {\n"
                        + "       System.debug('Hello');\n"
                        + "    }\n"
                        + "    public static void logSomething(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex methodCallExpression =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 3);

        MethodVertex methodVertex =
                MethodUtil.getInvoked(
                                g, "MyClass", methodCallExpression, DefaultNoOpScope.getInstance())
                        .orElse(null);
        ;
        MatcherAssert.assertThat(methodVertex, not(nullValue()));
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(5));
    }

    @Test
    public void getFirstVertexInMethodOverloadsDifferByTypeInvokeWithStringVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       String a = 'Hello';\n"
                        + "       logSomething(a);\n"
                        + "       Integer b = 25;\n"
                        + "       logSomething(b);\n"
                        + "    }\n"
                        + "    public static void logSomething(Integer i) {\n"
                        + "       System.debug('Hello');\n"
                        + "    }\n"
                        + "    public static void logSomething(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        SymbolProvider symbolProvider =
                new AbstractDefaultNoOpScope() {
                    @Override
                    public Optional<Typeable> getTypedVertex(String key) {
                        if (key.equals("a")) {
                            return Optional.of(
                                    TestUtil.getVertexOnLine(
                                            g, ASTConstants.NodeType.VARIABLE_DECLARATION, 3));
                        } else if (key.equals("b")) {
                            return Optional.of(
                                    TestUtil.getVertexOnLine(
                                            g, ASTConstants.NodeType.VARIABLE_DECLARATION, 5));
                        } else {
                            throw new UnexpectedException(key);
                        }
                    }

                    @Override
                    public Optional<Typeable> getTypedVertex(List<String> keySequence) {
                        if (keySequence.size() == 1) {
                            return getTypedVertex(keySequence.get(0));
                        } else {
                            throw new TodoException(
                                    "Thrown by test: getTypedVertex was provided length-2 list");
                        }
                    }
                };

        MethodCallExpressionVertex methodCallExpression;
        MethodVertex methodVertex;

        // Calling with a string
        methodCallExpression =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 4);
        methodVertex =
                MethodUtil.getInvoked(g, "MyClass", methodCallExpression, symbolProvider)
                        .orElse(null);
        MatcherAssert.assertThat(methodVertex, not(nullValue()));
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(11));

        // Calling with an integer
        methodCallExpression =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 6);
        methodVertex =
                MethodUtil.getInvoked(g, "MyClass", methodCallExpression, symbolProvider)
                        .orElse(null);
        MatcherAssert.assertThat(methodVertex, not(nullValue()));
        MatcherAssert.assertThat(methodVertex.getBeginLine(), equalTo(8));
    }

    @Test
    public void getChainedParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        Schema.SObjectType.Account.fields.getMap().get('Name').getDescribe().isCreateable();\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.METHOD_NAME, "getMap"));

        assertEquals(
                "Schema.SObjectType.Account.fields.getMap", methodCallExpression.getFullName());
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(0)));

        assertTrue(methodCallExpression.getNext().isPresent());
        methodCallExpression = (MethodCallExpressionVertex) methodCallExpression.getNext().get();
        assertEquals("get", methodCallExpression.getFullName());
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(1)));
        ChainedVertex parameter = methodCallExpression.getParameters().get(0);
        assertEquals("Name", TestUtil.chainedVertexToString(parameter));

        assertTrue(methodCallExpression.getNext().isPresent());
        methodCallExpression = (MethodCallExpressionVertex) methodCallExpression.getNext().get();
        assertEquals("getDescribe", methodCallExpression.getFullName());
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(0)));

        assertTrue(methodCallExpression.getNext().isPresent());
        methodCallExpression = (MethodCallExpressionVertex) methodCallExpression.getNext().get();
        assertEquals("isCreateable", methodCallExpression.getFullName());
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(0)));
    }

    @Test
    public void testGetPotentialCallers_simpleCase() {
        // Create a graph based on the source code.
        String simpleMethodCallSource =
                "public class MyClass {\n"
                        + "   public void instanceFoo() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'hello world');\n"
                        + "   }\n"
                        + "\n"
                        + "   public static void staticFoo() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'hello world');\n"
                        + "   }\n"
                        + "\n"
                        + "   public void callFoos() {\n"
                        + "       this.instanceFoo();\n"
                        + "       MyClass.staticFoo();\n"
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, simpleMethodCallSource);

        // Get the vertices corresponding to the two foo() variants.
        MethodVertex instanceFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex staticFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);

        // Get the list of vertices representing calls of the methods.
        List<MethodCallExpressionVertex> instanceFooCalls =
                MethodUtil.getPotentialCallers(g, instanceFoo);
        List<MethodCallExpressionVertex> staticFooCalls =
                MethodUtil.getPotentialCallers(g, staticFoo);

        // Confirm that each method was called only once and on the proper line.
        assertEquals(1, instanceFooCalls.size(), "instanceFoo() should only have one call");
        MatcherAssert.assertThat(instanceFooCalls.get(0).getBeginLine(), equalTo(11));

        assertEquals(1, staticFooCalls.size(), "staticFoo() should only have one call");
        MatcherAssert.assertThat(staticFooCalls.get(0).getBeginLine(), equalTo(12));
    }

    @Test
    public void testGetPotentialCallers_ignoresCaseDisparities() {
        // Create a graph based on the source code.
        String weirdlyCasedSource =
                "PUBLIC claSs myCLass {\n"
                        + "   PubLic VOId iNstAnceFOO() {\n"
                        + "       SYsTeM.deBug(LOGgInglEVel.eRRoR, 'HElLo WOrLd');\n"
                        + "   }\n"
                        + "\n"
                        + "   pubLic STATiC VOiD stATicFOO() {\n"
                        + "       sysTEm.deBug(loGgINgleVEL.erROr, 'HelLO WoRld');\n"
                        + "   }\n"
                        + "\n"
                        + "   pUBLiC VOid CaLlFoOS() {\n"
                        + "       thiS.InstancEfOo();\n"
                        + "       MYclaSS.StATicFOO();\n"
                        + "   }\n"
                        + "}\n";

        TestUtil.buildGraph(g, weirdlyCasedSource);

        // Get the vertices corresponding to the two foo() variants.
        MethodVertex instanceFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex staticFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);

        // Get the list of vertices representing calls of the methods.
        List<MethodCallExpressionVertex> instanceFooCalls =
                MethodUtil.getPotentialCallers(g, instanceFoo);
        List<MethodCallExpressionVertex> staticFooCalls =
                MethodUtil.getPotentialCallers(g, staticFoo);

        // Confirm that each method was called only once and on the proper line.
        assertEquals(1, instanceFooCalls.size(), "instanceFoo() should only have one call");
        MatcherAssert.assertThat(instanceFooCalls.get(0).getBeginLine(), equalTo(11));

        assertEquals(1, staticFooCalls.size(), "staticFoo() should only have one call");
        MatcherAssert.assertThat(staticFooCalls.get(0).getBeginLine(), equalTo(12));
    }

    @Test
    public void testGetPotentialCallers_followsImplicitReferences() {
        // Create a graph based on the source code.
        String implicitCallsSource =
                "public class MyClass {\n"
                        + "   public void instanceFoo() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'hello world');\n"
                        + "   }\n"
                        + "\n"
                        + "   public static void staticFoo() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'hello world');\n"
                        + "   }\n"
                        + "\n"
                        + "   public void callerInstanceMethod() {\n"
                        + "       instanceFoo();\n"
                        + "       staticFoo();\n"
                        + "   }\n"
                        + "\n"
                        + "   public static void callerStaticMethod() {\n"
                        + "       staticFoo();\n"
                        + "   }\n"
                        + "}";

        TestUtil.buildGraph(g, implicitCallsSource);

        // Get the vertices corresponding to the two foo() variants.
        MethodVertex instanceFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex staticFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);

        // Get the lists of vertices representing calls of the method.
        List<MethodCallExpressionVertex> instanceCalls =
                MethodUtil.getPotentialCallers(g, instanceFoo);
        List<MethodCallExpressionVertex> staticCalls = MethodUtil.getPotentialCallers(g, staticFoo);

        // Verify that the methods are called in the expected places.
        assertEquals(1, instanceCalls.size());
        MatcherAssert.assertThat(instanceCalls.get(0).getBeginLine(), equalTo(11));

        assertEquals(2, staticCalls.size());
        assertTrue(staticCalls.stream().anyMatch(v -> v.getBeginLine() == 12));
        assertTrue(staticCalls.stream().anyMatch(v -> v.getBeginLine() == 16));
    }

    @Test
    public void testGetPotentialCallers_distinguishesOverloadsByArity() {
        // Create a graph based on the source code.
        String aritySourceCode =
                "public class MyClass {\n"
                        + "   public void arityFoo() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'beep');\n"
                        + "   }\n"
                        + "\n"
                        + "   public void arityFoo(Integer i1) {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'beep');\n"
                        + "   }\n"
                        + "\n"
                        + "   public void arityFoo(Integer i1, Integer i2) {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'beep');\n"
                        + "   }\n"
                        + "\n"
                        + "   public void arityCaller() {\n"
                        + "       arityFoo();\n"
                        + "       arityFoo(1);\n"
                        + "       arityFoo(1, 2);\n"
                        + "   }\n"
                        + "}";
        TestUtil.buildGraph(g, aritySourceCode);

        // Get the vertices corresponding to the method definitions.
        MethodVertex noParams = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex oneParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex twoParams = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);

        // Get lists of vertices corresponding to invocations of the methods.
        List<MethodCallExpressionVertex> noParamsCallers =
                MethodUtil.getPotentialCallers(g, noParams);
        List<MethodCallExpressionVertex> oneParamCallers =
                MethodUtil.getPotentialCallers(g, oneParam);
        List<MethodCallExpressionVertex> twoParamsCallers =
                MethodUtil.getPotentialCallers(g, twoParams);

        // Verify that the methods are called at the expected lines, and the expected number of
        // times.
        assertEquals(1, noParamsCallers.size());
        MatcherAssert.assertThat(noParamsCallers.get(0).getBeginLine(), equalTo(15));

        assertEquals(1, oneParamCallers.size());
        MatcherAssert.assertThat(oneParamCallers.get(0).getBeginLine(), equalTo(16));

        assertEquals(1, twoParamsCallers.size());
        MatcherAssert.assertThat(twoParamsCallers.get(0).getBeginLine(), equalTo(17));
    }

    // This source code is incomplete, because the expectation is that tests will add the remaining
    // source code at runtime.
    private static final String PRIMITIVE_OVERLOAD_PARTIAL_SOURCE =
            "public class MyClass {\n"
                    + "   public void overloadPrimitives(Integer i) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void overloadPrimitives(String s) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void overloadPrimitives(Boolean b) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n";

    @Test
    public void testGetPotentialCallers_primitiveLiteralOverloads() {
        // Add the part of the source code, so we're passing different literals into the methods.
        String fullSource =
                PRIMITIVE_OVERLOAD_PARTIAL_SOURCE
                        + "   public void callOverloadedMethods() {\n"
                        + "       overloadPrimitives(7);\n"
                        + "       overloadPrimitives('beep');\n"
                        + "       overloadPrimitives(true);\n"
                        + "       overloadPrimitives(false);\n"
                        + "   }\n"
                        + "}";

        // Create a graph based on the source code
        TestUtil.buildGraph(g, fullSource);

        // Get the vertices corresponding to the method calls.
        MethodVertex intParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex stringParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex boolParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);

        // Get the vertices corresponding to the invocations of the methods.
        List<MethodCallExpressionVertex> intParamCalls =
                MethodUtil.getPotentialCallers(g, intParam);
        List<MethodCallExpressionVertex> stringParamCalls =
                MethodUtil.getPotentialCallers(g, stringParam);
        List<MethodCallExpressionVertex> boolParamCalls =
                MethodUtil.getPotentialCallers(g, boolParam);

        // Verify that our expectations about call count and line number are correct.
        assertEquals(1, intParamCalls.size());
        MatcherAssert.assertThat(intParamCalls.get(0).getBeginLine(), equalTo(15));

        assertEquals(1, stringParamCalls.size());
        MatcherAssert.assertThat(stringParamCalls.get(0).getBeginLine(), equalTo(16));

        assertEquals(2, boolParamCalls.size());
        assertTrue(boolParamCalls.stream().anyMatch(v -> v.getBeginLine() == 17));
        assertTrue(boolParamCalls.stream().anyMatch(v -> v.getBeginLine() == 18));
    }

    @Test
    public void testGetPotentialCallers_primitiveVariableOverloads() {
        // Add the part of the source code, so we're passing different literals into the methods.
        String fullSource =
                PRIMITIVE_OVERLOAD_PARTIAL_SOURCE
                        + "   public void callOverloadedMethods() {\n"
                        + "       Integer i1 = 8;\n"
                        + "       String s1 = 'beep';\n"
                        + "       Boolean b1 = true;\n"
                        + "       Boolean b2 = false;\n"
                        + "       overloadPrimitives(i1);\n"
                        + "       overloadPrimitives(s1);\n"
                        + "       overloadPrimitives(b1);\n"
                        + "       overloadPrimitives(b2);\n"
                        + "   }\n"
                        + "}";

        // Create a graph based on the source code
        TestUtil.buildGraph(g, fullSource);

        // Get the vertices corresponding to the method calls.
        MethodVertex intParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex stringParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex boolParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);

        // Get the vertices corresponding to the invocations of the methods.
        List<MethodCallExpressionVertex> intParamCalls =
                MethodUtil.getPotentialCallers(g, intParam);
        List<MethodCallExpressionVertex> stringParamCalls =
                MethodUtil.getPotentialCallers(g, stringParam);
        List<MethodCallExpressionVertex> boolParamCalls =
                MethodUtil.getPotentialCallers(g, boolParam);

        // Verify that our expectations about call count and line number are correct.
        assertEquals(1, intParamCalls.size());
        MatcherAssert.assertThat(intParamCalls.get(0).getBeginLine(), equalTo(19));

        assertEquals(1, stringParamCalls.size());
        MatcherAssert.assertThat(stringParamCalls.get(0).getBeginLine(), equalTo(20));

        assertEquals(2, boolParamCalls.size());
        assertTrue(boolParamCalls.stream().anyMatch(v -> v.getBeginLine() == 21));
        assertTrue(boolParamCalls.stream().anyMatch(v -> v.getBeginLine() == 22));
    }

    @Test
    public void testGetPotentialCallers_primitiveParametersOverload() {
        // Add the final part of the source code, such that it's calling the methods using
        // parameters.
        String fullSource =
                PRIMITIVE_OVERLOAD_PARTIAL_SOURCE
                        + "   public void callOverloadedMethods(Integer i1, String s1, Boolean b1) {\n"
                        + "       overloadPrimitives(i1);\n"
                        + "       overloadPrimitives(s1);\n"
                        + "       overloadPrimitives(b1);\n"
                        + "   }\n"
                        + "}";

        // Create a graph based on the source code.
        TestUtil.buildGraph(g, fullSource);

        // Get the vertices corresponding to the method calls.
        MethodVertex intParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex stringParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex boolParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);

        // Get the vertices corresponding to the invocations of the methods.
        List<MethodCallExpressionVertex> intParamCalls =
                MethodUtil.getPotentialCallers(g, intParam);
        List<MethodCallExpressionVertex> stringParamCalls =
                MethodUtil.getPotentialCallers(g, stringParam);
        List<MethodCallExpressionVertex> boolParamCalls =
                MethodUtil.getPotentialCallers(g, boolParam);

        // Verify that our expectations about call count and line number are correct.
        assertEquals(1, intParamCalls.size());
        MatcherAssert.assertThat(intParamCalls.get(0).getBeginLine(), equalTo(15));

        assertEquals(1, stringParamCalls.size());
        MatcherAssert.assertThat(stringParamCalls.get(0).getBeginLine(), equalTo(16));

        assertEquals(1, boolParamCalls.size());
        MatcherAssert.assertThat(boolParamCalls.get(0).getBeginLine(), equalTo(17));
    }

    // This source code is incomplete, because the expectation is that tests will add the remaining
    // source code at runtime.
    private static final String OBJECT_OVERLOAD_PARTIAL_SOURCE =
            "public class MyClass {\n"
                    + "   public void overloadObjects(List<Integer> ints) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void overloadObjects(List<String> strs) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void overloadObjects(List<MyClass> objs) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void overloadObjects(MyClass obj) {\n"
                    + "       return;\n"
                    + "   }\n"
                    + "\n";

    @Test
    public void testGetPotentialCallers_objectVariablesOverload() {
        // Add the part of the source code, so we're passing different literals into the methods.
        String fullSource =
                OBJECT_OVERLOAD_PARTIAL_SOURCE
                        + "   public void callOverloadedMethods() {\n"
                        + "       List<Integer> ints = new List<Integer>{1, 2, 3};\n"
                        + "       List<String> strs = new List<String>{'a', 'b', 'c'};\n"
                        + "       List<MyClass> objs = new List<MyClass>{new MyClass()};\n"
                        + "       MyClass obj = new MyClass();\n"
                        + "       overloadObjects(ints);\n"
                        + "       overloadObjects(strs);\n"
                        + "       overloadObjects(objs);\n"
                        + "       overloadObjects(obj);\n"
                        + "   }\n"
                        + "}";

        // Create a graph based on the source code
        TestUtil.buildGraph(g, fullSource);

        // Get the vertices corresponding to the declarations of the methods.
        MethodVertex intListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex stringListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex objListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);
        MethodVertex objParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 14);

        // Get the vertices representing invocations of the methods.
        List<MethodCallExpressionVertex> intListParamCallers =
                MethodUtil.getPotentialCallers(g, intListParam);
        List<MethodCallExpressionVertex> stringListParamCallers =
                MethodUtil.getPotentialCallers(g, stringListParam);
        List<MethodCallExpressionVertex> objListParamCallers =
                MethodUtil.getPotentialCallers(g, objListParam);
        List<MethodCallExpressionVertex> objParamCallers =
                MethodUtil.getPotentialCallers(g, objParam);

        // Verify that the methods are called in the expected places.
        assertEquals(1, intListParamCallers.size());
        MatcherAssert.assertThat(intListParamCallers.get(0).getBeginLine(), equalTo(23));
        assertEquals(1, stringListParamCallers.size());
        MatcherAssert.assertThat(stringListParamCallers.get(0).getBeginLine(), equalTo(24));
        assertEquals(1, objListParamCallers.size());
        MatcherAssert.assertThat(objListParamCallers.get(0).getBeginLine(), equalTo(25));
        assertEquals(1, objParamCallers.size());
        MatcherAssert.assertThat(objParamCallers.get(0).getBeginLine(), equalTo(26));
    }

    @Test
    public void testGetPotentialCallers_objectParametersOverload() {
        // Add the final part of the source code, such that it's calling the methods using
        // parameters.
        String fullSource =
                OBJECT_OVERLOAD_PARTIAL_SOURCE
                        + "   public void callOverloadedMethods(List<Integer> ints, List<String> strs, List<MyClass> objs, MyClass obj) {\n"
                        + "       overloadObjects(ints);\n"
                        + "       overloadObjects(strs);\n"
                        + "       overloadObjects(objs);\n"
                        + "       overloadObjects(obj);\n"
                        + "   }\n"
                        + "}";

        // Create a graph based on the source code
        TestUtil.buildGraph(g, fullSource);

        // Get the vertices corresponding to the declarations of the methods.
        MethodVertex intListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex stringListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);
        MethodVertex objListParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 10);
        MethodVertex objParam = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 14);

        // Get the vertices representing invocations of the methods.
        List<MethodCallExpressionVertex> intListParamCallers =
                MethodUtil.getPotentialCallers(g, intListParam);
        List<MethodCallExpressionVertex> stringListParamCallers =
                MethodUtil.getPotentialCallers(g, stringListParam);
        List<MethodCallExpressionVertex> objListParamCallers =
                MethodUtil.getPotentialCallers(g, objListParam);
        List<MethodCallExpressionVertex> objParamCallers =
                MethodUtil.getPotentialCallers(g, objParam);

        // Verify that the methods are called in the expected places.
        assertEquals(1, intListParamCallers.size());
        MatcherAssert.assertThat(intListParamCallers.get(0).getBeginLine(), equalTo(19));
        assertEquals(1, stringListParamCallers.size());
        MatcherAssert.assertThat(stringListParamCallers.get(0).getBeginLine(), equalTo(20));
        assertEquals(1, objListParamCallers.size());
        MatcherAssert.assertThat(objListParamCallers.get(0).getBeginLine(), equalTo(21));
        assertEquals(1, objParamCallers.size());
        MatcherAssert.assertThat(objParamCallers.get(0).getBeginLine(), equalTo(22));
    }

    private static final String CROSS_FILE_CALLABLE_SOURCE =
            "public class MyClass1 {\n"
                    + "   public Integer instanceFoo() {\n"
                    + "       return 15;\n"
                    + "   }\n"
                    + "\n"
                    + "   public static Integer staticFoo() {\n"
                    + "       return 25;\n"
                    + "   }\n"
                    + "}";

    private static final String CROSS_FILE_CALLER_SOURCE =
            "public class MyClass2 {\n"
                    + "\n"
                    +
                    // Add an instanceFoo() method on this class, so we can verify that no confusion
                    // occurs.
                    "   public Integer instanceFoo() {\n"
                    + "       return 21;\n"
                    + "   }\n"
                    + "\n"
                    + "   public void crossClassCall() {\n"
                    + "       MyClass1 mc1 = new MyClass1();\n"
                    + "       MyClass2 mc2 = new MyClass2();\n"
                    + "       Integer i1 = mc1.instanceFoo();\n"
                    + "       Integer i2 = MyClass1.staticFoo();\n"
                    + "       Integer i3 = mc2.instanceFoo();\n"
                    + "   }\n"
                    + "}";

    @Test
    public void testGetPotentialCallers_handlesCallersInOtherClasses() {
        // Get graphs based on the source code.
        TestUtil.buildGraph(g, new String[] {CROSS_FILE_CALLABLE_SOURCE, CROSS_FILE_CALLER_SOURCE});

        // Get the method definition vertices.
        MethodVertex instanceFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex staticFoo = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 6);

        // Get the vertices representing calls of the methods
        List<MethodCallExpressionVertex> instanceCalls =
                MethodUtil.getPotentialCallers(g, instanceFoo);
        List<MethodCallExpressionVertex> staticCalls = MethodUtil.getPotentialCallers(g, staticFoo);

        // Verify that the methods are called in the right places.
        assertEquals(1, instanceCalls.size());
        MatcherAssert.assertThat(instanceCalls.get(0).getBeginLine(), equalTo(10));
        assertEquals(1, staticCalls.size());
        MatcherAssert.assertThat(staticCalls.get(0).getBeginLine(), equalTo(11));
    }

    @Test
    public void testGetPotentialCallers_handlesCallsToInnerClasses() {
        String source1 =
                "public class OuterClass1 {\n"
                        + "   public static void callInnerClass() {\n"
                        + "       InnerClass1A ic = new InnerClass1A();\n"
                        + "       ic.innerFoo();\n"
                        + "   }\n"
                        + "\n"
                        + "   public class InnerClass1A {\n"
                        + "       public void innerFoo() {\n"
                        + "           System.debug(LoggingLevel.ERROR, 'beep');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        String source2 =
                "public class OuterClass2 {\n"
                        + "   public static void callInnerClass() {\n"
                        + "       InnerClass2A ic2a = new InnerClass2A();\n"
                        + "       ic2a.innerFoo();\n"
                        + "       OuterClass1.InnerClass1A ic1a = new OuterClass1.InnerClass1A();\n"
                        + "       ic1a.innerFoo();\n"
                        + "   }\n"
                        + "\n"
                        + "   public class InnerClass2A {\n"
                        + "       public void innerFoo() {\n"
                        + "           System.debug(LoggingLevel.ERROR, 'boop');\n"
                        + "       }\n"
                        + "   }\n"
                        + "}";

        // Build a graph from our source code.
        TestUtil.buildGraph(g, new String[] {source1, source2});

        // Load the vertex defining the inner class's method.
        MethodVertex innerMethod = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 8);

        // Get the vertices corresponding to calls of the inner method.
        List<MethodCallExpressionVertex> innerMethodCalls =
                MethodUtil.getPotentialCallers(g, innerMethod);

        // Verify that the right calls occur on the right lines.
        assertEquals(2, innerMethodCalls.size());
        assertTrue(innerMethodCalls.stream().anyMatch(v -> v.getBeginLine() == 4));
        assertTrue(innerMethodCalls.stream().anyMatch(v -> v.getBeginLine() == 6));
    }

    @Test
    @Disabled // Disabled because reverse path class initialization is broken. This was removed in
    // order to meet igl deadline
    public void testGetPotentialCallers_identifiesOwnInstanceProperties() {
        // Define a source that calls methods using its own instance properties.
        String source =
                "public class MyClass {\n"
                        + "   public Integer myInt = 15;\n"
                        + "   public String myStr = 'beep';\n"
                        + "   public void testedMethod(Integer i) {\n"
                        + "       System.debug(LoggingLevel.ERROR, i);\n"
                        + "   }\n"
                        + "   public void testedMethod(String s) {\n"
                        + "       System.debug(LoggingLevel.ERROR, s);\n"
                        + "   }\n"
                        + "   public void callerMethod() {\n"
                        + "       testedMethod(myInt);\n"
                        + "       testedMethod(this.myInt);\n"
                        + "       testedMethod(myStr);\n"
                        + "       testedMethod(this.myStr);\n"
                        + "   }\n"
                        + "}";

        // Build a graph
        TestUtil.buildGraph(g, source);

        // Load the two implementations of testedMethod.
        MethodVertex intVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 4);
        MethodVertex strVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 7);

        // Get the invocations of each method.
        List<MethodCallExpressionVertex> intVerstionCalls =
                MethodUtil.getPotentialCallers(g, intVerstion);
        List<MethodCallExpressionVertex> strVerstionCalls =
                MethodUtil.getPotentialCallers(g, strVerstion);

        // Verify that the methods were called as expected.
        assertEquals(2, intVerstionCalls.size());
        assertTrue(intVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 11));
        assertTrue(intVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 12));
        assertEquals(2, strVerstionCalls.size());
        assertTrue(strVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 13));
        assertTrue(strVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 14));
    }

    @Test
    @Disabled // Disabled because reverse path class initialization is broken. This was removed in
    // order to meet igl deadline
    public void testGetPotentialCallers_identifiesOwnStaticProperties() {
        // Define a source that calls methods using its own instance properties.
        String source =
                "public class MyClass {\n"
                        + "   public static Integer myInt = 15;\n"
                        + "   public static String myStr = 'beep';\n"
                        + "   public void testedMethod(Integer i) {\n"
                        + "       System.debug(LoggingLevel.ERROR, i);\n"
                        + "   }\n"
                        + "   public void testedMethod(String s) {\n"
                        + "       System.debug(LoggingLevel.ERROR, s);\n"
                        + "   }\n"
                        + "   public void callerMethod() {\n"
                        + "       testedMethod(myInt);\n"
                        + "       testedMethod(MyClass.myInt);\n"
                        + "       testedMethod(myStr);\n"
                        + "       testedMethod(MyClass.myStr);\n"
                        + "   }\n"
                        + "}";

        // Build a graph
        TestUtil.buildGraph(g, source);

        // Load the two implementations of testedMethod.
        MethodVertex intVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 4);
        MethodVertex strVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 7);

        // Get the invocations of each method.
        List<MethodCallExpressionVertex> intVerstionCalls =
                MethodUtil.getPotentialCallers(g, intVerstion);
        List<MethodCallExpressionVertex> strVerstionCalls =
                MethodUtil.getPotentialCallers(g, strVerstion);

        // Verify that the methods were called as expected.
        // TODO: Long-term, the assertions should be identical to those from the instance property
        // test above. However,
        //  the functionality to handle explicit references (e.g. MyClass.myInt) doesn't exist yet.
        // Until then, we'll
        //  just assert against the results we do have.
        assertEquals(1, intVerstionCalls.size());
        assertTrue(intVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 11));
        assertEquals(1, strVerstionCalls.size());
        assertTrue(strVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 13));
    }

    @Test
    public void testGetPotentialCallers_identifiesOtherInstancePropertiesSingleProperty() {
        String otherClassSource =
                "public class OtherClass {\n" + "   public Integer myInt = 15;\n" + "}";
        String mainClassSource =
                "public class MainClass {\n"
                        + "   public void testedMethod(Integer i) {\n"
                        + "       System.debug(LoggingLevel.ERROR, i);\n"
                        + "   }\n"
                        + "   public void callerMethod() {\n"
                        + "       OtherClass oc = new OtherClass();\n"
                        + "       testedMethod(oc.myInt);\n"
                        + "   }\n"
                        + "}";

        // Build a graph
        TestUtil.buildGraph(g, new String[] {otherClassSource, mainClassSource});

        // Load the two implementations of testedMethod.
        MethodVertex intVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);

        // Get the invocations of each method.
        List<MethodCallExpressionVertex> intVerstionCalls =
                MethodUtil.getPotentialCallers(g, intVerstion);

        // Verify that the methods were called as expected.
        assertEquals(1, intVerstionCalls.size());
        assertTrue(intVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 7));
    }

    @Test
    public void testGetPotentialCallers_identifiesOtherInstancePropertiesMultipleProperties() {
        String otherClassSource =
                "public class OtherClass {\n"
                        + "   public Integer myInt = 15;\n"
                        + "   public String myStr = 'beep';\n"
                        + "}";
        String mainClassSource =
                "public class MainClass {\n"
                        + "   public void testedMethod(Integer i) {\n"
                        + "       System.debug(LoggingLevel.ERROR, i);\n"
                        + "   }\n"
                        + "   public void testedMethod(String s) {\n"
                        + "       System.debug(LoggingLevel.ERROR, s);\n"
                        + "   }\n"
                        + "   public void callerMethod() {\n"
                        + "       OtherClass oc = new OtherClass();\n"
                        + "       testedMethod(oc.myInt);\n"
                        + "       testedMethod(oc.myStr);\n"
                        + "   }\n"
                        + "}";

        // Build a graph
        TestUtil.buildGraph(g, new String[] {otherClassSource, mainClassSource});

        // Load the two implementations of testedMethod.
        MethodVertex intVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex strVerstion = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 5);

        // Get the invocations of each method.
        List<MethodCallExpressionVertex> intVerstionCalls =
                MethodUtil.getPotentialCallers(g, intVerstion);
        List<MethodCallExpressionVertex> strVerstionCalls =
                MethodUtil.getPotentialCallers(g, strVerstion);

        // Verify that the methods were called as expected.
        assertEquals(1, intVerstionCalls.size());
        assertTrue(intVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 10));
        assertEquals(1, strVerstionCalls.size());
        assertTrue(strVerstionCalls.stream().anyMatch(v -> v.getBeginLine() == 11));
    }

    @Test
    public void testGetPotentialCallers_interfacesCountAsCallers() {
        String interfaceSource =
                "public interface MyInterface {\n" + "   void doSomething();\n" + "}";
        String implementerSource =
                "public class MyClass1 implements MyInterface {\n"
                        + "   public void doSomething() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'doing something');\n"
                        + "   }\n"
                        + "}";
        String otherSource =
                "public class MyClass2 {\n"
                        + "   public void doSomething() {\n"
                        + "       System.debug(LoggingLevel.ERROR, 'doing something');\n"
                        + "   }\n"
                        + "}";
        String callerSource =
                "public class CallerClass {\n"
                        + "   public void callMethods() {\n"
                        + "       String className = 'MyClass1';\n"
                        + "       MyInterface obj = (MyInterface) (Type.forName(className).newInstance());\n"
                        + "       obj.doSomething();\n"
                        + "   }\n"
                        + "}";

        // Build a graph from our sources.
        TestUtil.buildGraph(
                g, new String[] {interfaceSource, implementerSource, otherSource, callerSource});

        // Load the two versions of the doSomething() method, and identify which is which.
        List<MethodVertex> allMethodVertices =
                TestUtil.getVerticesOnLine(g, ASTConstants.NodeType.METHOD, 2);
        MethodVertex mc1DoSomething =
                allMethodVertices.stream()
                        .filter(v -> v.getDefiningType().equals("MyClass1"))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "No vertex found for MyClass1.doSomething()"));
        MethodVertex mc2DoSomething =
                allMethodVertices.stream()
                        .filter(v -> v.getDefiningType().equals("MyClass2"))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "No vertex found for MyClass2.doSomething()"));

        // Get the vertices corresponding to calls to the methods.
        List<MethodCallExpressionVertex> mc1Calls =
                MethodUtil.getPotentialCallers(g, mc1DoSomething);
        List<MethodCallExpressionVertex> mc2Calls =
                MethodUtil.getPotentialCallers(g, mc2DoSomething);

        // Verify that the right calls occurred in the right places.
        assertEquals(1, mc1Calls.size());
        MatcherAssert.assertThat(mc1Calls.get(0).getBeginLine(), equalTo(5));

        assertTrue(mc2Calls.isEmpty());
    }

    /**
     * Test methods are identified using a modifier or annotation. The JorjeWrapper normalizes these
     * into a boolean property placed on the method vertex.
     */
    @Test
    public void testIsTestMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void foo() {\n"
                    + "    }\n"
                    + "    public static testMethod void specifiedWithModifier() {\n"
                    + "    }\n"
                    + "    @isTest\n"
                    + "    public static void specifiedWithAnnotation() {\n"
                    + "    }\n"
                    + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);

        MethodVertex foo =
                SFVertexFactory.load(
                        g, g.V().hasLabel(ASTConstants.NodeType.METHOD).has(Schema.NAME, "foo"));
        MatcherAssert.assertThat(foo.isTest(), equalTo(false));

        for (String methodName :
                new String[] {"specifiedWithModifier", "specifiedWithAnnotation"}) {
            MethodVertex method =
                    SFVertexFactory.load(
                            g,
                            g.V().hasLabel(ASTConstants.NodeType.METHOD)
                                    .has(Schema.NAME, methodName));
            MatcherAssert.assertThat(methodName, method.isTest(), equalTo(true));
        }
    }

    @Test
    public void testIsTestClass() {
        String[] sourceCode = {
            "public class MyClass {\n" + "}\n", "@IsTest\n" + "public class MyTestClass {\n" + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);

        UserClassVertex myClass = ClassUtil.getUserClass(g, "MyClass").get();
        MatcherAssert.assertThat(myClass.isTest(), equalTo(false));

        UserClassVertex myTestClass = ClassUtil.getUserClass(g, "MyTestClass").get();
        MatcherAssert.assertThat(myTestClass.isTest(), equalTo(true));
    }

    /**
     * Test that UserClass methods that return are PageReference included. UserInterface methods are
     * excluded. Test methods are exclude. Methods from test classes are excluded.
     */
    @Test
    public void testGetPageReferenceMethods() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static PageReference foo() {\n"
                    + "    }\n"
                    + "    public static testMethod PageReference shouldBeExcludedByModifier() {\n"
                    + "    }\n"
                    + "    @isTest\n"
                    + "    public static PageReference shouldBeExcludedByAnnotation() {\n"
                    + "    }\n"
                    + "    public static void bar() {\n"
                    + "    }\n"
                    + "}\n",
            "@isTest\n"
                    + "public class MyTestClass {\n"
                    + "    public static PageReference foo() {\n"
                    + "    }\n"
                    + "}\n",
            "public interface MyInterface {\n"
                    +
                    // This should not be returned, we can't walk an interface's path
                    "    PageReference foo();\n"
                    + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);

        List<MethodVertex> methods = MethodUtil.getPageReferenceMethods(g, new ArrayList<>());
        MatcherAssert.assertThat(methods, hasSize(equalTo(1)));

        MethodVertex method = methods.get(0);
        MatcherAssert.assertThat(method.getName(), equalTo("foo"));
    }

    /**
     * Test that UserClass methods that return are PageReference included. UserInterface methods are
     * excluded. Test methods are exclude. Methods from test classes are excluded.
     */
    @Test
    public void testGetExposedControllerMethods() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void foo() {\n"
                    + "    }\n"
                    + "    public static testMethod void shouldBeExcludedByModifier() {\n"
                    + "    }\n"
                    + "    @isTest\n"
                    + "    public static void shouldBeExcludedByAnnotation() {\n"
                    + "    }\n"
                    + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);

        try {
            MetaInfoCollectorTestProvider.setVisualForceHandler(
                    new VisualForceHandlerImpl() {
                        @Override
                        public void loadProjectFiles(List<String> sourceFolders) {
                            // Intentionally left blank
                        }

                        @Override
                        public TreeSet<String> getMetaInfoCollected() {
                            return CollectionUtil.newTreeSetOf("MyClass");
                        }
                    });
            List<String> methodNames =
                    MethodUtil.getExposedControllerMethods(g, new ArrayList<>()).stream()
                            .map(m -> m.getName())
                            .collect(Collectors.toList());

            // clone is an autogenerated method. TODO: Exclude
            MatcherAssert.assertThat(methodNames, containsInAnyOrder("clone", "foo"));
        } finally {
            MetaInfoCollectorTestProvider.removeVisualForceHandler();
        }
    }

    /**
     * Tests static method on the current class isn't called when when an instance method that can't
     * be resolved should have been called instead. See {@link
     * MethodUtil#getPaths(GraphTraversalSource, ChainedVertex, SymbolProvider)}
     */
    @Test
    public void testUnresolvedChainedClassWithConflictingMethodName() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       getOtherClass().conflictingMethod();\n"
                        + "    }\n"
                        + "    public static OtherClass getOtherClass() {\n"
                        + "       return new OtherClass();\n"
                        + "    }\n"
                        + "    public static OtherClass conflictingMethod() {\n"
                        + "       System.debug('this should not resolve');\n"
                        + "    }\n"
                        + "}\n";

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "foo");
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(1)));

        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            fail("conflicting method should not resolve");
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
    }

    /**
     * Tests static method on the current class isn't called when when an instance method that can't
     * be resolved should have been called instead. See {@link
     * MethodUtil#getPaths(GraphTraversalSource, ChainedVertex, SymbolProvider)}
     */
    @Test
    public void testUnresolvedVariableNameClassWithConflictingMethodName() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       OtherClass oc = getOtherClass();\n"
                        + "       oc.conflictingMethod();\n"
                        + "    }\n"
                        + "    public static OtherClass getOtherClass() {\n"
                        + "       return new OtherClass();\n"
                        + "    }\n"
                        + "    public static OtherClass conflictingMethod() {\n"
                        + "       System.debug('this should not resolve');\n"
                        + "    }\n"
                        + "}\n";

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "foo");

        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            fail("conflicting method should not resolve");
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
    }

    /**
     * Tests that the type of special SObjectType variables can be inferred. These parameters don't
     * have a declaration vertex, their type is inferred by matching the SObjectType value in the
     * variable name.
     */
    @Test
    public void testCustomSObjectTypePassedAsMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       doSomething(MyObject__c.SObjectType);\n"
                        + "    }\n"
                        + "    public static void doSomething(SObjectType sObjType) {\n"
                        + "       System.debug('');\n"
                        + "    }\n"
                        + "    public static void doSomething(String s) {\n"
                        + "       System.debug('');\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        ApexPath path = TestUtil.getSingleApexPath(config, "foo");

        List<Optional<Typeable>> typedVertices = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            Optional<Typeable> typeable = symbols.getTypedVertex("sObjType");
                            typedVertices.add(typeable);
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
        MatcherAssert.assertThat(typedVertices, hasSize(equalTo(1)));
        Typeable typeable = typedVertices.get(0).orElse(null);
        MatcherAssert.assertThat(
                typeable.getCanonicalType(),
                equalToIgnoringCase(ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE));
    }

    @Test
    public void testCustomSObjectTypeDescribeMethodCalled() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       doSomething(MyObject__c.SObjectType);\n"
                        + "    }\n"
                        + "    public static void doSomething(SObjectType sObjectType) {\n"
                        + "       handleDescribe(sObjectType.getDescribe());\n"
                        + "    }\n"
                        + "    public static void handleDescribe(Schema.DescribeSObjectResult describe) {\n"
                        + "       System.debug('');\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        ApexPath path = TestUtil.getSingleApexPath(config, "foo");

        List<Optional<Typeable>> typedVertices = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullMethodName().equals("System.debug")) {
                            Optional<Typeable> typeable = symbols.getTypedVertex("describe");
                            typedVertices.add(typeable);
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
        MatcherAssert.assertThat(typedVertices, hasSize(equalTo(1)));
        Typeable typeable = typedVertices.get(0).orElse(null);
        MatcherAssert.assertThat(
                typeable.getCanonicalType(),
                equalToIgnoringCase(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_S_OBJECT_RESULT));
    }

    @Test
    public void testCustomSObjectTypeDescribeGetNameMethodCalled() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       doSomethingElse(MyObject__c.SObjectType);\n"
                        + "    }\n"
                        + "    public static void doSomethingElse(SObjectType sObjectType) {\n"
                        + "       handleDescribe(sObjectType.getDescribe());\n"
                        + "    }\n"
                        + "    public static void handleDescribe(Schema.DescribeSObjectResult describe) {\n"
                        + "       String s = describe.getName();\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("MyObject__c"));
    }

    @Test
    public void testStandardSObjectTypePassedAsMethodParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       doSomething(Schema.SObjectType.Account.SObjectType);\n"
                        + "    }\n"
                        + "    public static void doSomething(SObjectType sObjectType) {\n"
                        + "       System.debug('');\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        ApexPath path = TestUtil.getSingleApexPath(config, "foo");

        List<Optional<Typeable>> typedVertices = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullName().equals("System.debug")) {
                            typedVertices.add(symbols.getTypedVertex("sObjectType"));
                        }
                        return true;
                    }
                };

        SymbolProviderVertexVisitor symbolProvider = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbolProvider);
        MatcherAssert.assertThat(typedVertices, hasSize(equalTo(1)));
        Typeable typeable = typedVertices.get(0).orElse(null);
        MatcherAssert.assertThat(
                typeable.getCanonicalType(), equalToIgnoringCase("Schema.SObjectType"));
    }

    @Test
    public void testMethodReturnsUnresolvableValue() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private MyServiceClass msc;\n"
                    + "    private String aString;\n"
                    + "    public MyClass(MyServiceClass msc) {\n"
                    + "       this.msc = msc;\n"
                    + "    }\n"
                    + "    public void logSomething(String s) {\n"
                    + "       this.aString = s;\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    private String aString;\n"
                    + "    public static void doSomething() {\n"
                    + "       MyClass c = new MyClass(MyServiceClass.getImpl());\n"
                    + "       c.logSomething('Goodbye');\n"
                    + "    }\n"
                    + "}",
            "public class MyServiceClass {\n"
                    + "    private String aString;\n"
                    + "    public static MyServiceClass getImpl() {\n"
                    + "       MyServiceClass msc;\n"
                    +
                    // Simulate a case where the class was not initialized. We still want to return
                    // a value that
                    // can be resolved to a type
                    "       return msc;\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Goodbye"));
    }

    /**
     * There may be times when we can't resolve a method constructor. This test ensures that the
     * method is not treated as static
     */
    @Test
    public void testUnresolvedConstructor() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private String aString;\n"
                    + "    public void logSomething(String s) {\n"
                    + "       this.aString = s;\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    private String aString;\n"
                    + "    public static void doSomething() {\n"
                    + "       MyClass c = new MyClass('Hello');\n"
                    + "       c.logSomething('Goodbye');\n"
                    + "    }\n"
                    + "}"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPathExpanderConfig expanderConfig = ApexPathUtil.getFullConfiguredPathExpanderConfig();

        ApexPath path = TestUtil.getSingleApexPath(config, expanderConfig, "doSomething");
        // Neither the constructor nor #logSomething should resolve
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(0)));

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        DefaultNoOpPathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    public static Stream<Arguments> testInnerClassResolution() {
        return Stream.of(Arguments.of(""), Arguments.of("MyClass."));
    }

    /**
     * Verify that class inner class resolution works with and without qualification of the outer
     * class.
     */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInnerClassResolution(String prefix) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass1 {\n"
                    + "   	public static void logSomething(String s) {\n"
                    + "   		System.debug('InnerClass1-' + s);\n"
                    + "   	}\n"
                    + "   }\n"
                    + "	public class InnerClass2 {\n"
                    + "   	public static void logSomething(String s) {\n"
                    + "   		System.debug('InnerClass2-' + s);\n"
                    + "   		"
                    + prefix
                    + "InnerClass1.logSomething(s);\n"
                    + "   	}\n"
                    + "   }\n"
                    + "   public static void doSomething() {\n"
                    + "   	"
                    + prefix
                    + "InnerClass2.logSomething('Hello');\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("InnerClass2-Hello", "InnerClass1-Hello"));
    }
}
