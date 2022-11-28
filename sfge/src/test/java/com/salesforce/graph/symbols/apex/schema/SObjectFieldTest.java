package com.salesforce.graph.symbols.apex.schema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SObjectFieldTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @CsvSource({
            "Schema.Account.Fields.Name, Account, Name",
            "Account.Name, Account, Name",
            "Schema.MyObj__c.Fields.My_Field__c, MyObj__c, My_Field__c",
            "MyObj__c.My_Field__c, MyObj__c, My_Field__c"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testSObjectFieldFormat(String initializer, String sObjectTypeName, String fieldName) {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "       SObjectField sObjField = " + initializer + ";\n"
                        + "       System.debug(sObjField);\n"
                        + "       System.debug(sObjField.getDescribe());\n"
                        + "    }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        assertThat(visitor.getAllResults(), hasSize(equalTo(2)));

        // sObjectField
        SObjectField sObjectField = (SObjectField) visitor.getAllResults().get(0).get();
        assertThat(sObjectField.isIndeterminant(), equalTo(false));
        assertThat(TestUtil.apexValueToString(sObjectField.getFieldname()), equalTo(fieldName));

        // sObjField.getDescribe()
        DescribeFieldResult describeFieldResult =
                (DescribeFieldResult) visitor.getAllResults().get(1).get();
        assertThat(describeFieldResult.isIndeterminant(), equalTo(false));
        assertThat(
                TestUtil.apexValueToString(describeFieldResult.getSObjectType()),
                equalTo(sObjectTypeName));
        assertThat(describeFieldResult.getReturnedFrom().get(), instanceOf(SObjectField.class));
    }

}
