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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
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
        Map<String, InheritableSFVertex> implementedVerticesByName =
                findImplementedVerticesByName(vertex);

        // It's possible for the extendedVertex to be null, if the extended class is declared in a
        // file that wasn't scanned.
        if (extendedVertex != null) {
            addEdges(
                    extendedVertex.getId(),
                    vertex.getId(),
                    Schema.EXTENDED_BY,
                    Schema.EXTENSION_OF);
        }

        if (!implementedVerticesByName.isEmpty()) {
            List<String> implementedVertexDefiningTypes = new ArrayList<>();
            Long myId = vertex.getId();
            for (String implementedVertexName : implementedVerticesByName.keySet()) {
                InheritableSFVertex implementedVertex =
                        implementedVerticesByName.get(implementedVertexName);
                // If we actually have a vertex for this interface, we need to create edges
                // connecting it to the implementor,
                // and we can use its defining type for our list.
                if (implementedVertex != null) {
                    Long implId = implementedVertex.getId();
                    addEdges(implId, myId, Schema.IMPLEMENTED_BY, Schema.IMPLEMENTATION_OF);
                    implementedVertexDefiningTypes.add(implementedVertex.getDefiningType());
                } else {
                    // If there's no vertex for this interface, we can assume it's defined in some
                    // other codebase, and therefore
                    // whatever name was used by the implementor to reference it can be assumed to
                    // be the defining type.
                    implementedVertexDefiningTypes.add(implementedVertexName);
                }
            }
            // Give the implementor new properties indicating the full names of the implemented
            // interfaces.
            addProperties(myId, implementedVertexDefiningTypes);
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

    private Map<String, InheritableSFVertex> findImplementedVerticesByName(
            InheritableSFVertex vertex) {
        Map<String, InheritableSFVertex> results = new HashMap<>();
        // Only classes can implement interfaces.
        if (vertex instanceof UserClassVertex) {
            String inheritorType = vertex.getDefiningType();
            List<String> interfaceNames = ((UserClassVertex) vertex).getInterfaceNames();
            for (String interfaceName : interfaceNames) {
                InheritableSFVertex inheritedVertex =
                        findInheritedVertex(interfaceName, inheritorType).orElse(null);
                results.put(interfaceName, inheritedVertex);
            }
        }
        return results;
    }

    private Optional<InheritableSFVertex> findInheritedVertex(
            String inheritableType, String inheritorType) {
        if (inheritableType == null) {
            throw new UnexpectedException("inheritableType can't be null");
        }
        // If the inheritable type does NOT contain a period, then it's either an outer type, or an
        // inner type declared
        // in the same outer type as the inheritor type.
        // The latter takes priority over the former, so we should check if it's the case.
        if (!inheritableType.contains(".")) {
            // If the inheritor type contains a period, then it's an inner type, and we should use
            // its outer type.
            // Otherwise, we should use the inheritor type as a potential outer type.
            String potentialOuterType =
                    inheritorType.contains(".") ? inheritorType.split("\\.")[0] : inheritorType;
            String potentialInnerType = potentialOuterType + "." + inheritableType;
            if (verticesByDefiningType.containsKey(potentialInnerType)) {
                return Optional.ofNullable(verticesByDefiningType.get(potentialInnerType));
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

    private void addProperties(Long inheritorId, List<String> implementedVertexDefiningTypes) {
        GraphTraversal<Vertex,Vertex> traversal = g.V(inheritorId);
        // It's probably not best practice to be using an empty treeset here, but we no longer have access to the treeset
        // that was used when adding the base properties, and we're setting a property that definitely isn't set elsewhere,
        // so it should be fine.
        GremlinVertexUtil.addProperty(new TreeSet<>(), traversal, Schema.INTERFACE_DEFINING_TYPES, implementedVertexDefiningTypes);
        traversal.iterate();
    }
}
