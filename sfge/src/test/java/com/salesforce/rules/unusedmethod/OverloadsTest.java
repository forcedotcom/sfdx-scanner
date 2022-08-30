package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.graph.vertex.MethodVertex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for distinguishing between different overloads of the same method. */
public class OverloadsTest extends BaseUnusedMethodTest {

    /* =============== SECTION 1: INSTANCE METHODS =============== */
    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with different arities.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // Provide the arity of the *other* method, since that's the one that is uncalled.
        // One set per method, per visibility scope.
        // "public,  overloadedMethod(),  1",
        // "protected,  overloadedMethod(),  1",
        "private,  overloadedMethod(),  1",
        // "public,  overloadedMethod(false),  0",
        // "protected,  overloadedMethod(false),  0",
        "private,  overloadedMethod(false),  0"
    })
    @ParameterizedTest(name = "{displayName}: {0} {1}")
    public void callInstanceMethodWithDifferentArityOverloads_expectViolation(
            String scope, String invocation, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean overloadedMethod() {\n", scope)
                        + "        return true;\n"
                        + "    }\n"
                        + String.format("    %s boolean overloadedMethod(boolean b) {\n", scope)
                        + "        return b;\n"
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public boolean methodInvoker() {\n"
                        + String.format("        return %s;\n", invocation)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals(v.getSourceVertexName(), "overloadedMethod");
                    assertEquals(((MethodVertex) v.getSourceVertex()).getArity(), arity);
                });
    }

    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with the same arity, but different signatures.
     */
    @CsvSource({
        // Specify the beginning line of the overload that WASN'T called.
        // One test per method, per argument source.
        // Argument sources are:
        // - Literal values
        "overloadedMethod(42),  11",
        "overloadedMethod(true),  8",
        // - Method parameters (and their instance properties/methods)
        "overloadedMethod(iParam),  11",
        "overloadedMethod(bParam),  8",
        "overloadedMethod(phcParam.iExternalInstanceProp),  11",
        "overloadedMethod(phcParam.getIntegerProp()),  11",
        "overloadedMethod(phcParam.bExternalInstanceProp),  8",
        "overloadedMethod(phcParam.getBooleanProp()),  8",
        // - Variables (and their instance properties/methods)
        "overloadedMethod(iVar),  11",
        "overloadedMethod(bVar),  8",
        "overloadedMethod(phcVar.iExternalInstanceProp),  11",
        "overloadedMethod(phcVar.getIntegerProp()),  11",
        "overloadedMethod(phcVar.bExternalInstanceProp),  8",
        "overloadedMethod(phcVar.getBooleanProp()),  8",
        // - Internal instance method returns (and their instance properties/methods)
        "overloadedMethod(intReturner()),  11",
        "overloadedMethod(this.intReturner()),  11",
        "overloadedMethod(boolReturner()),  8",
        "overloadedMethod(this.boolReturner()),  8",
        "overloadedMethod(phcReturner().iExternalInstanceProp),  11",
        "overloadedMethod(this.phcReturner().iExternalInstanceProp),  11",
        "overloadedMethod(phcReturner().getIntegerProp()),  11",
        "overloadedMethod(this.phcReturner().getIntegerProp()),  11",
        "overloadedMethod(phcReturner().bExternalInstanceProp),  8",
        "overloadedMethod(this.phcReturner().bExternalInstanceProp),  8",
        "overloadedMethod(phcReturner().getBooleanProp()),  8",
        "overloadedMethod(this.phcReturner().getBooleanProp()),  8",
        // - Internal instance properties (and their instance properties/methods)
        "overloadedMethod(iInstanceProp),  11",
        "overloadedMethod(this.iInstanceProp),  11",
        "overloadedMethod(bInstanceProp),  8",
        "overloadedMethod(this.bInstanceProp),  8",
        "overloadedMethod(phcInstanceProp.iExternalInstanceProp),  11",
        "overloadedMethod(this.phcInstanceProp.iExternalInstanceProp),  11",
        "overloadedMethod(phcInstanceProp.getIntegerProp()),  11",
        "overloadedMethod(this.phcInstanceProp.getIntegerProp()),  11",
        "overloadedMethod(phcInstanceProp.bExternalInstanceProp),  8",
        "overloadedMethod(this.phcInstanceProp.bExternalInstanceProp),  8",
        "overloadedMethod(phcInstanceProp.getBooleanProp()),  8",
        "overloadedMethod(this.phcInstanceProp.getBooleanProp()),  8",
        // - Internal static method returns (and their instance properties/methods)
        "overloadedMethod(staticIntReturner()),  11",
        "overloadedMethod(MethodHostClass.staticIntReturner()),  11",
        "overloadedMethod(staticBoolReturner()),  8",
        "overloadedMethod(MethodHostClass.staticBoolReturner()),  8",
        "overloadedMethod(staticPhcReturner().iExternalInstanceProp),  11",
        "overloadedMethod(MethodHostClass.staticPhcReturner().iExternalInstanceProp),  11",
        "overloadedMethod(staticPhcReturner().getIntegerProp()),  11",
        "overloadedMethod(MethodHostClass.staticPhcReturner().getIntegerProp()),  11",
        "overloadedMethod(staticPhcReturner().bExternalInstanceProp),  8",
        "overloadedMethod(MethodHostClass.staticPhcReturner().bExternalInstanceProp),  8",
        "overloadedMethod(staticPhcReturner().getBooleanProp()),  8",
        "overloadedMethod(MethodHostClass.staticPhcReturner().getBooleanProp()),  8",
        // - Internal static properties (and their instance properties/methods)
        "overloadedMethod(iStaticProp),  11",
        "overloadedMethod(MethodHostClass.iStaticProp),  11",
        "overloadedMethod(bStaticProp),  8",
        "overloadedMethod(MethodHostClass.bStaticProp),  8",
        "overloadedMethod(phcStaticProp.iExternalInstanceProp),  11",
        "overloadedMethod(MethodHostClass.phcStaticProp.iExternalInstanceProp),  11",
        "overloadedMethod(phcStaticProp.getIntegerProp()),  11",
        "overloadedMethod(MethodHostClass.phcStaticProp.getIntegerProp()),  11",
        "overloadedMethod(phcStaticProp.bExternalInstanceProp),  8",
        "overloadedMethod(MethodHostClass.phcStaticProp.bExternalInstanceProp),  8",
        "overloadedMethod(phcStaticProp.getBooleanProp()),  8",
        "overloadedMethod(MethodHostClass.phcStaticProp.getBooleanProp()),  8",
        // - External static instance properties
        "overloadedMethod(PropertyHostClass.iExternalStaticProp),  11",
        "overloadedMethod(PropertyHostClass.bExternalStaticProp),  8"
    })
    @ParameterizedTest(name = "{displayName}: invocation of {0}")
    @Disabled
    public void callInstanceMethodWithDifferentSignatureOverloads_expectViolation(
            String invocation, int uncalledBeginLine) {
        String[] sourceCodes = {
            "global class MethodHostClass {\n"
                    + "    private static integer iStaticProp = 42;\n"
                    + "    private static boolean bStaticProp = false;\n"
                    + "    private static PropertyHostClass phcStaticProp = new PropertyHostClass();\n"
                    + "    private integer iInstanceProp = 32;\n"
                    + "    private boolean bInstanceProp = true;\n"
                    + "    private PropertyHostClass phcInstanceProp = new PropertyHostClass();\n"
                    + "    private boolean overloadedMethod(Integer i) {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    private boolean overloadedMethod(boolean b) {\n"
                    + "        return b;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public integer intReturner() {\n"
                    + "        return 7;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean boolReturner() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public PropertyHostClass phcReturner() {\n"
                    + "        return new PropertyHostClass();\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public static integer staticIntReturner() {\n"
                    + "        return 42;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public static boolean staticBoolReturner() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public static PropertyHostClass staticPhcReturner() {\n"
                    + "        return new PropertyHostClass();\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean methodInvoker(Integer iParam, Boolean bParam, PropertyHostClass phcParam) {\n"
                    + "        Integer iVar = 42;\n"
                    + "        Boolean bVar = true;\n"
                    + "        PropertyHostClass phcVar = new PropertyHostClass();\n"
                    + String.format("        return %s;\n", invocation)
                    + "    }\n"
                    + "}\n",
            "global class PropertyHostClass {\n"
                    + "    public static integer iExternalStaticProp = 11;\n"
                    + "    public static boolean bExternalStaticProp = false;\n"
                    + "    public integer iExternalInstanceProp = 9;\n"
                    + "    public boolean bExternalInstanceProp = true;\n"
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public integer getIntegerProp() {\n"
                    + "        return iExternalInstanceProp;\n"
                    + "    }\n"
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean getBooleanProp() {\n"
                    + "        return bExternalInstanceProp;\n"
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals(v.getSourceVertexName(), "overloadedMethod");
                    assertEquals(v.getSourceVertex().getBeginLine(), uncalledBeginLine);
                });
    }

    /* =============== SECTION 2: CONSTRUCTOR METHODS =============== */

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with different arities, invoked via the `new` keyword.
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called,
        // and have one variant per visibility scope.
        "public,  new MyClass(true),  2",
        "protected,  new MyClass(true),  2",
        "private,  new MyClass(true),  2",
        "public,  new MyClass(true, true),  1",
        "protected,  new MyClass(true, true),  1",
        "private,  new MyClass(true, true),  1"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    @Disabled
    public void callConstructorViaNewWithDifferentArityOverloads_expectViolation(
            String scope, String constructor, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s MyClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MyClass(boolean b, boolean c) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public static void constructorInvocation() {\n"
                        + String.format("        MyClass mc = %s;\n", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals(v.getSourceVertexName(), "<init>");
                    assertEquals(((MethodVertex) v.getSourceVertex()).getArity(), arity);
                });
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with the same arity, but different signatures, invoked
     * via the `new` keyword
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called.
        // One test per constructor, per visibility scope.
        "public,  new MethodHostClass(42),  4",
        "protected,  new MethodHostClass(42),  4",
        "private,  new MethodHostClass(42),  4",
        "public,  new MethodHostClass(true),  2",
        "protected,  new MethodHostClass(true),  2",
        "private,  new MethodHostClass(true),  2"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    @Disabled
    public void callConstructorViaNewWithDifferentSignatureOverloads_expectViolation(
            String scope, String constructor, int beginLine) {
        String sourceCode =
                "global class MethodHostClass {\n"
                        + String.format("    %s MethodHostClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MethodHostClass(Integer i) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public void methodInvoker() {\n"
                        + String.format("        MethodHostClass mhc = %s;\n", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(beginLine, ((MethodVertex) v.getSourceVertex()).getArity());
                });
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with different arities, invoked via the `this` keyword.
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called,
        // and have one variant per visibility scope.
        //        "public,  this(true),  2",
        "protected,  this(true),  2",
        "private,  this(true),  2",
        //        "public,  'this(true, true)', 1",
        "protected,  'this(true, true)', 1",
        "private,  'this(true, true)', 1"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    public void callConstructorViaThisWithDifferentArityOverloads_expectViolation(
            String scope, String constructor, int arity) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s MyClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MyClass(boolean b, boolean c) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */"
                        + "    public MyClass() {\n"
                        + String.format("        %s;\n", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(arity, ((MethodVertex) v.getSourceVertex()).getArity());
                });
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with the same arity, but different signatures, invoked
     * via the `this` keyword
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called.
        // One test per constructor, per visibility scope.
        "public,  this(42),  4",
        "protected,  this(42),  4",
        "private,  this(42),  4",
        "public,  this(true),  2",
        "protected,  this(true),  2",
        "private,  this(true),  2"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    @Disabled
    public void callConstructorViaThisWithDifferentSignatureOverloads_expectViolation(
            String scope, String constructor, int beginLine) {
        String sourceCode =
                "global class MethodHostClass {\n"
                        + String.format("    %s MethodHostClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MethodHostClass(Integer i) {\n", scope)
                        + "    }\n"
                        // Use the engine directive to prevent this method from tripping the rule.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    public MethodHostClass() {\n"
                        + String.format("        %s;", constructor)
                        + "    }\n"
                        + "}\n";
        assertViolations(
                sourceCode,
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals(beginLine, ((MethodVertex) v.getSourceVertex()).getArity());
                });
    }
}
