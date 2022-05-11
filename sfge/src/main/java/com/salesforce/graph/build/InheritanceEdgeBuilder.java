package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.InheritableSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.UserInterfaceVertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class InheritanceEdgeBuilder implements GraphBuilder {
    private static final Logger LOGGER = LogManager.getLogger(InheritanceEdgeBuilder.class);
    private final GraphTraversalSource g;
    private final Map<Long, InheritableSFVertex> verticesById;
    private final TreeMap<String, InheritableSFVertex> verticesByDefiningType;

    public InheritanceEdgeBuilder(GraphTraversalSource g) {
        this.g = g;
        this.verticesById = new HashMap<>();
        this.verticesByDefiningType = CollectionUtil.newTreeMap();
    }

    @Override
    public void build() {
        buildInheritanceMaps();

        for (InheritableSFVertex v : verticesById.values()) {
            processVertexInheritance(v);
        }
    }

    private void buildInheritanceMaps() {
        // Get all of the vertices representing definitions of types that could be inherited (i.e.
        // classes and interfaces).
        List<String> inheritableLabels =
                new ArrayList<>(
                        Arrays.asList(
                                ASTConstants.NodeType.USER_INTERFACE,
                                ASTConstants.NodeType.USER_CLASS));
        List<BaseSFVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().hasLabel(P.within(inheritableLabels))
                                .not(has(Schema.IS_STANDARD, true)));

        // Turn each vertex into one of our vertex objects and then map that by both its ID and its
        // DefiningType (i.e.
        // the full name, including any outer classes).
        for (BaseSFVertex vertex : vertices) {
            InheritableSFVertex inheritableSFVertex = (InheritableSFVertex) vertex;
            this.verticesById.put(inheritableSFVertex.getId(), inheritableSFVertex);
            this.verticesByDefiningType.put(
                    inheritableSFVertex.getDefiningType(), inheritableSFVertex);
        }
    }

    private void processVertexInheritance(InheritableSFVertex vertex) {
        InheritableSFVertex extendedVertex = findExtendedVertex(vertex).orElse(null);
        List<InheritableSFVertex> implementedVertices = findImplementedVertices(vertex);

        // It's possible for the extendedVertex to be null, if the extended class is declared in a
        // file that wasn't scanned.
        if (extendedVertex != null) {
            addEdges(
                    extendedVertex.getId(),
                    vertex.getId(),
                    Schema.EXTENDED_BY,
                    Schema.EXTENSION_OF);
        }

        if (implementedVertices != null) {
            for (InheritableSFVertex implementedVertex : implementedVertices) {
                // It's possible for the implementedVertex to be null if the interface being
                // implemented is declared in a file
                // that wasn't scanned.
                if (implementedVertex != null) {
                    Long implId = implementedVertex.getId();
                    Long myId = vertex.getId();
                    addEdges(implId, myId, Schema.IMPLEMENTED_BY, Schema.IMPLEMENTATION_OF);
                }
            }
        }
    }

    private Optional<InheritableSFVertex> findExtendedVertex(InheritableSFVertex vertex) {
        String superClassName = vertex.getSuperClassName().orElse(null);
        if (superClassName != null) {
            return findInheritedVertex(superClassName, vertex.getDefiningType());
        } else {
            return Optional.empty();
        }
    }

    private List<InheritableSFVertex> findImplementedVertices(InheritableSFVertex vertex) {
        if (vertex instanceof UserInterfaceVertex) {
            return new ArrayList<>();
        } else {
            String inheritorType = vertex.getDefiningType();
            return ((UserClassVertex) vertex)
                    .getInterfaceNames().stream()
                            .map(i -> findInheritedVertex(i, inheritorType))
                            .filter(v -> v.isPresent())
                            .map(v -> v.get())
                            .collect(Collectors.toList());
        }
    }

    private Optional<InheritableSFVertex> findInheritedVertex(
            String inheritableType, String inheritorType) {
        if (inheritableType == null) {
            throw new UnexpectedException("inheritableType can't be null");
        }
        // If the inheritor type contains a period, it's an inner type. If the inheritable type does
        // not contain a period,
        // it could be either an outer type, or an inner type declared in the same outer type. The
        // latter overrides the
        // former, so we'll derive the full name of such an inner type so we can check if it exists.
        if (inheritorType.contains(".") && !inheritableType.contains(".")) {
            String[] parts = inheritorType.split("\\.");

            String possibleInnerType = parts[0] + "." + inheritableType;
            if (verticesByDefiningType.containsKey(possibleInnerType)) {
                return Optional.ofNullable(verticesByDefiningType.get(possibleInnerType));
            }
        }

        // If we didn't find (or look for) an inner type, look for an outer type using the
        // originally provided name.
        if (verticesByDefiningType.containsKey(inheritableType)) {
            return Optional.ofNullable(verticesByDefiningType.get(inheritableType));
        }

        // If we still didn't find anything, log it and return null.
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    String.format(
                            "Type '%s' inherits from '%s', which is not declared in any of the scanned files.",
                            inheritorType, inheritableType));
        }
        return Optional.empty();
    }

    private void addEdges(Long inheritableId, Long inheritorId, String fromEdge, String toEdge) {
        Vertex inheritable = g.V(inheritableId).next();
        Vertex inheritor = g.V(inheritorId).next();
        g.addE(fromEdge).from(inheritable).to(inheritor).iterate();
        g.addE(toEdge).from(inheritor).to(inheritable).iterate();
    }
}
