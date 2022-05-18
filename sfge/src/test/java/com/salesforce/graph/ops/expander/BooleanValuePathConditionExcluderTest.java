package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerListMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests paths that are collapsible using only the {@link BooleanValuePathConditionExcluder} */
public class BooleanValuePathConditionExcluderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /** Validate a complex path */
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testCustomSettingsPathCollapsingStaticMethods(boolean withCollapsers) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "       EmailSettings__c emailSettings = CustomSettingClass.getEmailSettings();\n"
                    + "       System.debug(emailSettings);\n"
                    + "    }\n"
                    + "}\n",
            "public class CustomSettingClass {\n"
                    + "    static EmailSettings__c emailSettings;\n"
                    + "    static EmailSettings__c orgEmailSettings;\n"
                    + "    public static EmailSettings__c getEmailSettings() {\n"
                    + "       if (emailSettings == null) {\n"
                    + "           emailSettings = EmailSettings__c.getInstance();\n"
                    + "           if (emailSettings.Id == null) {\n"
                    + "               emailSettings = getOrgEmailSettings();\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return emailSettings;\n"
                    + "    }\n"
                    + "    public static EmailSettings__c getOrgEmailSettings() {\n"
                    + "       if (orgEmailSettings == null) {\n"
                    + "           orgEmailSettings = EmailSettings__c.getOrgDefaults();\n"
                    + "           if (orgEmailSettings.Id == null) {\n"
                    + "               orgEmailSettings.SetupOwnerId = UserInfo.getOrganizationId();\n"
                    + "               upsert orgEmailSettings;\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return orgEmailSettings;\n"
                    + "    }\n"
                    + "}\n"
        };

        ApexPathExpanderConfig expanderConfig;
        if (withCollapsers) {
            expanderConfig = getApexPathExpanderConfig();
        } else {
            expanderConfig = ApexPathUtil.getSimpleExpandingConfig();
        }

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();

        // The excluders will filter out the null cases that don't make sense. The only if is
        // emailSettings.Id == null and orgEmailSettings.Id == null

        if (withCollapsers) {
            MatcherAssert.assertThat(results, hasSize(equalTo(3)));
            // The results are only ApexCustomValue when the invalid paths are collapsed
            for (TestRunner.Result<SystemDebugAccumulator> result : results) {
                SystemDebugAccumulator visitor = result.getVisitor();
                ApexCustomValue value = visitor.getSingletonResult();
                MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));
                MatcherAssert.assertThat(
                        value.getTypeVertex().get().getCanonicalType(),
                        equalTo("EmailSettings__c"));
            }
        } else {
            MatcherAssert.assertThat(results, hasSize(equalTo(5)));
            int validPaths = 0;
            for (TestRunner.Result<SystemDebugAccumulator> result : results) {
                SystemDebugAccumulator visitor = result.getVisitor();
                if (visitor.getSingletonResult() instanceof ApexCustomValue) {
                    validPaths++;
                }
            }
            // This 3 because the 2 paths that are exclude never invoked the #getInstance or
            // #getOrgDefaults methods
            MatcherAssert.assertThat(validPaths, equalTo(3));
        }
    }

    /** This is not collapsed because the all of the values are unresolved */
    @Test
    public void testConsistentPathStringMethodIsNotCollapsedSimple() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    static String namespace;\n"
                    + "    public void doSomething() {\n"
                    + "       String s = getNamespace();\n"
                    + "    }\n"
                    + "    public static string getNamespace() {\n"
                    + "       if (namespace == null) {\n"
                    + "           String className = MyOtherClass.getSomeValue();\n"
                    + "           if (className.contains('.')) {\n"
                    + "               namespace = className.subStringBefore('.');\n"
                    + "           } else {\n"
                    + "               namespace = className.subStringAfter('.');\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return namespace;\n"
                    + "    }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));
    }

    /** This will not be collapsed because none of the results are resolved */
    @Test
    public void testUnresolvableStringMethodIsNotCollapsedStacked() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    static String namespace;\n"
                    + "    public void doSomething(Boolean b, String str) {\n"
                    + "       String s = addNamespacePrefix(b, str);\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "    public static string addNamespacePrefix(Boolean b, String str) {\n"
                    + "       String namespace = getNamespace(str);\n"
                    + "       if (b) {\n"
                    + "           return str;\n"
                    + "       } else {\n"
                    + "           return namespace + '__' + str;\n"
                    + "       }\n"
                    + "    }\n"
                    + "    public static String getNamespace(String str) {\n"
                    + "       if (str == '') {\n"
                    + "           return MyOtherClass1.getNamespace2();\n"
                    + "       } else {\n"
                    + "           return MyOtherClass2.getNamespace2();"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(4)));
    }

    /** getNamespace should resolve to a single path, since everything is resolvable */
    @Test
    public void testGetNamespace() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       String s = getNamespace();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "    public static String getNamespace() {\n"
                    + "       if (namespace == null) {\n"
                    + "           String className = MyClass.class.getName();\n"
                    + "           if (className.contains('.')) {\n"
                    + "               namespace = className.subStringBefore('.');\n"
                    + "           } else {\n"
                    + "               namespace = '';\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return namespace;\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getSingletonResult()), equalTo(""));
    }

    @Test
    public void testTokenizeNoNamespace() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       String s1 = StrTokenNSPrefix('hello');\n"
                    + "       System.debug(s1);\n"
                    + "       String s2 = StrTokenNSPrefix('goodbye');\n"
                    + "       System.debug(s2);\n"
                    + "       String s3 = StrTokenNSPrefix('congratulations');\n"
                    + "       System.debug(s3);\n"
                    + "    }\n"
                    + "    public static String getNamespace() {\n"
                    + "       if (namespace == null) {\n"
                    + "           String className = MyClass.class.getName();\n"
                    + "           if (className.contains('.')) {\n"
                    + "               namespace = className.subStringBefore('.');\n"
                    + "           } else {\n"
                    + "               namespace = '';\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return namespace;\n"
                    + "    }\n"
                    + "    public static string StrTokenNSPrefix(string str) {\n"
                    + "       if (getNamespace() == '') return str;\n"
                    + "       return getNamespace() + '__' + str;\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(0)), equalTo("hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(1)), equalTo("goodbye"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(2)),
                equalTo("congratulations"));
    }

    @Test
    public void testTokenizeNamespacePresent() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       String s1 = StrTokenNSPrefix('hello');\n"
                    + "       System.debug(s1);\n"
                    + "       String s2 = StrTokenNSPrefix('goodbye');\n"
                    + "       System.debug(s2);\n"
                    + "       String s3 = StrTokenNSPrefix('congratulations');\n"
                    + "       System.debug(s3);\n"
                    + "    }\n"
                    + "    public static String getNamespace() {\n"
                    + "       if (namespace == null) {\n"
                    + "           String className = 'myns.' + MyClass.class.getName();\n"
                    + "           if (className.contains('.')) {\n"
                    + "               namespace = className.subStringBefore('.');\n"
                    + "           } else {\n"
                    + "               namespace = '';\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return namespace;\n"
                    + "    }\n"
                    + "    public static string StrTokenNSPrefix(string str) {\n"
                    + "       if (getNamespace() == '') return str;\n"
                    + "       return getNamespace() + '__' + str;\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(0)), equalTo("myns__hello"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(1)),
                equalTo("myns__goodbye"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(visitor.getAllResults().get(2)),
                equalTo("myns__congratulations"));
    }

    @Test
    public void testUnresolvableIntegerMethodIsNotCollapsedStacked() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    static Integer namespace;\n"
                    + "    public void doSomething() {\n"
                    + "       String s = addNamespacePrefix('MyObject__c');\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "    public static string addNamespacePrefix(String str) {\n"
                    + "       if (getInt() == 10) {\n"
                    + "           return str;\n"
                    + "       } else {\n"
                    + "           return getInt() + '__' + str;\n"
                    + "       }\n"
                    + "    }\n"
                    + "    public static Integer getInt() {\n"
                    + "       return MyOtherClass.getInteger();\n"
                    + "    }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        // The methods will not be collapsed because the MyOtherClass.getInteger() is unresolved
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));
    }

    @Test
    public void testCachedSingletonIsCollapsed() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(MySingleton.getInstance());\n"
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
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue classInstanceValue =
                (ApexClassInstanceValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                classInstanceValue.getCanonicalType(), Matchers.equalTo("MySingleton"));
        // It should be the singleton created within the if statement
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(5));
    }

    @Test
    public void testCachedSingletonIsCollapsedInvertedLogic() throws Exception {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(MySingleton.getInstance());\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton != null) {\n"
                    + "           return singleton;\n"
                    + "       } else {\n"
                    + "           singleton = new MySingleton();\n"
                    + "           return singleton;\n"
                    + "       }\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue classInstanceValue =
                (ApexClassInstanceValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                classInstanceValue.getCanonicalType(), Matchers.equalTo("MySingleton"));
        // It should be the singleton created within the else statement
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(7));
    }

    @Test
    public void testCachedSingletonIsCollapsedInvertedLogicCreationOutsideOfElse()
            throws Exception {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(MySingleton.getInstance());\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       if (singleton != null) {\n"
                    + "           return singleton;\n"
                    + "       }\n"
                    + "       singleton = new MySingleton();\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue classInstanceValue =
                (ApexClassInstanceValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                classInstanceValue.getCanonicalType(), Matchers.equalTo("MySingleton"));
        // It should be the singleton created after the if statement
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(7));
    }

    /**
     * This test was added to ensure that a singleton method(#getName) which had a name that
     * collides with a static method of the consuming class acts as expected. Previously the
     * singleton code was not resolving correctly.
     */
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

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        ApexPath path = result.getPath();

        Map<InvocableVertex, ApexPath> methodPaths;

        // method paths from foo
        methodPaths = path.getInvocableVertexToPaths();
        MatcherAssert.assertThat(methodPaths.entrySet(), hasSize(equalTo(1)));

        // method paths from getName
        path = methodPaths.entrySet().iterator().next().getValue();
        methodPaths = path.getInvocableVertexToPaths();
        MatcherAssert.assertThat(methodPaths.entrySet(), hasSize(equalTo(2)));

        // Find the path that corresponds to #getInstance
        List<Map.Entry<InvocableVertex, ApexPath>> entries;

        // Validate getInstance path
        entries =
                methodPaths.entrySet().stream()
                        .filter(
                                e ->
                                        ((MethodCallExpressionVertex) e.getKey())
                                                .getMethodName()
                                                .equals("getInstance"))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(entries, hasSize(equalTo(1)));
        path = entries.get(0).getValue();
        // Match "return singleton;"
        MatcherAssert.assertThat(path.lastVertex().getBeginLine(), equalTo(7));

        // Validate getName path
        entries =
                methodPaths.entrySet().stream()
                        .filter(
                                e ->
                                        ((MethodCallExpressionVertex) e.getKey())
                                                .getMethodName()
                                                .equals("getName"))
                        .collect(Collectors.toList());
        MatcherAssert.assertThat(entries, hasSize(equalTo(1)));
        path = entries.get(0).getValue();
        // Match "return 'Acme Inc.'"
        MatcherAssert.assertThat(path.lastVertex().getBeginLine(), equalTo(10));
    }

    /** This will be collapsed because singleton is initialized to null by default */
    @Test
    public void testCachedSingletonVoidMethodIsCollapsed() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MySingleton.initialize();\n"
                    + "       System.debug(MySingleton.getInstance());\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    public void initialize() {\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "    }\n"
                    + "    public MySingleton getInstance() {\n"
                    + "    	return singleton;\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue classInstanceValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(classInstanceValue.getCanonicalType(), equalTo("MySingleton"));
        // It should be the singleton created within the if statement
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(5));
    }

    /** This will be collapsed because singleton is overwritten to null */
    @Test
    public void testNulledValueIsCollapsed() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(MySingleton.getInstance());\n"
                    + "       System.debug(MySingleton.getInstance().getGreeting());\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton = new MySingleton();\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       singleton = null;\n"
                    + "       if (singleton == null) {\n"
                    + "           singleton = new MySingleton();\n"
                    + "       }\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public String getGreeting() {\n"
                    + "    	return 'Hello';\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(IsEqual.equalTo(2)));

        ApexClassInstanceValue classInstanceValue =
                (ApexClassInstanceValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                classInstanceValue.getCanonicalType(), Matchers.equalTo("MySingleton"));
        // It should be the singleton created within the if statement
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(6));

        ApexStringValue greeting = (ApexStringValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(greeting), IsEqual.equalTo("Hello"));
    }

    public static Stream<Arguments> testStringContains() {
        return Stream.of(
                Arguments.of("!c.contains('.')", "'MyValue'", Arrays.asList("ifBranch")),
                Arguments.of("c.contains('.')", "'MyValue'", Arrays.asList("elseBranch")),
                Arguments.of("c.contains('.')", "'MyValue.WithDot'", Arrays.asList("ifBranch")),
                Arguments.of("!c.contains('.')", "'MyValue.WithDot'", Arrays.asList("elseBranch")),
                // Unresolved variable should force all branches
                Arguments.of("c.contains('.')", "x", Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testStringContains(
            String comparison, String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String c = "
                    + variableValue
                    + ";\n"
                    + "       if ("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testStringEquality() {
        return Stream.of(
                // ==
                Arguments.of("c == ''", "''", Arrays.asList("ifBranch")),
                Arguments.of("c == ''", "'foo'", Arrays.asList("elseBranch")),
                // Unresolved variable should force all branches
                Arguments.of("c == ''", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("c == 'foo'", "'foo'", Arrays.asList("ifBranch")),
                Arguments.of("c == 'foo'", "'notFoo'", Arrays.asList("elseBranch")),
                // Unresolved variable should force all branches
                Arguments.of("c == 'foo'", "x", Arrays.asList("ifBranch", "elseBranch")),

                // !=
                Arguments.of("c != ''", "''", Arrays.asList("elseBranch")),
                Arguments.of("c != ''", "'foo'", Arrays.asList("ifBranch")),
                // Unresolved variable should force all branches
                Arguments.of("c != ''", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("c != 'foo'", "'foo'", Arrays.asList("elseBranch")),
                Arguments.of("c != 'foo'", "'notFoo'", Arrays.asList("ifBranch")),
                // Unresolved variable should force all branches
                Arguments.of("c != 'foo'", "x", Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testStringEquality(
            String comparison, String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String c = getNamespace();\n"
                    + "       if ("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "    public static string getNamespace() {\n"
                    + "       return "
                    + variableValue
                    + ";\n"
                    + "    }\n"
                    + "}\n"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testIsRunningTest() {
        return Stream.of(
                Arguments.of("!Test.isRunningTest()", Arrays.asList("ifBranch")),
                Arguments.of("Test.isRunningTest()", Arrays.asList("elseBranch")),
                Arguments.of("!Test.isRunningTest() && b", Arrays.asList("elseBranch")),
                Arguments.of("Test.isRunningTest() && b", Arrays.asList("elseBranch")),
                Arguments.of("!Test.isRunningTest() || b", Arrays.asList("ifBranch")),
                Arguments.of("Test.isRunningTest() || b", Arrays.asList("elseBranch")),
                Arguments.of("!Test.isRunningTest() && x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("Test.isRunningTest() && x", Arrays.asList("elseBranch")),
                Arguments.of("!Test.isRunningTest() || x", Arrays.asList("ifBranch")),
                Arguments.of("Test.isRunningTest() || x", Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: comparison=({0}):expectedOutput=({1})")
    public void testIsRunningTest(String comparison, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static Boolean b;\n"
                    + "    public static void doSomething() {\n"
                    + "       if ("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testConstantBooleanVariable() {
        return Stream.of(
                Arguments.of("true", Arrays.asList("ifBranch")),
                Arguments.of("false", Arrays.asList("elseBranch")),
                Arguments.of("x", Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: variableValue=({0}):expectedOutput=({1})")
    public void testConstantBooleanVariable(String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(Boolean x) {\n"
                    + "       logSomething("
                    + variableValue
                    + ");\n"
                    + "    }\n"
                    + "    public static void logSomething(Boolean b) {\n"
                    + "       if(b) {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testConstantBoolean() {
        return Stream.of(
                Arguments.of("true", Arrays.asList("ifBranch")),
                Arguments.of("false", Arrays.asList("elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: variableValue=({0}):expectedOutput=({1})")
    public void testConstantBoolean(String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       logSomething();\n"
                    + "    }\n"
                    + "    public static void logSomething() {\n"
                    + "       if("
                    + variableValue
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testStringConstantNullVariable() {
        return Stream.of(
                Arguments.of("!o", "null", Arrays.asList("ifBranch")),
                Arguments.of("o", "null", Arrays.asList("elseBranch")),
                Arguments.of("o == null", "null", Arrays.asList("ifBranch")),
                Arguments.of("o != null", "null", Arrays.asList("elseBranch")),
                Arguments.of("!o", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("o", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("o == null", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("o != null", "x", Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testStringConstantNullVariable(
            String comparison, String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(String x) {\n"
                    + "       String s = "
                    + variableValue
                    + ";\n"
                    + "       logSomething(s);\n"
                    + "    }\n"
                    + "    public static void logSomething(String o) {\n"
                    + "       if("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testEnum() {
        return Stream.of(
                Arguments.of("!dt", "null", Arrays.asList("ifBranch")),
                Arguments.of("dt", "null", Arrays.asList("elseBranch")),
                Arguments.of("dt == null", "null", Arrays.asList("ifBranch")),
                Arguments.of("dt != null", "null", Arrays.asList("elseBranch")),
                Arguments.of("!dt", "dtParam", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("dt", "dtParam", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("dt == null", "dtParam", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("dt != null", "dtParam", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of("!dt", "DisplayType.ADDRESS", Arrays.asList("elseBranch")),
                Arguments.of("dt", "DisplayType.ADDRESS", Arrays.asList("ifBranch")),
                Arguments.of("dt == null", "DisplayType.ADDRESS", Arrays.asList("elseBranch")),
                Arguments.of("dt != null", "DisplayType.ADDRESS", Arrays.asList("ifBranch")),
                Arguments.of(
                        "dt == DisplayType.ANYTYPE",
                        "DisplayType.ADDRESS",
                        Arrays.asList("elseBranch")),
                Arguments.of(
                        "dt != DisplayType.ANYTYPE",
                        "DisplayType.ADDRESS",
                        Arrays.asList("ifBranch")));
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testEnum(String comparison, String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(DisplayType dtParam) {\n"
                    + "       DisplayType dt = "
                    + variableValue
                    + ";\n"
                    + "       logSomething(dt);\n"
                    + "    }\n"
                    + "    public static void logSomething(DisplayType dt) {\n"
                    + "       if("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testList() {
        return Stream.of(
                Arguments.of("o == null || o.size() == 0", "null", Arrays.asList("ifBranch")),
                Arguments.of("o == null || o.isEmpty()", "null", Arrays.asList("ifBranch")),
                Arguments.of(
                        "o == null || o.isEmpty()", "new List<User>()", Arrays.asList("ifBranch")),
                Arguments.of(
                        "o == null || o.isEmpty()",
                        "new List<User>{(new User(Id = 'a'))}",
                        Arrays.asList("elseBranch")),
                Arguments.of(
                        "o != null && !o.isEmpty()",
                        "new List<User>{(new User(Id = 'a'))}",
                        Arrays.asList("ifBranch")),
                Arguments.of("o != null && o.size() != 0", "null", Arrays.asList("elseBranch")),
                Arguments.of(
                        "o != null && o.size() != 0",
                        "new List<String>(); s.add('Hello')",
                        Arrays.asList("ifBranch")),
                Arguments.of(
                        "o == null || o.size() == 0",
                        "new List<String>(); s.add('Hello')",
                        Arrays.asList("elseBranch")),
                Arguments.of(
                        "o != null && o.size() != 0", "x", Arrays.asList("ifBranch", "elseBranch")),
                Arguments.of(
                        "o == null || o.size() == 0",
                        "x",
                        Arrays.asList("ifBranch", "elseBranch")));
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testList(String comparison, String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(List<String> x) {\n"
                    + "       List<String> s = "
                    + variableValue
                    + ";\n"
                    + "       logSomething(s);\n"
                    + "    }\n"
                    + "    public static void logSomething(List<String> o) {\n"
                    + "       if("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "    }\n"
                    + "}"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testConstantNullVariableOrConditionNegativeIfIsFilteredOut() {
        return Stream.of(
                Arguments.of("= null;", Arrays.asList("isEmpty")),
                Arguments.of("= new List<String>();", Arrays.asList("isEmpty")),
                Arguments.of("= new List<String>(); s.add('Hello');", Arrays.asList("isNotEmpty")),
                // Unresolved variable should force all branches
                Arguments.of(
                        "= MyOtherClass.getSomething();", Arrays.asList("isEmpty", "isNotEmpty")));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: variableValue=({0}):expectedOutput=({1})")
    public void testConstantNullVariableOrConditionNegativeIfIsFilteredOut(
            String variableValue, List<String> expectedOutput) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       List<String> s "
                        + variableValue
                        + "\n"
                        + "       logSomething(s);\n"
                        + "    }\n"
                        + "    public static void logSomething(List<String> o) {\n"
                        + "       if(o == null || o.size() == 0) {\n"
                        + "           System.debug('isEmpty');\n"
                        + "       } else {\n"
                        + "           System.debug('isNotEmpty');\n"
                        + "       }\n"
                        + "    }\n"
                        + "}";

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testStringBooleanAndCondition() {
        String[] comparisonFormats = {
            "null != o && %s == o.length()",
            "null != o && o.length() == %s",
            "o != null && %s == o.length()",
            "o != null && o.length() == %s"
        };

        List<Arguments> arguments = new ArrayList<>();
        for (String comparisonFormat : comparisonFormats) {
            arguments.add(
                    Arguments.of(
                            String.format(comparisonFormat, "Hello".length()),
                            "'Hello';",
                            Arrays.asList("ifBranch")));
            arguments.add(
                    Arguments.of(
                            String.format(comparisonFormat, "Hello".length() + 1),
                            "'Hello';",
                            Arrays.asList("elseBranch")));
            arguments.add(
                    Arguments.of(
                            String.format(comparisonFormat, 0),
                            "null;",
                            Arrays.asList("elseBranch")));
            // Unresolved variable should force all branches
            arguments.add(
                    Arguments.of(
                            String.format(comparisonFormat, 5),
                            "x;",
                            Arrays.asList("ifBranch", "elseBranch")));
        }

        return arguments.stream();
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testStringBooleanAndCondition(
            String comparison, String variableValue, List<String> expectedOutput) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(String x) {\n"
                        + "       String s = "
                        + variableValue
                        + "\n"
                        + "       logSomething(s);\n"
                        + "    }\n"
                        + "    public static void logSomething(String o) {\n"
                        + "       if("
                        + comparison
                        + ") {\n"
                        + "           System.debug('ifBranch');\n"
                        + "       } else {\n"
                        + "           System.debug('elseBranch');\n"
                        + "       }\n"
                        + "    }\n"
                        + "}";

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testStringBooleanOrCondition() {
        String[] comparisonFormats = {
            "'%s' == o || '%s' == o",
            "'%s' == o || o == '%s'",
            "o == '%s' || '%s' == o",
            "o == '%s' || o == '%s'"
        };

        List<String> comparisons = new ArrayList<>();
        for (String comparisonFormat : comparisonFormats) {
            comparisons.add(String.format(comparisonFormat, "Hello", "Goodbye"));
            comparisons.add(String.format(comparisonFormat, "Goodbye", "Hello"));
        }

        List<Arguments> arguments = new ArrayList<>();
        for (String comparison : comparisons) {
            arguments.addAll(
                    Arrays.asList(
                            Arguments.of(comparison, "'Hello';", Arrays.asList("ifBranch")),
                            Arguments.of(comparison, "'Goodbye';", Arrays.asList("ifBranch")),
                            Arguments.of(comparison, "'H';", Arrays.asList("elseBranch")),
                            Arguments.of(comparison, "null;", Arrays.asList("elseBranch")),
                            Arguments.of(comparison, "'';", Arrays.asList("elseBranch")),
                            // Unresolved variable should force all branches
                            Arguments.of(
                                    comparison, "x;", Arrays.asList("ifBranch", "elseBranch"))));
        }

        return arguments.stream();
    }

    @MethodSource
    @ParameterizedTest(
            name = "{displayName}: comparison=({0}):variableValue=({1}):expectedOutput=({2})")
    public void testStringBooleanOrCondition(
            String comparison, String variableValue, List<String> expectedOutput) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       String s = "
                        + variableValue
                        + "\n"
                        + "       logSomething(s);\n"
                        + "    }\n"
                        + "    public static void logSomething(String o) {\n"
                        + "       if("
                        + comparison
                        + ") {\n"
                        + "           System.debug('ifBranch');\n"
                        + "       } else {\n"
                        + "           System.debug('elseBranch');\n"
                        + "       }\n"
                        + "    }\n"
                        + "}";

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    public static Stream<Arguments> testIfElseIf() {
        return Stream.of(
                Arguments.of("'MyNamespace.MyClass'", Arrays.asList("Namespaced")),
                Arguments.of("'MyClass-WithDash'", Arrays.asList("WithDash")),
                Arguments.of("'MyClass'", Arrays.asList("InitialValue")),
                // Unresolved variable should force all branches
                Arguments.of("x", Arrays.asList("InitialValue", "Namespaced", "WithDash")));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: variableValue=({0}):expectedOutput=({1})")
    public void testIfElseIf(String variableValue, List<String> expectedOutput) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       String c = "
                    + variableValue
                    + ";\n"
                    + "       String s = 'InitialValue';\n"
                    + "       if (c.contains('.')) {\n"
                    + "           s = 'Namespaced';\n"
                    + "       } else if (c.contains('-')) {\n"
                    + "           s = 'WithDash';\n"
                    + "       }\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}\n"
        };

        testExpectedOutputs(sourceCode, expectedOutput);
    }

    /** This tests that the state is retained across the two invocations */
    @Test
    public void testCachedResponse() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       System.debug(getNamespace());\n"
                    + "       System.debug(getNamespace());\n"
                    + "    }\n"
                    + "    public static String getNamespace() {\n"
                    + "       if (namespace == null) {\n"
                    + "           String className = MyClass.class.getName();\n"
                    + "           if (className.contains('.')) {\n"
                    + "               namespace = className.subStringBefore('.');\n"
                    + "           } else {\n"
                    + "               namespace = 'None';\n"
                    + "           }\n"
                    + "       }\n"
                    + "       return namespace;\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));
        for (int i = 0; i < 2; i++) {
            ApexStringValue apexStringValue =
                    (ApexStringValue) visitor.getAllResults().get(i).get();
            MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("None"));
        }
    }

    @Test
    public void testMethodResult() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "       System.debug(addNamespace('Hello'));\n"
                    + "       System.debug(addNamespace('Hello'));\n"
                    + "       System.debug(addNamespace('Hello'));\n"
                    + "   }\n"
                    + "   public static string addNamespace(String str) {\n"
                    + "        if (getNamespace() == '') return str;\n"
                    + "        return getNamespace() + '__' + str;\n"
                    + "   }\n"
                    + "   public static string getNamespace() {\n"
                    + "        return '';\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = walkPath(sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

        for (Optional<ApexValue<?>> value : visitor.getAllResults()) {
            MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Hello"));
        }
    }

    @Test
    public void testJSONDeserializeFieldsAreIndeterminant() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "       public DisplayType dt;\n"
                    + "   	public void logDisplayType() {\n"
                    + "   		if (dt == DisplayType.ADDRESS) {\n"
                    + "   			System.debug('address');\n"
                    + "   		} else if (dt == DisplayType.ANYTYPE) {\n"
                    + "   			System.debug('anytype');\n"
                    + "   		}\n"
                    + "   	}\n"
                    + "   }\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	InnerClass ic = (InnerClass)JSON.deserialize(asJson, InnerClass.class);\n"
                    + "   	ic.logDisplayType();\n"
                    + "   }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("address", "anytype"));
    }

    private void testExpectedOutputs(String sourceCode, List<String> expectedOutput) {
        testExpectedOutputs(new String[] {sourceCode}, expectedOutput);
    }

    private void testExpectedOutputs(String[] sourceCode, List<String> expectedOutput) {
        List<TestRunner.Result<SystemDebugAccumulator>> results = walkPaths(sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(expectedOutput.size())));

        List<String> values = new ArrayList<>();
        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();

            ApexStringValue apexStringValue = visitor.getSingletonResult();
            values.add(apexStringValue.getValue().get());
        }
        MatcherAssert.assertThat(values, containsInAnyOrder(expectedOutput.toArray()));
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        return ApexPathExpanderConfig.Builder.get()
                .expandMethodCalls(true)
                .with(BooleanValuePathConditionExcluder.getInstance())
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
