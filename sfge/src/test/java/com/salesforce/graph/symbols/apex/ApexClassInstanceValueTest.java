package com.salesforce.graph.symbols.apex;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApexClassInstanceValueTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testThisCall() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + "   	MyOtherClass c = new MyOtherClass();\n"
                    + "   	System.debug(c.string1);\n"
                    + "   	System.debug(c.string2);\n"
                    + "   }\n"
                    + "}",
            "public class MyOtherClass {\n"
                    + "  	public String string1;\n"
                    + "  	public String string2;\n"
                    + "   public MyOtherClass() {\n"
                    + "   	this('Unknown1', 'Unknown2');\n"
                    + "   }\n"
                    + "   public MyOtherClass(String string1, String string2) {\n"
                    + "   	this.string1 = string1;\n"
                    + "   	this.string2 = string2;\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        MatcherAssert.assertThat(result, TestRunnerMatcher.hasValues("Unknown1", "Unknown2"));
    }

    @Test
    public void testFieldsAreNullByDefault() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "        @AuraEnabled public MyObject__c myObject {get; set;}\n"
                    + "   }\n"
                    + "   public static void doSomething() {\n"
                    + "   	InnerClass ic = new InnerClass();\n"
                    + "   	System.debug(ic);\n"
                    + "   	System.debug(ic.myObject);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexClassInstanceValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));

        ApexSingleValue myObject = visitor.getResult(1);
        MatcherAssert.assertThat(myObject.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(myObject.isNull(), equalTo(true));
    }

    @Test
    public void testUpdateFieldInMethod() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "        @AuraEnabled public MyObject__c myObject {get; set;}\n"
                    + "   }\n"
                    + "   public static void doSomething() {\n"
                    + "   	InnerClass ic = new InnerClass();\n"
                    + "   	updateSomething(ic);\n"
                    + "   	System.debug(ic.myObject);\n"
                    + "   }\n"
                    + "   public static void updateSomething(InnerClass ic) {\n"
                    + " 		MyObject__c obj = new MyObject__c();\n"
                    + "   	ic.myObject = obj;\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue myObject = visitor.getSingletonResult();
        MatcherAssert.assertThat(myObject.isDeterminant(), equalTo(true));
        MatcherAssert.assertThat(myObject.isNull(), equalTo(false));
    }

    @Test
    public void testUpdateFieldInMethodFromArray() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "        @AuraEnabled public MyObject__c myObject {get; set;}\n"
                    + "   }\n"
                    + "   public static void doSomething() {\n"
                    + "   	InnerClass ic = new InnerClass();\n"
                    + "   	updateSomething(ic);\n"
                    + "   	System.debug(ic.myObject);\n"
                    + "   }\n"
                    + "   public static void updateSomething(InnerClass ic) {\n"
                    + " 		List<MyObject__c> objs = [SELECT Id, Name FROM MyObject__c LIMIT 1];\n"
                    + "   	ic.myObject = objs[0];\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue myObject = visitor.getSingletonResult();
        MatcherAssert.assertThat(myObject.isDeterminant(), equalTo(false));
        MatcherAssert.assertThat(myObject.isNull(), equalTo(false));
    }

    @Test
    public void testJSONDeserializeFieldsAreIndeterminant() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public class InnerClass {\n"
                    + "        @AuraEnabled public MyObject__c myObject {get; set;}\n"
                    + "   }\n"
                    + "   public static void doSomething(String asJson) {\n"
                    + "   	InnerClass ic = (InnerClass)JSON.deserialize(asJson, InnerClass.class);\n"
                    + "   	System.debug(ic);\n"
                    + "   	System.debug(ic.myObject);\n"
                    + "   }\n"
                    + "}"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        ApexClassInstanceValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isDeterminant(), equalTo(true));

        ApexSingleValue myObject = visitor.getResult(1);
        MatcherAssert.assertThat(myObject.isDeterminant(), equalTo(false));
        MatcherAssert.assertThat(myObject.isNull(), equalTo(false));
    }

    // TODO: Test what happens if there is inline assignment and the class is deserialized, which
    // one wins, is it an error?

    @Test
    public void testMethodCallOnDeterminant() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething() {\n"
                    + "		Bean myBean = new Bean('hi');\n"
                    + "		System.debug(myBean);\n"
                    + "		System.debug(myBean.getValue());\n"
                    + "	}\n"
                    + "}\n",
            "public class Bean {\n"
                    + "private String value;\n"
                    + "public Bean(String val1) {\n"
                    + "	this.value = val1;\n"
                    + "}\n"
                    + "public String getValue() {\n"
                    + "	return this.value;\n"
                    + "}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue instanceValue = visitor.getResult(0);
        MatcherAssert.assertThat(instanceValue.getCanonicalType(), equalTo("Bean"));

        ApexStringValue methodCallValue = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(methodCallValue), equalTo("hi"));
    }

    @Test
    @Disabled // TODO: Indeterminant class value should be treated
    //  as an ApexClassInstanceValue instead of ApexSingleValue
    public void testMethodCallOnIndeterminantInstance() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething(Bean bean) {\n"
                    + "		System.debug(bean);\n"
                    + "		System.debug(bean.getValue());\n"
                    + "	}\n"
                    + "}\n",
            "public class Bean {\n"
                    + "private String value;\n"
                    + "public Bean(String val1) {\n"
                    + "	this.value = val1;\n"
                    + "}\n"
                    + "public String getValue() {\n"
                    + "	return this.value;\n"
                    + "}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.get(g, sourceCode).walkPath();
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexClassInstanceValue value = visitor.getResult(0);
        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));
        MatcherAssert.assertThat(value.getDeclaredType().get(), equalTo("Bean"));

        ApexStringValue methodCallValue = visitor.getResult(1);
        MatcherAssert.assertThat(methodCallValue.isIndeterminant(), equalTo(true));
    }
}
