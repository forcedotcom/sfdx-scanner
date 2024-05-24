package com.salesforce.rules;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableSet;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.cli.Result;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.cache.VertexCache;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.cache.VertexCacheTestProvider;
import com.salesforce.graph.source.ApexPathSource;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ReturnStatementVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import java.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuleRunnerTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        g = TestUtil.getGraph();
    }

    @Test
    public void properlyRunsStaticRule() {
        String sourceCode =
                "public class MyClass1 {\n"
                        + "	public boolean foo() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";
        TestUtil.buildGraph(g, sourceCode, true);
        List<AbstractRule> rules = new ArrayList<>();
        rules.add(new StaticTestRule());

        AbstractRuleRunner rr = new TestRuleRunner(g, VertexCacheProvider.get());
        final Result result = rr.runRules(rules);
        List<Violation> vs = result.getOrderedViolations();

        MatcherAssert.assertThat(vs, hasSize(equalTo(1)));
        assertEquals("Hard-coded static violation", vs.get(0).getMessage());
        assertEquals(1, vs.get(0).getSourceLineNumber());
    }

    @Test
    public void runsPathBasedRulesOnAuraMethods() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	@AuraEnabled\n"
                        + "	public boolean foo() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "\n"
                        + "	public boolean baz() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode, true);
        List<AbstractRule> rules = new ArrayList<>();
        rules.add(new PathTraversalTestRule());

        AbstractRuleRunner rr = new TestRuleRunner(g, VertexCacheProvider.get());
        final Result result = rr.runRules(rules);
        List<Violation> vs = result.getOrderedViolations();

        MatcherAssert.assertThat(vs, hasSize(equalTo(1)));
        assertEquals("Hard-coded path violation", vs.get(0).getMessage());
        assertEquals(4, vs.get(0).getSourceLineNumber());
    }

    @Test
    public void runsPathBasedRulesOnPageReferenceMethods() {
        String sourceCode =
                "public class MyClass {\n"
                        + "	public PageReference bar2() {\n"
                        + "		return null;\n"
                        + "	}\n"
                        + "\n"
                        + "	public boolean baz2() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";

        TestUtil.buildGraph(g, sourceCode, true);
        List<AbstractRule> rules = new ArrayList<>();
        rules.add(new PathTraversalTestRule());

        AbstractRuleRunner rr = new TestRuleRunner(g, VertexCacheProvider.get());
        final Result result = rr.runRules(rules);
        List<Violation> vs = result.getOrderedViolations();

        MatcherAssert.assertThat(vs, hasSize(equalTo(1)));
        assertEquals("Hard-coded path violation", vs.get(0).getMessage());
        assertEquals(3, vs.get(0).getSourceLineNumber());
    }

    @Test
    public void filtersViolationsFromNontargetedFiles() {
        String sourceCode1 =
                "public class MyClass1 {\n"
                        + "	public PageReference bar1() {\n"
                        + "		return null;\n"
                        + "	}\n"
                        + "\n"
                        + "	public boolean baz1() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";

        String sourceCode2 =
                "public class MyClass2 {\n"
                        + "	public PageReference bar2() {\n"
                        + "		return null;\n"
                        + "	}\n"
                        + "\n"
                        + "	public boolean baz2() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";

        String sourceCode3 =
                "public class MyClass3 {\n"
                        + "	public PageReference bar3() {\n"
                        + "		return null;\n"
                        + "	}\n"
                        + "\n"
                        + "	public boolean baz3() {\n"
                        + "		return true;\n"
                        + "	}\n"
                        + "}\n";

        TestUtil.buildGraph(g, new String[] {sourceCode1, sourceCode2, sourceCode3}, true);
        List<AbstractRule> rules = new ArrayList<>();
        rules.add(new StaticTestRule());
        rules.add(new PathTraversalTestRule());

        AbstractRuleRunner rr = new TestRuleRunner(g, VertexCacheProvider.get());
        List<RuleRunnerTarget> targets = new ArrayList<>();
        targets.add(TestUtil.createTarget("TestCode0", new ArrayList<>()));
        targets.add(TestUtil.createTarget("TestCode1", new ArrayList<>()));
        // Since we're providing a list of targeted files, only those files should have violations
        // associated with them.
        final Result result = rr.runRules(rules, targets);
        List<Violation> vs = result.getOrderedViolations();

        MatcherAssert.assertThat(vs, hasSize(4));
        Set<String> filesWithStaticViolation = new HashSet<>();
        Set<String> filesWithPathViolation = new HashSet<>();
        for (Violation v : vs) {
            if (v.getMessage().equals("Hard-coded static violation")) {
                filesWithStaticViolation.add(v.getSourceFileName());
            } else if (v.getMessage().equals("Hard-coded path violation")) {
                filesWithPathViolation.add(v.getSourceFileName());
            } else {
                fail("Unexpected violation message: " + v.getMessage());
            }
        }
        MatcherAssert.assertThat(
                filesWithStaticViolation, containsInAnyOrder("TestCode0", "TestCode1"));
        MatcherAssert.assertThat(filesWithStaticViolation, not(contains("TestCode2")));
        MatcherAssert.assertThat(
                filesWithPathViolation, containsInAnyOrder("TestCode0", "TestCode1"));
        MatcherAssert.assertThat(filesWithPathViolation, not(contains("TestCode2")));
    }

    private static class StaticTestRule extends AbstractStaticRule {
        private static StaticTestRule INSTANCE = null;

        @Override
        protected List<Violation> _run(
                GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
            // Throw a violation for UserClass vertices defining classes named MyClass1, MyClass2,
            // or MyClass3.
            List<BaseSFVertex> vertices =
                    SFVertexFactory.loadVertices(
                            g,
                            eligibleVertices
                                    .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                    .has(
                                            Schema.DEFINING_TYPE,
                                            P.within("MyClass1", "MyClass2", "MyClass3")));
            List<Violation> vs = new ArrayList<>();
            for (BaseSFVertex vertex : vertices) {
                vs.add(new Violation.StaticRuleViolation("Hard-coded static violation", vertex));
            }
            return vs;
        }

        @Override
        protected int getSeverity() {
            return SEVERITY.LOW.code;
        }

        @Override
        protected String getDescription() {
            // TODO: Replace this placeholder with a real value.
            return "Placeholder Description for " + this.getClass().getSimpleName();
        }

        @Override
        protected String getCategory() {
            return CATEGORY.INTERNAL_TESTING.name;
        }

        public static StaticTestRule getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new StaticTestRule();
            }
            return INSTANCE;
        }
    }

    private static class PathTraversalTestRule extends AbstractPathTraversalRule {
        @Override
        protected List<RuleThrowable> _run(
                GraphTraversalSource g, ApexPath path, BaseSFVertex vertex) {
            // The same vertex can be both source and sync. It doesn't matter terribly.
            Violation v =
                    new Violation.PathBasedRuleViolation(
                            "Hard-coded path violation", vertex, vertex);
            List<RuleThrowable> vs = new ArrayList<>();
            vs.add(v);
            return vs;
        }

        @Override
        protected int getSeverity() {
            return 0;
        }

        @Override
        protected String getDescription() {
            return null;
        }

        @Override
        protected String getCategory() {
            return null;
        }

        @Override
        public boolean test(BaseSFVertex vertex, SymbolProvider provider) {
            // The test should show interest in the return statement for any method defined in the
            // "MyClass" class.
            return vertex instanceof ReturnStatementVertex
                    && vertex.getParentClass().get().getDefiningType().startsWith("MyClass");
        }

        public static PathTraversalTestRule getInstance() {
            return LazyHolder.INSTANCE;
        }

        @Override
        public ImmutableSet<ApexPathSource.Type> getSourceTypes() {
            return ImmutableSet.copyOf(ApexPathSource.Type.values());
        }

        private static final class LazyHolder {
            // Postpone initialization until first use.
            private static final PathTraversalTestRule INSTANCE = new PathTraversalTestRule();
        }
    }

    /** Special RuleRunner that passes the VertexCache to the other thread */
    public static final class TestRuleRunner extends AbstractRuleRunner {
        private final VertexCache vertexCache;

        protected TestRuleRunner(GraphTraversalSource g, VertexCache vertexCache) {
            super(g);
            this.vertexCache = vertexCache;
        }

        protected RuleRunnerSubmission getRuleRunnerSubmission(
                GraphTraversalSource fullGraph,
                MethodVertex pathEntry,
                List<AbstractPathBasedRule> rules) {
            return new RuleRunnerSubmission(fullGraph, pathEntry, rules) {
                @Override
                public void initializeThreadLocals() {
                    super.initializeThreadLocals();
                    VertexCacheTestProvider.initializeForTest(vertexCache);
                }

                @Override
                public void afterRun(Result result) {
                    super.afterRun(result);
                    VertexCacheTestProvider.remove();
                }
            };
        }
    }
}
