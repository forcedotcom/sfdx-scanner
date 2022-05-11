package com.salesforce.graph.visitor;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.SymbolProviderUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.ObjectProperties;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexFieldDescribeMapValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.matchers.TestRunnerListMatcher;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DefaultSymbolProviderVertexVisitorTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimple() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = 'HelloFoo1';\n"
                        + "        System.debug(s);\n"
                        + "        s = 'HelloFoo2';\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("HelloFoo1", "HelloFoo2"));
    }

    @Test
    public void testBinaryLiteralExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = 'HelloFoo1' + 'HelloFoo2';\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testBinaryVariableExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String a = 'HelloFoo1';\n"
                        + "        String b = 'HelloFoo2';\n"
                        + "        String s = a + b;\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testTernaryLiteralExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = 'HelloFoo1' + 'HelloFoo2' + 'HelloFoo3';\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2HelloFoo3"));
    }

    @Test
    public void testTernaryVariableExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String a = 'HelloFoo1';\n"
                        + "        String b = 'HelloFoo2';\n"
                        + "        String c = 'HelloFoo3';\n"
                        + "        String s = a + b + c;\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2HelloFoo3"));
    }

    @Test
    public void testLocalVariablesDifferentMethods() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        new MyClass();\n"
                        + "        String s = 'HelloFoo1';\n"
                        + "        System.debug(s);\n"
                        + "        s = 'HelloFoo2';\n"
                        + "        System.debug(s);\n"
                        + "        bar();\n"
                        + "    }\n"
                        + "    public void bar() {\n"
                        + "        String s = 'HelloBar1';\n"
                        + "        System.debug(s);\n"
                        + "        s = 'HelloBar2';\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(
                result,
                TestRunnerMatcher.hasValues("HelloFoo1", "HelloFoo2", "HelloBar1", "HelloBar2"));
    }

    @Test
    public void testClassScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    private final String s = 'Hello';\n"
                        + "    public void doSomething() {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testMethodScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        bar('Hello');\n"
                        + "    }\n"
                        + "    public void bar(String s) {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testMethodScopeIsNotInherited() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = 'Hello';\n"
                        + "        logSomething();\n"
                        + "    }\n"
                        + "    public void logSomething() {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        MatcherAssert.assertThat(visitor.getOptionalSingletonResult().isPresent(), equalTo(false));
    }

    @Test
    public void testCrossClassScopeIsNotInherited() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private final String aString = 'Hello';\n"
                    + "    public static void doSomething() {\n"
                    + "        MyOtherClass c = new MyOtherClass();\n"
                    + "        c.logSomething();\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public void logSomething() {\n"
                    + "        System.debug(aString);\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        MatcherAssert.assertThat(visitor.getOptionalSingletonResult().isPresent(), equalTo(false));
    }

    @Test
    public void testMethodScopeWithVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String foo = 'Hello';\n"
                        + "        bar(foo);\n"
                        + "    }\n"
                        + "    public void bar(String s) {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testMethodScopeWithRecursiveVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String foo1 = 'Hello';\n"
                        + "        String foo2 = foo1;\n"
                        + "        bar(foo2);\n"
                        + "    }\n"
                        + "    public void bar(String s) {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testMethodScopeWithDoubleRecursiveVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String foo1 = 'Hello';\n"
                        + "        String foo2 = foo1;\n"
                        + "        String foo3 = foo2;\n"
                        + "        bar(foo3);\n"
                        + "    }\n"
                        + "    public void bar(String s) {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testStackedMethodScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + // 1
                        "    public void doSomething() {\n"
                        + // 2
                        "        String foo = 'a';\n"
                        + // Easy way to find a path // 3
                        "        bar1('Hello');\n"
                        + // 4
                        "    }\n"
                        + // 5
                        "    public void bar1(String s) {\n"
                        + // 6
                        "        bar2('Foo');\n"
                        + // 7
                        "        System.debug(s);\n"
                        + // 8
                        "    }\n"
                        + // 9
                        "    public void bar2(String s) {\n"
                        + // 10
                        "        System.debug(s);\n"
                        + // 11
                        "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("Foo", "Hello"));
    }

    @Test
    public void testStackedMethodScopeComplicated1() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        bar1('Hello');\n"
                        + "    }\n"
                        + "    public void bar1(String s) {\n"
                        + "        if (s == 'Hello') {\n"
                        + "           bar2('Foo1');\n"
                        + "        } else {\n"
                        + "           System.debug(s);\n"
                        + "           bar2('Foo2');\n"
                        + "        }\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public void bar2(String s) {\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        ApexPathExpanderConfig expanderConfig = ApexPathUtil.getSimpleExpandingConfig();
        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(
                results,
                TestRunnerListMatcher.hasValuesAnyOrder("Hello", "Foo1", "Hello", "Hello", "Foo2"));
    }

    @Test
    public void testStackedMethodScopeComplicated2() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        bar1('Hello');\n"
                        + "    }\n"
                        + "    public void bar1(String s) {\n"
                        + "        System.debug(s);\n"
                        + "        bar2('Foo2');\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public void bar2(String s) {\n"
                        + "        System.debug(s);\n"
                        + "        String d = 'a';\n"
                        + "        d = 'c';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("Hello", "Foo2", "Hello"));
    }

    @Test
    public void testComplexType() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        System.debug(m);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexFieldDescribeMapValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                ((MethodCallExpressionVertex) value.getValueVertex().get()).getFullMethodName(),
                equalTo("Schema.SObjectType.Account.fields.getMap"));
    }

    @Test
    public void testSimpleInnerScope() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = 'HelloFoo1';\n"
                        + "        if (x == 1) {\n"
                        + "           System.debug(s);\n"
                        + "           s = 'HelloFoo2';\n"
                        + "           System.debug(s);\n"
                        + "        }\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "}";

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results,
                TestRunnerListMatcher.hasValuesAnyOrder(
                        "HelloFoo1",
                        "HelloFoo2",
                        "HelloFoo2", // if
                        "HelloFoo1" // else
                        ));
    }

    @Test
    public void testNestedIf() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        Map<String,Schema.SObjectField> m = Schema.SObjectType.Account.fields.getMap();\n"
                        + "        if (m.get('Name').getDescribe().isCreateable()) {\n"
                        + "           if (m.get('Phone').getDescribe().isCreateable()) {\n"
                        + "               insert new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";

        List<ApexPath> paths = TestRunner.getPaths(g, sourceCode);
        MatcherAssert.assertThat(paths, hasSize(equalTo(3)));
        DmlInsertStatementVertex dmlInsertStatementVertex =
                TestUtil.getVertexOnLine(g, DmlInsertStatementVertex.class, 6);
        ApexPath path = TestUtil.getPathEndingAtVertex(paths, dmlInsertStatementVertex);

        List<Pair<MethodCallExpressionVertex, MethodCallExpressionVertex>> methodCallExpressions =
                new ArrayList<>();
        PathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        if ("m".equals(vertex.getSymbolicName().orElse(null))) {
                            MethodCallExpressionVertex resolvedMethod =
                                    (MethodCallExpressionVertex)
                                            symbols.getValue(vertex).orElse(null);
                            methodCallExpressions.add(Pair.of(vertex, resolvedMethod));
                        }
                        return true;
                    }
                };

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
        MatcherAssert.assertThat(methodCallExpressions, hasSize(equalTo(2)));

        MethodCallExpressionVertex methodCallExpression;
        methodCallExpression = methodCallExpressions.get(0).getRight();
        MatcherAssert.assertThat(
                methodCallExpression.getFullMethodName(),
                equalTo("Schema.SObjectType.Account.fields.getMap"));

        methodCallExpression = methodCallExpressions.get(0).getLeft();
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.chainedVertexToString(methodCallExpression.getParameters().get(0)),
                equalTo("Name"));

        methodCallExpression = methodCallExpressions.get(1).getRight();
        MatcherAssert.assertThat(
                methodCallExpression.getFullMethodName(),
                equalTo("Schema.SObjectType.Account.fields.getMap"));

        methodCallExpression = methodCallExpressions.get(1).getLeft();
        MatcherAssert.assertThat(methodCallExpression.getParameters(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(
                TestUtil.chainedVertexToString(methodCallExpression.getParameters().get(0)),
                equalTo("Phone"));
    }

    @Test
    public void testResolveListLiteral() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       String [] fieldsToCheck = new String [] {'Name', 'Phone'};\n"
                        + "       System.debug(fieldsToCheck);\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue value = visitor.getSingletonResult();
        List<String> values =
                value.getValues().stream()
                        .map(a -> TestUtil.apexValueToString(a))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(values, contains("Name", "Phone"));
    }

    // TODO: Test modification of list after creation
    // Test map contains variables

    @Test
    public void testParameterizedMapInConstructorCrossClassInstance() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyCreateable();\n"
                    + "       insert new Account(Name = 'Acme Inc.');\n"
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        m.get('Name').getDescribe().isCreateable();\n"
                    + "    }\n"
                    + "}\n",
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");

        DefaultNoOpPathVertexVisitor visitor =
                new InvokedMethodListener(7) {
                    @Override
                    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        assertGetWasCalledWithObject("Account", this);
                        return true;
                    }
                };

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    private static class InvokedMethodListener extends DefaultNoOpPathVertexVisitor {
        private final int lineWithDMLCheck;
        protected final Map<InvocableVertex, Map<ChainedVertex, ChainedVertex>> parameterMap;
        protected ApexBooleanValue isCreateableResult;
        protected final Map<String, ApexValue<?>> apexValueMap;

        private InvokedMethodListener(int lineWithDMLCheck) {
            this.lineWithDMLCheck = lineWithDMLCheck;
            this.parameterMap = new HashMap<>();
            this.apexValueMap = CollectionUtil.newTreeMap();
        }

        MethodCallExpressionVertex invokedMethod;
        MethodCallExpressionVertex resolvedMethod;

        @Override
        public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
            if (vertex.getBeginLine().equals(lineWithDMLCheck)) {
                if (vertex.getMethodName().equalsIgnoreCase("isCreateable")) {
                    isCreateableResult =
                            (ApexBooleanValue) ScopeUtil.resolveToApexValue(symbols, vertex).get();
                }
            }
        }

        @Override
        public boolean visit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
            if (vertex.getBeginLine().equals(lineWithDMLCheck)) {
                String symbolicName = vertex.getSymbolicName().orElse(null);
                if (symbolicName != null) {
                    ApexValue<?> apexValue = symbols.getApexValue(symbolicName).orElse(null);
                    if (apexValue != null) {
                        apexValueMap.put(vertex.getSymbolicName().get(), apexValue);
                    }
                }
                invokedMethod = vertex;
                resolvedMethod =
                        (MethodCallExpressionVertex) symbols.getValue(invokedMethod).orElse(null);
                parameterMap.putAll(
                        SymbolProviderUtil.resolveInvokedParameters(
                                symbols, invokedMethod.firstToList()));
                if (resolvedMethod != null) {
                    parameterMap.putAll(
                            SymbolProviderUtil.resolveInvokedParameters(
                                    symbols, resolvedMethod.firstToList()));
                }
            }
            return true;
        }
    }

    private void assertGetWasCalledWithObject(
            String objectType, InvokedMethodListener invokedMethodListener) {
        MatcherAssert.assertThat(invokedMethodListener.isCreateableResult, not(nullValue()));
        DescribeFieldResult describeFieldResult =
                (DescribeFieldResult)
                        invokedMethodListener.isCreateableResult.getReturnedFrom().get();
        DescribeSObjectResult describeSObjectResult =
                describeFieldResult.getDescribeSObjectResult().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(describeSObjectResult.getSObjectType()),
                equalTo(objectType));
    }

    @Test
    public void testParameterizedMapInConstructorCrossClassInstanceValueOverwritten() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyCreateable();\n"
                    + "       insert new Account(Name = 'Acme Inc.');\n"
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String objectName) {\n"
                    + "       objectName = 'Contact';\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        m.get('Name').getDescribe().isCreateable();\n"
                    + "    }\n"
                    + "}\n",
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");

        DefaultNoOpPathVertexVisitor visitor =
                new InvokedMethodListener(8) {
                    @Override
                    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        // Account is passed ot the constructor, but the variable is overwritten
                        // during the constructor execution
                        assertGetWasCalledWithObject("Contact", this);
                        return true;
                    }
                };

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    @Test
    public void testParameterizedMapInConstructorCrossClassInstanceValueDeclaredInConstructor() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       FLSClass f = new FLSClass('Account');\n"
                    + "       f.verifyCreateable();\n"
                    + "       insert new Account(Name = 'Acme Inc.');\n"
                    + "    }\n"
                    + "}\n",
            "public class FLSClass {\n"
                    + "    private final Map<String,Schema.SObjectField> m;\n"
                    + "    public FLSClass(String ignored) {\n"
                    + "       String objectName = 'Contact';\n"
                    + "       m = Schema.getGlobalDescribe().get(objectName).getDescribe().fields.getMap();\n"
                    + "    }\n"
                    + "    public void verifyCreateable() {\n"
                    + "        m.get('Name').getDescribe().isCreateable();\n"
                    + "    }\n"
                    + "}\n",
        };

        ApexPath path = TestUtil.getSingleApexPath(g, sourceCode, "doSomething");

        DefaultNoOpPathVertexVisitor visitor =
                new InvokedMethodListener(8) {
                    @Override
                    public boolean visit(DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        // Account is passed ot the constructor, but the variable is overwritten
                        // during the constructor execution
                        assertGetWasCalledWithObject("Contact", this);
                        return true;
                    }
                };

        DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    @Test
    public void testObjectPropertiesSObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().orElse(null);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        ObjectProperties objectProperties = apexSingleValue;
        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                objectProperties.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testObjectPropertiesStandardObject() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       Account obj = new Account(Name = 'Acme Inc.');\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ObjectProperties objectProperties = visitor.getSingletonResult();
        ApexSingleValue singleValue = (ApexSingleValue) objectProperties;
        MatcherAssert.assertThat(objectProperties, not(nullValue()));
        MatcherAssert.assertThat(objectProperties.getDeclaredType().get(), equalTo("Account"));
        MatcherAssert.assertThat(
                singleValue.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                objectProperties.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testObjectPropertiesStandardObjectExtraProperties() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       Account obj = new Account(Name = 'Acme Inc.');\n"
                        + "       obj.Phone = '415-555-1212';\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ObjectProperties objectProperties = visitor.getSingletonResult();
        ApexSingleValue singleValue = (ApexSingleValue) objectProperties;
        MatcherAssert.assertThat(objectProperties, not(nullValue()));
        MatcherAssert.assertThat(objectProperties.getDeclaredType().get(), equalTo("Account"));
        MatcherAssert.assertThat(
                singleValue.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(2)));

        Map.Entry<ApexValue<?>, ApexValue<?>> property;

        // Find the name property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Name"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));

        // Find the phone property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Phone"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Phone"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("415-555-1212"));
    }

    @Test
    public void testObjectPropertiesStandardObjectReassigned() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       Account obj = new Account(Name = 'Acme Inc.', Phone = '415-555-1212');\n"
                        + "       obj = new Account(Name = 'Acme Inc. 2');\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ObjectProperties objectProperties = visitor.getSingletonResult();
        ApexSingleValue singleValue = (ApexSingleValue) objectProperties;
        MatcherAssert.assertThat(objectProperties, not(nullValue()));
        MatcherAssert.assertThat(objectProperties.getDeclaredType().get(), equalTo("Account"));
        MatcherAssert.assertThat(
                singleValue.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        // Phone was overwritten
        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(1)));

        Map.Entry<ApexValue<?>, ApexValue<?>> property;

        // Find the name property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Name"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc. 2"));
    }

    @Test
    public void testObjectPropertiesStandardObjectDefaultConstructor() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       Account obj = new Account();\n"
                        + "       obj.Name = 'Acme Inc.';\n"
                        + "       obj.Phone = '415-555-1212';\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ObjectProperties objectProperties = visitor.getSingletonResult();
        ApexSingleValue singleValue = (ApexSingleValue) objectProperties;
        MatcherAssert.assertThat(objectProperties, not(nullValue()));
        MatcherAssert.assertThat(objectProperties.getDeclaredType().get(), equalTo("Account"));
        MatcherAssert.assertThat(
                singleValue.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(2)));

        Map.Entry<ApexValue<?>, ApexValue<?>> property;

        // Find the name property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Name"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));

        // Find the phone property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Phone"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Phone"));
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("415-555-1212"));
    }

    @Test
    public void testObjectPropertiesStandardObjectDefaultConstructorReassigned() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       Account obj = new Account();\n"
                        + "       obj.Name = 'Acme Inc.';\n"
                        + "       obj.Phone = '415-555-1212';\n"
                        + "       obj = new Account();\n"
                        + "       obj.Name = 'Acme Inc. 2';\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ObjectProperties objectProperties = visitor.getSingletonResult();
        ApexSingleValue singleValue = (ApexSingleValue) objectProperties;
        MatcherAssert.assertThat(objectProperties, not(nullValue()));
        MatcherAssert.assertThat(objectProperties.getDeclaredType().get(), equalTo("Account"));
        MatcherAssert.assertThat(
                singleValue.getTypeVertex().get().getCanonicalType(), equalTo("Account"));

        // Only one property because phone was overwritten
        MatcherAssert.assertThat(
                objectProperties.getApexValueProperties().keySet(), hasSize(equalTo(1)));

        Map.Entry<ApexValue<?>, ApexValue<?>> property;

        // Find the name property
        property =
                objectProperties.getApexValueProperties().entrySet().stream()
                        .filter(e -> TestUtil.apexValueToString(e.getKey()).equals("Name"))
                        .collect(Collectors.toList())
                        .get(0);
        MatcherAssert.assertThat(property.getKey(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(property.getValue(), instanceOf(ApexStringValue.class));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc. 2"));
    }

    @Test
    public void testObjectPropertiesSObjectReassigned() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       obj = Schema.getGlobalDescribe().get('Contact').newSObject();\n"
                        + "       obj.put('Phone', '415-555-1212');\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Contact"));

        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                apexSingleValue.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Phone"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("415-555-1212"));
    }

    private static final String NESTED_CLASS_SOURCE =
            "public class NestedClass {\n"
                    + "   public Boolean instanceBool;\n"
                    + "   public static String staticStr;\n"
                    + "   public NestedClass() {\n"
                    + "       instanceBool = true;\n"
                    + "       staticStr = 'beep';\n"
                    + "   }\n"
                    + "}";

    private static final String OTHER_CLASS_SOURCE =
            "public class OtherClass {\n"
                    + "   public Integer instanceInt;\n"
                    + "   public static String staticStr;\n"
                    + "   public NestedClass nc;\n"
                    + "   public OtherClass() {\n"
                    + "       instanceInt = 25;\n"
                    + "       staticStr = 'boop';\n"
                    + "       nc = new NestedClass();\n"
                    + "   }\n"
                    + "}";

    @Test
    public void testGetTypedVertexFromObjectProperties() {
        String mainClassSource =
                "public class MainClass {\n"
                        + "   public void testedMethod() {\n"
                        + "       OtherClass oc = new OtherClass();\n"
                        + "       NestedClass nc = new NestedClass();\n"
                        + "       System.debug(oc.instanceInt);\n"
                        + "       System.debug(OtherClass.staticStr);\n"
                        + "       System.debug(nc.instanceBool);\n"
                        + "       System.debug(NestedClass.staticStr);\n"
                        + "   }\n"
                        + "}";

        // Create a graph from our sources.
        TestUtil.buildGraph(
                g, new String[] {NESTED_CLASS_SOURCE, OTHER_CLASS_SOURCE, mainClassSource});

        // Get the vertex defining testedMethod().
        MethodVertex testedMethod = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);

        // Get the paths through testedMethod(). There should only be one.
        ApexPath path = ApexPathUtil.getForwardPaths(g, testedMethod, true).get(0);

        // We'll map method calls to the typed vertices associated with their arguments.
        // Some of the cases will be throwing TodoExceptions, so we'll want to track those as well.
        Map<MethodCallExpressionVertex, Typeable> typedVerticesByMethodCall = new HashMap<>();
        Map<MethodCallExpressionVertex, TodoException> todoExceptionsByMethodCall = new HashMap<>();
        // Declare an overridden visitor that performs the traversal we're looking for.
        PathVertexVisitor pvv =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        // We only care about invocations of System.debug.
                        if (vertex.getFullMethodName().equals("System.debug")) {
                            // Get the vertex representing the first (and only) argument.
                            ChainedVertex argVertex = vertex.getParameters().get(0);
                            // The argument should always be an instance of
                            // VariableExpressionVertex. Verify that so we can
                            // downcast it.
                            if (argVertex instanceof VariableExpressionVertex) {
                                // Get the chain of symbolic names leading to the Typeable we're
                                // after.
                                List<String> symbolicNameChain =
                                        ((VariableExpressionVertex) argVertex)
                                                .getSymbolicNameChain();
                                // Attempt to follow the chain all the way down.
                                try {
                                    Typeable declaration =
                                            symbols.getTypedVertex(symbolicNameChain).orElse(null);
                                    // We expect all of these declarations to be non-null. So if one
                                    // returns null, throw a TodoException.
                                    if (declaration == null) {
                                        throw new TodoException(
                                                "Thrown By Test: getTypedVertex() returned null");
                                    }
                                    // Map a successful result by the method call vertex.
                                    typedVerticesByMethodCall.put(vertex, declaration);
                                } catch (TodoException ex) {
                                    // Map a TodoException by the method call vertex.
                                    todoExceptionsByMethodCall.put(vertex, ex);
                                }
                            }
                        }
                        return true;
                    }
                };

        // Declare a symbol provider.
        SymbolProviderVertexVisitor sp = new DefaultSymbolProviderVertexVisitor(g);

        // Walk the path.
        ApexPathWalker.walkPath(g, path, pvv, sp);

        // Assert that the right values were picked up along the way.
        MethodCallExpressionVertex ocInstance =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 5);
        MethodCallExpressionVertex ocStatic =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 6);
        MethodCallExpressionVertex ncInstance =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 7);
        MethodCallExpressionVertex ncStatic =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 8);

        assertEquals(2, typedVerticesByMethodCall.size());
        assertTrue(typedVerticesByMethodCall.containsKey(ocInstance));
        MatcherAssert.assertThat(
                typedVerticesByMethodCall.get(ocInstance).getCanonicalType(), equalTo("Integer"));
        assertTrue(typedVerticesByMethodCall.containsKey(ncInstance));
        MatcherAssert.assertThat(
                typedVerticesByMethodCall.get(ncInstance).getCanonicalType(), equalTo("Boolean"));

        assertEquals(2, todoExceptionsByMethodCall.size());
        assertTrue(todoExceptionsByMethodCall.containsKey(ocStatic));
        assertTrue(
                todoExceptionsByMethodCall
                        .get(ocStatic)
                        .getMessage()
                        .toLowerCase()
                        .startsWith("thrown by test"));
        assertTrue(todoExceptionsByMethodCall.containsKey(ncStatic));
        assertTrue(
                todoExceptionsByMethodCall
                        .get(ncStatic)
                        .getMessage()
                        .toLowerCase()
                        .startsWith("thrown by test"));
    }

    @Test
    @Disabled // This is now a log message
    public void testGetTypedVertexFromNestedObjectProperties() {
        String mainClassSource =
                "public class MainClass {\n"
                        + "   public void testedMethod() {\n"
                        + "       OtherClass oc = new OtherClass();\n"
                        + "       System.debug(oc.nc.instanceBool);\n"
                        + "   }\n"
                        + "}";

        // Create a graph from our sources.
        TestUtil.buildGraph(
                g, new String[] {NESTED_CLASS_SOURCE, OTHER_CLASS_SOURCE, mainClassSource});

        // Get the vertex defining testedMethod().
        MethodVertex testedMethod = TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD, 2);

        // Get the paths through testedMethod(). There should only be one.
        ApexPath path = ApexPathUtil.getForwardPaths(g, testedMethod, true).get(0);

        // We'll map method calls to the typed vertices associated with their arguments.
        // Some of the cases will be throwing TodoExceptions, so we'll want to track those as well.
        Map<MethodCallExpressionVertex, Typeable> typedVerticesByMethodCall = new HashMap<>();
        Map<MethodCallExpressionVertex, TodoException> todoExceptionsByMethodCall = new HashMap<>();
        // Declare an overridden visitor that performs the traversal we're looking for.
        PathVertexVisitor pvv =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public boolean visit(
                            MethodCallExpressionVertex vertex, SymbolProvider symbols) {
                        // We only care about invocations of System.debug.
                        if (vertex.getFullMethodName().equals("System.debug")) {
                            // Get the vertex representing the first (and only) argument.
                            ChainedVertex argVertex = vertex.getParameters().get(0);
                            // The argument should always be an instance of
                            // VariableExpressionVertex. Verify that so we can
                            // downcast it.
                            if (argVertex instanceof VariableExpressionVertex) {
                                // Get the chain of symbolic names leading to the Typeable we're
                                // after.
                                List<String> symbolicNameChain =
                                        ((VariableExpressionVertex) argVertex)
                                                .getSymbolicNameChain();
                                // Attempt to follow the chain all the way down.
                                try {
                                    Typeable declaration =
                                            symbols.getTypedVertex(symbolicNameChain).orElse(null);
                                    // We expect all of these declarations to be non-null. So if one
                                    // returns null, throw a TodoException.
                                    if (declaration == null) {
                                        throw new TodoException(
                                                "Thrown By Test: getTypedVertex() returned null");
                                    }
                                    // Map a successful result by the method call vertex.
                                    typedVerticesByMethodCall.put(vertex, declaration);
                                } catch (TodoException ex) {
                                    // Map a TodoException by the method call vertex.
                                    todoExceptionsByMethodCall.put(vertex, ex);
                                }
                            }
                        }
                        return true;
                    }
                };

        // Declare a symbol provider.
        SymbolProviderVertexVisitor sp = new DefaultSymbolProviderVertexVisitor(g);

        // Walk the path.
        ApexPathWalker.walkPath(g, path, pvv, sp);

        // Assert that the right values were picked up along the way.
        MethodCallExpressionVertex nestedReference =
                TestUtil.getVertexOnLine(g, ASTConstants.NodeType.METHOD_CALL_EXPRESSION, 4);
        assertEquals(0, typedVerticesByMethodCall.size());
        assertEquals(1, todoExceptionsByMethodCall.size());
        assertTrue(todoExceptionsByMethodCall.containsKey(nestedReference));
        // The exception should be coming from within the production code.
        assertFalse(
                todoExceptionsByMethodCall
                        .get(nestedReference)
                        .getMessage()
                        .toLowerCase()
                        .startsWith("thrown by test"));
    }

    @Test
    public void testObjectPropertiesSObjectNulled() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       obj = null;\n"
                        + "       System.debug(obj);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexSingleValue.getReturnedFrom().isPresent(), equalTo(false));
        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(0)));
    }

    @Test
    public void testVariableOverwritesItself() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       String s = 'Hello';\n"
                        + "       bar(s);\n"
                        + "    }\n"
                        + "    public void bar(String s) {\n"
                        + "       s = s;\n"
                        + "       System.debug(s);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    @Test
    public void testVariableRedirectionToAnotherVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        SObjectType obj = Schema.getGlobalDescribe().get('Contact');\n"
                        + "        Map<String,Schema.SObjectField> m = obj.getDescribe().fields.getMap();\n"
                        + "        System.debug(obj);\n"
                        + "        System.debug(m);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        SObjectType sObjectType = visitor.getResult(0);
        MatcherAssert.assertThat(TestUtil.apexValueToString(sObjectType), equalTo("Contact"));

        ApexFieldDescribeMapValue fieldDescribeMap = visitor.getResult(1);
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(
                        fieldDescribeMap.getAssociatedObjectType().get().getType()),
                equalTo("Contact"));
    }

    /** This tests that the DefaultNoOpPathVertexVisitor is correctly cloning the stack */
    @Test
    // Copied from github.com/SalesforceFoundation/NPSP
    public void testForkedPaths() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public void doSomething() {\n"
                    + "       Schema.DescribeSObjectResult result = DescribeSingleton.getObjectDescribe(MyObject__c.SObjectType);\n"
                    + "       System.debug(result);\n"
                    + "   }\n"
                    + "}",
            "public class DescribeSingleton {\n"
                    + "   private static Map<Schema.SObjectType, Schema.DescribeSObjectResult> objectDescribesByType = new Map<Schema.SObjectType, Schema.DescribeSObjectResult>();\n"
                    + "   private static Map<String, Schema.SObjectType> gd;\n"
                    + "   private static Strong aString;\n"
                    + "   public static Schema.DescribeSObjectResult getObjectDescribe(SObjectType objType) {\n"
                    + "       fillMapsForObject(objType.getDescribe().getName());\n"
                    + "       return objectDescribesByType.get(objType);\n"
                    + "   }\n"
                    + "   static void fillMapsForObject(string objectName) {\n"
                    + "       objectName = objectName.toLowerCase();\n"
                    + "       gd = Schema.getGlobalDescribe();\n"
                    + "       Schema.DescribeSObjectResult objDescribe = gd.get(objectName).getDescribe();\n"
                    + "       objectDescribesByType.put(objDescribe.getSObjectType(), objDescribe);\n"
                    + "       if (aString == null) {\n"
                    + "           logNull('is null');\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n"
        };

        // Use a config that doesn't do any collapsing
        ApexPathExpanderConfig expanderConfig =
                ApexPathExpanderConfig.Builder.get().expandMethodCalls(true).build();

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            DescribeSObjectResult describeSObjectResult = visitor.getSingletonResult();
            MatcherAssert.assertThat(describeSObjectResult.isDeterminant(), IsEqual.equalTo(true));
        }
    }
}
