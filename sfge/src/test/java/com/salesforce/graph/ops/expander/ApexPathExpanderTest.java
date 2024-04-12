package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.ClassStaticScope;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerListMatcher;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexPathExpanderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @AfterEach
    public void cleanup() {
        SfgeConfigTestProvider.remove();
    }

    @Test
    public void testSingleton() {
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

        List<ApexPath> paths;
        ApexPath path;

        paths =
                TestRunner.get(g, sourceCode)
                        .withExpanderConfig(ApexPathUtil.getSimpleNonExpandingConfig())
                        .getPaths();
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        path = paths.get(0);
        // The path should not have any method calls mapped since we used the non-expanding config
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(0)));

        paths =
                ApexPathExpanderUtil.expand(
                                g, paths.get(0), ApexPathUtil.getSimpleExpandingConfig())
                        .getAcceptedResults();
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        path = paths.get(0);
        // The path should have a mapping for #getInstance and #getName
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(2)));
    }

    @Test
    public void testRegistryCleanup() {
        // Set the maximum expander registry size to a very low value.
        setPathRegistryLimit(6);
        // spotless:off
        String sourceCode =
            "public class MyClass {\n"
                // This method has three forks that create a total of 8 paths, which is greater
                // than the maximum registry size.
                // Since the paths fork via multiple methods instead of a single method,
                // the expanders are added to the stack gradually and should be removed at a similar
                // rate. So if they're being removed properly, the limit should never actually be reached,
                // and expansion should succeed.
          + "    public static void doSomething(boolean b1, boolean b2, boolean b3) {\n"
          + "        innerMethod1(b1);\n"
          + "        innerMethod2(b2);\n"
          + "        innerMethod3(b3);\n"
          + "    }\n"
          + "    \n"
          + "    private static void innerMethod1(boolean b) {\n"
          + "        if (b) {\n"
          + "            System.debug('b is true');\n"
          + "        } else {\n"
          + "            System.debug('b is false');\n"
          + "        }\n"
          + "    }\n"
          + "    \n"
          + "    private static void innerMethod2(boolean b) {\n"
          + "        if (b) {\n"
          + "            System.debug('b is true');\n"
          + "        } else {\n"
          + "            System.debug('b is false');\n"
          + "        }\n"
          + "    }\n"
          + "    \n"
          + "    private static void innerMethod3(boolean b) {\n"
          + "        if (b) {\n"
          + "            System.debug('b is true');\n"
          + "        } else {\n"
          + "            System.debug('b is false');\n"
          + "        }\n"
          + "    }\n"
          + "}\n";
        // spotless:on

        List<ApexPath> paths;
        ApexPath path;

        paths =
                TestRunner.get(g, sourceCode)
                        .withExpanderConfig(ApexPathUtil.getSimpleNonExpandingConfig())
                        .getPaths();
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        path = paths.get(0);
        // The path should not have any method calls mapped since we used the non-expanding config
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(0)));

        paths =
                ApexPathExpanderUtil.expand(
                                g, paths.get(0), ApexPathUtil.getSimpleExpandingConfig())
                        .getAcceptedResults();
        MatcherAssert.assertThat(paths, hasSize(equalTo(8)));
        path = paths.get(0);
        // The path should have a mapping for #getInstance and #getName
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(3)));
    }

    /**
     * Tests that fork in a static initialization path is correctly handled. There was previously a
     * bug in which the ApexPathExpander was invoking {@link ClassStaticScope#setInitialized()} too
     * early and the forked scopes weren't properly initialized.
     */
    @Test
    public void testSingletonWithForkedSingletonInstantiationPath() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    static MySingleton singleton = MySingleton.getInstance();\n"
                    + "    public static void doSomething() {\n"
                    + "       String s = singleton.getName();\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MySingleton {\n"
                    + "    private static MySingleton singleton;\n"
                    + "    private String aSetting;\n"
                    + "    public static MySingleton getInstance() {\n"
                    + "       singleton = new MySingleton();\n"
                    + "       singleton.configure();\n"
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    private void configure() {\n"
                    + "       if (MyOtherClass.getSetting()) {\n"
                    + "    		aSetting = 'Acme Inc. True';\n"
                    + "    	} else {\n"
                    + "    		aSetting = 'Acme Inc. False';\n"
                    + "    	}\n"
                    + "    }\n"
                    + "    public string getName() {\n"
                    + "       return aSetting;\n"
                    + "    }\n"
                    + "}",
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results,
                TestRunnerListMatcher.hasValuesAnyOrder("Acme Inc. True", "Acme Inc. False"));
    }

    @Test
    public void testNewObjectExpression() {
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
                    + "}",
        };

        List<ApexPath> paths;
        ApexPath path;

        paths =
                TestRunner.get(g, sourceCode)
                        .withExpanderConfig(ApexPathUtil.getSimpleNonExpandingConfig())
                        .getPaths();
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        path = paths.get(0);
        // The path should not have any method calls mapped since we used the non-expanding config
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(0)));

        paths =
                ApexPathExpanderUtil.expand(
                                g, paths.get(0), ApexPathUtil.getSimpleExpandingConfig())
                        .getAcceptedResults();
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));
        path = paths.get(0);
        // The path should have a mapping for new MyClass() and #doSomething
        MatcherAssert.assertThat(path.getInvocableVertexToPaths().entrySet(), hasSize(equalTo(2)));
    }

    @Test
    public void testInlineSingleton() {
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
                    + "       return singleton;\n"
                    + "    }\n"
                    + "    public String getGreeting() {\n"
                    + "    	return 'Hello';\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexClassInstanceValue classInstanceValue =
                (ApexClassInstanceValue) visitor.getAllResults().get(0).get();
        MatcherAssert.assertThat(classInstanceValue.isIndeterminant(), Matchers.equalTo(false));
        MatcherAssert.assertThat(classInstanceValue.isDeterminant(), Matchers.equalTo(true));
        MatcherAssert.assertThat(
                classInstanceValue.getCanonicalType(), Matchers.equalTo("MySingleton"));
        MatcherAssert.assertThat(
                classInstanceValue.getValueVertex().get().getBeginLine(), Matchers.equalTo(2));

        ApexStringValue greeting = (ApexStringValue) visitor.getAllResults().get(1).get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(greeting), equalTo("Hello"));
    }

    /**
     * MyThirdClass#getSettings has a branch which causes the static initialization to throw a
     * forked exception
     */
    @Test
    public void testStaticInitializationWithMultiplePaths() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static String doSomething() {\n"
                    + "   	MyFirstClass c = new MyFirstClass();\n"
                    + "       c.doSomethingFirst();"
                    + "       return '';"
                    + "	}\n"
                    + "}",
            "public class MyFirstClass {\n"
                    + "	public void doSomethingFirst() {\n"
                    + "		MySecondClass.doSomethingSecond();\n"
                    + "	}\n"
                    + "}",
            "public class MySecondClass {\n"
                    + "	public static MySettings__c mySettings = MyThirdClass.getSettings();\n"
                    + "	public static void doSomethingSecond() {\n"
                    + "		System.debug(mySettings.MyField__c);\n"
                    + "	}\n"
                    + "}",
            "public class MyThirdClass {\n"
                    + "	public static MySettings__c getSettings() {\n"
                    + "		MySettings__c orgSettings = MySettings__c.getOrgDefaults();\n"
                    + "		if (orgSettings.Id == null) {\n"
                    + "			orgSettings.MyField__c = 'Hello';\n"
                    + "		} else {\n"
                    + "			orgSettings.MyField__c = 'Goodbye';\n"
                    + "		}\n"
                    + "		return orgSettings;\n"
                    + "	}\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(
                results, TestRunnerListMatcher.hasValuesAnyOrder("Hello", "Goodbye"));
    }

    /**
     * MySecondClass#getSettings has a branch that ends in an exception. This should be filtered
     * out.
     */
    @Test
    public void testStaticInitializationWithMultiplePathsWithException() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public static void doSomething() {\n"
                    + "       MyFirstClass.doSomethingFirst();"
                    + "	}\n"
                    + "}",
            "public class MyFirstClass {\n"
                    + "	public static MySettings__c mySettings = MySecondClass.getSettings();\n"
                    + "	public static void doSomethingFirst() {\n"
                    + "		System.debug(mySettings.MyField__c);\n"
                    + "	}\n"
                    + "}",
            "public class MySecondClass {\n"
                    + "	public static MySettings__c getSettings() {\n"
                    + "		MySettings__c orgSettings = MySettings__c.getOrgDefaults();\n"
                    + "		configSettings(orgSettings);\n"
                    + "		return orgSettings;\n"
                    + "	}\n"
                    + "	public static void configSettings(MySettings__c settings) {\n"
                    + "		if (settings.Id == null) {\n"
                    + "			throw new MyException();\n"
                    + "		}\n"
                    + "		settings.MyField__c = 'Hello';\n"
                    + "	}\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    public static Stream<Arguments> testEnumSwitchStatement() {
        return Stream.of(
                Arguments.of("DisplayType.ADDRESs", "address or currency"),
                Arguments.of("DisplayType.CURRENCy", "address or currency"),
                Arguments.of("DisplayType.ANYTYPe", "any-type"),
                Arguments.of("DisplayType.BaSE64", "unknown"),
                Arguments.of("null", "null"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testEnumSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       System.debug('before-case');\n"
                        + "		DisplayType dt = "
                        + initializer
                        + ";\n"
                        + "       switch on dt {\n"
                        + "       	when ADdRESS, CUrRENCY {\n"
                        + "           	System.debug('address or currency');\n"
                        + "           }\n"
                        + "       	when ANyTYPE {\n"
                        + "           	System.debug('any-type');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "       System.debug('after-case');\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        // Check that scopes work as expected when the case is not the only item in the method
        MatcherAssert.assertThat(
                result, TestRunnerMatcher.hasValues("before-case", expected, "after-case"));
    }

    @MethodSource(value = "testEnumSwitchStatement")
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testEnumSwitchStatementWithReturn(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		DisplayType dt = "
                        + initializer
                        + ";\n"
                        + "    	System.debug(getString(dt));\n"
                        + "    }\n"
                        + "    public static String getString(DisplayType dt) {\n"
                        + "       switch on dt {\n"
                        + "       	when ADdRESS, CUrRENCY {\n"
                        + "           	return 'address or currency';\n"
                        + "           }\n"
                        + "       	when ANyTYPE {\n"
                        + "           	return 'any-type';\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	return 'null';\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	return 'unknown';\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    @MethodSource(value = "testEnumSwitchStatement")
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testMethodReturnEnumSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       switch on getDisplayType() {\n"
                        + "       	when ADdRESS, CUrRENCY {\n"
                        + "           	System.debug('address or currency');\n"
                        + "           }\n"
                        + "       	when ANyTYPE {\n"
                        + "           	System.debug('any-type');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "    public static DisplayType getDisplayType() {\n"
                        + "    	return "
                        + initializer
                        + ";\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    public static Stream<Arguments> testIntegerSwitchStatement() {
        return Stream.of(
                Arguments.of("1", "1 or 2"),
                Arguments.of("2", "1 or 2"),
                Arguments.of("3", "3"),
                Arguments.of("4", "unknown"),
                Arguments.of("null", "null"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testIntegerSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		Integer i = "
                        + initializer
                        + ";\n"
                        + "       switch on i {\n"
                        + "       	when 1, 2 {\n"
                        + "           	System.debug('1 or 2');\n"
                        + "           }\n"
                        + "       	when 3 {\n"
                        + "           	System.debug('3');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    // TODO: Integers aassigned to Longs should be promoted to longs
    public static Stream<Arguments> testLongSwitchStatement() {
        return Stream.of(
                Arguments.of("1l", "1 or 2"),
                Arguments.of("2l", "1 or 2"),
                Arguments.of("3l", "3"),
                Arguments.of("4l", "unknown"),
                Arguments.of("null", "null"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testLongSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		Long l = "
                        + initializer
                        + ";\n"
                        + "       switch on l {\n"
                        + "       	when 1l, 2l {\n"
                        + "           	System.debug('1 or 2');\n"
                        + "           }\n"
                        + "       	when 3l {\n"
                        + "           	System.debug('3');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    public static Stream<Arguments> testStringSwitchStatement() {
        return Stream.of(
                Arguments.of("'Hello'", "hello or goodbye"),
                Arguments.of("'Goodbye'", "hello or goodbye"),
                Arguments.of("'Congratulations'", "congratulations"),
                Arguments.of("'UnknownValue'", "unknown"),
                Arguments.of("null", "null"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testStringSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		String s = "
                        + initializer
                        + ";\n"
                        + "       switch on s {\n"
                        + "       	when 'Hello', 'Goodbye' {\n"
                        + "           	System.debug('hello or goodbye');\n"
                        + "           }\n"
                        + "       	when 'Congratulations' {\n"
                        + "           	System.debug('congratulations');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    public static Stream<Arguments> testInstanceOfSwitchStatement() {
        return Stream.of(
                Arguments.of("new Account(Name = 'Acme Inc.')", "account"),
                Arguments.of("new Contact(Name = 'Acme Inc.')", "contact"),
                Arguments.of("new MyObject__c(Name = 'Acme Inc.')", "unknown"),
                Arguments.of("null", "null"));
    }

    @MethodSource
    @ParameterizedTest(name = "{displayName}: initializer=({0})-expected=({1})")
    public void testInstanceOfSwitchStatement(String initializer, String expected) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		SObject obj = "
                        + initializer
                        + ";\n"
                        + "       switch on obj {\n"
                        + "       	when Account a {\n"
                        + "           	System.debug('account');\n"
                        + "           }\n"
                        + "       	when Contact c {\n"
                        + "           	System.debug('contact');\n"
                        + "           }\n"
                        + "       	when null {\n"
                        + "           	System.debug('null');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expected));
    }

    @Test
    public void testSwitchStatementForLoop() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(List<DisplayType> dts) {\n"
                        + "       for (DisplayType dt : dts) {\n"
                        + "       	switch on dt {\n"
                        + "       		when ADDRESS, CURRENCY {\n"
                        + "           		System.debug('address or currency');\n"
                        + "           	}\n"
                        + "       		when ANYTYPE {\n"
                        + "           		System.debug('anytype');\n"
                        + "           	}\n"
                        + "       		when else {\n"
                        + "           		System.debug('unknown');\n"
                        + "           	}\n"
                        + "       	}\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(3)));
        // We don't know the values of the list, nothing is excluded
        MatcherAssert.assertThat(
                results,
                TestRunnerListMatcher.hasValuesAnyOrder(
                        "address or currency", "anytype", "unknown"));
    }

    @Test
    public void testSwitchStatementIndeterminantValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething(DisplayType dt) {\n"
                        + "       switch on dt {\n"
                        + "       	when ADDRESS, CURRENCY {\n"
                        + "           	System.debug('address or currency');\n"
                        + "           }\n"
                        + "       	when ANYTYPE {\n"
                        + "           	System.debug('anytype');\n"
                        + "           }\n"
                        + "       	when else {\n"
                        + "           	System.debug('unknown');\n"
                        + "           }\n"
                        + "       }\n"
                        + "    }\n"
                        + "}\n";

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(3)));
        MatcherAssert.assertThat(
                results,
                TestRunnerListMatcher.hasValuesAnyOrder(
                        "address or currency", "anytype", "unknown"));
    }

    @Test
    public void testInlineResolvableTwoLevelMethodCall() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   void doSomething() {\n"
                    + "       System.debug(Factory.createInstance().secondLevel());\n"
                    + "   }\n"
                    + "}\n",
            "public class Factory {\n"
                    + "   public static Factory createInstance() {\n"
                    + "       return new Factory();\n"
                    + "   }\n"
                    + "   public String secondLevel() {\n"
                    + "       return 'hello';\n"
                    + "   }\n"
                    + "}\n"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(1)));
        MatcherAssert.assertThat(results, TestRunnerListMatcher.hasValuesAnyOrder("hello"));
    }

    @Test
    public void testMethodInvocationOnMethodInvocation() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static String doSomething() {\n"
                    + "        boolean output = UTIL_Describe.getIsDeletable();\n"
                    + "       System.debug(output);\n"
                    + "    }\n"
                    + "}",
            "public class UTIL_Describe {\n"
                    + "    private static Map<String, Schema.DescribeSObjectResult> objectToDescribeResult = new Map<String, Schema.DescribeSObjectResult>();\n"
                    + "    public static boolean getIsDeletable() {\n"
                    + "        boolean deletable = getSObjectDescribe('Account').isDeletable();\n"
                    + "        return deletable;\n"
                    + "    }\n"
                    + "\n"
                    + "    public static Schema.DescribeSObjectResult getSObjectDescribe(String objectName) {\n"
                    + "       if (!objectToDescribeResult.contains(objectName)) {\n"
                    + "           objectToDescribeResult.put(objectName, SObjectType.Account);\n" // Hard coding to keep the code simple
                    + "       }\n"
                    + "        return objectToDescribeResult.get(objectName);\n"
                    + "    }\n"
                    + "}"
        };

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.walkPaths(g, sourceCode);
        MatcherAssert.assertThat(results, hasSize(equalTo(1)));

        TestRunner.Result<SystemDebugAccumulator> systemDebugAccumulatorResult = results.get(0);
        ApexBooleanValue boolValue = systemDebugAccumulatorResult.getVisitor().getSingletonResult();

        MatcherAssert.assertThat(boolValue.isIndeterminant(), equalTo(true));
    }

    private void setPathRegistryLimit(int limit) {
        SfgeConfigTestProvider.set(
                new TestSfgeConfig() {
                    @Override
                    public int getPathExpansionLimit() {
                        return 6;
                    }
                });
    }
}
