package com.salesforce.graph.ops;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
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

    private TraversalUtil() {}
}
