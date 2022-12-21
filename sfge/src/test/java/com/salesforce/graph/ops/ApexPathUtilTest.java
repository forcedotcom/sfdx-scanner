package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ApexPathVertexMetaInfo;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.ops.expander.ApexPathExpanderUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.AbstractVisitingVertexPredicate;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.ApexValueAccumulator;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerListMatcher;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexPathUtilTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimple() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       System.debug('Hello1');\n"
                        + "       System.debug('Hello2');\n"
                        + "       System.debug('Hello3');\n"
                        + "    }\n"
                        + "}";

        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        return vertex instanceof MethodCallExpressionVertex;
                    }
                };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPathExpanderConfig expanderConfig =
                ApexPathUtil.getFullConfiguredPathExpanderConfigBuilder()
                        // Register an interest in collecting information about
                        // MethodCallExpressionVertex
                        // vertices. These vertices will then be available in the ApexPath's
                        // ApexPathMetaInfo
                        .withVertexPredicate(predicate)
                        .build();
        List<ApexPath> paths = TestUtil.getApexPaths(config, expanderConfig, "foo");
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.totalNumberOfVertices(), equalTo(4));

        ApexPathVertexMetaInfo apexPathVertexMetaInfo = path.getApexPathMetaInfo().get();
        MatcherAssert.assertThat(
                apexPathVertexMetaInfo.getMatches(MethodCallExpressionVertex.class),
                hasSize(equalTo(3)));

        List<MethodCallExpressionVertex> methodsCalled = new ArrayList<>();
        List<MethodCallExpressionVertex> recursiveCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        methodsCalled.add(vertex);
                        return true;
                    }

                    @Override
                    public void recursionDetected(
                            ApexPath currentPath,
                            MethodCallExpressionVertex methodCallExpressionVertex,
                            ApexPath recursivePath) {
                        recursiveCalls.add(methodCallExpressionVertex);
                    }
                };
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        MatcherAssert.assertThat(methodsCalled, hasSize(equalTo(3)));
        List<Integer> lineNumbers =
                methodsCalled.stream().map(v -> v.getBeginLine()).collect(Collectors.toList());
        MatcherAssert.assertThat(lineNumbers, contains(3, 4, 5));
        MatcherAssert.assertThat(recursiveCalls, hasSize(equalTo(0)));
    }

    @Test
    public void testSimpleSingleMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       debug1('hi');\n"
                        + "   }\n"
                        + "   public void debug1(String s) {\n"
                        + "       System.debug(s);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValue().get(), equalTo("hi"));
    }

    @Test
    public void testSimpleWithMethodCalls() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       debug1('Hello');\n"
                        + "    }\n"
                        + "    public static void debug1(String s) {\n"
                        + "       debug2(s);\n"
                        + "    }\n"
                        + "    public static void debug2(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        List<ApexPath> paths = TestUtil.getApexPaths(g, sourceCode, "foo", true);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.totalNumberOfVertices(), equalTo(6));

        List<MethodCallExpressionVertex> methodsCalled = new ArrayList<>();
        List<MethodCallExpressionVertex> recursiveCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        methodsCalled.add(vertex);
                        return true;
                    }

                    @Override
                    public void recursionDetected(
                            ApexPath currentPath,
                            MethodCallExpressionVertex methodCallExpressionVertex,
                            ApexPath recursivePath) {
                        recursiveCalls.add(methodCallExpressionVertex);
                    }
                };
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        MatcherAssert.assertThat(methodsCalled, hasSize(equalTo(3)));
        List<Integer> lineNumbers =
                methodsCalled.stream().map(v -> v.getBeginLine()).collect(Collectors.toList());
        MatcherAssert.assertThat(lineNumbers, contains(3, 6, 9));
        MatcherAssert.assertThat(recursiveCalls, hasSize(equalTo(0)));
    }

    @Test
    public void testSimpleExpanded() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       debug1('Hello');\n"
                        + "    }\n"
                        + "    public static void debug1(String s) {\n"
                        + "       if (s == 'yes') {\n"
                        + "           System.debug('yes');\n"
                        + "       } else {\n"
                        + "           System.debug('no');\n"
                        + "       }\n"
                        + "    }\n"
                        + "}";

        TestUtil.buildGraph(g, sourceCode);
        MethodVertex method = TestUtil.getVertexOnLine(g, MethodVertex.class, 2);
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method, false);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        paths =
                ApexPathExpanderUtil.expand(
                        g, paths.get(0), ApexPathUtil.getSimpleExpandingConfig());
        MatcherAssert.assertThat(paths, hasSize(equalTo(2)));

        List<MethodCallExpressionVertex> methodsCalled = new ArrayList<>();
        List<MethodCallExpressionVertex> recursiveCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if (vertex.getFullMethodName().equals("System.debug")) {
                            methodsCalled.add(vertex);
                        }
                        return true;
                    }

                    @Override
                    public void recursionDetected(
                            ApexPath currentPath,
                            MethodCallExpressionVertex methodCallExpressionVertex,
                            ApexPath recursivePath) {
                        recursiveCalls.add(methodCallExpressionVertex);
                    }
                };

        for (ApexPath path : paths) {
            DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
            ApexPathWalker.walkPath(g, path, visitor, symbols);
        }
        MatcherAssert.assertThat(methodsCalled, hasSize(equalTo(2)));
        List<Integer> lineNumbers =
                methodsCalled.stream().map(v -> v.getBeginLine()).collect(Collectors.toList());
        MatcherAssert.assertThat(lineNumbers, containsInAnyOrder(7, 9));
        MatcherAssert.assertThat(recursiveCalls, hasSize(equalTo(0)));
    }

    @Test
    public void testRecursion() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       debug1('Hello');\n"
                        + "    }\n"
                        + "    public static void debug1(String s) {\n"
                        + "       debug2(s);\n"
                        + "    }\n"
                        + "    public static void debug2(String s) {\n"
                        + "       debug1(s);\n"
                        + "    }\n"
                        + "}";

        // Ensure that the getForwardPaths method does not go into an infinite loop
        List<ApexPath> paths = TestUtil.getApexPaths(g, sourceCode, "foo", true);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);

        List<MethodCallExpressionVertex> methodsCalled = new ArrayList<>();
        List<MethodCallExpressionVertex> recursiveCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        methodsCalled.add(vertex);
                        return true;
                    }

                    @Override
                    public void recursionDetected(
                            ApexPath currentPath,
                            MethodCallExpressionVertex methodCallExpressionVertex,
                            ApexPath recursivePath) {
                        recursiveCalls.add(methodCallExpressionVertex);
                    }
                };
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        MatcherAssert.assertThat(methodsCalled, hasSize(equalTo(2)));
        List<Integer> lineNumbers =
                methodsCalled.stream().map(v -> v.getBeginLine()).collect(Collectors.toList());
        MatcherAssert.assertThat(lineNumbers, contains(3, 6));
        MatcherAssert.assertThat(recursiveCalls, hasSize(equalTo(1)));
        MatcherAssert.assertThat(recursiveCalls.get(0).getBeginLine(), equalTo(9));
    }

    /** Check that we don't accidentally consider iteration to be recursion */
    @Test
    public void testIteration() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void foo() {\n"
                        + "       debug1('Hello');\n"
                        + "       debug1('Goodbye');\n"
                        + "    }\n"
                        + "    public static void debug1(String s) {\n"
                        + "       debug2(s);\n"
                        + "    }\n"
                        + "    public static void debug2(String s) {\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}";

        List<ApexPath> paths = TestUtil.getApexPaths(g, sourceCode, "foo", true);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);

        List<MethodCallExpressionVertex> methodsCalled = new ArrayList<>();
        List<MethodCallExpressionVertex> recursiveCalls = new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        methodsCalled.add(vertex);
                        return true;
                    }

                    @Override
                    public void recursionDetected(
                            ApexPath currentPath,
                            MethodCallExpressionVertex methodCallExpressionVertex,
                            ApexPath recursivePath) {
                        recursiveCalls.add(methodCallExpressionVertex);
                    }
                };
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        MatcherAssert.assertThat(methodsCalled, hasSize(equalTo(6)));
        List<Integer> lineNumbers =
                methodsCalled.stream().map(v -> v.getBeginLine()).collect(Collectors.toList());
        MatcherAssert.assertThat(lineNumbers, contains(3 /*Hello*/, 7, 10, 4 /*Goodbye*/, 7, 10));
        MatcherAssert.assertThat(recursiveCalls, hasSize(equalTo(0)));
    }

    @Test
    public void testStackedInstanceMethods() {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "    public void foo() {\n"
                    + "        FLSClass f = new FLSClass();\n"
                    + "        f.verifyCreateable();\n"
                    + "        insert new Account(Name = 'Acme Inc.');\n"
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    public void verifyCreateable() {\n"
                    + "        internalVerifyCreateable1();\n"
                    + "    }\n"
                    + "    public void internalVerifyCreateable1() {\n"
                    + "        internalVerifyCreateable2();\n"
                    + "    }\n"
                    + "    public void internalVerifyCreateable2() {\n"
                    + "        if (!Schema.sObjectType.Account.fields.Name.isCreateable()) {\n"
                    + "            throw new MyException();\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n",
        };

        TestUtil.buildGraph(g, sourceCode);

        DmlInsertStatementVertex dmlInsertStatement =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 5);

        List<ApexPath> paths = ApexPathUtil.getReversePaths(g, dmlInsertStatement);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet().size(), equalTo(2));
    }

    @Test
    public void testEmptyMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void foo() {\n"
                        + "       bar(getAccount());\n"
                        + "    }\n"
                        + "    public static void bar(Account a) {\n"
                        + "    }\n"
                        + "    public static Account getAccount() {\n"
                        + "       return new Account();\n"
                        + "    }\n"
                        + "}\n";

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "foo");
        MatcherAssert.assertThat(path.resolvedInvocableCallCountInCurrentMethod(), equalTo(2));
        PathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    @Test
    public void testExceptionalForwardPathIsPruned() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       PermissionSingleton.canDelete(MyObject__c.SObjectType);\n"
                    + "    }\n"
                    + "}\n",
            "public class PermissionSingleton {\n"
                    + "    public static void canDelete(SObjectType objType) {\n"
                    + "       DescribeSingleton.describeObject(objType);\n"
                    + "       String s = 'HelloFoo';\n"
                    + // These vertices will only execute in the non-exceptional case
                    "       System.debug(s);\n"
                    + "    }\n"
                    + "}\n",
            "public class DescribeSingleton {\n"
                    + "    public static void describeObject(SObjectType objType) {\n"
                    + "       if (objType == null) {\n"
                    + "           throw new MyException('this is an exception');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    /** Verify that the #endsInException method works as expected */
    @Test
    public void testPathEndsInExceptionIsSetCorrectly() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       String s = 'HelloFoo';\n"
                    + "       MySingleton.checkNull1(s);\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static void checkNull1(String s) {\n"
                    + "       checkNull2(s);\n"
                    + "    }\n"
                    + "    public static void checkNull2(String s) {\n"
                    + "       if (s != null) {\n"
                    + "           System.info(s);\n"
                    + "       } else {\n"
                    + "           throw new MyException('this is an exception');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    @Test
    public void testExceptionalReversePathIsPrunedSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       String s = 'HelloFoo';\n"
                    + "       if (MySingleton.checkNull(s)) {\n"
                    + "           System.debug(s);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static Boolean checkNull(String s) {\n"
                    + "       if (s != null) {\n"
                    + "           return true;\n"
                    + "       } else {\n"
                    + "           throw new MyException('this is an exception');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        TestUtil.buildGraph(config);

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.FULL_METHOD_NAME, "System.debug"));
        List<ApexPath> paths = ApexPathUtil.getReversePaths(g, methodCallExpression);
        MatcherAssert.assertThat(paths, hasSize(IsEqual.equalTo(1)));
        ApexPath path = paths.get(0);

        // Make sure that it correctly resolved to the checkNullMethod
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(1)));
        MethodCallExpressionVertex checkNullMethodCallExpression =
                (MethodCallExpressionVertex)
                        path.getInvocableVertexToPaths().entrySet().iterator().next().getKey();
        MatcherAssert.assertThat(
                checkNullMethodCallExpression.getFullMethodName(),
                equalTo("MySingleton.checkNull"));

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(5)), equalToIgnoringCase("HelloFoo"));
    }

    @Test
    public void testExceptionalReversePathIsPrunedStacked() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       String s = 'HelloFoo';\n"
                    + "       checkNull(s);\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "    public void checkNull(String s) {\n"
                    + "       MySingleton.checkNull1(s);\n"
                    + "   }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static void checkNull1(String s) {\n"
                    + "       checkNull2(s);\n"
                    + "    }\n"
                    + "    public static void checkNull2(String s) {\n"
                    + "       if (s != null) {\n"
                    + "           System.debug(s);\n"
                    + "       } else {\n"
                    + "           throw new MyException('this is an exception');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();

        TestUtil.buildGraph(config);

        MethodCallExpressionVertex methodCallExpression =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD_CALL_EXPRESSION)
                                .has(Schema.DEFINING_TYPE, "MyClass")
                                .has(Schema.FULL_METHOD_NAME, "System.debug"));
        List<ApexPath> paths = ApexPathUtil.getReversePaths(g, methodCallExpression);
        MatcherAssert.assertThat(paths, hasSize(IsEqual.equalTo(1)));
        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(1)));

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(2)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(5)), equalToIgnoringCase("HelloFoo"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(7)), equalToIgnoringCase("HelloFoo"));
    }

    @Test
    public void testSyntheticSetter() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       set { aString = value; } \n"
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
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(2)));
    }

    @Test
    public void testSyntheticSetter2_1() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       set { aString = 'Hello' + value; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.aString = 'Goodbye';\n"
                    + "       String s = c.aString;\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(6)), equalTo("HelloGoodbye"));
    }

    @Test
    public void testSyntheticSetterMutatesValueGetFromAssignmentExpression() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get;\n"
                    + "       set { aString = 'Hello' + value; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.aString = 'Goodbye';\n"
                    + "       String s;\n"
                    + "       s = c.aString;\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(7)), equalTo("HelloGoodbye"));
    }

    @Test
    public void testSyntheticSetterMutatesValueVariableDeclaration() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aString {\n"
                    + "       get; set { aString = 'Hello' + value; }\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       MyClass c = new MyClass();\n"
                    + "       c.aString = 'Goodbye';\n"
                    + "       String s = c.aString;\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(results.get(6)), equalTo("HelloGoodbye"));
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
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        ApexPath path =
                TestUtil.getSingleApexPath(
                        config, ApexPathUtil.getFullConfiguredPathExpanderConfig(), "doSomething");

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexValueAccumulator visitor = new ApexValueAccumulator(Pair.of("System.debug", "s"));
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;
        results = visitor.getSingleResultPerLineByName("s");
        ;
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(TestUtil.apexValueToString(results.get(5)), equalTo("Hello"));
    }

    /**
     * Test variable assignment and chained method call passed as a parameter
     *
     * @param debugOutput
     */
    @ValueSource(strings = {"s", "new MyClass().getValue()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNewObjectExpressionWithChainedMethodCall(String debugOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String getValue() {\n"
                    + "        return 'Hello';\n"
                    + "   }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething() {\n"
                    + "           String s = new MyClass().getValue();\n"
                    + "           System.debug("
                    + debugOutput
                    + ");\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testNewObjectExpressionWithChainedPropertyGetter() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String aValue {\n"
                    + "        get { return 'Hello'; }\n"
                    + "   }\n"
                    + "   private class InnerClass {\n"
                    + "       public void doSomething() {\n"
                    + "           String s = new MyClass().aValue;\n"
                    + "           System.debug(s);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testConstructorWithForkedPath() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private String s;\n"
                    + "   public MyClass(String x) {\n"
                    + "       this.s = mapString(x);\n"
                    + "   }\n"
                    + "   public static String mapString(String x) {\n"
                    + "       if (x == null) {\n"
                    + "           return 'isTrue';\n"
                    + "       } else {\n"
                    + "           return 'isFalse';\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("isFalse", "isTrue"));
    }

    @Test
    public void testConstructorWithForkedPathShadowedVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private String s;\n"
                    + "   public MyClass(String s) {\n"
                    + "       this.s = mapString(s);\n"
                    + "   }\n"
                    + "   public static String mapString(String s) {\n"
                    + "       if (s == null) {\n"
                    + "           return 'isTrue';\n"
                    + "       } else {\n"
                    + "           return 'isFalse';\n"
                    + "       }\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("isFalse", "isTrue"));
    }

    /**
     * This is the same as the previous test except that the 's' variable shadows the instance
     * property. This tests code in ApexPathExpanderUtil that pushes an indeterminant
     * MethodInvocationScope before the constructor is visited.
     */
    @Test
    public void testConstructorShadowedVariable() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private String s;\n"
                    + "   public MyClass(String s) {\n"
                    + "       logSomething(s);\n"
                    + "       this.s = mapString(s);\n"
                    + "   }\n"
                    + "   public void logSomething(String s) {\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       MyOtherClass.noOp();\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue apexStringValue = visitor.getSingletonResult();
        BaseSFVertex declarationVertex =
                (BaseSFVertex) apexStringValue.getDeclarationVertex().get();
        MatcherAssert.assertThat(declarationVertex.getBeginLine(), equalTo(3));
        MatcherAssert.assertThat(apexStringValue.isNull(), equalTo(false));
    }

    /**
     * Tests where the fork occurs in the class initialization for the initial method being walked.
     */
    @Test
    public void testInitializerWithForkedPath1() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   private String s = MyOtherClass.getString();\n"
                    + "   public MyClass() {\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public static String getString() {\n"
                    + "       if (SomeOtherClass.isProduction()) {\n"
                    + "           return 'isTrue';\n"
                    + "       } else {\n"
                    + "           return 'isFalse';\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("isFalse", "isTrue"));
    }

    @Test
    public void testNewObjectInstantiatedInline() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static SObjectType sObjType = MyObject__c.SObjectType;\n"
                    + "   static MyOtherClass myOtherClass = new MyOtherClass(sObjType);\n"
                    + "   public static void doSomething() {\n"
                    + "		System.debug(myOtherClass.sObjType);\n"
                    + "	}\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   public SObjectType sObjType;\n"
                    + "   public MyOtherClass(SObjectType sObjType) {\n"
                    + "       this.sObjType = sObjType;\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        SObjectType sObjectType = visitor.getSingletonResult();
        MatcherAssert.assertThat(sObjectType.getCanonicalType(), equalTo("Schema.SObjectType"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("MyObject__c"));
    }

    @Test
    public void testSingletonInstantiatedInline() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static MySingleton singleton = MySingleton.getInstance();\n"
                    + "   public static void doSomething() {\n"
                    + "       singleton.logSomething('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "   public void logSomething(String a) {\n"
                    + "       System.debug(a);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testInlineAssignmentWithFork() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static String myString = MySingleton.getString();\n"
                    + "   public static void doSomething() {\n"
                    + "		System.debug(myString);\n"
                    + "	}\n"
                    + "}\n",
            "public class MySingleton {\n"
                    + "    public static String getString() {\n"
                    + "       if (SomeOtherClass.sayHello()) {\n"
                    + "           return 'Hello';\n"
                    + "       } else {\n"
                    + "           return 'Goodbye';\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("Hello", "Goodbye"));
    }

    @Test
    public void test_BDI_BatchNumberSettingsController_line_50() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   static SObjectType sObjType = MyObject__c.SObjectType;\n"
                    + "   static MyOtherClass myOtherClass = new MyOtherClass(sObjType);\n"
                    + "   public static void activate() {\n"
                    + "      myOtherClass.doSomething('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "   SObjectType sObjType;\n"
                    + "   public MyOtherClass(SObjectType sObjType) {\n"
                    + "       this.sObjType = sObjType;\n"
                    + "   }\n"
                    + "   public void doSomething(String a) {\n"
                    + "       System.debug(a);\n"
                    + "       System.debug(sObjType);\n"
                    + "   }\n"
                    + "}\n"
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "activate");
        ApexValueAccumulator visitor =
                new ApexValueAccumulator(
                        Pair.of("System.debug", "a"), Pair.of("System.debug", "sObjType"));
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);

        Map<Integer, Optional<ApexValue<?>>> results;

        results = visitor.getSingleResultPerLineByName("a");
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        ApexValue<?> apexValue = results.get(7).get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexValue), IsEqual.equalTo("Hello"));

        results = visitor.getSingleResultPerLineByName("sObjType");
        MatcherAssert.assertThat(results.keySet(), hasSize(equalTo(1)));
        SObjectType sObjectType = (SObjectType) results.get(8).get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), IsEqual.equalTo("MyObject__c"));
    }

    /**
     * User defined exceptions that are instantiated with a superclass constructor have special
     * handling in InvocableWithParametersVertex. The superclass constructor doesn't define names
     * for the parameters. This test ensures that the method is resolved and the path can be walked.
     */
    @Test
    public void testExceptionConstructor() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MyException ex = new MyException('Hello');\n"
                    + "    }\n"
                    + "    private class MyException extends Exception {}\n"
                    + "}"
        };

        List<ApexPath> paths = TestRunner.getPaths(g, sourceCode);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        ApexPath path = paths.get(0);
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().size(), equalTo(1));
    }

    /** Ensures that the parameter for a TernaryExpression is used to match the method */
    @Test
    public void testTernaryOperationSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String a = 'Hello';\n"
                    + "       String b = 'Goodbye';\n"
                    + "       logSomething(String.isEmpty(a) ? b : a);\n"
                    + "    }\n"
                    + "    public static void logSomething(String s) {\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}"
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");
        DefaultNoOpPathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().size(), equalTo(1));
    }

    /** Test a TernaryExpression that contains another TernaryExpression */
    @Test
    public void testTernaryOperationComplex() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String a = 'Hello';\n"
                    + "       String b = 'Goodbye';\n"
                    + "       String c = 'Congratulations';\n"
                    + "       logSomething(String.isEmpty(a) ? (String.isEmpty(b) ? c : b) : a);\n"
                    + "    }\n"
                    + "    public static void logSomething(String s) {\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}"
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");
        DefaultNoOpPathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().size(), equalTo(1));
    }

    @Test
    public void testIfWithMethod() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       if (verifyCreateable()) {\n"
                        + "           insert new Account(Name = 'Acme Inc.');\n"
                        + "       }\n"
                        + "    }\n"
                        + "    public boolean verifyCreateable() {\n"
                        + "       return Schema.sObjectType.Account.fields.Name.isCreateable();\n"
                        + "    }\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode);

        DmlInsertStatementVertex dmlInsertStatement =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 4);

        List<ApexPath> paths = ApexPathUtil.getReversePaths(g, dmlInsertStatement);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
    }

    /** This fails with a scoping issue. MyClass gets pushed on the stack twice. */
    @Disabled
    @Test
    public void testAfterVisitIsCalledOnWrongScope() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static String value = MyUtilityClass1.getValue();\n"
                    + "   public static void doSomething() {\n"
                    + "		MyUtilityClass1.getValue();\n"
                    + "  	}\n"
                    + "}\n",
            "public class MyUtilityClass1 {\n"
                    + "   public static String getValue() {\n"
                    + "   	return MyUtilityClass2.getValue();\n"
                    + "  	}\n"
                    + "}\n",
            "public class MyUtilityClass2 {\n"
                    + "	private static final MySettings__c mySettings = MyUtilityClass3.getSettings();\n"
                    + "   public static String getValue() {\n"
                    + "   	return '';\n"
                    + "   }\n"
                    + "}\n",
            " public class MyUtilityClass3 {\n"
                    + " 	public static MySettings__c getSettings() {\n"
                    + " 		MySettings__c settings = MySettings__c.getInstance();\n"
                    + "       if (settings.Id == null) {\n"
                    + "       	settings = MySettings__c.getOrgDefaults();\n"
                    + "       }\n"
                    + "       return settings;\n"
                    + "	}\n"
                    + "}\n",
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(3)));
    }
}
