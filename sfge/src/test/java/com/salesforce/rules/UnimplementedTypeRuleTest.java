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

public class UnimplementedTypeRuleTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @MethodSource("paramProvider_testOuterTypes")
    @ParameterizedTest(name = "{displayName}: Tested type = {0}")
    public void testOuterTypes(
            String inheriteeDef,
            String inheritorDef,
            String inheritance,
            List<String> expectedTypes) {
        // This type is unimplemented.
        String unextendedSource = inheriteeDef + " UnextendedType {}";
        // This type is implemented.
        String extendedSource = inheriteeDef + " ExtendedType {}";
        // This type does the implementing.
        String extenderSource = inheritorDef + " ExtenderType " + inheritance + " ExtendedType {}";

        executeTest(expectedTypes, extendedSource, unextendedSource, extenderSource);
    }

    @MethodSource("paramProvider_testInnerTypes")
    @ParameterizedTest(name = "{displayName}: Tested type = {0}")
    public void testInnerTypes(
            String inheriteeDef,
            String inheritorDef,
            String inheritance,
            List<String> expectedTypes) {
        // spotless:off
        String innerTypeSource =
                "global class HasInnerTypes {\n"
                        // This type is used by an inner type.
                        + inheriteeDef + " InternallyUsed {}\n"
                        // This type is used by an outer type in another file.
                        + inheriteeDef + " ExternallyUsed {}\n"
                        // This type is unused.
                        + inheriteeDef + " Unused {}\n"
                        // This is the internal type that uses another type.
                        + inheritorDef + " InternalUser " + inheritance + " InternallyUsed {}\n"
                        + "}";
        // spotless:on
        // This external type uses another type.
        String externalTypeSource =
                inheritorDef + " ExternalUser " + inheritance + " HasInnerTypes.ExternallyUsed {}";
        executeTest(expectedTypes, innerTypeSource, externalTypeSource);
    }

    @MethodSource("paramProvider_testCollidingNames")
    @ParameterizedTest(name = "{displayName}: Tested type = {0}")
    public void testCollidingNames(
            String inheriteeDef,
            String inheritorDef,
            String inheritance,
            List<String> expectedTypes) {
        // spotless:off
        String innerTypeSource =
                "global class HasInnerTypes {\n"
                        // This name collides with an outer type. This variant IS USED.
                        + inheriteeDef + " CollidingName1 {}\n"
                        // This name collides with an outer type. This variant IS USED.
                        + inheriteeDef + " CollidingName2 {}\n"
                        // This name collides with an outer type. This variant IS NOT USED.
                        + inheriteeDef + " CollidingName3 {}\n"
                        // This is the usage of one of the inner types.
                        + inheritorDef + " InternalUser " + inheritance + " CollidingName1 {}\n"
                        + "}";
        // spotless:on
        // This name collides with an inner type. This variant IS NOT USED.
        String outerTypeSource1 = inheriteeDef + " CollidingName1 {}";
        // This name collides with an inner type. This variant IS NOT USED.
        String outerTypeSource2 = inheriteeDef + " CollidingName2 {}";
        // This name collides with an inner type. This variant IS USED.
        String outerTypeSource3 = inheriteeDef + " CollidingName3 {}";
        // This is the usage of another inner type.
        String outerUserSource1 =
                inheritorDef + " OuterUser1 " + inheritance + " HasInnerTypes.CollidingName2 {}";
        // This is the usage of an outer type.
        String outerUserSource2 =
                inheritorDef + " OuterUser2 " + inheritance + " CollidingName3 {}";
        executeTest(
                expectedTypes,
                innerTypeSource,
                outerTypeSource1,
                outerTypeSource2,
                outerTypeSource3,
                outerUserSource1,
                outerUserSource2);
    }

    // ======= HELPER METHODS/PARAM PROVIDERS =======

    private void executeTest(List<String> expectedTypes, String... sources) {
        // Build the graph
        TestUtil.buildGraph(g, sources);
        // Get and run the rule.
        StaticRule rule = UnimplementedTypeRule.getInstance();
        List<Violation> violations = rule.run(g);

        // Make sure we got the expected number of violations.
        MatcherAssert.assertThat(
                "Wrong number of violations found", violations, hasSize(expectedTypes.size()));
        // Turn the list of actual violations into a set of defining types mentioned by those
        // violations.
        Set<String> actualTypes =
                violations.stream()
                        .map(Violation::getSourceDefiningType)
                        .collect(Collectors.toSet());
        // Make sure each of the expected defining types is present in the set of actual defining
        // types.
        for (String expectedType : expectedTypes) {
            assertTrue(
                    actualTypes.contains(expectedType),
                    String.format("Type %s did not throw expected violation", expectedType));
        }
    }

    private static Stream<Arguments> paramProvider_testOuterTypes() {
        // Arguments are as follows:
        // 1. The type and scope of the thing being implemented (or not implemented).
        // 2. The type and scope of the thing that does the implementing.
        // 3. The keyword by which the implementation is done.
        // 4. A list of types that should throw violations.
        return Stream.of(
                // Global types should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of(
                        "global interface", "global interface", "extends", new ArrayList<String>()),
                Arguments.of(
                        "global interface", "global class", "implements", new ArrayList<String>()),
                Arguments.of(
                        "global abstract class",
                        "global class",
                        "extends",
                        new ArrayList<String>()),
                // Public types should be included in consideration.
                Arguments.of(
                        "public interface",
                        "global interface",
                        "extends",
                        Collections.singletonList("UnextendedType")),
                Arguments.of(
                        "public interface",
                        "global class",
                        "implements",
                        Collections.singletonList("UnextendedType")),
                Arguments.of(
                        "public abstract class",
                        "global class",
                        "extends",
                        Collections.singletonList("UnextendedType")));
    }

    private static Stream<Arguments> paramProvider_testInnerTypes() {
        // Arguments are as follows:
        // 1. The type and scope of the thing being implemented (or not implemented).
        // 2. The type and scope of the thing that does the implementing.
        // 3. The keyword by which the implementation is done.
        // 4. A list of types that should throw violations.
        return Stream.of(
                // Global types should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of(
                        "global interface", "global interface", "extends", new ArrayList<String>()),
                Arguments.of(
                        "global interface", "global class", "implements", new ArrayList<String>()),
                Arguments.of(
                        "global abstract class",
                        "global class",
                        "extends",
                        new ArrayList<String>()),
                // Public types should be included in consideration.
                Arguments.of(
                        "public interface",
                        "global interface",
                        "extends",
                        Collections.singletonList("HasInnerTypes.Unused")),
                Arguments.of(
                        "public interface",
                        "global class",
                        "implements",
                        Collections.singletonList("HasInnerTypes.Unused")),
                Arguments.of(
                        "public abstract class",
                        "global class",
                        "extends",
                        Collections.singletonList("HasInnerTypes.Unused")));
    }

    private static Stream<Arguments> paramProvider_testCollidingNames() {
        // Arguments are as follows:
        // 1. The type and scope of the thing being implemented (or not implemented).
        // 2. The type and scope of the thing that does the implementing.
        // 3. The keyword by which the implementation is done.
        // 4. A list of types that should throw violations.
        return Stream.of(
                // Global types should be excluded from consideration,
                // since they're accessible to other packages.
                Arguments.of(
                        "global interface", "global interface", "extends", new ArrayList<String>()),
                Arguments.of(
                        "global interface", "global class", "implements", new ArrayList<String>()),
                Arguments.of(
                        "global abstract class",
                        "global class",
                        "extends",
                        new ArrayList<String>()),
                // Public types should be included in consideration.
                Arguments.of(
                        "public interface",
                        "global interface",
                        "extends",
                        Arrays.asList(
                                "HasInnerTypes.CollidingName3",
                                "CollidingName1",
                                "CollidingName2")),
                Arguments.of(
                        "public interface",
                        "global class",
                        "implements",
                        Arrays.asList(
                                "HasInnerTypes.CollidingName3",
                                "CollidingName1",
                                "CollidingName2")),
                Arguments.of(
                        "public abstract class",
                        "global class",
                        "extends",
                        Arrays.asList(
                                "HasInnerTypes.CollidingName3",
                                "CollidingName1",
                                "CollidingName2")));
    }
}
