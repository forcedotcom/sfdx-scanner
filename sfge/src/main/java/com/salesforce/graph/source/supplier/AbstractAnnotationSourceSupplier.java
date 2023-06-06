package com.salesforce.graph.source.supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Abstract base class for source suppliers tied to specified code Annotations (e.g.,
 * {@code @AuraEnabled}, etc).
 */
abstract class AbstractAnnotationSourceSupplier extends AbstractSourceSupplier {
    /**
     * @return The specific annotation this source covers.
     */
    protected abstract String getAnnotation();

    @Override
    public List<MethodVertex> getVertices(GraphTraversalSource g, List<String> targetFiles) {
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .where(
                                out(Schema.CHILD)
                                        .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                        .out(Schema.CHILD)
                                        .where(
                                                CaseSafePropertyUtil.H.has(
                                                        ASTConstants.NodeType.ANNOTATION,
                                                        Schema.NAME,
                                                        getAnnotation())))
                        .order(Scope.global)
                        .by(Schema.DEFINING_TYPE, Order.asc)
                        .by(Schema.NAME, Order.asc));
    }

    @Override
    public boolean isPotentialSource(MethodVertex methodVertex) {
        return methodVertex.hasAnnotation(getAnnotation());
    }
}
