package com.salesforce.graph.ops;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TraversalUtilTest {
    private GraphTraversalSource g;
    /**
     * The methods defined in both this file and in {@link #sourceCode2} have unique numbers of
     * lines, to ensure that they have unique numbers of vertices. This should make tests that count
     * vertices more robust.
     */
    private String sourceCode1 =
            "public class MyClass1 {\n"
                    + "	public boolean foo() {\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "	public boolean bar() {\n"
                    + "		int i = 0;\n"
                    + "		return false;\n"
                    + "	}\n"
                    + "}\n";
    /**
     * The methods defined in both this file and in {@link #sourceCode1} have unique numbers of
     * lines, to ensure that they have unique numbers of vertices. This should make tests that count
     * vertices more robust.
     */
    private String sourceCode2 =
            "public class MyClass2 {\n"
                    + "	public boolean foo() {\n"
                    + "		int i = 0;\n"
                    + "		int j = 0;\n"
                    + "		return true;\n"
                    + "	}\n"
                    + "	public boolean baz() {\n"
                    + "		int i = 0;\n"
                    + "		int j = 0;\n"
                    + "		int k = 0;\n"
                    + "		return false;\n"
                    + "	}\n"
                    + "	public boolean baz() {\n"
                    + "		int i = 0;\n"
                    + "		int j = 0;\n"
                    + "		int k = 0;\n"
                    + "		int l = 0;\n"
                    + "		return false;\n"
                    + "	}\n"
                    + "}\n";

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void ruleTargetTraversal_WholeFile() {
        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2}, true);

        // Instantiate a target encompassing the entire first file.
        RuleRunnerTarget target = TestUtil.createTarget("TestCode0", new ArrayList<>());
        // Send it through the traversal generator, and turn that into a list of vertices.
        List<Vertex> targetResults =
                TraversalUtil.ruleTargetTraversal(g, Collections.singletonList(target)).toList();

        // Manually generate a query returning all of the vertices in the file.
        List<Vertex> expectedResults =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, "TestCode0")
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();

        // Both queries should have returned the same number of vertices, and that number should not
        // be 0.
        MatcherAssert.assertThat(targetResults.size(), greaterThan(0));
        MatcherAssert.assertThat(targetResults, hasSize(expectedResults.size()));
    }

    @Test
    public void ruleTargetTraversal_TwoWholeFiles() {
        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2}, true);

        // Instantiate two targets, each one encompassing an entire file.
        RuleRunnerTarget target1 = TestUtil.createTarget("TestCode0", new ArrayList<>());
        RuleRunnerTarget target2 = TestUtil.createTarget("TestCode1", new ArrayList<>());

        // Send those targets through the traversal generator, and turn the results into a list of
        // vertices.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(target1);
        targets.add(target2);
        List<Vertex> targetResults = TraversalUtil.ruleTargetTraversal(g, targets).toList();

        // Manually generate a query returning all of the vertices in both files.
        List<Vertex> expectedResults =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, P.within("TestCode0", "TestCode1"))
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();

        // Both queries should have the same number of vertices, and that number should not be 0.
        MatcherAssert.assertThat(targetResults.size(), greaterThan(0));
        MatcherAssert.assertThat(targetResults, hasSize(expectedResults.size()));
    }

    @Test
    public void ruleTargetTraversal_OneMethod() {
        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2}, true);

        // Instantiate a target encompassing only one method in one file.
        RuleRunnerTarget target =
                TestUtil.createTarget("TestCode0", Collections.singletonList("foo"));
        // Send that target through the traversal generator, and turn the results into a list of
        // vertices.
        List<Vertex> targetResults =
                TraversalUtil.ruleTargetTraversal(g, Collections.singletonList(target)).toList();

        // Manually generate a query containing only the vertices in the targeted method.
        List<Vertex> expectedResults =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, "TestCode0")
                        .repeat(__.out(Schema.CHILD))
                        .until(__.hasLabel(ASTConstants.NodeType.METHOD).has(Schema.NAME, "foo"))
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();

        // Both queries should have the same number of vertices, and that number should not be 0.
        MatcherAssert.assertThat(targetResults.size(), greaterThan(0));
        MatcherAssert.assertThat(targetResults, hasSize(expectedResults.size()));
    }

    @Test
    public void ruleTargetTraversal_TwoMethods() {
        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2}, true);

        // Instantiate a target encompassing two method in one file.
        RuleRunnerTarget target = TestUtil.createTarget("TestCode1", Arrays.asList("foo", "bar"));

        // Send that target through the traversal generator, and turn the results into a list of
        // vertices.
        List<Vertex> targetResults =
                TraversalUtil.ruleTargetTraversal(g, Collections.singletonList(target)).toList();

        // Manually generate a query containing only the vertices in the targeted method.
        List<Vertex> expectedResults =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, "TestCode1")
                        .repeat(__.out(Schema.CHILD))
                        .until(
                                __.hasLabel(ASTConstants.NodeType.METHOD)
                                        .has(Schema.NAME, P.within("foo", "bar")))
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();

        // Both queries should have the same number of vertices, and that number should not be 0.
        MatcherAssert.assertThat(targetResults.size(), greaterThan(0));
        MatcherAssert.assertThat(targetResults, hasSize(expectedResults.size()));
    }

    @Test
    public void ruleTargetTraversal_MethodAndFile() {
        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2}, true);

        // Instantiate two targets, one encompassing a whole file and the other encompassing a
        // single method in a different file.
        RuleRunnerTarget target1 = TestUtil.createTarget("TestCode0", new ArrayList<>());
        RuleRunnerTarget target2 =
                TestUtil.createTarget("TestCode1", Collections.singletonList("baz"));

        // Send those targets through the traversal generator, and turn the results into a list of
        // vertices.
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(target1);
        targets.add(target2);
        List<Vertex> targetResults = TraversalUtil.ruleTargetTraversal(g, targets).toList();

        // Manually generate queries containing only the targeted vertices.
        List<Vertex> expectedWholeFile =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, "TestCode0")
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();
        List<Vertex> expectedSingleMethod =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .has(Schema.FILE_NAME, "TestCode1")
                        .repeat(__.out(Schema.CHILD))
                        .until(__.hasLabel(ASTConstants.NodeType.METHOD).has(Schema.NAME, "baz"))
                        .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                        .toList();

        // If either of these assertions fail, our test data is bad.
        MatcherAssert.assertThat(expectedWholeFile.size(), greaterThan(0));
        MatcherAssert.assertThat(expectedSingleMethod.size(), greaterThan(0));

        // The target query should have a number of vertices equal to the sum of both expected
        // results, and that number should
        // not be 0.
        MatcherAssert.assertThat(
                targetResults, hasSize(expectedWholeFile.size() + expectedSingleMethod.size()));
    }
}
