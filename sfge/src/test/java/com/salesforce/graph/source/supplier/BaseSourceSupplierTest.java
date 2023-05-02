package com.salesforce.graph.source.supplier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.messaging.CliMessager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Base class with helper methods for testing {@link AbstractSourceSupplier} subclasses. */
public abstract class BaseSourceSupplierTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
        CliMessager.getInstance().resetMessages();
    }

    @AfterEach
    public void teardown() {
        CliMessager.getInstance().resetMessages();
    }

    /**
     * Builds a graph using {@code sourceCodes}, uses {@code supplier} to load methods, and checks
     * those methods against {@code expectedMethodKeys}.
     */
    protected void testSupplier_positive(
            String[] sourceCodes, AbstractSourceSupplier supplier, Set<String> expectedMethodKeys) {
        // Build the graph.
        TestUtil.buildGraph(g, sourceCodes, false);

        // Load the supplier's sources.
        List<MethodVertex> sources = supplier.getVertices(g, new ArrayList<>());

        // Verify that the right number of sources were returned.
        MatcherAssert.assertThat(sources, hasSize(equalTo(expectedMethodKeys.size())));

        // Verify that each of the sources is one we expected to receive.
        for (MethodVertex source : sources) {
            assertTrue(expectedMethodKeys.contains(source.generateUniqueKey()));
        }
    }
}
