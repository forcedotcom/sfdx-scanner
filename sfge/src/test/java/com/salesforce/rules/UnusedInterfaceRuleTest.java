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

public class UnusedInterfaceRuleTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @MethodSource("paramProvider_testOuterInterface")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testOuterInterfaceImplementation(String scope, Set<String> expectedDefiningTypes) {
        // This interface is never used by anything.
        String unusedInterfaceSource = scope + " interface UnusedInterface {}";
        // This interface is used.
        String usedInterfaceSource = scope + " interface UsedInterface {}";
        // This is where the interface is used.
        String interfaceUserSource = "global class InterfaceUser implements UsedInterface {}";

        TestUtil.buildGraph(g, unusedInterfaceSource, usedInterfaceSource, interfaceUserSource);
        StaticRule rule = UnusedInterfaceRule.getInstance();
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

    @MethodSource("paramProvider_testOuterInterface")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testOuterInterfaceExtension(String scope, Set<String> expectedDefiningTypes) {
        // This interface is never used by anything.
        String unusedInterfaceSource = scope + " interface UnusedInterface {}";
        // This interface is used.
        String usedInterfaceSource = scope + " interface UsedInterface {}";
        // This is where the interface is used.
        String interfaceUserSource = "global interface InterfaceExtender extends UsedInterface {}";

        TestUtil.buildGraph(g, unusedInterfaceSource, usedInterfaceSource, interfaceUserSource);
        StaticRule rule = UnusedInterfaceRule.getInstance();
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

    @MethodSource("paramProvider_testInnerInterface")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testInnerInterfaceImplementation(String scope, Set<String> expectedDefiningTypes) {
        String innerInterfaceSource =
                "global class HasInnerInterfaces {\n"
                        // This interface is used by another inner class.
                        + scope
                        + " interface InternallyUsedInterface {}\n"
                        + "\n"
                        // This interface is used by an external class.
                        + scope
                        + " interface ExternallyUsedInterface {}\n"
                        + "\n"
                        // This interface is never used.
                        + scope
                        + " interface UnusedInterface {}\n"
                        + "\n"
                        + "global class InnerUser implements InternallyUsedInterface {}\n"
                        + "}";
        String externalUserSource =
                "global class ExternalUser implements HasInnerInterfaces.ExternallyUsedInterface {}";
        TestUtil.buildGraph(g, innerInterfaceSource, externalUserSource);

        StaticRule rule = UnusedInterfaceRule.getInstance();
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

    @MethodSource("paramProvider_testInnerInterface")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testInnerInterfaceExtension(String scope, Set<String> expectedDefiningTypes) {
        String innerInterfaceSource =
                "global class HasInnerInterfaces {\n"
                        // This interface is used by another inner class.
                        + scope
                        + " interface InternallyUsedInterface {}\n"
                        + "\n"
                        // This interface is used by an external class.
                        + scope
                        + " interface ExternallyUsedInterface {}\n"
                        + "\n"
                        // This interface is never used.
                        + scope
                        + " interface UnusedInterface {}\n"
                        + "\n"
                        + "global interface InnerUser extends InternallyUsedInterface {}\n"
                        + "}";
        String externalUserSource =
                "global interface ExternalUser extends HasInnerInterfaces.ExternallyUsedInterface {}";
        TestUtil.buildGraph(g, innerInterfaceSource, externalUserSource);

        StaticRule rule = UnusedInterfaceRule.getInstance();
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

    @MethodSource("paramProvider_testCollidingName")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testCollidingNameImplementation(String scope, Set<String> expectedDefiningTypes) {
        String innerInterfaceSource =
                "global class HasInnerInterfaces {\n"
                        // This name collides with an outer interface. This variant IS USED.
                        + scope
                        + " interface CollidingName1 {}\n"
                        // This name collides with an outer interface. This variant IS USED.
                        + scope
                        + " interface CollidingName2 {}\n"
                        // This name collides with an outer interface. This variant IS NOT USED.
                        + scope
                        + " interface CollidingName3 {}\n"
                        // This is the usage of one of the inner interfaces.
                        + "global class InnerImplementer implements CollidingName1 {}\n"
                        + "}";
        // This name collides with an inner interface. This variant IS NOT USED.
        String outerInterfaceSource1 = scope + " interface CollidingName1 {}";
        // This name collides with an inner interface. This variant IS NOT USED.
        String outerInterfaceSource2 = scope + " interface CollidingName2 {}";
        // This name collides with an inner interface. This variant IS USED.
        String outerInterfaceSource3 = scope + " interface CollidingName3 {}";
        // This is the usage of another inner interface.
        String outerUserSource1 =
                "global class OuterImplementer1 implements HasInnerInterfaces.CollidingName2 {}";
        // This is the usage of an outer interface.
        String outerUserSource2 = "global class OuterImplementer2 implements CollidingName3 {}";
        TestUtil.buildGraph(
                g,
                innerInterfaceSource,
                outerInterfaceSource1,
                outerInterfaceSource2,
                outerInterfaceSource3,
                outerUserSource1,
                outerUserSource2);
        StaticRule rule = UnusedInterfaceRule.getInstance();
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

    @MethodSource("paramProvider_testCollidingName")
    @ParameterizedTest(name = "{displayName}: Scope {0}")
    public void testCollidingNameExtension(String scope, Set<String> expectedDefiningTypes) {
        String innerInterfaceSource =
                "global class HasInnerInterfaces {\n"
                        // This name collides with an outer interface. This variant IS USED.
                        + scope
                        + " interface CollidingName1 {}\n"
                        // This name collides with an outer interface. This variant IS USED.
                        + scope
                        + " interface CollidingName2 {}\n"
                        // This name collides with an outer interface. This variant IS NOT USED.
                        + scope
                        + " interface CollidingName3 {}\n"
                        // This is the usage of one of the inner interfaces.
                        + "global interface InnerExtender extends CollidingName1 {}\n"
                        + "}";
        // This name collides with an inner interface. This variant IS NOT USED.
        String outerInterfaceSource1 = scope + " interface CollidingName1 {}";
        // This name collides with an inner interface. This variant IS NOT USED.
        String outerInterfaceSource2 = scope + " interface CollidingName2 {}";
        // This name collides with an inner interface. This variant IS USED.
        String outerInterfaceSource3 = scope + " interface CollidingName3 {}";
        // This is the usage of another inner interface.
        String outerUserSource1 =
                "global interface OuterExtender1 extends HasInnerInterfaces.CollidingName2 {}";
        // This is the usage of an outer interface.
        String outerUserSource2 = "global interface OuterExtender2 extends CollidingName3 {}";
        TestUtil.buildGraph(
                g,
                innerInterfaceSource,
                outerInterfaceSource1,
                outerInterfaceSource2,
                outerInterfaceSource3,
                outerUserSource1,
                outerUserSource2);
        StaticRule rule = UnusedInterfaceRule.getInstance();
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
    private static Stream<Arguments> paramProvider_testOuterInterface() {
        return Stream.of(
                // Global interfaces should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global", new HashSet<String>()),
                // Public interfaces should be included in consideration.
                Arguments.of(
                        "public", new HashSet<>(Collections.singletonList("UnusedInterface"))));
    }

    private static Stream<Arguments> paramProvider_testInnerInterface() {
        return Stream.of(
                // Global interfaces should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global", new HashSet<String>()),
                // Public interfaces should be included in consideration.
                Arguments.of(
                        "public",
                        new HashSet<>(
                                Collections.singletonList("HasInnerInterfaces.UnusedInterface"))));
    }

    private static Stream<Arguments> paramProvider_testCollidingName() {
        return Stream.of(
                // Global interfaces should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of("global", new HashSet<String>()),
                // Public interfaces should be included in consideration.
                Arguments.of(
                        "public",
                        new HashSet<>(
                                Arrays.asList(
                                        "HasInnerInterfaces.CollidingName3",
                                        "CollidingName1",
                                        "CollidingName2"))));
    }
}
