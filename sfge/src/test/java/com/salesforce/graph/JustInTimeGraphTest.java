package com.salesforce.graph;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.cache.VertexCache;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.cache.VertexCacheTestProvider;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexBooleanValue;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JustInTimeGraphTest {
    /**
     * This test runs with and without the {@link InMemoryJustInTimeGraph} in order to ensure that
     * the JustInTimeGraph works as expected. It runs the test in a separate thread to match how
     * rules are run.
     *
     * <p>The class accesses a series of three files. It demonstrates that importing a Subclass also
     * imports its parent and an interface that the parent implements.
     *
     * <p>public static void doSomething(String s, String sObjectType) { Boolean isSearchable =
     * Schema.getGlobalDescribe().get(objectName).getDescribe().isSearchable();
     * System.debug(isSearchable); }
     */
    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "{displayName}: {0}")
    public void testJustInTimeGraph(boolean useJustInTimeGraph, TestInfo testInfo)
            throws Exception {
        final GraphTraversalSource fullGraph = GraphUtil.getGraph();
        TestUtil.compileTestFiles(fullGraph, testInfo);
        final VertexCache originalVertexCache = VertexCacheProvider.get();

        Runnable runnable =
                () -> {
                    try {
                        final GraphTraversalSource g;
                        if (useJustInTimeGraph) {
                            g = JustInTimeGraphProvider.create(fullGraph, "MySubClass");
                            validateGraphsAreIdentical(fullGraph, g);
                        } else {
                            g = fullGraph;
                        }

                        // Use the VertexCache from the main thread. This simulates the production
                        // case where the VertexCache
                        // is a singleton shared by all threads.
                        VertexCacheTestProvider.initializeForTest(originalVertexCache);
                        final MethodVertex method =
                                TestUtil.getMethodVertex(g, "MyParentClass", "doSomething");

                        // Verify that the standard library files were loaded into graph
                        final List<UserClassVertex> vertices =
                                SFVertexFactory.loadVertices(
                                        g,
                                        g.V()
                                                .hasLabel(NodeType.USER_CLASS)
                                                .has(Schema.IS_STANDARD, true));
                        MatcherAssert.assertThat(vertices, hasSize(equalTo(21)));

                        // walk the path ensuring that the boolean is indeterminant and was returned
                        // from a DescribeSObjectResult
                        final List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, method);

                        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

                        final ApexPath path = paths.get(0);

                        final SystemDebugAccumulator visitor = new SystemDebugAccumulator();
                        final DefaultSymbolProviderVertexVisitor symbols =
                                new DefaultSymbolProviderVertexVisitor(g);
                        ApexPathWalker.walkPath(g, path, visitor, symbols);

                        final ApexBooleanValue value = visitor.getSingletonResult();
                        MatcherAssert.assertThat(value.isIndeterminant(), equalTo(true));

                        final DescribeSObjectResult describeSObjectResult =
                                (DescribeSObjectResult) value.getReturnedFrom().orElse(null);
                        MatcherAssert.assertThat(
                                describeSObjectResult.getSObjectType().isPresent(), equalTo(true));
                    } finally {
                        if (useJustInTimeGraph) {
                            JustInTimeGraphProvider.remove();
                        }
                    }
                };

        // Execute the thread and wait for it to complete
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(runnable);
        future.get();
    }

    private void validateGraphsAreIdentical(
            GraphTraversalSource fullGraph, GraphTraversalSource jitGraph) {
        // Verify that all of the edges and vertices were copied over. This occurs even though only
        // the Subclass
        // was used to seed the graph. This is because the import follows all edges and imports all
        // vertices that the
        // edges point to.
        List<Map<Object, Object>> vertices = fullGraph.V().elementMap().toList();
        for (Map<Object, Object> vertex : vertices) {
            Long id = (Long) vertex.get(T.id);
            List<Map<Object, Object>> jitVertices = jitGraph.V(id).elementMap().toList();
            MatcherAssert.assertThat(vertex.toString(), jitVertices, hasSize(equalTo(1)));
            Map<Object, Object> jitVertex = jitVertices.get(0);
            MatcherAssert.assertThat(vertex, equalTo(jitVertex));
        }

        List<Map<Object, Object>> edges = fullGraph.E().elementMap().toList();
        for (Map<Object, Object> edge : edges) {
            Long id = (Long) edge.get(T.id);
            List<Map<Object, Object>> jitEdges = jitGraph.E(id).elementMap().toList();
            MatcherAssert.assertThat(edge.toString(), jitEdges, hasSize(equalTo(1)));
            Map<Object, Object> jitEdge = jitEdges.get(0);
            MatcherAssert.assertThat(edge, equalTo(jitEdge));
        }
    }
}
