package com.salesforce.graph.ops;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.not;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.expander.*;
import com.salesforce.graph.ops.expander.switches.ApexPathCaseStatementExcluder;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public final class ApexPathUtil {

    /**
     * Only allow forward path traversal form a method boundary. This helps ensure that all stacks
     * and scopes are correctly pushed an popped across boundaries.
     */
    public static List<ApexPath> getForwardPaths(GraphTraversalSource g, MethodVertex method) {
        return getForwardPaths(g, method, true);
    }

    public static List<ApexPath> getForwardPaths(
            GraphTraversalSource g, MethodVertex method, boolean expandMethodCalls) {
        ApexPathExpanderConfig config =
                ApexPathExpanderConfig.Builder.get().expandMethodCalls(expandMethodCalls).build();
        return getForwardPaths(g, method, config);
    }

    public static List<ApexPath> getForwardPaths(
            GraphTraversalSource g, MethodVertex method, ApexPathExpanderConfig config) {
        // We can get the path summary, and just return the accepted paths.
        return summarizeForwardPaths(g, method, config).getAcceptedPaths();
    }

    /**
     * Return all valid paths starting at the provided method, as well as the reasons that invalid paths were rejected.
     * @param g
     * @param method
     * @param config
     * @return
     */
    public static ApexPathRetrievalSummary summarizeForwardPaths(
            GraphTraversalSource g, MethodVertex method, ApexPathExpanderConfig config) {
        BlockStatementVertex blockStatement =
                method.getOnlyChildOrNull(ASTConstants.NodeType.BLOCK_STATEMENT);
        if (blockStatement == null) {
            // Default constructors don't have a block statement. Create a synthetic one
            // TODO: Consider adding this to the graph instead
            ApexPath apexPath = new ApexPath(method);
            HashMap<Object, Object> map = new HashMap<>();
            map.put(T.id, Long.valueOf(-1));
            map.put(T.label, ASTConstants.NodeType.BLOCK_STATEMENT);
            map.put(Schema.END_SCOPES, ASTConstants.NodeType.BLOCK_STATEMENT);
            blockStatement = new BlockStatementVertex(map);
            apexPath.addVertices(Collections.singletonList(blockStatement));
            return new ApexPathRetrievalSummary(Collections.singletonList(apexPath));
        } else {
            return getPaths(g, method, blockStatement, Direction.FORWARD, config);
        }
    }

    public static List<ApexPath> getReversePaths(
            GraphTraversalSource g, BaseSFVertex startingVertex) {
        return getReversePaths(g, startingVertex, true);
    }

    public static List<ApexPath> getReversePaths(
            GraphTraversalSource g, BaseSFVertex startingVertex, boolean expandMethodCalls) {
        ApexPathExpanderConfig config =
                ApexPathExpanderConfig.Builder.get().expandMethodCalls(expandMethodCalls).build();
        return getReversePaths(g, startingVertex, config);
    }

    public static List<ApexPath> getReversePaths(
            GraphTraversalSource g, BaseSFVertex startingVertex, ApexPathExpanderConfig config) {
        BaseSFVertex topLevelVertex =
                GraphUtil.getControlFlowVertex(g, startingVertex)
                        .orElseThrow(() -> new UnexpectedException(startingVertex));

        MethodVertex method = topLevelVertex.getParentMethod().get();
        List<ApexPath> paths =
                getPaths(g, method, topLevelVertex, Direction.BACKWARD, config).getAcceptedPaths();

        // Filter out paths that end in exceptions.
        if (startingVertex instanceof ThrowStatementVertex) {
            // Only return paths that match the throwStatement, filtering out other throw statements
            paths =
                    paths.stream()
                            .filter(
                                    p ->
                                            p.endsInException()
                                                    && p.getThrowStatement().equals(startingVertex))
                            .collect(Collectors.toList());
        } else {
            // By definition, paths that end in an an exception can't reach the startingVertex
            paths = paths.stream().filter(p -> !p.endsInException()).collect(Collectors.toList());
        }

        return paths;
    }

    private static ApexPathRetrievalSummary getPaths(
            GraphTraversalSource g,
            MethodVertex method,
            BaseSFVertex startingVertex,
            Direction direction,
            ApexPathExpanderConfig expanderConfig) {
        List<ApexPath> results = new ArrayList<>();

        Supplier<GraphTraversal<Vertex, Vertex>> repeatTraversal =
                () ->
                        direction.equals(Direction.FORWARD)
                                ? out(Schema.CFG_PATH)
                                : in(Schema.CFG_PATH);

        List<Path> paths =
                g.V(startingVertex.getId())
                        .repeat(repeatTraversal.get())
                        .until(not(repeatTraversal.get()))
                        .path()
                        // TODO: Why is the dedup necessary?
                        .dedup()
                        .toList();

        if (paths.isEmpty()) {
            // This can happen with an empty method
            ApexPath apexPath = new ApexPath(method);
            apexPath.addVertices(Collections.singletonList(startingVertex));
            results.add(apexPath);
        } else {
            for (Path path : paths) {
                Iterator it = path.iterator();
                List<BaseSFVertex> vertices = new ArrayList<>();
                while (it.hasNext()) {
                    BaseSFVertex sfVertex = SFVertexFactory.load(g, (Vertex) it.next());
                    if (sfVertex instanceof StandardConditionVertex.Unknown) {
                        StandardConditionVertex.Unknown unknownCondition =
                                (StandardConditionVertex.Unknown) sfVertex;
                        // The positive path is identified by the next vertex in the path being the
                        // next sibling
                        // of the StandardCondition. We have two handle the case differently
                        // depending if we are
                        // traversing forwards or backwards.
                        if (direction.equals(Direction.BACKWARD)) {
                            BaseSFVertex lastVertex = vertices.get(vertices.size() - 1);
                            if (lastVertex.getPreviousSibling().equals(unknownCondition)) {
                                vertices.add(unknownCondition.convertToPositive());
                            } else {
                                vertices.add(unknownCondition.convertToNegative());
                            }
                        } else {
                            BaseSFVertex nextVertexInPath =
                                    SFVertexFactory.load(g, (Vertex) it.next());
                            if (unknownCondition.getNextSibling().equals(nextVertexInPath)) {
                                vertices.add(unknownCondition.convertToPositive());
                            } else {
                                vertices.add(unknownCondition.convertToNegative());
                            }
                            vertices.add(nextVertexInPath);
                        }
                    } else {
                        vertices.add(sfVertex);
                    }
                }
                if (direction.equals(Direction.BACKWARD)) {
                    Collections.reverse(vertices);
                }
                ApexPath apexPath = new ApexPath(method);
                apexPath.addVertices(vertices);
                results.add(apexPath);
            }
        }

        if (expanderConfig.getExpandMethodCalls()) {
            // If we're expected to expand paths, add the results of each path expansion to the summary.
            ApexPathRetrievalSummary summary = new ApexPathRetrievalSummary();
            for (ApexPath path : results) {
                summary.addExpansionResults(ApexPathExpanderUtil.expand(g, path, expanderConfig));
            }
            return summary;
        } else {
            // If we don't have to expand, then the paths we already have can be put into a summary and returned.
            return new ApexPathRetrievalSummary(results);
        }
    }

    /**
     * @return a builder that contains all of the known collapsers
     */
    public static ApexPathExpanderConfig.Builder getFullConfiguredPathExpanderConfigBuilder() {
        return ApexPathExpanderConfig.Builder.get()
                .expandMethodCalls(true)
                .with(ReturnResultPathCollapser.getInstance())
                .with(SyntheticResultReturnValuePathCollapser.getInstance())
                .with(BooleanValuePathConditionExcluder.getInstance())
                .with(ApexPathCaseStatementExcluder.getInstance())
                .with(NullApexValueConstrainer.getInstance());
    }

    /**
     * @return a config that contains all of the known collapsers
     */
    public static ApexPathExpanderConfig getFullConfiguredPathExpanderConfig() {
        return getFullConfiguredPathExpanderConfigBuilder().build();
    }

    /**
     * @return a config that contains no collapsers and expands methods
     */
    public static ApexPathExpanderConfig getSimpleExpandingConfig() {
        return ApexPathExpanderConfig.Builder.get().expandMethodCalls(true).build();
    }

    /**
     * @return a config that contains no collapsers and does not expand methods
     */
    public static ApexPathExpanderConfig getSimpleNonExpandingConfig() {
        return ApexPathExpanderConfig.Builder.get().expandMethodCalls(false).build();
    }

    private enum Direction {
        FORWARD,
        BACKWARD;
    }

    /**
     * Object generated during path retrieval. Includes information about all accepted paths, as well as all reasons that paths were rejected.
     */
    public static final class ApexPathRetrievalSummary {
        private final List<ApexPath> acceptedPaths;
        private final List<PathExpansionException> rejectionReasons;

        private ApexPathRetrievalSummary() {
            this.acceptedPaths = new ArrayList<>();
            this.rejectionReasons = new ArrayList<>();
        }

        private ApexPathRetrievalSummary(List<ApexPath> acceptedPaths) {
            this(acceptedPaths, new ArrayList<>());
        }

        private ApexPathRetrievalSummary(
                List<ApexPath> acceptedPaths, List<PathExpansionException> rejectionReasons) {
            this.acceptedPaths = acceptedPaths;
            this.rejectionReasons = rejectionReasons;
        }

        /**
         * Add the results collected by the provided {@link ApexPathCollector} to this summary.
         * @param collector
         */
        private void addExpansionResults(ApexPathCollector collector) {
            this.acceptedPaths.addAll(collector.getAcceptedResults());
            this.rejectionReasons.addAll(collector.getRejectionReasons());
        }

        public List<ApexPath> getAcceptedPaths() {
            return acceptedPaths;
        }

        public List<PathExpansionException> getRejectionReasons() {
            return rejectionReasons;
        }
    }

    private ApexPathUtil() {}
}
