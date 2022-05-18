package com.salesforce.graph.symbols.apex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerMatcher;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApexObjectPropertiesTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleObjectProperties() {
        final String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       Account a = new Account();\n"
                        + "		a.Name = 'Value';\n"
                        + "       System.debug(a.name);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("Value"));
    }

    @Test
    public void testOverwriteObjectProperties_apexStringValue() {
        final String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       Account a = new Account();\n"
                        + "		a.Name = 'Value 1';\n"
                        + "		a.name = 'Value 2';\n"
                        + "       System.debug(a.Name);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("Value 2"));
    }

    @Test
    public void testOverwriteObjectProperties_keyVertex() {
        final String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       Account a = new Account(Name = 'Value 1' );\n"
                        + "		a.name = 'Value 2';\n"
                        + "       System.debug(a.Name);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        assertThat(result, TestRunnerMatcher.hasValue("Value 2"));
    }

    @Test
    public void testOverwriteObjectProperties_apexSingleValue() {
        final String sourceCode =
                "public class MyClass {\n"
                        + "   public void doSomething() {\n"
                        + "       My_Custom_Object__c a = new My_Custom_Object__c();\n"
                        + "		a.Account = new Account(name = 'Acme Inc');\n"
                        + "		a.account = new Account(phone = '415-555-1212');\n"
                        + "       System.debug(a.Account);\n"
                        + "   }\n"
                        + "}";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexSingleValue apexValue = visitor.getSingletonResult();
        // "name" key wouldn't exist on this Account
        final Optional<ApexValue<?>> nameValue = apexValue.getApexValue("name");
        assertThat(nameValue.isPresent(), equalTo(false));
        // Only "phone" key would exist
        final Optional<ApexValue<?>> phoneValue = apexValue.getApexValue("phone");
        assertThat(phoneValue.isPresent(), equalTo(true));
        assertThat(TestUtil.apexValueToString(phoneValue), equalTo("415-555-1212"));
    }

    @Test
    public void testIndeterminantKey() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething(My_Data__c myData) {\n"
                        + "		System.debug(myData.My_Field__c);\n"
                        + "	}\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        final ApexSingleValue apexValue = visitor.getSingletonResult();

        assertThat(apexValue.isIndeterminant(), Matchers.equalTo(true));
        assertThat(
                apexValue.getTypeVertex().get().getCanonicalType(),
                Matchers.equalToIgnoringCase("My_Field__c"));
    }
}
