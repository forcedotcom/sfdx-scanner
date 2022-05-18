package com.salesforce.graph.symbols;

import static org.hamcrest.Matchers.instanceOf;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexMapValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates that the correct ApexValue is returned from ApexValueBuilder. It has 4 permutations
 * covering cases where
 *
 * <ol>
 *   <li>Variable is declared but not assigned an initial value
 *   <li>Variable is declared and assigned an initial NULL value
 *   <li>Variable is declared and assigned an initial value
 *   <li>Variable is declared but not assigned an initial value, later assigned a value
 * </ol>
 */
public class ApexValueBuilderTest {
    private GraphTraversalSource g;

    private static final class Input {
        private final String variableDeclaration;
        private final String initializer;
        // Type when the value hasn't been assigned
        private final Class<?> uninitializedType;
        // Type when a value has been assigned
        private final Class<?> initializedType;

        Input(
                String variableDeclaration,
                String initializer,
                Class<?> uninitializedType,
                Class<?> initializedType) {
            this.variableDeclaration = variableDeclaration;
            this.initializer = initializer;
            this.uninitializedType = uninitializedType;
            this.initializedType = initializedType;
        }
    }

    public static Stream<Arguments> testExpectedClassType() {
        final List<Arguments> values = new ArrayList<>();

        List<Input> inputs =
                Arrays.asList(
                        new Input(
                                "String",
                                "'Account'",
                                ApexStringValue.class,
                                ApexStringValue.class),
                        new Input(
                                "List<String>",
                                "new List<String>()",
                                ApexListValue.class,
                                ApexListValue.class),
                        new Input(
                                "String[]",
                                "new String[] {'Account', 'Contact'}",
                                ApexListValue.class,
                                ApexListValue.class),
                        new Input(
                                "Map<String, String>",
                                "new Map<String, String>()",
                                ApexMapValue.class,
                                ApexMapValue.class),
                        new Input(
                                "Account",
                                "new Account()",
                                ApexSingleValue.class,
                                ApexSingleValue.class),
                        new Input(
                                "Account",
                                "new Account(Name='Acme Inc.')",
                                ApexSingleValue.class,
                                ApexSingleValue.class),
                        new Input(
                                "List<Account>",
                                "[SELECT Name FROM Account]",
                                ApexListValue.class,
                                ApexListValue.class),
                        // DescribeSObjectResult can be declared in two different manners
                        new Input(
                                "DescribeSObjectResult",
                                "MyObject__c.SObjectType.getDescribe()",
                                DescribeSObjectResult.class,
                                DescribeSObjectResult.class),
                        new Input(
                                "Schema.DescribeSObjectResult",
                                "MyObject__c.SObjectType.getDescribe()",
                                DescribeSObjectResult.class,
                                DescribeSObjectResult.class),
                        // These types switch types from ApexSingleValue to more specific type when
                        // initialized
                        new Input(
                                "SObjectType",
                                "MyObject__c.SObjectType",
                                SObjectType.class,
                                SObjectType.class),
                        new Input(
                                "MyClass",
                                "new MyClass()",
                                ApexSingleValue.class,
                                ApexClassInstanceValue.class),
                        new Input(
                                "Account",
                                "[SELECT Name FROM Account]",
                                ApexSingleValue.class,
                                ApexSoqlValue.class));

        for (Input input : inputs) {
            // no initial value, no assignment
            values.add(
                    Arguments.of(input.variableDeclaration, null, null, input.uninitializedType));
            // null initial value, no assignment
            values.add(
                    Arguments.of(input.variableDeclaration, "NULL", null, input.uninitializedType));
            // initial value, no assignment
            values.add(
                    Arguments.of(
                            input.variableDeclaration,
                            input.initializer,
                            null,
                            input.initializedType));
            // no initial value, assignment
            values.add(
                    Arguments.of(
                            input.variableDeclaration,
                            null,
                            input.initializer,
                            input.initializedType));
        }

        return values.stream();
    }

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @MethodSource
    @ParameterizedTest(name = "Type:{0}-Initializer:{1}-Assignment:{2}")
    public void testExpectedClassType(
            String variableDeclaration,
            String initializer,
            String assignment,
            Class<?> expectedClass) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "   public static void doSomething() {\n"
                    + getDeclarationLine(variableDeclaration, initializer)
                    + getAssignmentLine(assignment)
                    + "       System.debug(var);\n"
                    + "   }\n"
                    + "}\n"
        };

        TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
        SystemDebugAccumulator visitor = result.getVisitor();

        ApexValue<?> apexValue = visitor.getSingletonResult();
        MatcherAssert.assertThat(apexValue, instanceOf(expectedClass));
    }

    private String getDeclarationLine(String variableDeclaration, String initializer) {
        if (initializer == null) {
            return variableDeclaration + " var;\n";
        } else {
            return variableDeclaration + " var = " + initializer + ";\n";
        }
    }

    private String getAssignmentLine(String assignment) {
        if (assignment != null) {
            return " var = " + assignment + ";\n";
        } else {
            return "";
        }
    }
}
