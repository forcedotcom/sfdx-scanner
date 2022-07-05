package com.salesforce.graph.ops;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public final class TraversalUtil {
    /**
     * Delegates to {@link #fileRootTraversal(GraphTraversalSource, String[], List)} using {@link
     * ASTConstants.NodeType#ROOT_VERTICES} as the list of labels.
     */
    public static GraphTraversal<Vertex, Vertex> fileRootTraversal(
            GraphTraversalSource g, List<String> targetFiles) {
        return fileRootTraversal(g, ASTConstants.NodeType.ROOT_VERTICES, targetFiles);
    }

    /**
     * Returns a traversal containing the root vertices defined by {@code labels} of each specified
     * file. An empty array implicitly targets all files.
     */
    public static GraphTraversal<Vertex, Vertex> fileRootTraversal(
            GraphTraversalSource g, String[] labels, List<String> targetFiles) {
        // We want to start with a traversal of all vertices that could be roots.
        // Note: There's no .hasLabel(String...) overload, so we're using the
        // .hasLabel(String,String...) overload instead.
        GraphTraversal<Vertex, Vertex> roots = g.V().hasLabel(labels[0], labels);

        return targetFiles.isEmpty()
                // If the list of target files is empty, then we want every vertex with a non-null
                // FileName.
                ? roots.has(Schema.FILE_NAME)
                // If there are target files specified, we only want the roots of those files.
                : roots.has(Schema.FILE_NAME, P.within(targetFiles));
    }

    /**
     * Returns a traversal containing every vertex indicated by the provided targets. An empty
     * target array implicitly targets the whole graph.
     */
    public static GraphTraversal<Vertex, Vertex> ruleTargetTraversal(
            GraphTraversalSource g, List<RuleRunnerTarget> targets) {
        List<String> targetFiles =
                targets.stream().map(RuleRunnerTarget::getTargetFile).collect(Collectors.toList());
        return targetFiles.isEmpty()
                // If there are no targets, then we can cut to the chase and return a traversal
                // containing the whole graph.
                ? g.V()
                // If there are targets, then for performance reasons, we start with the roots of
                // those files...
                : fileRootTraversal(g, targetFiles)
                        .as("Root")
                        // ...and use that as the base for traversals of those individual files to
                        // get the targets within, which are
                        // all unioned together.
                        // NOTE: No deduping is performed. If a file appears in two different
                        // targets, there may be duplicates in the
                        // query's results.
                        .union(
                                targets.stream()
                                        .map(t -> t.createTraversal(__.select("Root")))
                                        .toArray(GraphTraversal[]::new));
    }

    public static GraphTraversal<Vertex, Vertex> traverseImplementationsOf(
            GraphTraversalSource g, List<String> targetFiles, String interfaceName) {
        // For our traversal, we want to start with every class that implements either the target
        // interface or one of its
        // subtypes. Do that with a union of these two subtraversals.
        // Also, start with the hasLabel() call, because doing an initial filter along an indexed
        // field saves us a ton of time.
        GraphTraversal<Vertex, Vertex> traversal =
                g.V().hasLabel(NodeType.USER_CLASS, NodeType.USER_INTERFACE)
                        .union(
                                // Subtraversal 1: Get every class that implements the target
                                // interface, via a helper method.
                                __.where(
                                        H.hasArrayContaining(
                                                NodeType.USER_CLASS,
                                                Schema.INTERFACE_DEFINING_TYPES,
                                                interfaceName)),
                                // Subtraversal 2: Get every class that implements a subtype of the
                                // target interface, by starting with all
                                // the direct subtypes of the interface...
                                __.where(
                                                H.has(
                                                        NodeType.USER_INTERFACE,
                                                        Schema.SUPER_INTERFACE_NAME,
                                                        interfaceName))
                                        .union(
                                                __.identity(),
                                                // ...recursively adding subtypes of those
                                                // interfaces...
                                                __.repeat(__.out(Schema.EXTENDED_BY)).emit())
                                        // ... and getting every class that implements one of those
                                        // interfaces.
                                        .out(Schema.IMPLEMENTED_BY)
                                // Now, we add every subclass of any of these classes, recursively.
                                )
                        .union(__.identity(), __.repeat(__.out(Schema.EXTENDED_BY)).emit());

        // If there are no target files, we're clear to return this traversal since it includes all
        // relevant classes in
        // the graph.
        if (targetFiles.isEmpty()) {
            return traversal;
        } else {
            // Otherwise, we need to filter so we're only returning the classes in a target file. To
            // do that, do a traversal
            // to get all the IDs of classes defined in the target files.
            Object[] targetIds =
                    fileRootTraversal(g, targetFiles)
                            .union(__.identity(), __.repeat(__.out(Schema.CHILD)).emit())
                            .hasLabel(NodeType.USER_CLASS)
                            .id()
                            .toList()
                            .toArray();
            // Note, there's no .hasId(Object...) overload, so we're using the .hasId(Object,
            // ...Object) overload instead.
            return traversal.hasId(targetIds);
        }
    }

    private TraversalUtil() {}
}
