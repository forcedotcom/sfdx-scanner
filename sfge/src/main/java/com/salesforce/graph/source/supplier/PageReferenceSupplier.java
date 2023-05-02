package com.salesforce.graph.source.supplier;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Supplier for methods that return a {@code PageReference} object. */
public class PageReferenceSupplier extends AbstractSourceSupplier {
    @Override
    public List<MethodVertex> getVertices(GraphTraversalSource g, List<String> targetFiles) {
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.RETURN_TYPE,
                                        Schema.PAGE_REFERENCE))
                        .order(Scope.global)
                        .by(Schema.DEFINING_TYPE, Order.asc)
                        .by(Schema.NAME, Order.asc));
    }

    @Override
    public boolean isPotentialSource(MethodVertex methodVertex) {
        return methodVertex.getReturnType().equalsIgnoreCase(Schema.PAGE_REFERENCE);
    }
}
