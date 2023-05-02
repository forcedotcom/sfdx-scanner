package com.salesforce.rules.unusedmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * These tests verify that constructors are properly handled by {@link
 * com.salesforce.rules.UnusedMethodRule}.
 */
public class ConstructorsTest extends BaseUnusedMethodTest {

    /**
     * Simple tests verifying that an obviously unused constructor is flagged as unused.
     *
     * @param declaration - The declaration of the tested constructor
     * @param arity - The constructor's arity
     */
    @CsvSource({
        // One test per constructor, per visibility scope.
        // EXCEPTION: No test for private 0-arity, since such methods are ineligible.
        "public MyClass(), 0",
        "protected MyClass(), 0",
        "public MyClass(boolean b), 1",
        "protected MyClass(boolean b), 1",
        "private MyClass(boolean b), 1"
    })
    @ParameterizedTest(name = "{displayName}: {1}-arity constructor: {0}")
    public void constructorWithoutInvocation_expectViolation(String declaration, int arity) {
        // spotless:off
        String sourceCode =
            "global class MyClass {\n"
          + "    " + declaration + " {}\n"
          + "    \n"
          + "    global static boolean entrypoint() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertNoUsage(new String[] {sourceCode}, "MyClass", "entrypoint", "MyClass#<init>#2");
    }

    /**
     * Test for cases where a class calls its own constructor internally via the `this()` syntax.
     *
     * @param visibility - The visibility of the target constructor.
     */
    @ValueSource(strings = {"public", "protected", "private"})
    @ParameterizedTest(name = "{displayName}: {0} constructor")
    public void constructorCalledViaThis_expectNoViolation(String visibility) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global class MyClass {\n"
            // Declare the tested constructor with the specified visibility.
          + "    " + visibility + " MyClass(boolean b, boolean b2) {}\n"
          + "    \n"
          + "    public MyClass(boolean b) {\n"
            // Invocation of the tested constructor via `this`
          + "        this(b, true);\n"
          + "    }\n"
          + "}",
            // Add an entrypoint that calls the constructor indirectly via the other constructor.
            String.format(SIMPLE_ENTRYPOINT, "new MyClass(false)")
        };
        // spotless:on
        assertUsage(sourceCodes, "MyEntrypoint", "entrypointMethod", "MyClass#<init>@2");
    }

    /**
     * Tests for cases where a subclass calls its parent's constructor via {@code super()}.
     *
     * @param visibility - Visibility of the parent constructor
     * @param arity - Arity of the parent constructor
     */
    @CsvSource({"public, 0", "public, 1", "protected, 0", "protected, 1"})
    @ParameterizedTest(name = "{displayName}: {0} constructor with arity {1}")
    public void constructorCalledViaSuper_expectNoViolation(String visibility, Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global virtual class ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    " + visibility + " ParentClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global class ChildClass extends ParentClass {\n"
          + "    public ChildClass() {\n"
            // Invoke the parent constructor via super
          + "        super(" + StringUtils.repeat("true", arity) + ");\n"
          + "    }\n"
          + "}",
            // An entrypoint to call the super-using constructor variant.
            String.format(SIMPLE_ENTRYPOINT, "new ChildClass()")
        };
        // spotless:on
        assertUsage(sourceCodes, "MyEntrypoint", "entrypointMethod", "ParentClass#<init>@2");
    }

    /**
     * Tests for cases where a class with a declared default constructor has a subclass without a
     * default constructor, and the subclass is instantiated via {@code new}. In this case, the
     * subclass's implicit default constructor should call the parent's default constructor.
     *
     * @param visibility - The visibility of the parent default constructor.
     */
    @ValueSource(strings = {"public", "protected"})
    @ParameterizedTest(name = "{displayName}: parent constructor scope {0}")
    @Disabled // TODO: FIX AND ENABLE THIS TEST
    public void constructorCalledViaImplicitSubclassConstructor_expectNoViolation(
            String visibility) {
        // spotless:off
        String[] sourceCodes= new String[] {
            // Declare a parent class with an explicitly-defined 0-arity constructor.
            "global virtual class ParentClass {\n"
          + "    " + visibility + " ParentClass() {}\n"
          + "}",
            // Declare a child class without a 0-arity constructor.
            "global class ChildClass extends ParentClass {\n"
            // Give the child class a method that returns a boolean.
          + "    public boolean getBoolean() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Add an entrypoint that instantiates the child class using
            // the implicitly-created default constructor.
            String.format(SIMPLE_ENTRYPOINT, "new ChildClass().getBoolean()")
        };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Arrays.asList("ChildClass#<init>@1", "ParentClass#<init>@2"),
                new ArrayList<>());
    }

    /**
     * Unless a subclass constructor calls another {@code this}-constructor or a specific {@code
     * super}-constructor, it will implicitly call the superclass's default constructor. Therefore,
     * constructing a grandchild class will implicitly invoke the parent class constructor even if
     * the child class doesn't explicitly call {@code super()}.
     *
     * @param arity - The arity of the tested constructor.
     */
    @ValueSource(ints = {0, 1})
    @ParameterizedTest(name = "{displayName}: arity {0}")
    @Disabled // TODO: FIX AND ENABLE THIS TEST
    public void constructorImplicitlyCalledByGrandchild_expectNoViolation(Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            "global virtual class ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    protected ParentClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global virtual class ChildClass extends ParentClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    public ChildClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            "global class GrandchildClass extends ChildClass {\n"
            // Declare a constructor that invokes super, which will only go up one level.
          + "    public GrandchildClass() {\n"
          + "        super(" + StringUtils.repeat("true", arity) + ");\n"
          + "    }\n"
          + "}",
            // An entrypoint to call the grandchild constructor, which will not invoke the parent variant.
            String.format(SIMPLE_ENTRYPOINT, "new GrandchildClass()")
        };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Arrays.asList(
                        "GrandchildClass#<init>@2", "ChildClass#<init>@2", "ParentClass#<init>@2"),
                new ArrayList<>());
    }

    /**
     * Simple tests for cases where one class's constructor is called somewhere in another class.
     *
     * @param arity - The arity of the tested constructor.
     */
    @ValueSource(ints = {0, 1})
    @ParameterizedTest(name = "{displayName}: Arity {0}")
    public void constructorCalledViaNew_expectNoViolation(Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare our tested class.
            "global class TestedClass {\n"
            // Declare a constructor with the expected arity that does nothing in particular.
          + "    public TestedClass(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            // Declare a class to invoke the tested constructor.
            "global class InvokerClass {\n"
          + "    global static TestedClass invokeTestedConstructor() {\n"
            // Invoke the tested constructor with however many parameters it expects.
          + "        return new TestedClass(" + StringUtils.repeat("true", arity) + ");\n"
          + "    }\n"
          + "}"
        };
        // spotless:on
        assertUsage(sourceCodes, "InvokerClass", "invokeTestedConstructor", "TestedClass#<init>@2");
    }

    /**
     * Tests for cases where an inner class's constructor is called in the outer class.
     *
     * @param variable - The way the variable is declared.
     * @param constructor - The way the constructor is invoked.
     */
    @CsvSource({
        "OuterClass.InnerClass, OuterClass.InnerClass",
        "OuterClass.InnerClass, InnerClass",
        "InnerClass, OuterClass.InnerClass",
        "InnerClass, InnerClass"
    })
    @ParameterizedTest(name = "{displayName}: Declared as {0}, constructed as new {1}()")
    public void innerConstructorUsedByOuter_expectNoViolation(String variable, String constructor) {
        // spotless:off
        String sourceCode =
            "global class OuterClass {\n"
            // Declare a method in the outer class to call the inner constructor
          + "    global static boolean invoker() {\n"
          + "        " + variable + " obj = new " + constructor + "(false);\n"
          + "        return true;\n"
          + "    }\n"
          + "    global class InnerClass {\n"
            // Declare a constructor for the inner class
          + "        public InnerClass(boolean b) {}\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertUsage(
                new String[] {sourceCode},
                "OuterClass",
                "invoker",
                "OuterClass.InnerClass#<init>@7");
    }

    /**
     * Tests for cases where an inner class's constructor is called by a sibling inner class.
     *
     * @param variable - The way the variable is declared.
     * @param constructor - The way the constructor is invoked.
     */
    @CsvSource({
        "OuterClass.InnerClass, OuterClass.InnerClass",
        "OuterClass.InnerClass, InnerClass",
        "InnerClass, OuterClass.InnerClass",
        "InnerClass, InnerClass"
    })
    @ParameterizedTest(name = "{displayName}: Declared as {0}, constructed as new {1}()")
    public void innerConstructorUsedBySiblingInner_expectNoViolation(
            String variable, String constructor) {
        // spotless:off
        String sourceCode =
            "global class OuterClass {\n"
          + "    global class InnerClass {\n"
            // Declare a constructor for the inner class
          + "        public InnerClass(boolean b) {}\n"
          + "    }\n"
          + "    \n"
          + "    global class InnerClass2 {\n"
            // Declare a method on the sibling inner class that instantiates the tested one.
          + "        global static boolean invoker() {\n"
          + "            " + variable + " obj = new " + constructor + "(false);\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
          + "}";
        // spotless:on
        assertUsage(
                new String[] {sourceCode},
                "OuterClass.InnerClass2",
                "invoker",
                "OuterClass.InnerClass#<init>@3");
    }

    /**
     * These tests cover variants of a weird edge case: If an inner class and an outer class share
     * the same name (e.g., {@code Whatever} and {@code Outer.Whatever}), then calling {@code new
     * Whatever()} in the inner class's outer/sibling classes should count as an invocation for the
     * inner class, not the outer class.
     *
     * @param referencer - The class that references the inner class.
     * @param arity - The arity of the tested constructor.
     */
    @CsvSource({
        "OuterClass, 0",
        "OuterClass, 1",
        "OuterClass.SiblingClass, 0",
        "OuterClass.SiblingClass, 1"
    })
    @ParameterizedTest(name = "{displayName}: {1}-arity constructor, called via {0}")
    public void externalReferenceSyntaxCollision_expectViolation(String referencer, Integer arity) {
        // spotless:off
        String[] sourceCodes = new String[]{
            // Declare an outer class with a constructor.
            "global class CollidingName {\n"
          + "    public CollidingName(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "}",
            // Declare another outer class.
            "global class OuterClass {\n"
            // Give the outer class an inner class whose name collides with the tested outer class.
          + "    global class CollidingName {\n"
            // Give the inner class a constructor with the same params as the outer class.
          + "        public CollidingName(" + StringUtils.repeat("boolean b", arity) + ") {}\n"
          + "    }\n"
          + "    \n"
            // Add another inner class with a method that instantiates the tested inner class.
          + "    global class SiblingClass {\n"
          + "        global boolean callConstructor() {\n"
          + "            CollidingName obj = new CollidingName(" + StringUtils.repeat("true", arity) + ");\n"
          + "            return true;\n"
          + "        }\n"
          + "    }\n"
            // Give the outer class a method that instantiates the tested inner class.
          + "    global boolean callConstructor() {\n"
            // IMPORTANT: This is where the constructor is called.
          + "        CollidingName obj = new CollidingName(" + StringUtils.repeat("true", arity) +");\n"
          + "        return true;\n"
          + "    }\n"
          + "}",
            // Add an entrypoint that calls the desired method.
            String.format(SIMPLE_ENTRYPOINT, "new " + referencer + "().callConstructor()")
        };
        // spotless:on
        assertExpectations(
                sourceCodes,
                "MyEntrypoint",
                "entrypointMethod",
                Collections.singletonList("OuterClass.CollidingName#<init>@3"),
                Collections.singletonList("CollidingName#<init>@2"));
    }
}
