package com.salesforce.rules;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UnextendedAbstractClassRuleTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @MethodSource("paramProvider_testOuterClasses")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testOuterClasses(String scope, List<String> expectedDefiningTypes) {
        // This class is never extended.
        String unextendedSource = scope + " abstract class UnextendedClass {}";
        // This class is extended.
        String extendedSource = scope + " abstract class ExtendedClass {}";
        // This class does the extension.
        String extenderSource = "public class ExtenderClass extends ExtendedClass {}";

        executeTest(expectedDefiningTypes, unextendedSource, extendedSource, extenderSource);
    }

    @MethodSource("paramProvider_testInnerClasses")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testInnerClasses(String scope, List<String> expectedDefiningTypes) {
        String innerClassSource =
                "global class HasInnerClasses {\n"
                        + scope
                        + " abstract class InternallyUsedClass {}\n"
                        + scope
                        + " abstract class ExternallyUsedClass {}\n"
                        + scope
                        + " abstract class UnusedClass {}\n"
                        + "public class InternalUserClass extends InternallyUsedClass {}\n"
                        + "}\n";
        String externalUserSource =
                "public class ExternalUserClass extends HasInnerClasses.ExternallyUsedClass {}\n";
        executeTest(expectedDefiningTypes, innerClassSource, externalUserSource);
    }

    @MethodSource("paramProvider_testCollidingNames")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testCollidingNames(String scope, List<String> expectedDefiningTypes) {
        String innerClassSource =
                "global class HasInnerClasses {\n"
                        // This name collides with an outer class. This variant IS USED.
                        + scope
                        + " abstract class CollidingName1 {}\n"
                        // This name collides with an outer class. This variant IS USED.
                        + scope
                        + " abstract class CollidingName2 {}\n"
                        // This name collides with an outer class. This variant IS NOT USED.
                        + scope
                        + " abstract class CollidingName3 {}\n"
                        // This is the usage of one of the inner classes.
                        + "global class InnerExtender extends CollidingName1 {}\n"
                        + "}";
        // This name collides with an inner class. This variant IS NOT USED.
        String outerClassSource1 = scope + " abstract class CollidingName1 {}";
        // This name collides with an inner class. This variant IS NOT USED.
        String outerClassSource2 = scope + " abstract class CollidingName2 {}";
        // This name collides with an inner class. This variant IS USED.
        String outerClassSource3 = scope + " abstract class CollidingName3 {}";
        // This is the usage of another inner class.
        String outerUserSource1 =
                "public class OuterExtender1 extends hasInnerClasses.CollidingName2 {}";
        // This is the usage of an outer class.
        String outerUserSource2 = "public class OuterExtender2 extends CollidingName3 {}";
        executeTest(
                expectedDefiningTypes,
                innerClassSource,
                outerClassSource1,
                outerClassSource2,
                outerClassSource3,
                outerUserSource1,
                outerUserSource2);
    }

    // ======= HELPER METHODS/PARAM PROVIDERS =======

    private void executeTest(List<String> expectedDefiningTypes, String... sources) {
        // Build the graph.
        TestUtil.buildGraph(g, sources);
        // Get and run the rule.
        StaticRule rule = UnextendedAbstractClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        // Make sure we got the expected number of violations.
        MatcherAssert.assertThat(
                "Wrong number of violations found",
                violations,
                hasSize(expectedDefiningTypes.size()));
        // Turn the list of actual violations into a set of defining types mentioned by those
        // violations.
        Set<String> actualDefiningTypes =
                violations.stream()
                        .map(Violation::getSourceDefiningType)
                        .collect(Collectors.toSet());
        // Make sure each of the expected defining types is present in the set of actual defining
        // types.
        for (String expectedDefiningType : expectedDefiningTypes) {
            assertTrue(
                    actualDefiningTypes.contains(expectedDefiningType),
                    String.format(
                            "Class %s did not throw expected violation", expectedDefiningType));
        }
    }

    private static Stream<Arguments> paramProvider_testOuterClasses() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global abstract", new ArrayList<String>()),
                // Public classes should be included in consideration.
                Arguments.of("public abstract", Collections.singletonList("UnextendedClass")));
    }

    private static Stream<Arguments> paramProvider_testInnerClasses() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global", new ArrayList<String>()),
                // Public classes should be included in consideration.
                Arguments.of("public", Collections.singletonList("HasInnerClasses.UnusedClass")));
    }

    private static Stream<Arguments> paramProvider_testCollidingNames() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global", new ArrayList<String>()),
                // Public classes should be included in consideration.
                Arguments.of(
                        "public",
                        Arrays.asList(
                                "HasInnerClasses.CollidingName3",
                                "CollidingName1",
                                "CollidingName2")));
    }
}
