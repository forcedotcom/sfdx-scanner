package com.salesforce.graph;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.ops.TraversalUtil;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.Attachable;

/**
 * This class operates on two graphs. The full graph that contains all of the source code and the
 * graph that the rule is interested in. This allows the rule to execute quicker. The necessary
 * classes are loaded dynamically before any queries that reference {@link Schema#DEFINING_TYPE} are
 * invoked. This is done by copying the vertices from the {@code fullGraph} to the {@code ruleGraph}
 * preserving all ids.
 */
public final class InMemoryJustInTimeGraph implements JustInTimeGraph {
    private static final Logger LOGGER = LogManager.getLogger(InMemoryJustInTimeGraph.class);
    private static final String LOG_SUFFIX = " definingType={}, ruleGraph={}, hc={}";

    private final GraphTraversalSource fullGraph;
    private final GraphTraversalSource ruleGraph;
    private final TreeSet<String> importedTypes;

    InMemoryJustInTimeGraph(GraphTraversalSource fullGraph, String definingType) {
        this.fullGraph = fullGraph;
        this.ruleGraph = GraphUtil.getGraph();
        this.importedTypes = CollectionUtil.newTreeSet();
        importStandardLibrary();
        loadUserClass(definingType);
    }

    GraphTraversalSource getRuleGraph() {
        return ruleGraph;
    }

    /** Find all of the standard Apex classes and copy them over to the rule graph */
    private void importStandardLibrary() {
        GraphTraversal<Vertex, Vertex> traversal =
                TraversalUtil.fileRootTraversal(fullGraph, Collections.emptyList())
                        .has(Schema.IS_STANDARD, true);

        for (Vertex vertex : traversal.toList()) {
            importVertexAndEdges(vertex);
        }
    }

    /**
     * Import the closure of {@code vertex} from the {@code fullGraph} to the {@code ruleGraph}.
     * Follow all edges and import the vertex that the edge points to. Import the edges after the
     * vertex has been imported.
     */
    private void importVertexAndEdges(Vertex vertex) {
        if (ruleGraph.V(vertex.id()).hasNext()) {
            // The vertex has already been imported
            return;
        }
        final Attachable<Vertex> attachableVertex = () -> vertex;
        Attachable.Method.createVertex(attachableVertex, ruleGraph.getGraph());
        final Iterator<Edge> edgeIterator = vertex.edges(Direction.OUT);
        while (edgeIterator.hasNext()) {
            final Edge edge = edgeIterator.next();
            importVertexAndEdges(edge.inVertex());
            importVertexAndEdges(edge.outVertex());
        }
        importEdges(vertex);
    }

    /** Import all of the edges into and out of {@code vertex} */
    private void importEdges(Vertex vertex) {
        final Iterator<Edge> edgeIterator = vertex.edges(Direction.OUT);
        while (edgeIterator.hasNext()) {
            final Edge edge = edgeIterator.next();
            final Attachable<Edge> attachableEdge = () -> edge;
            Attachable.Method.createEdge(attachableEdge, ruleGraph.getGraph());
        }
    }

    /**
     * @return a traversal that is used to load the vertex identified by {@code userClassName}
     */
    private static GraphTraversal<Vertex, Vertex> getUserClassTraversal(
            GraphTraversalSource graph, String userClassName) {
        return graph.V()
                .where(H.has(ASTConstants.NodeType.USER_CLASS, Schema.DEFINING_TYPE, userClassName))
                .where(H.has(ASTConstants.NodeType.USER_CLASS, Schema.NAME, userClassName));
    }

    @Override
    public void loadUserClass(String definingType) {
        if (this.importedTypes.contains(definingType)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Already imported." + LOG_SUFFIX,
                        definingType,
                        ruleGraph,
                        System.identityHashCode(ruleGraph));
            }
            return;
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Importing." + LOG_SUFFIX,
                    definingType,
                    ruleGraph,
                    System.identityHashCode(ruleGraph));
        }
        this.importedTypes.add(definingType);

        // Load the outer class, this will automatically load any inner classes
        final String userClassName = definingType.split("\\.")[0];

        // See if the class has already been imported into the rule graph
        final List<Vertex> existingVertices =
                getUserClassTraversal(ruleGraph, userClassName).toList();
        if (!existingVertices.isEmpty()) {
            // Don't load the vertex if it already exists in the graph. This can happen if the class
            // was loaded because
            // it was referenced by a class loaded via this method previously.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Skipping Existing Class." + LOG_SUFFIX,
                        definingType,
                        ruleGraph,
                        System.identityHashCode(ruleGraph));
            }
            return;
        }

        // Find the vertex in the full graph and import it
        final List<Vertex> vertices = getUserClassTraversal(fullGraph, userClassName).toList();
        if (vertices.size() > 1) {
            throw new UnexpectedException(
                    "UserClassName=" + userClassName + ", vertices=" + vertices);
        } else if (vertices.size() == 1) {
            final Vertex userClassVertex = vertices.get(0);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Importing UserClassName="
                                + userClassName
                                + ", id="
                                + userClassVertex.id()
                                + ", ruleGraph="
                                + ruleGraph
                                + ", hc="
                                + System.identityHashCode(ruleGraph));
            }
            importVertexAndEdges(userClassVertex);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Imported UserClassName="
                                + userClassName
                                + ", id="
                                + userClassVertex.id()
                                + ", ruleGraph="
                                + ruleGraph
                                + ", hc="
                                + System.identityHashCode(ruleGraph));
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Unable to find." + LOG_SUFFIX,
                        definingType,
                        ruleGraph,
                        System.identityHashCode(ruleGraph));
            }
        }
    }
}
