package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.empty;

import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.TreeBuilderVisitor;
import com.salesforce.graph.Schema;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApexStandardLibraryVertexBuilderTest {
    /** The graph is stored in a static because all tests use the same graph for the tests. */
    private static GraphTraversalSource g;

    @BeforeAll
    public static void setupAll() {
        // Get a unique graph since this will load the standard apex classes
        ApexStandardLibraryVertexBuilderTest.g = TestUtil.getUniqueGraph();
        new ApexStandardLibraryVertexBuilder(g).build();
    }

    /**
     * Verify that all vertices imported by {@link ApexStandardLibraryVertexBuilder} are marked as
     * standard.
     */
    @Test
    public void testAllVerticesAreMarkedAsStandard() {
        List<Vertex> vertices = g.V().not(has(Schema.IS_STANDARD, true)).toList();
        List<String> labels = vertices.stream().map(v -> v.label()).collect(Collectors.toList());
        MatcherAssert.assertThat(labels, empty());
    }

    public static Stream<Arguments> testIgnoredVerticesAreNotImported() {
        return TreeBuilderVisitor.IGNORED_NODES.stream().map(s -> Arguments.of(s));
    }

    /** Verify that IGNORED_VERTICES nodes are filtered out */
    @MethodSource
    @ParameterizedTest(name = "{0}")
    public void testIgnoredVerticesAreNotImported(String label) {
        List<Vertex> vertices = g.V().hasLabel(label).toList();
        MatcherAssert.assertThat(vertices, empty());
    }
}
