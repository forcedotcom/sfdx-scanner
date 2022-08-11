package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.Violation;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A collection of tests for simple positive and negative cases. Comparable to smoke tests. If any
 * of these unexpectedly fail, something's deeply wrong.
 */
public class SimpleCasesTest extends BaseUnusedMethodTest {

    /** Obviously unused static/instance methods are unused. */
    // TODO: Enable subsequent tests as we implement functionality.
    @ValueSource(
            strings = {
                // "public static",
                // "public",
                // "protected", // No need for protected static, since those are mutually exclusive.
                // "private static",
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void outerMethodWithoutInvocation_expectViolation(String methodScope) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean unusedMethod() {\n", methodScope)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertViolations(sourceCode, "unusedMethod");
    }

    /** Obviously unused inner class instance methods are unused. */
    // TODO: Enable subsequent tests as we implement functionality.
    @ValueSource(
            strings = {
                /*"public", "protected", */
                "private"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void innerInstanceMethodWithoutInvocation_expectViolation(String scope) {
        String sourceCode =
                "global class MyClass {\n"
                        + "    global class MyInnerClass {\n"
                        + String.format("        %s boolean unusedMethod() {\n", scope)
                        + "            return true;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n";
        assertViolations(sourceCode, "unusedMethod");
    }

    /**
     * We want public and protected tests for arity of both 0 and 1, but
     * a private test for only the arity 1 constructor, since a private constructor
     * with arity 0 is ineligible.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // One test per constructor, per visibility scope.
        //        "public MyClass(),  0",
        //        "protected MyClass(),  0",
        //        "public MyClass(boolean b) , 1",
        //        "protected MyClass(boolean b),  1",
        "private MyClass(boolean b),  1"
    })
    @ParameterizedTest(name = "{displayName}: Declared constructor {0}, arity {1}")
    public void declaredConstructorWithoutInvocation_expectViolation(
            String declaration, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s {\n", declaration)
                        + "    }\n"
                        + "}\n";
        Consumer<Violation.RuleViolation> assertion =
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(arity, ((MethodVertex) v.getSourceVertex()).getArity());
                };
        assertViolations(sourceCode, assertion);
    }
}
