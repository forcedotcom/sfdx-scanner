package com.salesforce.graph.source.supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.TraversalUtil;
import com.salesforce.graph.vertex.MethodVertex;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Abstract base class for source suppliers. Each concrete subclass should correspond to a value of
 * {@link com.salesforce.graph.ApexPathSource.Type}.
 */
public abstract class AbstractSourceSupplier {
    /**
     * Get methods within the specified files that match this source type.
     *
     * @param targetFiles - Files to target. Empty list implicitly targets all files.
     */
    public abstract List<MethodVertex> getVertices(
            GraphTraversalSource g, List<String> targetFiles);

    /** Is the specified method a source of this type? */
    public abstract boolean isPotentialSource(MethodVertex methodVertex);

    /**
     * Helpful method for creating a traversal of all the non-test methods in {@code targetFiles}.
     */
    protected GraphTraversal<Vertex, Vertex> rootMethodTraversal(
            GraphTraversalSource g, List<String> targetFiles) {
        // Only look at UserClass vertices. Not interested in Enums, Interfaces, or Triggers
        final String[] labels = new String[] {ASTConstants.NodeType.USER_CLASS};
        return TraversalUtil.fileRootTraversal(g, labels, targetFiles)
                .not(has(Schema.IS_TEST, true))
                .repeat(__.out(Schema.CHILD))
                .until(__.hasLabel(ASTConstants.NodeType.METHOD))
                .not(has(Schema.IS_TEST, true));
    }
}
