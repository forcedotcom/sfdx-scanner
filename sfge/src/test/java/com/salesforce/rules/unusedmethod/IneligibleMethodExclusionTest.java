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
public class IneligibleMethodExclusionTest extends BaseUnusedMethodTest {

    /* ============ SECTION 1: CONSTRUCTORS ============ */
    /**
     * When no constructor is declared on a class, a no-parameter constructor is implicitly
     * generated. To reduce noise, this should always count as used.
     */
    @Test
    public void impliedConstructorWithoutInvocation_expectIneligible() {
        String sourceCode = "public class MyClass {}";
        assertMethodIneligibility(sourceCode, "MyClass", "<init>", 1);
    }

    /**
     * When a class has a private, 0-arity constructor, that constructor is ineligible for analysis.
     * This pattern is common for utility classes with static methods, and we want to minimize false
     * positives.
     */
    @Test
    public void privateArity0ConstructorWithoutInvocation_expectIneligible() {
        String sourceCode =
                "public class MyClass {\n" + "    private MyClass() {\n" + "    }\n" + "}\n";
        assertMethodIneligibility(sourceCode, "MyClass", "<init>", 2);
    }

    /* ============ SECTION 2: ENGINE DIRECTIVES ============ */
    /**
     * Template for test cases about using engine directive. Has the following wildcards:
     *
     * <ol>
     *   <li>%s for optionally adding a class-level directive
     *   <li>%s for optionally adding a line-level directive to {@code staticMethod()}
     *   <li>%s for optionally adding a line-level directive to {@code instanceMethod()}
     *   <li>%s for optionally adding a line-level directive to the constructor
     * </ol>
     */
    // spotless:off
    private static final String DIRECTIVE_TEMPLATE =
        // Add a space for a class-level directive.
        "%s\n"
      + "public class MyClass {\n"
        // A static method, with a space for an annotation.
      + "    %s\n"
      + "    public static boolean staticMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
        // An instance method, with a space for an annotation.
      + "    %s\n"
      + "    public boolean instanceMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
        // A constructor, with a space for an annotation.
      + "    %s\n"
      + "    public MyClass() {}\n"
      + "}";
    // spotless:on

    /**
     * Tests verifying that the line-level directives (e.g. {@code sfge-disable-stack} and {@code
     * sfge-disable-next-line}) cause otherwise eligible vertices to be excluded from analysis.
     *
     * @param directive - The specific directive to use
     */
    @ValueSource(
            strings = {
                "sfge-disable-stack",
                // "sfge-disable-next-line" // TODO: FIX AND ENABLE THIS TEST
            })
    @ParameterizedTest(name = "{displayName}: Directive {0}")
    public void applyLineLevelDirective_expectNoAnalysis(String directive) {
        String directiveLine = "/* " + directive + " UnusedMethodRule */";
        String sourceCode =
                String.format(
                        DIRECTIVE_TEMPLATE,
                        // No class-level directive
                        "",
                        // Use the directive at every opportunity.
                        directiveLine,
                        directiveLine,
                        directiveLine);
        assertMethodIneligibility(
                sourceCode,
                new String[] {"MyClass", "MyClass", "MyClass"},
                new String[] {"staticMethod", "instanceMethod", "<init>"},
                new int[] {4, 9, 14});
    }

    /**
     * Test verifying that applying the {@code sfge-disable} annotation to a class excludes
     * otherwise eligible methods in that class.
     */
    @Test
    public void applyClassLevelDirective_expectNoAnalysis() {
        String sourceCode =
                String.format(
                        DIRECTIVE_TEMPLATE, "/* sfge-disable UnusedMethodRule */", "", "", "");
        assertMethodIneligibility(
                sourceCode,
                new String[] {"MyClass", "MyClass", "MyClass"},
                new String[] {"staticMethod", "instanceMethod", "<init>"},
                new int[] {4, 9, 14});
    }

    /* =============== SECTION 3: PATH ENTRY POINTS =============== */
    // REASONING: Public-facing entry points probably aren't explicitly invoked within the codebase,
    //            but we must assume they're used by external sources.

    /** Global methods are entrypoints, and should count as used. */
    @ValueSource(strings = {"global", "global static"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void globalMethod_expectNoAnalysis(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    %s boolean someMethod() {\n", annotation)
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertMethodIneligibility(sourceCode, "MyClass", "someMethod", 2);
    }

    /** public methods on controllers are entrypoints, and should count as used. */
    @Test
    public void publicControllerMethod_expectNoAnalysis() {
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
            assertMethodIneligibility(sourceCode, "MyController", "getSomeProperty", 2);
        } finally {
            MetaInfoCollectorTestProvider.removeVisualForceHandler();
        }
    }

    /** Methods returning PageReferences are entrypoints, and should count as used. */
    @Test
    public void pageReferenceMethod_expectNoAnalysis() {
        String sourceCode =
                "global class MyClass {\n"
                        + "    private PageReference someMethod() {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertMethodIneligibility(sourceCode, "MyClass", "someMethod", 2);
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
    public void annotatedMethod_expectNoAnalysis(String annotation) {
        String sourceCode =
                "global class MyClass {\n"
                        + String.format("    @%s\n", annotation)
                        + "    private boolean someMethod() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n";
        assertMethodIneligibility(sourceCode, "MyClass", "someMethod", 3);
    }

    /**
     * If a class implements Messaging.InboundEmailHandler, its handleInboundEmail() method is an
     * entrypoint, and should count as used.
     */
    @Test
    public void emailHandlerMethod_expectNoAnalysis() {
        String sourceCode =
                "global class MyClass implements Messaging.InboundEmailhandler {\n"
                        + "    private Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {\n"
                        + "        return null;\n"
                        + "    }\n"
                        + "}\n";
        assertMethodIneligibility(sourceCode, "MyClass", "handleInboundEmail", 2);
    }

    /* =============== SECTION 4: PROPERTY GETTERS AND SETTERS =============== */
    // REASONING: Public setters are often used by Visualforce, and private setters are often
    //            declared to prevent a variable from being modified entirely.

    @Test
    public void getterSetterDeclaration_expectNoAnalysis() {
        String sourceCode =
                "global class MyClass {\n"
                        + "    public Boolean someProperty {\n"
                        + "        get {\n"
                        + "            return this.someProperty;\n"
                        + "        }\n"
                        + "        private set;\n"
                        + "    }\n"
                        + "}\n";
        assertMethodIneligibility(
                sourceCode,
                new String[] {"MyClass", "MyClass"},
                new String[] {"__sfdc_someProperty", "__sfdc_someProperty"},
                new int[] {2, 2});
    }

    /* =============== SECTION 5: ABSTRACT METHOD DECLARATION =============== */
    // REASONING: If an interface/class has abstract methods, then subclasses must implement
    //            those methods for the code to compile. And if the interface/class isn't
    //            implemented anywhere, we have separate rules for surfacing that.

    /** Abstract methods on abstract classes/interfaces are abstract, and count as used. */
    @Test
    public void abstractMethodDeclaration_expectNoAnalysis() {
        String[] sourceCodes = {
            "global abstract class AbstractWithPublic {\n"
                    + "    public abstract boolean someMethod();\n"
                    + "}\n",
            "global abstract class AbstractWithProtected {\n"
                    + "    protected abstract boolean someMethod();\n"
                    + "}\n",
            "global interface MyInterface {\n" + "    boolean anotherMethod();\n" + "}\n"
        };
        assertMethodIneligibility(
                sourceCodes,
                new String[] {"AbstractWithPublic", "AbstractWithProtected", "MyInterface"},
                new String[] {"someMethod", "someMethod", "anotherMethod"},
                new int[] {2, 2, 2});
    }
}
