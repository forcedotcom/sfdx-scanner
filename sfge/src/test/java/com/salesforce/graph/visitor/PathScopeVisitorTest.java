package com.salesforce.graph.visitor;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.exception.UserActionException;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathScopeVisitorTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /** Instance of case statements introduce a new variable that is of the correct type */
    @Test
    public void testInstanceOfSwitchStatementPromotesVariable() {
        String sourceCode =
                "public class MyClass {\n"
                        + "    public static void doSomething() {\n"
                        + "		SObject obj = new Account(Name = 'Acme Inc.');\n"
                        + "       switch on obj {\n"
                        + "       	when Account a {\n"
                        + "           	System.debug(a);\n"
                        + "           	System.debug(a.name);\n"
                        + "           	a.put('Name', 'New Name');\n"
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
                        + "       System.debug(a);\n"
                        + "       System.debug(obj.name);\n"
                        + "    }\n"
                        + "}\n";

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();
        MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(4)));

        ApexSingleValue apexSingleValue = visitor.getResult(0);
        // TODO: Ideally this would be an Account. See TODO: comment in
        // PathScopeVisitor#visit(TypeWhenBlockVertex vertex)
        MatcherAssert.assertThat(
                apexSingleValue.getTypeVertex().get().getCanonicalType(), equalTo("SObject"));

        ApexStringValue apexStringValue;

        apexStringValue = visitor.getResult(1);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("Acme Inc."));

        // Assert that 'a' goes out of scope
        Optional<ApexValue<?>> optResult = visitor.getOptionalResult(2);
        MatcherAssert.assertThat(optResult.isPresent(), equalTo(false));

        // Assert that mutations applied to "a" are applied to the original object
        apexStringValue = visitor.getResult(3);
        MatcherAssert.assertThat(TestUtil.apexValueToString(apexStringValue), equalTo("New Name"));
    }

    @Test
    public void testParameterNameShadowsField() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "		List<String> values = new List<String> {'value1', 'value2'};\n"
                    + "		MyOtherClass c = new MyOtherClass();\n"
                    + "		c.addValues(values);\n"
                    + "		c.logValues();\n"
                    + "    }\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "	private String[] values = new String[0];\n"
                    +
                    // Make sure that the parameter array is added to the instance array
                    "   public void addValues(String[] values) {\n"
                    + "		this.values.addAll(values);\n"
                    + "	}\n"
                    + "   public void logValues() {\n"
                    + "		System.debug(values);\n"
                    + "	}\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexListValue value = visitor.getSingletonResult();
        MatcherAssert.assertThat(value.getValues(), hasSize(equalTo(2)));

        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getValues().get(0)), equalTo("value1"));
        MatcherAssert.assertThat(
                TestUtil.apexValueToString(value.getValues().get(1)), equalTo("value2"));
    }

    @Test
    public void testVariableNameReuseThrowsUserActionException() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   public static void doSomething() {\n"
                        + "       String myStr = 'hi';\n"
                        + "       String myStr = 'hello';\n"
                        + "   }\n"
                        + "}\n";

        UserActionException thrown =
                assertThrows(
                        UserActionException.class,
                        () -> TestRunner.walkPath(g, sourceCode),
                        "UserActionException should've been thrown before this point");

        MatcherAssert.assertThat(thrown.getMessage(), containsString("MyClass:4"));
    }

    @Test
    public void testParameterNameAndFieldDoNotClash() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   String myStr;\n"
                        + "   public void doSomething(String myStr) {\n"
                        + "       this.myStr = myStr;\n"
                        + "   }\n"
                        + "}\n";

        Assertions.assertDoesNotThrow(() -> TestRunner.walkPath(g, sourceCode));
    }
}
