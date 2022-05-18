package com.salesforce.graph.symbols;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApexValueIndeterminantTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testJsonDeserializeValueShouldBeDeterminant() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething(String payLoad) {\n"
                    + "		MyFormat myf = (MyFormat) JSON.deserialize(payLoad, MyFormat.class);\n"
                    + "		System.debug(myf);\n"
                    + "		My_Data__c myd = myf.myData;\n"
                    + "		System.debug(myd);\n"
                    + "		System.debug(myd.My_Custom_Object__c);\n"
                    + "	}\n"
                    + "}\n",
            "public class MyFormat {\n" + "	public My_Data__c myData{get; set;}\n" + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        ApexClassInstanceValue deserializedValue = (ApexClassInstanceValue) allResults.get(0).get();
        assertThat(deserializedValue.isIndeterminant(), equalTo(false));

        ApexSingleValue propertyValue = (ApexSingleValue) allResults.get(1).get();
        assertThat(propertyValue.isIndeterminant(), equalTo(true));
        assertThat(propertyValue.getTypeVertex().get().getCanonicalType(), equalTo("My_Data__c"));

        ApexSingleValue secondLevelPropertyValue = (ApexSingleValue) allResults.get(2).get();
        assertThat(secondLevelPropertyValue.isIndeterminant(), equalTo(true));
        assertThat(
                secondLevelPropertyValue.getTypeVertex().get().getCanonicalType(),
                equalTo("My_Custom_Object__c"));
    }

    @Test // TODO: parameterize
    public void testJsonDeserializeValueShouldBeDeterminant_Assignment() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething(String payLoad) {\n"
                    + "		MyFormat myf = (MyFormat) JSON.deserializeStrict(payLoad, MyFormat.class);\n"
                    + "		System.debug(myf);\n"
                    + "		My_Data__c myd;\n"
                    + "		myd = myf.myData;\n"
                    + "		System.debug(myd);\n"
                    + "		System.debug(myd.My_Custom_Object__c);\n"
                    + "	}\n"
                    + "}\n",
            "public class MyFormat {\n" + "	public My_Data__c myData{get; set;}\n" + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        ApexClassInstanceValue deserializedValue = (ApexClassInstanceValue) allResults.get(0).get();
        assertThat(deserializedValue.isDeterminant(), equalTo(true));

        ApexSingleValue propertyValue = (ApexSingleValue) allResults.get(1).get();
        assertThat(propertyValue.isIndeterminant(), equalTo(true));
        assertThat(propertyValue.getTypeVertex().get().getCanonicalType(), equalTo("My_Data__c"));

        ApexSingleValue secondLevelPropertyValue = (ApexSingleValue) allResults.get(2).get();
        assertThat(secondLevelPropertyValue.isIndeterminant(), equalTo(true));
        assertThat(
                secondLevelPropertyValue.getTypeVertex().get().getCanonicalType(),
                equalTo("My_Custom_Object__c"));
    }

    @Test
    public void testJsonDeserializeValueFromStdObject() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	void doSomething(String payLoad) {\n"
                    + "		Account acc = (Account) JSON.deserialize(payLoad, Account.class);\n"
                    + "		System.debug(acc);\n"
                    + "		My_Custom_Field__c mycf = acc.My_Custom_Field__c;\n"
                    + "		System.debug(mycf);\n"
                    + "		System.debug(mycf.My_Custom_Object__c);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        final List<Optional<ApexValue<?>>> allResults = visitor.getAllResults();

        ApexSingleValue deserializedValue = (ApexSingleValue) allResults.get(0).get();
        assertThat(deserializedValue.isIndeterminant(), equalTo(false));

        ApexSingleValue propertyValue = (ApexSingleValue) allResults.get(1).get();
        assertThat(propertyValue.isIndeterminant(), equalTo(true));
        assertThat(
                propertyValue.getTypeVertex().get().getCanonicalType(),
                equalTo("My_Custom_Field__c"));

        ApexSingleValue secondLevelPropertyValue = (ApexSingleValue) allResults.get(2).get();
        assertThat(secondLevelPropertyValue.isIndeterminant(), equalTo(true));
        assertThat(
                secondLevelPropertyValue.getTypeVertex().get().getCanonicalType(),
                equalTo("My_Custom_Object__c"));
    }

    @Test
    public void testDataObjectWithNewKeyword() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		Account acc = new Account();\n"
                        + "		System.debug(acc.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(true));
        assertThat(singleValue.isNull(), equalTo(true));
    }

    @Test
    public void testCustomSettingWithNewKeyword() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		Custom_Setting__c custSettings = Custom_Setting__c.getOrgDefaults();\n"
                        + "		System.debug(custSettings.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isIndeterminant(), equalTo(true));
    }

    @Test
    @Disabled // TODO: Handle unknown property on unknown type
    public void testUnknownTypeWithNewKeyword() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		UnknownClass uc = new UnknownClass();\n"
                        + "		System.debug(uc.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(true));
        assertThat(singleValue.isNull(), equalTo(true));
    }

    @Test
    @Disabled // TODO: Handle undefined properties on NewObjectExpression
    public void testObjectWithNewKeyword_Assignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething() {\n"
                        + "		Account acc = new Account();\n"
                        + "		String name = acc.Name;\n"
                        + "		System.debug(name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue stringValue = visitor.getSingletonResult();

        assertThat(stringValue.isDeterminant(), equalTo(true));
        assertThat(stringValue.isNull(), equalTo(true));
    }

    @Test
    public void testObjectWithNewKeyword_ClassMember() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	Account acc;\n"
                        + "	public MyClass() {\n"
                        + "		acc = new Account();\n"
                        + "	}\n"
                        + "	public void doSomething() {\n"
                        + "		System.debug(acc.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(true));
        assertThat(singleValue.isNull(), equalTo(true));
    }

    @Test
    public void testPropertyOnIndeterminantParameter() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething(Account acc) {\n"
                        + "		System.debug(acc.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(false));
        assertThat(singleValue.isNull(), equalTo(false));
    }

    @Test
    public void testPropertyOnIndeterminantParameter_Assignment() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public void doSomething(Account acc) {\n"
                        + "		String name = acc.Name;\n"
                        + "		System.debug(name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue stringValue = visitor.getSingletonResult();

        assertThat(stringValue.isDeterminant(), equalTo(false));
        assertThat(stringValue.isNull(), equalTo(false));
    }

    @Test
    public void testNullCheckOnSoqlResults_fieldInQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		Account acc = [SELECT Id, Name FROM Account LIMIT 1];\n"
                        + "		System.debug(acc.Name);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(false));
        assertThat(singleValue.isNull(), equalTo(false));
    }

    @Test
    @Disabled // TODO: Handle scenario where a field that was not included in a query is invoked
    public void testNullCheckOnSoqlResults_fieldNotInQuery() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		Account acc = [SELECT Id, Name FROM Account LIMIT 1];\n"
                        + "		System.debug(acc.Phone);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(true));
        assertThat(singleValue.isNull(), equalTo(true));
    }

    @Test
    public void testIndeterminantForLoopValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething(List<Account> accList) {\n"
                        + "		for (Account acc: accList) {\n"
                        + "			System.debug(acc.Name);\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(false));
        assertThat(singleValue.isNull(), equalTo(false));
    }

    @Test
    public void testDeterminantForLoopWithKnownValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		List<Account> accList = new Account[]{new Account(Name='Acme Inc')};\n"
                        + "		for (Account acc: accList) {\n"
                        + "			System.debug(acc.Name);\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexStringValue stringValue = visitor.getSingletonResult();

        assertThat(stringValue.isDeterminant(), equalTo(true));
        assertThat(stringValue.isNull(), equalTo(false));
    }

    @Test
    public void testDeterminantForLoopWithUnknownValue() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		List<Account> accList = new Account[]{new Account(Name='Acme Inc')};\n"
                        + "		for (Account acc: accList) {\n"
                        + "			System.debug(acc.Phone);\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexSingleValue singleValue = visitor.getSingletonResult();

        assertThat(singleValue.isDeterminant(), equalTo(true));
        assertThat(singleValue.isNull(), equalTo(true));
    }
}
