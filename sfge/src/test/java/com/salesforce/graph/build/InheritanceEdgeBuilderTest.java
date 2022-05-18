package com.salesforce.graph.build;

import com.salesforce.TestUtil;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InheritanceEdgeBuilderTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @Test
    public void testSimpleExtension() {
        // TODO
    }

    @Test
    public void testInnerTypeExtension() {
        // TODO
    }

    @Test
    public void testSimpleImplementation() {
        // TODO
    }

    @Test
    public void testInnerTypeImplementation() {
        // TODO
    }
}
