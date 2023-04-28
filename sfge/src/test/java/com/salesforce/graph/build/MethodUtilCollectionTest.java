package com.salesforce.graph.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
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
import org.junit.jupiter.params.provider.MethodSource;

public class MethodUtilCollectionTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    private static Stream<Arguments> provideParametersToGeneralization() {
        return Stream.of(
                Arguments.of(
                        "List of Standard Object to list of SObject",
                        "List<Account>",
                        "new List<Account>{new Account(Name='Acme Inc')}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "List of Standard Object from Soql to list of SObject",
                        "List<Account>",
                        "[SELECT Id, Name FROM Account]",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "List of Custom Object to list of SObject",
                        "List<Custom_Object__c>",
                        "new List<Custom_Object__c>{new Custom_Object__c()}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "List of SObject to list of SObject",
                        "List<SObject>",
                        "new List<SObject>{new SObject()}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "List of SObject to list of Object",
                        "List<SObject>",
                        "new List<SObject>{new SObject()}",
                        "List<Object>",
                        true),
                Arguments.of(
                        "Not allowed: List of SObject to list of Standard Object",
                        "List<SObject>",
                        "new List<SObject>{new SObject()}",
                        "List<Account>",
                        false),
                Arguments.of(
                        "Not allowed: List of SObject to list of Custom Object",
                        "List<SObject>",
                        "new List<SObject>{new SObject()}",
                        "List<Custom_Object__c>",
                        false),
                Arguments.of(
                        "Not allowed: List of SObject to list of String",
                        "List<SObject>",
                        "new List<SObject>{new SObject()}",
                        "List<String>",
                        false),
                Arguments.of(
                        "Array-style list of Standard Object to list of SObject",
                        "List<Account>",
                        "new Account[]{new Account(Name='Acme Inc')}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "Array-style list of Custom Object to list of SObject",
                        "List<Custom_Object__c>",
                        "new Custom_Object__c[]{new Custom_Object__c()}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "Array-style list of SObject to list of SObject",
                        "List<SObject>",
                        "new SObject[]{new SObject()}",
                        "List<SObject>",
                        true),
                Arguments.of(
                        "Array-style list of SObject to list of Object",
                        "List<SObject>",
                        "new SObject[]{new SObject()}",
                        "List<Object>",
                        true),
                Arguments.of(
                        "Not allowed: Array-style list of SObject to list of Standard Object",
                        "List<SObject>",
                        "new SObject[]{new SObject()}",
                        "List<Account>",
                        false),
                Arguments.of(
                        "Not allowed: Array-style list of SObject to list of Custom Object",
                        "List<SObject>",
                        "new SObject[]{new SObject()}",
                        "List<Custom_Object__c>",
                        false),
                Arguments.of(
                        "Map of Standard Object to Map of Standard Object",
                        "Map<String, Account>",
                        "new Map<String, Account>{'myKey' => new Account(Name='Acme Inc')}",
                        "Map<String, Account>",
                        true),
                Arguments.of(
                        "Not allowed: Map of Standard Object to Map of SObject",
                        "Map<String, Account>",
                        "new Map<String, Account>{'myKey' => new Account(Name='Acme Inc')}",
                        "Map<String, SObject>",
                        false),
                Arguments.of(
                        "Not allowed: Map of Standard Object to Map of Object",
                        "Map<String, Account>",
                        "new Map<String, Account>{'myKey' => new Account(Name='Acme Inc')}",
                        "Map<String, Object>",
                        false),
                Arguments.of(
                        "Map of Custom Object to Map of Custom Object",
                        "Map<String, Custom_Object__c>",
                        "new Map<String, Custom_Object__c>{'myKey' => new Custom_Object__c()}",
                        "Map<String, Custom_Object__c>",
                        true),
                Arguments.of(
                        "Not allowed: Map of Custom Object to Map of SObject",
                        "Map<String, Custom_Object__c>",
                        "new Map<String, Custom_Object__c>{'myKey' => new Custom_Object__c()}",
                        "Map<String, SObject>",
                        false),
                Arguments.of(
                        "Not allowed: Map of Custom Object to Map of Object",
                        "Map<String, Custom_Object__c>",
                        "new Map<String, Custom_Object__c>{'myKey' => new Custom_Object__c()}",
                        "Map<String, Object>",
                        false),
                Arguments.of(
                        "Map of SObject to Map of SObject",
                        "Map<String, SObject>",
                        "new Map<String, SObject>{'myKey' => new SObject()}",
                        "Map<String, SObject>",
                        true),
                Arguments.of(
                        "Not allowed: Map of SObject to Map of String",
                        "Map<String, SObject>",
                        "new Map<String, SObject>{'myKey' => new SObject()}",
                        "Map<String, String>",
                        false),
                Arguments.of(
                        "Set of Standard Object to Set of Standard Object",
                        "Set<Account>",
                        "new Set<Account> {new Account(Name='Acme Inc')}",
                        "Set<Account>",
                        true),
                Arguments.of(
                        "Not allowed: Set of Standard Object to Set of SObject",
                        "Set<Account>",
                        "new Set<Account> {new Account(Name='Acme Inc')}",
                        "Set<SObject>",
                        false),
                Arguments.of(
                        "Not allowed: Set of Standard Object to Set of Object",
                        "Set<Account>",
                        "new Set<Account> {new Account(Name='Acme Inc')}",
                        "Set<Object>",
                        false),
                Arguments.of(
                        "Set of Custom Object to Set of Custom Object",
                        "Set<Custom_Object__c>",
                        "new Set<Custom_Object__c>{new Custom_Object__c()}",
                        "Set<Custom_Object__c>",
                        true),
                Arguments.of(
                        "Not allowed: Set of Custom Object to Set of SObject",
                        "Set<Custom_Object__c>",
                        "new Set<Custom_Object__c>{new Custom_Object__c()}",
                        "Set<SObject>",
                        false),
                Arguments.of(
                        "Not allowed: Set of Custom Object to Set of Object",
                        "Set<Custom_Object__c>",
                        "new Set<Custom_Object__c>{new Custom_Object__c()}",
                        "Set<Object>",
                        false),
                Arguments.of(
                        "Set of SObject to Set of SObject",
                        "Set<SObject>",
                        "new Set<SObject>{new SObject()}",
                        "Set<SObject>",
                        true),
                Arguments.of(
                        "Not allowed: Set of SObject to Set of String",
                        "Set<SObject>",
                        "new Set<SObject>{new SObject()}",
                        "Set<String>",
                        false));
    }

    @MethodSource("provideParametersToGeneralization")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testCollectionOverloadWithSObject(
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
                        + "    public static void debug(List<String> s) {\n"
                        + // Alternative method call that should always fail
                        "       System.debug(s);\n"
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
    public void testOverloadWithSObject_classMember(
            String testName,
            String invocableType,
            String invocableInitializer,
            String parameterType,
            boolean expectResult) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	"
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

    private static Stream<Arguments> provideParametersForClassHierarchy() {
        return Stream.of(
                Arguments.of(
                        "List: Choose exact match",
                        "List<ChildClass>",
                        "new List<ChildClass>(); a.add(new ChildClass())",
                        "List<ChildClass>",
                        true),
                //			Arguments.of("List: Choose generalization", "List<ChildClass>", "new
                // List<ChildClass>(); a.add(new ChildClass())", "List<ParentClass>", true),
                Arguments.of(
                        "List: Don't choose specialization",
                        "List<ParentClass>",
                        "new List<ParentClass>(); a.add(new ParentClass())",
                        "List<ChildClass>",
                        false),
                Arguments.of(
                        "List: Don't choose based on initialized value",
                        "List<ParentClass>",
                        "new List<ChildClass>(); a.add(new ChildClass())",
                        "List<ChildClass>",
                        false),
                Arguments.of(
                        "List: Choose based on declaration type",
                        "List<ParentClass>",
                        "new List<ChildClass>(); a.add(new ChildClass())",
                        "List<ParentClass>",
                        true),
                Arguments.of(
                        "List: Choose Object when only option",
                        "List<ChildClass>",
                        "new List<ChildClass>(); a.add(new ChildClass())",
                        "List<Object>",
                        true),
                Arguments.of(
                        "Array-style list: Choose exact match",
                        "List<ChildClass>",
                        "new ChildClass[] {new ChildClass()}",
                        "List<ChildClass>",
                        true),
                //			Arguments.of("Array-style list: Choose generalization", "List<ChildClass>",
                // "new ChildClass[] {new ChildClass()}", "List<ParentClass>", true),
                Arguments.of(
                        "Array-style list: Don't choose specialization",
                        "List<ParentClass>",
                        "new ParentClass[] {new ParentClass()}",
                        "List<ChildClass>",
                        false),
                Arguments.of(
                        "Array-style list: Don't choose based on initialized value",
                        "List<ParentClass>",
                        "new ChildClass[] {new ChildClass()}",
                        "List<ChildClass>",
                        false),
                Arguments.of(
                        "Array-style list: Choose based on declaration type",
                        "List<ParentClass>",
                        "new ChildClass[] {new ChildClass()}",
                        "List<ParentClass>",
                        true),
                Arguments.of(
                        "Array-style list: Choose Object when only option",
                        "List<ChildClass>",
                        "new ChildClass[] {new ChildClass()}",
                        "List<Object>",
                        true),
                Arguments.of(
                        "Map: Choose exact match",
                        "Map<String, ChildClass>",
                        "new Map<String, ChildClass>(); a.put('newKey', new ChildClass())",
                        "Map<String, ChildClass>",
                        true),
                //			Arguments.of("Map: Not supported: Choose generalization", "Map<String,
                // ChildClass>", "new Map<String, ChildClass>(); a.put('newKey', new ChildClass())",
                // "Map<String, ParentClass>", true),
                Arguments.of(
                        "Map: Don't choose specialization",
                        "Map<String, ParentClass>",
                        "new Map<String, ParentClass>(); a.put('newKey', new ParentClass())",
                        "Map<String, ChildClass>",
                        false),
                Arguments.of(
                        "Map: Don't choose based on initialized value",
                        "Map<String, ParentClass>",
                        "new Map<String, ChildClass>(); a.put('newKey', new ChildClass())",
                        "Map<String, ChildClass>",
                        false),
                Arguments.of(
                        "Map: Choose based on declaration type",
                        "Map<String, ParentClass>",
                        "new Map<String, ChildClass>(); a.put('newKey', new ChildClass())",
                        "Map<String, ParentClass>",
                        true),
                //			Arguments.of("Map: Not supported: Choose Object when only option", "Map<String,
                // ChildClass>", "new Map<String, ChildClass>(); a.put('newKey', new ChildClass())",
                // "Map<String, Object>", true)

                Arguments.of(
                        "Set: Choose exact match",
                        "Set<ChildClass>",
                        "new Set<ChildClass>(); a.add(new ChildClass())",
                        "Set<ChildClass>",
                        true),
                Arguments.of(
                        "Set: Don't choose generalization",
                        "Set<ChildClass>",
                        "new Set<ChildClass>(); a.add(new ChildClass())",
                        "Set<ParentClass>",
                        false),
                Arguments.of(
                        "Set: Don't choose specialization",
                        "Set<ParentClass>",
                        "new Set<ParentClass>(); a.add(new ParentClass())",
                        "Set<ChildClass>",
                        false),
                Arguments.of(
                        "Set: Don't choose based on initialized value",
                        "Set<ParentClass>",
                        "new Set<ChildClass>(); a.add(new ChildClass())",
                        "Set<ChildClass>",
                        false),
                Arguments.of(
                        "Set: Choose based on declaration type",
                        "Set<ParentClass>",
                        "new Set<ChildClass>(); a.add(new ChildClass())",
                        "Set<ParentClass>",
                        true),
                Arguments.of(
                        "Set: Don't choose Object when only option",
                        "Set<ChildClass>",
                        "new Set<ChildClass>(); a.add(new ChildClass())",
                        "Set<Object>",
                        false));
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
                    + "}\n"
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
    public void testListOfList() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		List<SObject> o = new List<SObject> {new Account()};\n"
                        + "		List<List<SObject>> s = new List<List<SObject>> {o};\n"
                        + "       debug(s);\n"
                        + "    }\n"
                        + "    public static void debug(List<List<SObject>> p) {\n"
                        + "       System.debug(p);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
    }

    @Test
    public void testMapWithMoreThanOneEntry() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "		Map<String, SObject> o = new Map<String, SObject> {'key1' => new SObject(), 'key2' => new SObject()};\n"
                        + "       debug(o);\n"
                        + "    }\n"
                        + "    public static void debug(Map<String, SObject> m) {\n"
                        + "       System.debug(m);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        assertThat(visitor.getSingletonResult(), Matchers.notNullValue());
    }

    @Test
    public void testForLoopOnArrayExpressionLookup() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void doSomething() {\n"
                        + "       String[] myList = new String[] {'hi'};\n"
                        + "       for (Integer i = 0; i < myList.size(); i++) {\n"
                        + "           System.debug(myList[i]);\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();
        final List<ApexValue<?>> items = forLoopValue.getForLoopValues();
        assertThat(items.size(), equalTo(1));
        final ApexStringValue stringValue = (ApexStringValue) items.get(0);
        assertThat(TestUtil.apexValueToString(stringValue), equalTo("hi"));
    }

    @Test
    public void testForLoopOnArrayExpressionLookup_classInstance() {
        String sourceCode[] = {
            "public class MyClass {\n"
                    + "   void doSomething() {\n"
                    + "       Bean[] beans = new Bean[] {new Bean()};\n"
                    + "       for (Integer i = 0; i < beans.size(); i++) {\n"
                    + "           System.debug(beans[i]);\n"
                    + "       }\n"
                    + "   }\n"
                    + "}\n",
            "public class Bean {}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final ApexForLoopValue forLoopValue = visitor.getSingletonResult();
        List<ApexValue<?>> items = forLoopValue.getForLoopValues();
        assertThat(items.size(), equalTo(1));
        final ApexClassInstanceValue classInstanceValue = (ApexClassInstanceValue) items.get(0);
        assertThat(classInstanceValue.getDefiningType().orElse(""), equalTo("Bean"));
    }
}
