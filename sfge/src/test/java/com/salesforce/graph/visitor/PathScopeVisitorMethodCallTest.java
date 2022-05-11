package com.salesforce.graph.visitor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PathScopeVisitorMethodCallTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /** Declaration and method call on same line */
    @Test
    public void testLiteralDeclarationWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = getValue();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public String getValue() {\n"
                        + "        return 'HelloFoo';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    /** Declaration and assignment on different calls */
    @Test
    public void testLiteralAssignmentWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s;\n"
                        + "        s = getValue();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public String getValue() {\n"
                        + "        return 'HelloFoo';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    @Test
    public void testLiteralAssignmentMethodInOtherClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "        String s = MyOtherClass.getValue();\n"
                    + "        System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public static String getValue() {\n"
                    + "        return 'HelloFoo';\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    @Test
    public void testLiteralAssignmentChainedMethodInOtherClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "        String s = MyOtherClass1.getValue();\n"
                    + "        System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass1 {\n"
                    + "    public static String getValue() {\n"
                    + "        return MyOtherClass2.getValue();\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass2 {\n"
                    + "    public static String getValue() {\n"
                    + "        return 'HelloFoo';\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    /** Called method returns a variable that is only in scope for the method */
    @Test
    public void testVariableAssignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        String s = getValue();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public String getValue() {\n"
                        + "        String a = 'HelloFoo';\n"
                        + "        return a;\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    /** Called method returns a chain of variables that are only defined in scope */
    @Test
    public void testVariableAssignmentChainedOtherClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "        String s = MyOtherClass1.getValue();\n"
                    + "        System.debug(s);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass1 {\n"
                    + "    public static String getValue() {\n"
                    + "        String a = MyOtherClass2.getValue();\n"
                    + "        return a;\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass2 {\n"
                    + "    public static String getValue() {\n"
                    + "        String b = 'HelloFoo';\n"
                    + "        return b;\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    /** Method call returns an SObject */
    @Test
    public void testObjectPropertiesDeclarationWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        SObject obj = getValue();\n"
                        + "        System.debug(obj);\n"
                        + "    }\n"
                        + "    public SObject getValue() {\n"
                        + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "       obj.put('Name', 'Acme Inc.');\n"
                        + "       return obj;\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                apexSingleValue.getTypeVertex().get().getCanonicalType(),
                equalTo(ApexStandardLibraryUtil.Type.S_OBJECT));
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                apexSingleValue.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testObjectPropertiesDeclarationWithMethodCallOtherClass() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public void doSomething() {\n"
                    + "        SObject obj = MyOtherClass.getValue();\n"
                    + "        System.debug(obj);\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public SObject getValue() {\n"
                    + "       SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                    + "       obj.put('Name', 'Acme Inc.');\n"
                    + "       return obj;\n"
                    + "    }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(
                apexSingleValue.getTypeVertex().get().getCanonicalType(),
                equalTo(ApexStandardLibraryUtil.Type.S_OBJECT));
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                apexSingleValue.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testAppendObjectPropertiesDeclarationWithMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "        appendFields(obj);\n"
                        + "        System.debug(obj);\n"
                        + "    }\n"
                        + "    public static void appendFields(SObject obj1) {\n"
                        + "       obj1.put('Name', 'Acme Inc.');\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                apexSingleValue.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testAppendObjectPropertiesDeclarationWithChainedMethodCall() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public void doSomething() {\n"
                        + "        SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();\n"
                        + "        appendFields1(obj);\n"
                        + "        System.debug(obj);\n"
                        + "    }\n"
                        + "    public appendFields1(SObject obj1) {\n"
                        + "       appendFields2(obj1);\n"
                        + "    }\n"
                        + "    public appendFields2(SObject obj2) {\n"
                        + "       obj2.put('Name', 'Acme Inc.');\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue apexSingleValue = visitor.getSingletonResult();
        SObjectType sObjectType = (SObjectType) apexSingleValue.getReturnedFrom().get();
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(sObjectType.getType()), equalTo("Account"));

        MatcherAssert.assertThat(
                apexSingleValue.getApexValueProperties().keySet(), hasSize(equalTo(1)));
        Map.Entry<ApexValue<?>, ApexValue<?>> property =
                apexSingleValue.getApexValueProperties().entrySet().stream().findFirst().get();
        MatcherAssert.assertThat(TestUtil.apexValueToString(property.getKey()), equalTo("Name"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(property.getValue()), equalTo("Acme Inc."));
    }

    @Test
    public void testBinaryLiteralAndMethodExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = 'HelloFoo1' + getAString();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public static String getAString() {\n"
                        + "        return 'HelloFoo2';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testBinaryLiteralAndChainedMethodExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = 'HelloFoo1' + getAString();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public static String getAString() {\n"
                        + "        return getAnotherString();\n"
                        + "    }\n"
                        + "    public static String getAnotherString() {\n"
                        + "        return 'HelloFoo2';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testMethodReturnsBinaryLiteralExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = getAString();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public static String getAString() {\n"
                        + "        return 'HelloFoo1' + 'HelloFoo2';\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testMethodReturnsBinaryVariableExpression() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "        String s = getAString();\n"
                        + "        System.debug(s);\n"
                        + "    }\n"
                        + "    public static String getAString() {\n"
                        + "        String a = 'HelloFoo1';\n"
                        + "        String b = 'HelloFoo2';\n"
                        + "        return a + b;\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo1HelloFoo2"));
    }

    @Test
    public void testReturnClassInstance() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       Singleton s = Singleton.getInstance();\n"
                    + "       s.logSomething('HelloFoo');\n"
                    + "    }\n"
                    + "}\n",
            "public class Singleton {\n"
                    + "    public static Singleton getInstance() {\n"
                    + "       return new Singleton();\n"
                    + "    }\n"
                    + "    public void logSomething(String s) {\n"
                    + "       System.debug(s);\n"
                    + "    }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("HelloFoo"));
    }

    @Test
    public void testCatchThrowsException() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething(List<Contact> contacts) {\n"
                    + "        contacts = null;\n"
                    + "        MyOtherClass.getQuery();\n"
                    + "    }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "    public static void getQuery() {\n"
                    + "        methodWithException();\n"
                    + "    	 System.debug('Hello');\n"
                    + "    }\n"
                    + "    public static String methodWithException() {\n"
                    + "        try {\n"
                    + "           return null;\n"
                    + "        } catch (Exception ex) {\n"
                    + "           throw new AuraHandledException('');\n"
                    + "        }\n"
                    + "    }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
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
                    + "       System.debug(s);\n"
                    + "   }\n"
                    + "}",
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue("Hello"));
    }

    /** Verify that a new object created from an Apex property is correctly resolved */
    @ValueSource(strings = {"theObject", "this.theObject"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSyntheticGetterObject(String variableReference) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String theObject {\n"
                    + "       get { return new MyObject__c(Name__c = 'Acme Inc.', Active__c = true); }\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       insert "
                    + variableReference
                    + ";\n"
                    + "   }\n"
                    + "}"
        };

        List<ApexSingleValue> values = new ArrayList<>();
        DefaultNoOpPathVertexVisitor visitor =
                new DefaultNoOpPathVertexVisitor() {
                    @Override
                    public void afterVisit(
                            DmlInsertStatementVertex vertex, SymbolProvider symbols) {
                        VariableExpressionVertex variableExpression = vertex.getOnlyChild();
                        ApexSingleValue myObject =
                                (ApexSingleValue)
                                        ScopeUtil.resolveToApexValue(symbols, variableExpression)
                                                .get();
                        values.add(myObject);
                    }
                };

        TestRunner.get(g, sourceCode).withPathVertexVisitor(() -> visitor).walkPath();

        MatcherAssert.assertThat(values, hasSize(equalTo(1)));
        ApexSingleValue value = values.get(0);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
        MatcherAssert.assertThat(
                value.getTypeVertex().get().getCanonicalType(), equalTo("MyObject__c"));
    }

    /** Verify a chained soql query in an Apex Property */
    @Test
    public void testChainedSoqlMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public Boolean isEnabled {\n"
                    + "       get { return ![SELECT Id FROM SomeSetting__c WHERE Active__c = true].isEmpty(); }\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + "       System.debug(isEnabled);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexBooleanValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
    }

    /**
     * This is a special case that has the potential to cause recursion or have scoping issues. The
     * class {@link VariableExpressionVertex.SelfReferentialInstanceProperty} is used to prevent
     * recursion.
     */
    @CsvSource({
        "set;, get { return theObject; }, theObject, Hello",
        "set;, get { return theObject; }, this.theObject, Hello",
        "set;, get;, theObject, Hello",
        "set;, get;, this.theObject, Hello",
        "set { theObject = value; }, get;, theObject, Hello",
        "set { theObject = value; }, get;, this.theObject, Hello",
        "set { theObject = value; }, get { return theObject; }, theObject, Hello",
        "set { theObject = value; }, get { return theObject; }, this.theObject, Hello",
        "set { theObject = value + 'There'; }, get;, theObject, HelloThere",
        "set { theObject = value + 'There'; }, get;, this.theObject, HelloThere",
        "set { theObject = value + 'There'; }, get { return theObject; }, theObject, HelloThere",
        "set { theObject = value + 'There'; }, get { return theObject; }, this.theObject, HelloThere",
        "set { theObject = value; }, get { return theObject + 'There'; }, theObject, HelloThere",
        "set { theObject = value; }, get { return theObject + 'There'; }, this.theObject, HelloThere"
    })
    @ParameterizedTest(
            name =
                    "{displayName}: setterBlock=({0}):getterBlock=({1}):getterBlock=({2}):expectedValue=({3})")
    public void testSyntheticSelfReferencedProperty(
            String setterBlock,
            String getterBlock,
            String variableReference,
            String expectedValue) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public String theObject {\n"
                    + "       "
                    + setterBlock
                    + "\n"
                    + "       "
                    + getterBlock
                    + "\n"
                    + "   }\n"
                    + "   public void doSomething() {\n"
                    + variableReference
                    + " = 'Hello';\n"
                    + "       System.debug("
                    + variableReference
                    + ");\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValue(expectedValue));
    }

    @Test
    @Disabled // This causes a stack overflow because the objects are both null. The example that is
    // failing from the
    // repo has a builder that should set one of these values, but the value is indeterminant,
    // causing the overflow
    public void testSelfReferentialRecursive() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public Set<Id> theIds {\n"
                    + "   	get {\n"
                    + "   		if (theIds == null && theObjects != null) {\n"
                    + "   			theIds = (new Map<Id, MyObject__c>(theObjects)).keySet();\n"
                    + "   		}\n"
                    + "   		return theIds;\n"
                    + "   	}\n"
                    + "  	}\n"
                    + "   public List<MyObject__c> theObjects {\n"
                    + "   	get {\n"
                    + "   		if (theObjects == null && theIds != null) {\n"
                    + "   			theObjects = [SELECT Id, Name FROM MyObject__c WHERE Id IN :theIds];\n"
                    + "   		}\n"
                    + "   		return theObjects;\n"
                    + "   	}\n"
                    + "  	}\n"
                    + "   public void doSomething() {\n"
                    + "   	System.debug(theIds);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
    }
}
