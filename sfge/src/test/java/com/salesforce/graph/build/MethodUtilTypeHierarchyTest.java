package com.salesforce.graph.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class MethodUtilTypeHierarchyTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    private static Stream<Arguments> provideParametersToGeneralization() {
        return Stream.of(
                Arguments.of(
                        "Standard Object to SObject",
                        "Account",
                        "new Account(Name='Acme Inc')",
                        "SObject",
                        true),
                Arguments.of(
                        "Custom Object to SObject",
                        "Custom_Object__c",
                        "new Custom_Object__c()",
                        "SObject",
                        true),
                Arguments.of("SObject to SObject", "SObject", "new SObject()", "SObject", true),
                Arguments.of("SObject to Object", "SObject", "new SObject()", "Object", true),
                Arguments.of(
                        "Not allowed: SObject to Standard Object",
                        "SObject",
                        "new SObject()",
                        "Account",
                        false),
                Arguments.of(
                        "Not allowed: SObject to Custom Object",
                        "SObject",
                        "new SObject()",
                        "Custom_Object__c",
                        false));
    }

    @MethodSource("provideParametersToGeneralization")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverloadWithSObject(
            String testName,
            String invocableType,
            String invocableInitializer,
            String parameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		"
                        + invocableType
                        + " a = "
                        + invocableInitializer
                        + ";\n"
                        + "       debug(a);\n"
                        + "    }\n"
                        + "    public static void debug("
                        + parameterType
                        + " o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    @MethodSource("provideParametersToGeneralization")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverloadWithSObject_multipleParameters(
            String testName,
            String invocableType,
            String invocableInitializer,
            String parameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		"
                        + invocableType
                        + " a = "
                        + invocableInitializer
                        + ";\n"
                        + "       debug(1, a);\n"
                        + "    }\n"
                        + "    public static void debug(Integer i, "
                        + parameterType
                        + " o) {\n"
                        + // TODO: how do we rank multiple parameters?
                        "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    @MethodSource("provideParametersToGeneralization")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverloadWithSObject_passInitializer(
            String testName,
            String invocableType,
            String invocableInitializer,
            String parameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       debug("
                        + invocableInitializer
                        + ");\n"
                        + "    }\n"
                        + "    public static void debug("
                        + parameterType
                        + " o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    @MethodSource("provideParametersToGeneralization")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverloadWithSObject_fieldObject(
            String testName,
            String invocableType,
            String invocableInitializer,
            String parameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + invocableType
                        + " a = "
                        + invocableInitializer
                        + ";\n"
                        + "    public void doSomething() {\n"
                        + "       debug(a);\n"
                        + "    }\n"
                        + "    public static void debug("
                        + parameterType
                        + " o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    private static Stream<Arguments> provideParametersForMultipleOptions() {
        return Stream.of(
                Arguments.of(
                        "Choose exact match over generalization",
                        "Account",
                        "new Account(Name='Acme Inc')",
                        "Account",
                        "SObject",
                        true),
                Arguments.of(
                        "Choose generalization over Not allowed match",
                        "Account",
                        "new Account(Name='Acme Inc')",
                        "SObject",
                        "Contact",
                        true),
                Arguments.of(
                        "Choose exact match of declaration type, when declaration type matches object value type",
                        "SObject",
                        "new SObject()",
                        "SObject",
                        "Account",
                        true),
                Arguments.of(
                        "Choose exact match of declaration type, when declaration type does not match object value type",
                        "SObject",
                        "new Account(Name = 'Acme Inc')",
                        "SObject",
                        "Account",
                        true));
    }

    @MethodSource("provideParametersForMultipleOptions")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testOverloadWithMultipleOptions(
            String testName,
            String invocableType,
            String invocableInitializer,
            String suitableParameterType,
            String unsuitableParameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		"
                        + invocableType
                        + " a = "
                        + invocableInitializer
                        + ";\n"
                        + "       debug(a);\n"
                        + "    }\n"
                        + "    public static void debug("
                        + suitableParameterType
                        + " o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "    public static void debug("
                        + unsuitableParameterType
                        + " o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        if (expectResult) {
            final List<Optional<ApexValue<?>>> results = visitor.getResults("MyClass", 7);
            assertThat(results, hasSize(1));
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    private static Stream<Arguments> provideParametersForClassHierarchy() {
        return Stream.of(
                Arguments.of(
                        "Choose exact match", "ChildClass", "new ChildClass()", "ChildClass", true),
                Arguments.of(
                        "Choose generalization",
                        "ChildClass",
                        "new ChildClass()",
                        "ParentClass",
                        true),
                Arguments.of(
                        "Don't choose specialization",
                        "ParentClass",
                        "new ParentClass()",
                        "ChildClass",
                        false),
                Arguments.of(
                        "Don't choose based on initialized value",
                        "ParentClass",
                        "new ChildClass()",
                        "ChildClass",
                        false),
                Arguments.of(
                        "Choose based on declaration type",
                        "ParentClass",
                        "new ChildClass()",
                        "ParentClass",
                        true),
                Arguments.of(
                        "Choose Object when only option",
                        "ChildClass",
                        "new ChildClass()",
                        "Object",
                        true));
    }

    @MethodSource("provideParametersForClassHierarchy")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testClassInheritanceInMethodCall(
            String testName,
            String invocableType,
            String invocableInitialization,
            String methodParameterType,
            boolean expectResult) {
        String[] sourceCode = {
            "virtual public class ParentClass {}\n",
            "public class ChildClass extends ParentClass {}\n",
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "		"
                    + invocableType
                    + " a = "
                    + invocableInitialization
                    + ";\n"
                    + "       debug(a);\n"
                    + "    }\n"
                    + "    public static void debug("
                    + methodParameterType
                    + " p) {\n"
                    + "       System.debug(p);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    @MethodSource("provideParametersForClassHierarchy")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testClassMemberInheritanceInMethodCall(
            String testName,
            String invocableType,
            String invocableInitialization,
            String methodParameterType,
            boolean expectResult) {
        String[] sourceCode = {
            "virtual public class ParentClass {}\n",
            "public class ChildClass extends ParentClass {}\n",
            "public class MyClass {\n"
                    + "	"
                    + invocableType
                    + " a = "
                    + invocableInitialization
                    + ";\n"
                    + "    public void doSomething() {\n"
                    + "       debug(a);\n"
                    + "    }\n"
                    + "    public static void debug("
                    + methodParameterType
                    + " p) {\n"
                    + "       System.debug(p);\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        if (expectResult) {
            assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
        } else {
            assertThat(visitor.getAllResults(), empty());
        }
    }

    @Test
    public void testOverloadWithSObject_soql() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       debug([Select Id, Name from Account limit 1]);\n"
                        + "    }\n"
                        + "    public static void debug(SObject o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
    }

    @Test
    public void testNullValueParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "       debug(null);\n"
                        + "    }\n"
                        + "    public static void debug(String o) {\n"
                        + "       System.debug(o);\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
    }

    @Test
    public void testSoqlToListThroughMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "        List<Account> accounts = getAccounts();\n"
                        + "		 String str = serialize(accounts);\n"
                        + "        System.debug(str);\n"
                        + "    }\n"
                        + "	public List<Account> getAccounts() {\n"
                        + "        return [SELECT Name, Id FROM Account];\n"
                        + "   }\n"
                        + "	public String serialize(List<Account> accounts) {\n"
                        + "		return JSON.serialize(accounts);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexStringValue resultValue = visitor.getSingletonResult();
        assertThat(resultValue, Matchers.notNullValue());
    }

    @Test
    public void testIdToString() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		Id userId = UserInfo.getUserId();\n"
                        + "		myMethod(userId);\n"
                        + "	}\n"
                        + "	void myMethod(String userId) {\n"
                        + "		System.debug(userId);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexValue<?> resultValue = visitor.getOptionalSingletonResult().orElse(null);
        assertThat(resultValue.isIndeterminant(), equalTo(true));
    }

    @Test
    public void testStringToId() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		String userId = UserInfo.getUserId();\n"
                        + "		myMethod(userId);\n"
                        + "	}\n"
                        + "	void myMethod(Id userId) {\n"
                        + "		System.debug(userId);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexValue<?> resultValue = visitor.getOptionalSingletonResult().orElse(null);
        assertThat(resultValue.isIndeterminant(), equalTo(true));
    }

    @CsvSource({
        //		"Id,UserInfo.getUserId();,Id chosen", // TODO: even though UserInfo.getUserId() returns
        // an ApexStringValue, we should use the declaration type of Id
        "String,UserInfo.getUserId();,String chosen",
        "Id,'005Zz0000000012';,Id chosen",
        "String,'005Zz0000000012';,String chosen"
    })
    @ParameterizedTest
    public void testIdStringPriority(
            String definitionType, String valueCreation, String expectedValue) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		"
                        + definitionType
                        + " userId = "
                        + valueCreation
                        + "\n"
                        + "		myMethod(userId);\n"
                        + "	}\n"
                        + "	void myMethod(Id userId) {\n"
                        + "		System.debug('Id chosen');\n"
                        + "	}\n"
                        + "	void myMethod(String userId) {\n"
                        + "		System.debug('String chosen');\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexStringValue resultValue = visitor.getSingletonResult();
        assertThat(resultValue.getValue().get(), equalTo(expectedValue));
    }
}
