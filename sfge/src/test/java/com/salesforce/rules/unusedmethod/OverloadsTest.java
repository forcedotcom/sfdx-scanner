package com.salesforce.rules.unusedmethod;

import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for distinguishing between different overloads of the same method. These tests are
 * complicated enough to justify being their own file.
 */
public class OverloadsTest extends BaseUnusedMethodTest {

    /* =============== SECTION 1: INSTANCE METHODS =============== */
    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with different arities.
     */
    @CsvSource({
        // One set per method, per visibility scope.
        "public,  overloadedMethod()",
        "protected,  overloadedMethod()",
        "private,  overloadedMethod()",
        "public,  overloadedMethod(false)",
        "protected,  overloadedMethod(false)",
        "private,  overloadedMethod(false)"
    })
    @ParameterizedTest(name = "{displayName}: invocation {0}")
    public void callInstanceMethodWithDifferentArityOverloads_expectViolation(
            String scope, String invocation) {
        // spotless:off
        String sourceCode =
                "global class MyClass {\n"
              + String.format("    %s boolean overloadedMethod() {\n", scope)
              + "        return true;\n"
              + "    }\n"
              + String.format("    %s boolean overloadedMethod(boolean b) {\n", scope)
              + "        return b;\n"
              + "    }\n"
                // Use the engine directive to prevent this method from tripping the rule.
              + "    global static boolean entrypointMethod() {\n"
              + "        MyClass mc = new MyClass();\n"
              + String.format("        return mc.%s;\n", invocation)
              + "    }\n"
              + "}\n";
        // spotless:on
        int usedStartLine = invocation.contains("false") ? 5 : 2;
        int unusedStartLine = invocation.contains("false") ? 2 : 5;
        assertExpectations(
                new String[] {sourceCode},
                "MyClass",
                "entrypointMethod",
                Collections.singletonList("MyClass#overloadedMethod@" + usedStartLine),
                Collections.singletonList("MyClass#overloadedMethod@" + unusedStartLine));
    }

    /**
     * If there's different overloads of an instance method, then only the ones that are actually
     * invoked count as used. Specific case: Methods with the same arity, but different signatures.
     */
    @CsvSource({
        // One test per method, per argument source.
        // Argument sources are:
        // - Literal values
        "overloadedMethod(42), 8, 11",
        "overloadedMethod(true), 11, 8",
        // - Method parameters (and their instance properties/methods)
        "overloadedMethod(iParam), 8, 11",
        "overloadedMethod(bParam), 11, 8",
        "overloadedMethod(phcParam.iExternalInstanceProp), 8, 11",
        "overloadedMethod(phcParam.getIntegerProp()), 8, 11",
        "overloadedMethod(phcParam.bExternalInstanceProp), 11, 8",
        "overloadedMethod(phcParam.getBooleanProp()), 11, 8",
        // - Variables (and their instance properties/methods)
        "overloadedMethod(iVar), 8, 11",
        "overloadedMethod(bVar), 11, 8",
        "overloadedMethod(phcVar.iExternalInstanceProp), 8, 11",
        "overloadedMethod(phcVar.getIntegerProp()), 8, 11",
        "overloadedMethod(phcVar.bExternalInstanceProp), 11, 8",
        "overloadedMethod(phcVar.getBooleanProp()), 11, 8",
        // - Internal instance method returns (and their instance properties/methods)
        "overloadedMethod(intReturner()), 8, 11",
        "overloadedMethod(this.intReturner()), 8, 11",
        "overloadedMethod(boolReturner()), 11, 8",
        "overloadedMethod(this.boolReturner()), 11, 8",
        // "overloadedMethod(phcReturner().iExternalInstanceProp), 8, 11", TODO FIX AND ENABLE TESTS
        // "overloadedMethod(this.phcReturner().iExternalInstanceProp), 8, 11", TODO FIX AND ENABLE
        // TESTS
        "overloadedMethod(phcReturner().getIntegerProp()), 8, 11",
        "overloadedMethod(this.phcReturner().getIntegerProp()), 8, 11",
        // "overloadedMethod(phcReturner().bExternalInstanceProp), 11, 8", TODO: FIX AND ENABLE
        // "overloadedMethod(this.phcReturner().bExternalInstanceProp), 11, 8", TODO: FIX AND ENABLE
        "overloadedMethod(phcReturner().getBooleanProp()), 11, 8",
        "overloadedMethod(this.phcReturner().getBooleanProp()), 11, 8",
        // - Internal instance properties (and their instance properties/methods)
        "overloadedMethod(iInstanceProp), 8, 11",
        "overloadedMethod(this.iInstanceProp), 8, 11",
        "overloadedMethod(bInstanceProp), 11, 8",
        "overloadedMethod(this.bInstanceProp), 11, 8",
        "overloadedMethod(phcInstanceProp.iExternalInstanceProp), 8, 11",
        "overloadedMethod(this.phcInstanceProp.iExternalInstanceProp), 8, 11",
        "overloadedMethod(phcInstanceProp.getIntegerProp()), 8, 11",
        "overloadedMethod(this.phcInstanceProp.getIntegerProp()), 8, 11",
        "overloadedMethod(phcInstanceProp.bExternalInstanceProp), 11, 8",
        "overloadedMethod(this.phcInstanceProp.bExternalInstanceProp), 11,  8",
        "overloadedMethod(phcInstanceProp.getBooleanProp()), 11, 8",
        "overloadedMethod(this.phcInstanceProp.getBooleanProp()), 11, 8",
        // - Internal static method returns (and their instance properties/methods)
        "overloadedMethod(staticIntReturner()), 8, 11",
        "overloadedMethod(MethodHostClass.staticIntReturner()), 8, 11",
        "overloadedMethod(staticBoolReturner()), 11, 8",
        "overloadedMethod(MethodHostClass.staticBoolReturner()), 11, 8",
        // "overloadedMethod(staticPhcReturner().iExternalInstanceProp), 8, 11", TODO FIX AND ENABLE
        // TEST
        // "overloadedMethod(MethodHostClass.staticPhcReturner().iExternalInstanceProp), 8, 11",
        // TODO FIX AND ENABLE TEST
        "overloadedMethod(staticPhcReturner().getIntegerProp()), 8, 11",
        "overloadedMethod(MethodHostClass.staticPhcReturner().getIntegerProp()), 8, 11",
        // "overloadedMethod(staticPhcReturner().bExternalInstanceProp), 11, 8", TODO: FIX AND
        // ENABLE TEST
        // "overloadedMethod(MethodHostClass.staticPhcReturner().bExternalInstanceProp), 11, 8",
        // TODO: FIX AND ENABLE TEST
        "overloadedMethod(staticPhcReturner().getBooleanProp()), 11, 8",
        "overloadedMethod(MethodHostClass.staticPhcReturner().getBooleanProp()), 11, 8",
        // - Internal static properties (and their instance properties/methods)
        "overloadedMethod(iStaticProp), 8, 11",
        // "overloadedMethod(MethodHostClass.iStaticProp), 8, 11", TODO: FIX AND ENABLE
        "overloadedMethod(bStaticProp), 11, 8",
        // "overloadedMethod(MethodHostClass.bStaticProp), 11, 8", TODO: FIX AND ENABLE
        "overloadedMethod(phcStaticProp.iExternalInstanceProp), 8, 11",
        // "overloadedMethod(MethodHostClass.phcStaticProp.iExternalInstanceProp), 8, 11", TODO: FIX
        // AND ENABLE
        "overloadedMethod(phcStaticProp.getIntegerProp()), 8, 11",
        // "overloadedMethod(MethodHostClass.phcStaticProp.getIntegerProp()), 8, 11", TODO: FIX AND
        // ENABLE
        "overloadedMethod(phcStaticProp.bExternalInstanceProp), 11, 8",
        // "overloadedMethod(MethodHostClass.phcStaticProp.bExternalInstanceProp), 11, 8", TODO: FIX
        // AND ENABLE
        "overloadedMethod(phcStaticProp.getBooleanProp()), 11, 8",
        // "overloadedMethod(MethodHostClass.phcStaticProp.getBooleanProp()), 11, 8", TODO: FIX AND
        // ENABLE
        // - External static instance properties
        // "overloadedMethod(PropertyHostClass.iExternalStaticProp), 8, 11",TODO: FIX AND ENABLE
        // "overloadedMethod(PropertyHostClass.bExternalStaticProp), 11, 8" TODO: FIX AND ENABLE
    })
    @ParameterizedTest(name = "{displayName}: invocation of {0}")
    public void callInstanceMethodWithDifferentSignatureOverloads_expectViolation(
            String invocation, int calledBeginLine, int uncalledBeginLine) {
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
                    + "    public integer intReturner() {\n"
                    + "        return 7;\n"
                    + "    }\n"
                    + "    public boolean boolReturner() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "    public PropertyHostClass phcReturner() {\n"
                    + "        return new PropertyHostClass();\n"
                    + "    }\n"
                    + "    public static integer staticIntReturner() {\n"
                    + "        return 42;\n"
                    + "    }\n"
                    + "    public static boolean staticBoolReturner() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "    public static PropertyHostClass staticPhcReturner() {\n"
                    + "        return new PropertyHostClass();\n"
                    + "    }\n"
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
                    + "    public integer getIntegerProp() {\n"
                    + "        return iExternalInstanceProp;\n"
                    + "    }\n"
                    + "    public boolean getBooleanProp() {\n"
                    + "        return bExternalInstanceProp;\n"
                    + "    }\n"
                    + "}\n",
            String.format(
                    COMPLEX_ENTRYPOINT,
                    "MethodHostClass mch = new MethodHostClass();\n"
                            + "PropertyHostClass pch = new PropertyHostClass();\n"
                            + "return mch.methodInvoker(42, false, pch);\n")
        };
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("MethodHostClass#overloadedMethod@" + calledBeginLine),
                Collections.singletonList("MethodHostClass#overloadedMethod@" + uncalledBeginLine));
    }

    /* =============== SECTION 2: CONSTRUCTOR METHODS =============== */

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with different arities, invoked via the `new` keyword.
     */
    @CsvSource({
        // One variant per visibility scope and constructor.
        "public,  'new MyClass(true)'",
        "protected,  'new MyClass(true)'",
        "private,  'new MyClass(true)'",
        "public,  'new MyClass(true, true)'",
        "protected,  'new MyClass(true, true)'",
        "private,  'new MyClass(true, true)'"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    public void callConstructorViaNewWithDifferentArityOverloads_expectViolation(
            String scope, String constructor) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s MyClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MyClass(boolean b, boolean c) {\n", scope)
                        + "    }\n"
                        + "    global static boolean constructorInvocation() {\n"
                        + String.format("        MyClass mc = %s;\n", constructor)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        int usedLineNumber = constructor.contains(",") ? 4 : 2;
        int unusedLineNumber = constructor.contains(",") ? 2 : 4;
        assertExpectations(
                new String[] {sourceCode},
                "MyClass",
                "constructorInvocation",
                Collections.singletonList("MyClass#<init>@" + usedLineNumber),
                Collections.singletonList("MyClass#<init>@" + unusedLineNumber));
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with the same arity, but different signatures, invoked
     * via the `new` keyword
     */
    @CsvSource({
        // Use the arity of the constructor that ISN'T being called.
        // One test per constructor, per visibility scope.
        "public,  new MethodHostClass(42)",
        "protected,  new MethodHostClass(42)",
        "private,  new MethodHostClass(42)",
        "public,  new MethodHostClass(true)",
        "protected,  new MethodHostClass(true)",
        "private,  new MethodHostClass(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    public void callConstructorViaNewWithDifferentSignatureOverloads_expectViolation(
            String scope, String constructor) {
        String sourceCode =
                "global class MethodHostClass {\n"
                        + String.format("    %s MethodHostClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MethodHostClass(Integer i) {\n", scope)
                        + "    }\n"
                        + "    global static boolean methodInvoker() {\n"
                        + String.format("        MethodHostClass mhc = %s;\n", constructor)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        int usedLine = constructor.contains("true") ? 2 : 4;
        int unusedLine = constructor.contains("true") ? 4 : 2;
        assertExpectations(
                new String[] {sourceCode},
                "MethodHostClass",
                "methodInvoker",
                Collections.singletonList("MethodHostClass#<init>@" + usedLine),
                Collections.singletonList("MethodHostClass#<init>@" + unusedLine));
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with different arities, invoked via the `this` keyword.
     */
    @CsvSource({
        // One variant per visibility scope and constructor option.
        "public,  this(true)",
        "protected,  this(true)",
        "private,  this(true)",
        "public,  'this(true, true)'",
        "protected,  'this(true, true)'",
        "private,  'this(true, true)'"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    public void callConstructorViaThisWithDifferentArityOverloads_expectViolation(
            String scope, String constructor) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global class MyClass {\n"
                + String.format("    %s MyClass(boolean b) {\n", scope)
                + "    }\n"
                + String.format("    %s MyClass(boolean b, boolean c) {\n", scope)
                + "    }\n"
                + "    public MyClass() {\n"
                + String.format("        %s;\n", constructor)
                + "    }\n"
                + "}\n",
            String.format(COMPLEX_ENTRYPOINT,
                // Invoke the no-param constructor to indirectly use the `this` keyword.
                "        MyClass mc = new MyClass();\n"
              + "        return true;\n"
            )
        };
        // spotless:on
        int usedLine = constructor.contains(",") ? 4 : 2;
        int unusedLine = constructor.contains(",") ? 2 : 4;
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("MyClass#<init>@" + usedLine),
                Collections.singletonList("MyClass#<init>@" + unusedLine));
    }

    /**
     * If there's different overloads of a constructor, then only the ones that are actually invoked
     * count as used. Specific case: Methods with the same arity, but different signatures, invoked
     * via the `this` keyword
     */
    @CsvSource({
        // One test per constructor, per visibility scope.
        "public,  this(42)",
        "protected,  this(42)",
        "private,  this(42)",
        "public,  this(true)",
        "protected,  this(true)",
        "private,  this(true)"
    })
    @ParameterizedTest(name = "{displayName}: {0} constructor {1}")
    public void callConstructorViaThisWithDifferentSignatureOverloads_expectViolation(
            String scope, String constructor) {
        String sourceCode =
                "global class MethodHostClass {\n"
                        + String.format("    %s MethodHostClass(boolean b) {\n", scope)
                        + "    }\n"
                        + String.format("    %s MethodHostClass(Integer i) {\n", scope)
                        + "    }\n"
                        + "    public MethodHostClass() {\n"
                        + String.format("        %s;", constructor)
                        + "    }\n"
                        + "}\n";
        String entrypoint =
                String.format(
                        COMPLEX_ENTRYPOINT,
                        "MethodHostClass mch = new MethodHostClass();\n" + "return true;\n");
        int usedLine = constructor.contains("true") ? 2 : 4;
        int unusedLine = constructor.contains("true") ? 4 : 2;
        assertExpectations(
                new String[] {sourceCode, entrypoint},
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("MethodHostClass#<init>@" + usedLine),
                Collections.singletonList("MethodHostClass#<init>@" + unusedLine));
    }
}
