package com.salesforce.rules.npe;

import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexNullPointerExceptionRule;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ApexNullPointerExceptionRuleTest extends BasePathBasedRuleTest {

    /** This is the template for tests pertaining specifically to variables. */
    // spotless:off
    private static final String VARIABLE_SOURCE_CODE_TEMPLATE =
        "public class MyClass {\n"
      + "    public void foo() {\n"
      // Test-specific code can go here.
      + "%s"
      + "    }\n"
      + "    \n"
      + "    private String getNullStr() {\n"
      + "        return null;\n"
      + "    }\n"
      + "    \n"
      + "    private Integer getNullInt() {\n"
      + "        return null;\n"
      + "    }\n"
      + "    \n"
      + "    private String getTruthyStr() {\n"
      + "        return 'beep';\n"
      + "    }\n"
      + "    \n"
      + "    private Integer getTruthyInt() {\n"
      + "       return 532;\n"
      + "    }\n"
      + "    \n"
      + "    private String getFalseyStr() {\n"
      + "        return '';\n"
      + "    }\n"
      + "    \n"
      + "    private Integer getFalseyInt() {\n"
      + "        return 0;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /** This is the template for tests pertaining specifically to unconstrained parameters. */
    // spotless:off
    private static final String UNCONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE =
        "public class MyClass {\n"
        // These parameters are here in case a test needs an indeterminant.
      + "    public void foo(Integer indeterminantInt, String indeterminantStr) {\n"
      // Test-specific method argument can go here.
      + "        paramUser(%s);\n"
      + "    }\n"
      + "    \n"
      // Test-specific param declaration can go here.
      + "    public void paramUser(%s) {\n"
      // Test-specific param invocation can go here.
      + "        Integer ref = %s;\n"
      + "    }\n"
      + "}";
    // spotless:on

    /** This is the template for tests pertaining to constrained indeterminant parameters. */
    // spotless:off
    private static final String CONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE =
        "public class MyClass {\n"
        // These parameters are indeterminants that can be constrained.
      + "    public void foo(Integer i, String s) {\n"
      // The constraint can go here.
      + "        if (%s) {\n"
      // An operation to be done if the constraint is satisfied.
      + "            Integer i2 = %s;\n"
      + "        } else {\n"
      // An operation to be done if the constraint is unsatisfied.
      + "            Integer i2 = %s;\n"
      + "        }\n"
      + "    }\n"
      + "}";
    // spotless:on

    /**
     * Tests for cases where a variable is initialized to a null value (implicity or explicitly) and
     * then referenced, thereby causing a violation.
     *
     * @param initialization - Code snippet initializing a null variable.
     * @param reference - Code snippet referencing the null variable.
     * @param op - The specific op we expect to see in a violation message. TODO: These values are
     *     tentative, and may change depending on the final rule implementation.
     */
    @CsvSource({
        // Initialization without assignment produces null value.
        "String s, Integer problem = s.length(), s.length",
        // "Integer i, Integer problem = i + 0, i + 0",
        // Explicit assignment to null produces null value.
        "String s = null, Integer problem = s.length(), s.length",
        // "Integer i = null, Integer problem = i + 0, i + 0",
        // Assigning to a null return produces a null value.
        "String s = getNullStr(), Integer problem = s.length(), s.length",
        // "Integer i = getNullInt(), Integer problem = i + 0, i + 0",
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullInitialization_expectViolation(
            String initialization, String reference, String op) {
        // spotless:off
        String sourceCodeInsert =
            "    " + initialization + ";\n"
          + "    " + reference + ";\n";
        // spotless:on
        String sourceCode = String.format(VARIABLE_SOURCE_CODE_TEMPLATE, sourceCodeInsert);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertViolations(rule, sourceCode, expect(4, op));
    }

    /**
     * Tests for cases where a variable is initialized to a non-null value and then referenced,
     * thereby triggering NO violation.
     *
     * @param initialization - Code snippet initializing a non-null variable.
     * @param reference - Code snippet referencing the non-null variable.
     */
    @CsvSource({
        // Truthy values are non-null.
        "String s = 'beep', Integer len = s.length()",
        "Integer i = 57, Integer i2 = i + 7",
        // Non-null falsey values are non-null.
        "String s = '', Integer len = s.length()",
        "Integer i = 0, Integer i2 = i + 7",
        // Returned non-null values are non-null.
        "String s = getTruthyStr(), Integer len = s.length()",
        "String s = getFalseyStr(), Integer len = s.length()",
        "Integer i = getTruthyInt(), Integer i2 = i + 7",
        "Integer i = getFalseyInt(), Integer i2 = i + 7"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNonNullInitialization_expectNoViolation(
            String initialization, String reference) {
        // spotless:off
        String sourceCodeInsert =
            "        " + initialization + ";\n"
          + "        " + reference + ";\n";
        // spotless:on
        String sourceCode = String.format(VARIABLE_SOURCE_CODE_TEMPLATE, sourceCodeInsert);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertNoViolation(rule, sourceCode);
    }

    /**
     * Tests for cases where a variable is initialized to null, reassigned to non-null, and then
     * referenced, thereby triggering NO violation.
     *
     * @param initialization - Code snippet initializing a variable to null.
     * @param reassignment - Code snippet reassigning the variable to non-null.
     * @param reference - Code snippet referencing the no-longer-null variable.
     */
    @CsvSource({
        // Reassignment to truthy value is non-null.
        "String s = null, s = 'beep', Integer len = s.length()",
        "Integer i = null, i = 53, Integer i2 = i + 2",
        // Reassignment to falsey non-null is still non-null.
        "String s = null, s = '', Integer len = s.length()",
        "Integer i = null, i = 0, Integer i2 = i + 2",
        // Reassignment to non-null method return is non-null.
        "String s = null, s = getTruthyStr(), Integer len = s.length()",
        "String s = null, s = getFalseyStr(), Integer len = s.length()",
        "Integer i = null, i = getTruthyInt(), Integer i2 = i + 2",
        "Integer i = null, i = getFalseyInt(), Integer i2 = i + 2",
    })
    @ParameterizedTest(name = "{displayName}: init {0}; assignment {1}")
    public void testReassignmentToNonNull_expectNoViolation(
            String initialization, String reassignment, String reference) {
        // spotless:off
        String sourceCodeInsert =
          "        " + initialization + ";\n"
        + "        " + reassignment + ";\n"
        + "        " + reference + ";\n";
        // spotless:on
        String sourceCode = String.format(VARIABLE_SOURCE_CODE_TEMPLATE, sourceCodeInsert);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertNoViolation(rule, sourceCode);
    }

    /**
     * Tests for cases where a variable is declared using an inline reference to a method that
     * returns null, thereby triggering a violation.
     *
     * @param reference - Code snippet referencing the null-returning method.
     * @param op - The specific op we expect to see in a violation message.
     */
    @CsvSource({
        "Integer i = getNullStr().length(), length",
        // "Integer i = getNullInt() + 2, getNullInt() + 2"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInlineNullMethodReturn_expectViolation(String reference, String op) {
        // spotless:off
        String sourceCodeInsert = "        " + reference + ";\n";
        // spotless:on
        String sourceCode = String.format(VARIABLE_SOURCE_CODE_TEMPLATE, sourceCodeInsert);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertViolations(rule, sourceCode, expect(3, op));
    }

    /**
     * Tests for cases where a variable is declared using an inline reference to a method that
     * returns non-null, thereby triggering NO violation.
     *
     * @param reference - Code snippet referencing the non-null-returning method.
     */
    @ValueSource(
            strings = {
                // Truthy values are non-null.
                "getTruthyStr().length()",
                "getTruthyInt() + 2",
                // Falsey values are still non-null.
                "getFalseyStr().length()",
                "getFalseyInt() + 2"
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testInlineNonNullMethodReturn_expectNoViolation(String reference) {
        String sourceCodeInsert = "    Integer i = " + reference + ";\n";
        String sourceCode = String.format(VARIABLE_SOURCE_CODE_TEMPLATE, sourceCodeInsert);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertNoViolation(rule, sourceCode);
    }

    /**
     * Tests for cases where a guaranteed-to-be-null parameter is referenced, thereby triggering a
     * violation.
     *
     * @param param - The declaration of the null parameter in the method sig
     * @param reference - The reference to the null parameter
     * @param op - The specific op expected in the violation message. TODO: This may change slightly
     *     once the rule is implemented.
     */
    @CsvSource({
        "String s, s.length(), s.length",
        //        "Integer i, i + 2, i + 2"
    })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullParamReference_expectViolation(String param, String reference, String op) {
        String sourceCode =
                String.format(UNCONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE, "null", param, reference);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertViolations(rule, sourceCode, expect(7, op));
    }

    /**
     * Tests for cases where a param whose non-nullness is either guaranteed (hard value) or simply
     * unknown (unconstrained indeterminant) is referenced, thereby triggering NO violation.
     *
     * @param value - The value assigned to the parameter
     * @param param - The declaration of the parameter
     * @param reference - The reference to the parameter
     */
    @CsvSource({
        // Truthy values are non-null.
        "'beep', String s, s.length()",
        "52, Integer i, i + 2",
        // Falsey values are still non-null.
        "'', String s, s.length()",
        "0, Integer i, i + 2",
        // An unconstrained indeterminant might be non-null.
        "indeterminantStr, String s, s.length()",
        "indeterminantInt, Integer i, i + 2"
    })
    @ParameterizedTest(name = "{displayName}: param value of {0}")
    public void testNonNullParamReference_expectNoViolation(
            String value, String param, String reference) {
        String sourceCode =
                String.format(UNCONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE, value, param, reference);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertNoViolation(rule, sourceCode);
    }

    /**
     * Tests for cases where an indeterminant is constrained in a way that guarantees its nullness
     * and then referenced, thereby triggering a violation.
     *
     * @param constraint - The constraint applied to the parameter
     * @param reference - A reference to the parameter
     * @param line - The line we expect an NPE to throw at
     * @param op - The specific op to be referenced by the violation message. TODO: THIS IS
     *     TENTATIVE.
     */
    @CsvSource({
        // Constraining to null should cause a violation in the IF-branch.
        "s == null, s.length(), 4, s.length",
        // "i == null, i + 2, 4, i + 2",
        // Constraining to generalized "not null" should cause a violation in the ELSE,
        // since failing a "not null" constraint is equivalent to passing a null constraint.
        "s != null, s.length(), 6, s.length",
        // "i != null, i + 2, 6, i + 2"
    })
    @ParameterizedTest(name = "{displayName}: constraint is {0}")
    public void testNullConstrainedIndeterminant_expectViolation(
            String constraint, String reference, int line, String op) {
        // Use the same reference for both sides of the constraint.
        String sourceCode =
                String.format(
                        CONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE, constraint, reference, reference);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertViolations(rule, sourceCode, expect(line, op));
    }

    /**
     * Tests for cases where an indeterminant is constrained without guaranteeing its nullness and
     * then referenced, thereby triggering NO violation.
     *
     * @param constraint - The constraint applied to the parameter
     * @param reference - A reference to the parameter
     */
    @CsvSource({
        // Constraints to specific value cause no violation in the ELSE, since "not that value"
        // doesn't imply nullness.
        "s == 'beep', s.length()",
        "i == 7, i + 2",
        "s == '', s.length()",
        "i == 0, i + 2",
        // Constraining to "not a specific value" causes no violation in the IF, since that
        // constraint doesn't imply nullness.
        "s != 'beep', s.length()",
        "i != 7, i + 2",
        "s != '', s.length()",
        "i != 0, i + 2"
    })
    @ParameterizedTest(name = "{displayName}: constraint is {0}")
    public void testInconclusivelyConstrainedIndeterminant_expectNoViolation(
            String constraint, String reference) {
        String sourceCode =
                String.format(
                        CONSTRAINED_PARAM_SOURCE_CODE_TEMPLATE, constraint, reference, reference);
        AbstractPathBasedRule rule = ApexNullPointerExceptionRule.getInstance();
        assertNoViolation(rule, sourceCode);
    }

    protected ViolationWrapper.NullPointerViolationBuilder expect(int line, String operation) {
        return ViolationWrapper.NullPointerViolationBuilder.get(line, operation);
    }
}
