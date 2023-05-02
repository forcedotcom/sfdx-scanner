package com.salesforce.graph.source.supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.ops.TraversalUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.metainfo.MetaInfoCollectorProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/** Supplier for {@code global}/{@code public} non-test methods on VisualForce controllers. */
public class ExposedControllerMethodSupplier extends AbstractSourceSupplier {
    @Override
    public List<MethodVertex> getVertices(GraphTraversalSource g, List<String> targetFiles) {
        Set<String> referencedVfControllers =
                MetaInfoCollectorProvider.getVisualForceHandler().getMetaInfoCollected();
        // If none of the VF files referenced an Apex class, we can just return an empty list.
        if (referencedVfControllers.isEmpty()) {
            return new ArrayList<>();
        }
        List<MethodVertex> allControllerMethods =
                SFVertexFactory.loadVertices(
                        g,
                        TraversalUtil.fileRootTraversal(g, targetFiles)
                                // Only outer classes can be VF controllers, so we should
                                // restrict
                                // our query to UserClasses.
                                .where(
                                        CaseSafePropertyUtil.H.hasWithin(
                                                ASTConstants.NodeType.USER_CLASS,
                                                Schema.DEFINING_TYPE,
                                                referencedVfControllers))
                                .repeat(__.out(Schema.CHILD))
                                .until(__.hasLabel(ASTConstants.NodeType.METHOD))
                                .not(has(Schema.IS_TEST, true))
                                // We want to ignore constructors.
                                .where(
                                        __.not(
                                                CaseSafePropertyUtil.H.hasWithin(
                                                        ASTConstants.NodeType.METHOD,
                                                        Schema.NAME,
                                                        Schema.INSTANCE_CONSTRUCTOR_CANONICAL_NAME,
                                                        Schema
                                                                .STATIC_CONSTRUCTOR_CANONICAL_NAME))));
        // Gremlin isn't sophisticated enough to perform this kind of filtering in the actual
        // query.
        // So we'll just do it
        // manually here.
        return allControllerMethods.stream()
                .filter(
                        methodVertex ->
                                methodVertex.getModifierNode().isPublic()
                                        || methodVertex.getModifierNode().isGlobal())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentialSource(MethodVertex methodVertex) {
        Set<String> vfControllers =
                MetaInfoCollectorProvider.getVisualForceHandler().getMetaInfoCollected().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
        return vfControllers.contains(methodVertex.getDefiningType().toLowerCase());
    }
}
