package com.salesforce.rules.unusedmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.salesforce.rules.Violation;
import java.util.function.Consumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for calls of methods on subclasses and superclasses. */
public class InheritanceTest extends BaseUnusedMethodTest {

    /* =============== SECTION 1: STATIC METHODS =============== */
    /**
     * If a subclass invokes a static method on its parent class, then that method counts as used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @CsvSource({
        // Invocation in static method, with implicit/explicit `this`,
        // and explicit references to the parent and child classes
        "static boolean,  this.staticMethod()",
        "static boolean,  staticMethod()",
        "static boolean,  ParentClass.staticMethod()",
        "static boolean,  ChildClass.staticMethod()",
        // Invocation in instance method, with implicit/explicit class reference.
        "boolean,  staticMethod()",
        "boolean,  ParentClass.staticMethod()"
    })
    @ParameterizedTest(name = "{displayName}: invoker scope {0}; invocation {1}")
    @Disabled
    public void staticMethodInvokedInSubclass_expectNoViolation(
            String invokerScope, String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + "    public static boolean staticMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public %s invokingMethod() {\n", invokerScope)
                    + String.format("        return %s;\n", invocation)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * If a subclass's inner class invokes a static method on the parent class, then that method
     * counts as used.
     */
    // (NOTE: No need for a `protected` case, since methods can't be both
    // `protected` and `static`.)
    @ValueSource(
            strings = {"staticMethod()", "ParentClass.staticMethod()", "ChildClass.staticMethod()"})
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled
    public void staticMethodInvokedInSubclassInnerClass_expectNoViolation(String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + "    public static boolean staticMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    + "    global class InnerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "        public boolean invokingMethod() {\n"
                    + String.format("            return %s;\n", invocation)
                    + "        }\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 1);
    }

    /* =============== SECTION 2: INSTANCE METHODS =============== */

    /**
     * If a subclass invokes methods it inherited from the parent without overriding them, those
     * methods count as used.
     */
    @CsvSource({
        // Implicit/explicit `this`, and `super`, for each of the two relevant
        // visibility scopes.
        "public,  parentMethod1,  parentMethod2",
        "protected,  parentMethod1,  parentMethod2",
        "public,  this.parentMethod1,  this.parentMethod2",
        "protected,  this.parentMethod1,  this.parentMethod2",
        "public,  super.parentMethod1,  super.parentMethod2",
        "protected,  super.parentMethod1,  super.parentMethod2"
    })
    @ParameterizedTest(
            name =
                    "{displayName}: method scopes {0}, method 1 reference {1}, method 2 reference {2}")
    @Disabled
    public void instanceMethodInvokedWithinNonOverridingSubclass_expectNoViolation(
            String scope, String method1Reference, String method2Reference) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + String.format("    %s boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean childMethod() {\n"
                    + String.format("        return %s();\n", method1Reference)
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean grandchildMethod() {\n"
                    + String.format("        return %s();\n", method2Reference)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 2);
    }

    /**
     * If a subclass inherits a method from a parent without overriding it, and that method is
     * called on an instance of the subclass, then the parent method counts as used.
     */
    @CsvSource({
        // Both the child and grandchild classes, for each of the two
        // relevant visibility scopes.
        "public,  ChildClass",
        "protected,  ChildClass",
        "public,  GrandchildClass",
        "protected,  GrandchildClass",
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, invoking class {1}")
    @Disabled
    public void instanceMethodInvokedOnInstanceOfNonOverridingSubclass_expectNoViolation(
            String scope, String subclass) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n}\n",
            "global class GrandchildClass extends ChildClass {\n}\n",
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public boolean invokerMethod(%s instance) {\n", subclass)
                    + "        return instance.parentMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * If a subclass overrides an inherited method, but calls the `super` version of that method,
     * the parent method counts as used.
     */
    @ValueSource(strings = {"public", "protected"})
    @ParameterizedTest(name = "{displayName}: method scope {0}")
    @Disabled
    public void instanceMethodInvokedViaSuperInOverridingSubclass_expectNoViolation(String scope) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + String.format("    %s virtual boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod1() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean childMethod() {\n"
                    // Invoke the super version instead of the override version.
                    + "        return super.parentMethod1();\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod2() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean grandchildMethod() {\n"
                    // Invoke the super version instead of the override version.
                    + "        return super.parentMethod2();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 2);
    }

    /**
     * If a subclass overrides an inherited method and calls its own version of the method rather
     * than the `super`, then the original parent method doesn't count as used.
     */
    @CsvSource({
        // Implicit/explicit `this`, for each of the two relevant
        // visibility scopes.
        "public,  this.parentMethod",
        "protected,  this.parentMethod",
        "public,  parentMethod",
        "protected,  parentMethod",
    })
    @ParameterizedTest(name = "{displayName}: method scope {0}, method reference {1}")
    @Disabled
    public void overrideMethodInSubclass_expectViolation(String scope, String invocation) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // This calls the overrider, not the original method, so it shouldn't count.
                    + String.format("        return %s();\n", invocation)
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // This calls the overrider, not the original method, so it shouldn't count.
                    + String.format("        return %s();\n", invocation)
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("parentMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * If an overridden method is invoked on an instance of a subclass, then the overridden version
     * of the method on the parent doesn't count as used.
     */
    @CsvSource({
        // Child/Grandchild class, for each of the two relevant visibility scopes.
        "public,  ChildClass",
        "protected,  ChildClass",
        "public,  GrandchildClass",
        "protected,  GrandchildClass",
    })
    @ParameterizedTest(name = "{displayName}: method visibility {0}, instance type {1}")
    @Disabled
    public void overrideMethodOnInstanceOfSubclass_expectViolation(
            String scope, String instanceType) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s virtual boolean parentMethod() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public override boolean parentMethod() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n" + "}\n",
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format(
                            "    public boolean invokeMethod(%s instance) {\n", instanceType)
                    + "        return instance.parentMethod();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertViolations(
                sourceCodes,
                v -> {
                    assertEquals("parentMethod", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceVertex().getDefiningType());
                });
    }

    /**
     * If a superclass internally calls an instance method, that counts as invoking all overriding
     * versions of that method on subclasses. Reasoning: This is commonly done with abstract classes
     * especially, but using elsewhere isn't unheard of.
     */
    @CsvSource({
        // All four combinations of implicit/explicit `this` on each method, for each parent type,
        // for each of the two relevant visibility scopes.
        "public,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  VirtualParent,  'this.parentMethod1() && parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && parentMethod2()",
        "public,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  VirtualParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  VirtualParent,  'parentMethod1() && parentMethod2()",
        "protected,  VirtualParent,  'parentMethod1() && parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && parentMethod2()",
        "public,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "protected,  AbstractParent,  'this.parentMethod1() && this.parentMethod2()",
        "public,  AbstractParent,  'parentMethod1() && parentMethod2()",
        "protected,  AbstractParent,  'parentMethod1() && parentMethod2()",
    })
    @ParameterizedTest(name = "{displayName}: Method scope {0}; Parent class {1}; invocation {2}")
    @Disabled
    public void invocationOfOverriddenInstanceMethodInSuperclass_expectNoViolation(
            String scope, String parentClass, String invocation) {
        String[] sourceCodes = {
            // Have a virtual class that defines a set of methods.
            "global virtual class VirtualParent {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    // The super method needs to be at least as visible as its override,
                    // or else the code won't compile.
                    + String.format("    %s virtual boolean parentMethod1() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    // The super method needs to be at least as visible as its override,
                    // or else the code won't compile.
                    + String.format("    %s virtual boolean parentMethod2() {\n", scope)
                    + "        return true;\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // Invoke both virtual methods.
                    + String.format("        return %s;\n", invocation)
                    + "    }\n"
                    + "}\n",
            // Have an abstract class that defines the same set of methods.
            "global abstract class AbstractParent {\n"
                    // The super methods need to be at least as visible as their overrides,
                    // or else the code won't compile.
                    + String.format("    %s abstract boolean parentMethod1();\n", scope)
                    + String.format("    %s abstract boolean parentMethod2();\n", scope)
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public boolean invokerMethod() {\n"
                    // Invoke both abstract methods.
                    + String.format("        return %s() && %s();\n", invocation)
                    + "    }\n"
                    + "}\n",
            // Have a child class that extends one of the two available parents. Make it abstract
            // to guarantee compilation in all test cases.
            String.format("global abstract class ChildClass extends %s {\n", parentClass)
                    // The child class overrides one of the parent methods.
                    + String.format("    %s override boolean parentMethod1() {\n", scope)
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // The grandchild class overrides the other.
                    + String.format("    %s override boolean parentMethod2() {\n", scope)
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 2);
    }

    /**
     * Calling an instance method on a parent class also counts as calling all overrides of that
     * method on subclasses. Reasoning: Particularly with abstract classes, it's frequent for
     * something typed as a parent class to actually be an instance of the child class.
     */
    @ValueSource(strings = {"InterfaceParent", "VirtualParent", "AbstractParent"})
    @ParameterizedTest(name = "{displayName}: Parent class {0}")
    @Disabled
    public void externalInvocationOfOverriddenMethodOnSuperclass_expectNoViolation(
            String parentClass) {
        String[] sourceCodes = {
            // Have an interface that declares some methods.
            "global interface InterfaceParent {\n"
                    + "    public boolean inheritedMethod1();\n"
                    + "    public boolean inheritedMethod2();\n"
                    + "}\n",
            // Have a virtual class that declares the same methods.
            "global virtual class VirtualParent {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public virtual boolean inheritedMethod1() {\n"
                    + "        return true;\n\n"
                    + "    }\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + "    public virtual boolean inheritedMethod2() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n",
            // have an abstract class that declares the same methods.
            "global abstract class AbstractParent {\n"
                    + "    public abstract boolean inheritedMethod1();\n"
                    + "    public abstract boolean inheritedMethod2();\n"
                    + "}\n",
            // Have a child class that extends a specified parent class. Make it abstract
            // to guarantee compilation.
            String.format("global abstract class ChildClass extends %s {\n", parentClass)
                    // Have the child class extend one of the parent methods.
                    + "    public override boolean inheritedMethod1() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            // Have a grandchild class that extends the child class.
            "global class GrandchildClass extends ChildClass {\n"
                    // Have the grandchild class extend the other parent method.
                    + "    public override boolean inheritedMethod2() {\n"
                    + "        return false;\n"
                    + "    }\n"
                    + "}\n",
            // Have an unrelated class that uses an instance of a superclass to invoke a method.
            "global class InvokerClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public boolean doInvocation(%s instance) {\n", parentClass)
                    // Invoke both methods on the instance.
                    + "        return instance.inheritedMethod1() && instance.inheritedMethod2();\n"
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 2);
    }

    /* =============== SECTION 3: CONSTRUCTOR METHODS =============== */

    /**
     * If subclass's constructor calls a `super` constructor, the relevant parent constructor counts
     * as used. (Note: Tests for both explicitly-declared 0-arity and 1-arity constructors.)
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        //        "public,  (),  ()",
        "protected,  (),  ()",
        //        "public,  (boolean b),  (b)",
        "protected,  (boolean b),  (b)"
    })
    @ParameterizedTest(name = "{displayName}: scope {0}; signature{1}")
    public void constructorInvokedViaSuperInSubclass_expectNoViolation(
            String scope, String paramTypes, String invocationArgs) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    + String.format("    %s ParentClass%s {\n", scope, paramTypes)
                    + "    }\n"
                    + "}\n",
            "global class ChildClass extends ParentClass {\n"
                    // Use the engine directive to prevent this method from tripping the rule.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    public ChildClass%s {\n", paramTypes)
                    + String.format("        super%s;\n", invocationArgs)
                    + "    }\n"
                    + "}\n"
        };
        assertNoViolations(sourceCodes, 1);
    }

    /**
     * A class's constructor is only available to its immediate children. If a grandchild class
     * calls `super()` in its constructor, that refers to the child class, not the parent class.
     * (Note: Tests for both explicitly-declared 0-arity and 1-arity constructors.)
     */
    // TODO: Enable subsequent tests as we implement functionality.
    @CsvSource({
        //        "public,  (),  ()",
        "protected,  (),  ()",
        //        "public,  (boolean b),  (b)",
        "protected, (boolean b),  (b)"
    })
    @ParameterizedTest(name = "{displayName}: scope {0}; signature {1}")
    public void superConstructorInvokedInGrandchild_expectViolation(
            String scope, String paramTypes, String invocationArgs) {
        String[] sourceCodes = {
            "global virtual class ParentClass {\n"
                    // Declare a constructor for the parent class.
                    + String.format("    %s ParentClass%s {\n", scope, paramTypes)
                    + "    }\n"
                    + "}\n",
            "global virtual class ChildClass extends ParentClass {"
                    // Give the child class a constructor with the same signature, but
                    // have it do nothing in particular.
                    + String.format("    %s ChildClass%s {\n", scope, paramTypes)
                    + "    }\n"
                    + "}\n",
            "global class GrandchildClass extends ChildClass {\n"
                    // Give the grandchild class a constructor with the same signature, and
                    // have it call the super method.
                    // Annotate it so it is skipped.
                    + "    /* sfge-disable-stack UnusedMethodRule */\n"
                    + String.format("    %s GrandchildClass%s {\n", scope, paramTypes)
                    + String.format("        super%s;\n", invocationArgs)
                    + "    }\n"
                    + "}\n"
        };
        Consumer<Violation.RuleViolation> assertion =
                v -> {
                    assertEquals("<init>", v.getSourceVertexName());
                    assertEquals("ParentClass", v.getSourceDefiningType());
                };
        assertViolations(sourceCodes, assertion);
    }
}
