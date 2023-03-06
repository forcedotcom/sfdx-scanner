package com.salesforce.graph.ops.expander;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestRunner;
import com.salesforce.TestUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ApexPathVertexMetaInfo;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.Constraint;
import com.salesforce.graph.vertex.AbstractVisitingVertexPredicate;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import com.salesforce.matchers.TestRunnerListMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests paths that are collapsible using only the {@link NullApexValueConstrainer}.
 * NullApexValueConstrainer always requires {@link BooleanValuePathConditionExcluder}. It's the
 * excluder that consumes the constraints and collapses the paths.
 */
public class NullApexValueConstrainerTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    public static Stream<Arguments> testPathsAreCollapsedBasedOnSameNullComparison() {
        return Stream.of(
                Arguments.of("String", "x == null"),
                Arguments.of("String", "null == x"),
                Arguments.of("String", "x != null"),
                Arguments.of("String", "null != x"),
                Arguments.of("MyObject__c", "x.MyField__c == null"));
    }

    /**
     * Verifies that the same comparison on an indeterminant value results in the paths being
     * collapsed. This path would normally return 4 paths. {@link NullApexValueConstrainer} adds a
     * constraint to 'x' after the first if/else statement. The path that follows the ifBranch adds
     * a Positive {@link Constraint#Null} to x, the else branch adds the Negative constraint. This
     * allows the {@link BooleanValuePathConditionExcluder} to correctly exclude the paths in the
     * second if that don't make sense.
     */
    // TODO: This is currently testing the combination of the Constrainer and Excluder. Write
    // Constrainer only tests
    @MethodSource
    @ParameterizedTest(name = "{displayName}: comparison=({0}, {1})")
    public void testPathsAreCollapsedBasedOnSameNullComparison(
            String typeDeclaration, String comparison) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething("
                    + typeDeclaration
                    + " x) {\n"
                    + "       if ("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "       if ("
                    + comparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "       x = 'Hello';\n"
                    + "       System.debug(x);\n"
                    + "    }\n"
                    + "}\n"
        };

        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        return vertex instanceof StandardConditionVertex;
                    }
                };

        // Create a config that will track StandardCondition vertices
        ApexPathExpanderConfig expanderConfig =
                getApexPathExpanderConfigBuilder().withVertexPredicate(predicate).build();

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

            ApexStringValue value;

            // Validate the constraints based on which path was taken
            ApexPath path = result.getPath();
            ApexPathVertexMetaInfo metaInfo = path.getApexPathMetaInfo().get();
            MatcherAssert.assertThat(metaInfo.getAllMatches(), hasSize(equalTo(2)));
            String expected;
            if (metaInfo.getMatches(StandardConditionVertex.Positive.class).size() == 2) {
                expected = "ifBranch";
            } else if (metaInfo.getMatches(StandardConditionVertex.Negative.class).size() == 2) {
                expected = "elseBranch";
            } else {
                throw new UnexpectedException("Expecting two standard conditions");
            }

            for (int i = 0; i < 2; i++) {
                value = (ApexStringValue) visitor.getAllResults().get(i).get();
                MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo(expected));
            }

            // Validate after it is assigned
            value = (ApexStringValue) visitor.getAllResults().get(2).get();
            MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
            MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Hello"));
        }
    }

    public static Stream<Arguments> testPathsAreCollapsedBasedOnDifferentNullComparison() {
        return Stream.of(
                Arguments.of("x == null", "x != null"),
                Arguments.of("null == x", "null != x"),
                Arguments.of("x != null", "x == null"),
                Arguments.of("null != x", "null == x"));
    }

    /**
     * Verifies that different comparisons on an indeterminant value results in the paths being
     * collapsed. This differs from the previous test in that the if/else pairs use different
     * comparisons. The path should include follow the first-if->second-else or
     * first-else->second-if paths.
     */
    @MethodSource
    @ParameterizedTest(name = "{displayName}: comparison=({0})")
    public void testPathsAreCollapsedBasedOnDifferentNullComparison(
            String firstComparison, String secondComparison) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething(String x) {\n"
                    + "       if ("
                    + firstComparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "       if ("
                    + secondComparison
                    + ") {\n"
                    + "           System.debug('ifBranch');\n"
                    + "       } else {\n"
                    + "           System.debug('elseBranch');\n"
                    + "       }\n"
                    + "       x = 'Hello';\n"
                    + "       System.debug(x);\n"
                    + "    }\n"
                    + "}\n"
        };

        VertexPredicate predicate =
                new AbstractVisitingVertexPredicate() {
                    @Override
                    public boolean test(BaseSFVertex vertex) {
                        return vertex instanceof StandardConditionVertex;
                    }
                };

        // Create a config that will track StandardCondition vertices
        ApexPathExpanderConfig expanderConfig =
                getApexPathExpanderConfigBuilder().withVertexPredicate(predicate).build();

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(results, hasSize(equalTo(2)));

        for (TestRunner.Result<SystemDebugAccumulator> result : results) {
            SystemDebugAccumulator visitor = result.getVisitor();
            MatcherAssert.assertThat(visitor.getAllResults(), hasSize(equalTo(3)));

            ApexStringValue value;

            // Validate the constraints based on which path was taken
            ApexPath path = result.getPath();
            ApexPathVertexMetaInfo metaInfo = path.getApexPathMetaInfo().get();
            MatcherAssert.assertThat(metaInfo.getAllMatches(), hasSize(equalTo(2)));
            List<String> expected;

            if (metaInfo.getMatches(StandardConditionVertex.Positive.class).size() != 1) {
                throw new UnexpectedException("Expecting one positive conditions");
            }

            if (metaInfo.getMatches(StandardConditionVertex.Negative.class).size() != 1) {
                throw new UnexpectedException("Expecting one negative conditions");
            }

            ApexPathVertexMetaInfo.PredicateMatch predicateMatch =
                    metaInfo.getMatches(StandardConditionVertex.Positive.class).get(0);
            StandardConditionVertex positiveStandardCondition =
                    (StandardConditionVertex) predicateMatch.getPathVertex().getVertex();
            // The expected output depends on whether the first StandardCondition indicates the
            // positive evaluation
            if (positiveStandardCondition.getBeginLine().equals(4)) {
                expected = Arrays.asList("ifBranch", "elseBranch");
            } else if (positiveStandardCondition.getBeginLine().equals(9)) {
                expected = Arrays.asList("elseBranch", "ifBranch");
            } else {
                throw new UnexpectedException(positiveStandardCondition);
            }

            for (int i = 0; i < 2; i++) {
                value = (ApexStringValue) visitor.getAllResults().get(i).get();
                MatcherAssert.assertThat(
                        TestUtil.apexValueToString(value), equalTo(expected.get(i)));
            }

            // Validate after it is assigned
            value = (ApexStringValue) visitor.getAllResults().get(2).get();
            MatcherAssert.assertThat(value.isIndeterminant(), equalTo(false));
            MatcherAssert.assertThat(TestUtil.apexValueToString(value), equalTo("Hello"));
        }
    }

    /**
     * This code would normally have (2^(number-ifs))^2 paths. Collapsing the paths using
     * constraints reduces this to 2^(number-ifs)
     */
    @CsvSource({"true, 4", "false, 16"})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testPreviouslySetCustomSettingPathIsCollapsed(
            boolean withCollapser, int expectedPaths) {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    private static String namespace;\n"
                    + "    public static void doSomething() {\n"
                    + "       MySettings__c ms = MySettings__c.getOrgDefaults();\n"
                    + "       configureMySettings(ms);\n"
                    + "       configureMySettings(ms);\n"
                    + "    }\n"
                    + "    public static void configureMySettings(MySettings__c ms) {\n"
                    + "       if (ms.MyInt1__c == null) {\n"
                    + "           ms.MyInt1__c = 10;\n"
                    + "       }\n"
                    + "       if (ms.MyInt2__c == null) {\n"
                    + "           ms.MyInt2__c = 10;\n"
                    + "       }\n"
                    + "    }\n"
                    + "}\n"
        };

        ApexPathExpanderConfig expanderConfig;
        if (withCollapser) {
            expanderConfig = getApexPathExpanderConfig();
        } else {
            expanderConfig = ApexPathUtil.getSimpleExpandingConfig();
        }

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(results, hasSize(equalTo(expectedPaths)));
    }

    static Stream<Arguments> generateForDataProperty() {
        return Stream.of(
                Arguments.of(true, 1, new String[] {"ifPath"}),
                Arguments.of(false, 2, new String[] {"ifPath", "elsePath"}));
    }

    @MethodSource("generateForDataProperty")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testDataPropertyNotSet(boolean withCollapser, int expectedPaths, String[] values) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		Account acc = new Account();\n"
                        + "		if (acc.Name == null) {\n"
                        + // Name is not set explicitly
                        "			System.debug('ifPath');\n"
                        + "		} else {\n"
                        + "			System.debug('elsePath');\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        verifyPaths(withCollapser, expectedPaths, values, sourceCode);
    }

    @MethodSource("generateForDataProperty")
    @ParameterizedTest(name = "{displayName}: {0}")
    @Disabled // TODO: handle properties on NewObjectExpression
    public void testDataPropertyNotSet_Assignment(
            boolean withCollapser, int expectedPaths, String[] values) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		Account acc = new Account();\n"
                        + "		String s = acc.Name;\n"
                        + // Name is not set explicitly
                        "		if (s == null) {\n"
                        + "			System.debug('ifPath');\n"
                        + "		} else {\n"
                        + "			System.debug('elsePath');\n"
                        + "		}\n"
                        + "	}\n"
                        + "}\n";

        verifyPaths(withCollapser, expectedPaths, values, sourceCode);
    }

    static Stream<Arguments> generateForSoql() {
        return Stream.of(
                Arguments.of(true, 1, new String[] {"elsePath"}),
                Arguments.of(false, 2, new String[] {"ifPath", "elsePath"}));
    }

    @MethodSource("generateForSoql")
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testNullCheckOnSoqlResults(
            boolean withCollapser, int expectedPaths, String[] values) {
        String sourceCode =
                "public class MyClass {\n"
                        + "	void doSomething() {\n"
                        + "		List<Account> acc = [SELECT Id, Name FROM Account];\n"
                        + "		if (acc == null) {\n"
                        + "			System.debug('ifPath');\n"
                        + "		} else {"
                        + "			System.debug('elsePath');\n"
                        + "		}"
                        + "	}\n"
                        + "}\n";

        verifyPaths(withCollapser, expectedPaths, values, sourceCode);
    }

    private void verifyPaths(
            boolean withCollapser, int expectedPaths, String[] values, String sourceCode) {
        ApexPathExpanderConfig expanderConfig;
        if (withCollapser) {
            expanderConfig = getApexPathExpanderConfig();
        } else {
            expanderConfig = ApexPathUtil.getSimpleExpandingConfig();
        }

        List<TestRunner.Result<SystemDebugAccumulator>> results =
                TestRunner.get(g, sourceCode).withExpanderConfig(expanderConfig).walkPaths();
        MatcherAssert.assertThat(results, hasSize(equalTo(expectedPaths)));

        // Verify test data's validity
        MatcherAssert.assertThat(values.length, equalTo(expectedPaths));
        MatcherAssert.assertThat(results, TestRunnerListMatcher.hasValuesAnyOrder(values));
    }

    private ApexPathExpanderConfig getApexPathExpanderConfig() {
        return getApexPathExpanderConfigBuilder().build();
    }

    private ApexPathExpanderConfig.Builder getApexPathExpanderConfigBuilder() {
        return ApexPathExpanderConfig.Builder.get()
                .expandMethodCalls(true)
                .with(BooleanValuePathConditionExcluder.getInstance())
                .with(NullApexValueConstrainer.getInstance());
    }

    private TestRunner.Result<SystemDebugAccumulator> walkPath(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPath();
    }

    private List<TestRunner.Result<SystemDebugAccumulator>> walkPaths(String[] sourceCode) {
        return TestRunner.get(g, sourceCode)
                .withExpanderConfig(getApexPathExpanderConfig())
                .walkPaths();
    }
}
