package com.salesforce.rules;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import java.util.*;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UnnecessarilyExtensibleClassRuleTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @MethodSource("paramProvider_testOuterClasses")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testOuterClasses(String scope, Set<String> expectedDefiningTypes) {
        // This class is never extended.
        String unextendedSource = scope + " class UnextendedClass {}";
        // This class is extended.
        String extendedSource = scope + " class ExtendedClass {}";
        // This class does the extension.
        String extenderSource = "public class ExtenderClass extends ExtendedClass {}";

        TestUtil.buildGraph(g, unextendedSource, extendedSource, extenderSource);

        StaticRule rule = UnnecessarilyExtensibleClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(
                "Wrong number of violations found",
                violations,
                hasSize(expectedDefiningTypes.size()));
        for (int i = 0; i < expectedDefiningTypes.size(); i++) {
            String actual = violations.get(i).getSourceDefiningType();
            assertTrue(
                    expectedDefiningTypes.contains(actual),
                    String.format("%s should not be unextended", actual));
        }
    }

    @MethodSource("paramProvider_testInnerClasses")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testInnerClasses(String scope, Set<String> expectedDefiningTypes) {
        String innerClassSource =
                "global class HasInnerClasses {\n"
                        + scope
                        + " class InternallyUsedClass {}\n"
                        + scope
                        + " class ExternallyUsedClass {}\n"
                        + scope
                        + " class UnusedClass {}\n"
                        + "public class InternalUserClass extends InternallyUsedClass {}\n"
                        + "}\n";
        String externalUserSource =
                "public class ExternalUserClass extends HasInnerClasses.ExternallyUsedClass {}\n";
        TestUtil.buildGraph(g, innerClassSource, externalUserSource);

        StaticRule rule = UnnecessarilyExtensibleClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(
                "Wrong number of violations found",
                violations,
                hasSize(expectedDefiningTypes.size()));
        for (int i = 0; i < expectedDefiningTypes.size(); i++) {
            String actual = violations.get(i).getSourceDefiningType();
            assertTrue(
                    expectedDefiningTypes.contains(actual),
                    String.format("%s should not be unextended", actual));
        }
    }

    @MethodSource("paramProvider_testCollidingNames")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testCollidingNames(String scope, Set<String> expectedDefiningTypes) {
        String innerClassSource =
                "global class HasInnerClasses {\n"
                        // This name collides with an outer class. This variant IS USED.
                        + scope
                        + " class CollidingName1 {}\n"
                        // This name collides with an outer class. This variant IS USED.
                        + scope
                        + " class CollidingName2 {}\n"
                        // This name collides with an outer class. This variant IS NOT USED.
                        + scope
                        + " class CollidingName3 {}\n"
                        // This is the usage of one of the inner classes.
                        + "global class InnerExtender extends CollidingName1 {}\n"
                        + "}";
        // This name collides with an inner class. This variant IS NOT USED.
        String outerClassSource1 = scope + " class CollidingName1 {}";
        // This name collides with an inner class. This variant IS NOT USED.
        String outerClassSource2 = scope + " class CollidingName2 {}";
        // This name collides with an inner class. This variant IS USED.
        String outerClassSource3 = scope + " class CollidingName3 {}";
        // This is the usage of another inner class.
        String outerUserSource1 =
                "public class OuterExtender1 extends hasInnerClasses.CollidingName2 {}";
        // This is the usage of an outer class.
        String outerUserSource2 = "public class OuterExtender2 extends CollidingName3 {}";
        TestUtil.buildGraph(
                g,
                innerClassSource,
                outerClassSource1,
                outerClassSource2,
                outerClassSource3,
                outerUserSource1,
                outerUserSource2);

        StaticRule rule = UnnecessarilyExtensibleClassRule.getInstance();
        List<Violation> violations = rule.run(g);

        MatcherAssert.assertThat(
                "Wrong number of violations found",
                violations,
                hasSize(expectedDefiningTypes.size()));
        for (int i = 0; i < expectedDefiningTypes.size(); i++) {
            String actual = (violations.get(i)).getSourceDefiningType();
            assertTrue(
                    expectedDefiningTypes.contains(actual),
                    String.format("%s should not be unused", actual));
        }
    }

    // ======= HELPER METHODS/PARAM PROVIDERS =======
    private static Stream<Arguments> paramProvider_testOuterClasses() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global virtual", new HashSet<String>()),
                Arguments.of("global abstract", new HashSet<String>()),
                // Public classes should be included in consideration.
                Arguments.of(
                        "public virtual",
                        new HashSet<>(Collections.singletonList("UnextendedClass"))),
                Arguments.of(
                        "public abstract",
                        new HashSet<>(Collections.singletonList("UnextendedClass"))));
    }

    private static Stream<Arguments> paramProvider_testInnerClasses() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global virtual", new HashSet<String>()),
                Arguments.of("global abstract", new HashSet<String>()),
                // Public classes should be included in consideration.
                Arguments.of(
                        "public virtual",
                        new HashSet<>(Collections.singletonList("HasInnerClasses.UnusedClass"))),
                Arguments.of(
                        "public abstract",
                        new HashSet<>(Collections.singletonList("HasInnerClasses.UnusedClass"))));
    }

    private static Stream<Arguments> paramProvider_testCollidingNames() {
        return Stream.of(
                // Global classes should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global virtual", new HashSet<String>()),
                Arguments.of("global abstract", new HashSet<String>()),
                // Public classes should be included in consideration.
                Arguments.of(
                        "public virtual",
                        new HashSet<>(
                                Arrays.asList(
                                        "HasInnerClasses.CollidingName3",
                                        "CollidingName1",
                                        "CollidingName2"))),
                Arguments.of(
                        "public abstract",
                        new HashSet<>(
                                Arrays.asList(
                                        "HasInnerClasses.CollidingName3",
                                        "CollidingName1",
                                        "CollidingName2"))));
    }
}
