package com.salesforce.graph.source.supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/** Supplier for non-test {@code global}-scoped methods. */
public class GlobalMethodSupplier extends AbstractSourceSupplier {
    @Override
    public List<MethodVertex> getVertices(GraphTraversalSource g, List<String> targetFiles) {
        // Get all methods in the target files.
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .filter(
                                __.and(
                                        // If a method has at least one block statement, then it
                                        // is
                                        // definitely actually declared, as
                                        // opposed to being an implicit method.
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.BLOCK_STATEMENT)
                                                .count()
                                                .is(P.gte(1)),
                                        // We only want global methods.
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                                .has(Schema.GLOBAL, true),
                                        // Ignore any standard and implicit methods, otherwise will
                                        // get a ton
                                        // of extra results.
                                        __.not(__.has(Schema.IS_STANDARD, true)),
                                        __.not(__.has(Schema.IS_IMPLICIT, true)))));
    }

    @Override
    public boolean isPotentialSource(MethodVertex methodVertex) {
        return methodVertex.getModifierNode().isGlobal();
    }
}
