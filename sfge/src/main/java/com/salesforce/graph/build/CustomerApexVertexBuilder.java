package com.salesforce.graph.build;

import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/** Converts customer provided Apex code into Gremlin vertices. */
public final class CustomerApexVertexBuilder extends AbstractApexVertexBuilder
        implements GraphBuilder {
    private final List<Util.CompilationDescriptor> compilations;

    public CustomerApexVertexBuilder(
            GraphTraversalSource g, List<Util.CompilationDescriptor> compilations) {
        super(g);
        this.compilations = compilations;
    }

    @Override
    public void build() {
        for (Util.CompilationDescriptor compilation : compilations) {
            buildVertices(compilation.getCompilation(), compilation.getFileName());
        }
    }

    @Override
    protected void afterFileInsert(GraphTraversalSource g, Vertex vNode) {
        ApexPropertyAnnotator.apply(g, vNode);
    }
}
