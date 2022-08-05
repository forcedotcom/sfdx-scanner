package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.Schema;
import com.salesforce.metainfo.MetaInfoCollectorTestProvider;
import com.salesforce.metainfo.VisualForceHandlerImpl;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Some methods shouldn't even be considered eligible candidates for analysis. These tests make sure
 * that this behavior is preserved.
 */
public class UnusedMethodRuleTest_IneligibleMethodExclusion extends UnusedMethodRuleTest_BaseClass {

    /* ============ SECTION 1: IMPLICIT CONSTRUCTOR ============ */
    /**
     * When no constructor is declared on a class, a no-parameter constructor is implicitly
     * generated. To reduce noise, this should always count as used.
     */
    @Test
    public void impliedConstructorWithoutInvocation_expectNoViolation() {
        String sourceCode = "public class MyClass {}";
        assertNoAnalysis(sourceCode);
    }

    /* ============ SECTION 2: ENGINE DIRECTIVES ============ */
    @Test
    public void applySkipStack_expectNoViolation() {
        String sourceCode =
                "public class MyClass {\n"
                        // Unused static method, annotated with the directive.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private static boolean unusedStaticMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused instance method, annotated with the directive.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private boolean unusedInstanceMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused constructor, annotated with the method.
                        + "    /* sfge-disable-stack UnusedMethodRule */\n"
                        + "    private MyClass () {\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    @Test
    public void applySkipClassDirective_expectNoViolation() {
        String sourceCode =
                "/* sfge-disable UnusedMethodRule */\n"
                        + "public class MyClass {\n"
                        // Unused static method
                        + "    private static boolean unusedStaticMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused instance method
                        + "    private boolean unusedInstanceMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        // Unused constructor
                        + "    private MyClass () {\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /* =============== SECTION 3: PATH ENTRY POINTS =============== */
    // REASONING: Public-facing entry points probably aren't explicitly invoked within the codebase,
    //            but we must assume they're used by external sources.

    /** Global methods are entrypoints, and should count as used. */
    @ValueSource(strings = {"global", "global static"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void globalMethod_expectNoViolation(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean someMethod() {\n", annotation)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /** public methods on controllers are entrypoints, and should count as used. */
    @Test
    public void publicControllerMethod_expectNoViolation() {
        try {
            String sourceCode =
                    "global class MyController {\n"
                            + "    public String getSomeProperty() {\n"
                            + "        return 'beep';\n"
                            + "    }\n"
                            + "}\n";

            MetaInfoCollectorTestProvider.setVisualForceHandler(
                    new VisualForceHandlerImpl() {
                        private final TreeSet<String> references =
                                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

                        @Override
                        public void loadProjectFiles(List<String> sourceFolders) {
                            // NO-OP
                        }

                        @Override
                        public TreeSet<String> getMetaInfoCollected() {
                            references.add("MyController");
                            return references;
                        }
                    });
            assertNoAnalysis(sourceCode);
        } finally {
            MetaInfoCollectorTestProvider.removeVisualForceHandler();
        }
    }

    /** Methods returning PageReferences are entrypoints, and should count as used. */
    @Test
    public void pageReferenceMethod_expectNoViolation() {
        String sourceCode =
                "global class MyClass {\n"
                        + "    private PageReference someMethod() {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /** Certain annotated methods are entrypoints, and should count as used. */
    @ValueSource(
            strings = {
                Schema.AURA_ENABLED,
                Schema.INVOCABLE_METHOD,
                Schema.REMOTE_ACTION,
                Schema.NAMESPACE_ACCESSIBLE
            })
    @ParameterizedTest(name = "{displayName}: {0}")
    public void annotatedMethod_expectNoViolation(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    @%s\n", annotation)
                        + "    private boolean someMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /**
     * If a class implements Messaging.InboundEmailHandler, its handleInboundEmail() method is an
     * entrypoint, and should count as used.
     */
    @Test
    public void emailHandlerMethod_expectNoViolation() {
        String sourceCode =
                "global class MyClass implements Messaging.InboundEmailhandler {\n"
                        + "    private Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /* =============== SECTION 4: PROPERTY GETTERS AND SETTERS =============== */
    // REASONING: Public setters are often used by Visualforce, and private setters are often
    //            declared to prevent a variable from being modified entirely.

    @Test
    public void getterSetterDeclaration_expectNoViolation() {
        String sourceCode =
                "global class MyClass {\n"
                        + "    public Boolean someProperty {\n"
                        + "        get {\n"
                        + "            return this.someProperty;\n"
                        + "        }\n"
                        + "        private set;\n"
                        + "    }\n"
                        + "}\n";
        assertNoAnalysis(sourceCode);
    }

    /* =============== SECTION 5: ABSTRACT METHOD DECLARATION =============== */
    // REASONING: If an interface/class has abstract methods, then subclasses must implement
    //            those methods for the code to compile. And if the interface/class isn't
    //            implemented anywhere, we have separate rules for surfacing that.

    /** Abstract methods on abstract classes/interfaces are abstract, and count as used. */
    @Test
    public void abstractMethodDeclaration_expectNoViolation() {
        String[] sourceCodes = {
            "global abstract class AbstractWithPublic {\n"
                    + "    public abstract boolean someMethod();\n"
                    + "}\n",
            "global abstract class AbstractWithProtected {\n"
                    + "    protected abstract boolean someMethod();\n"
                    + "}\n",
            "global interface MyInterface {\n" + "    boolean anotherMethod();\n" + "}\n"
        };
        assertNoAnalysis(sourceCodes);
    }
}
