package com.salesforce.graph.vertex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.build.GremlinUtil;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.ops.ReflectionUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Responsible for interacting with the graph database and returning strongly typed Java objects
 * that represent the specific type of vertex which was retrieved from the vertex.
 *
 * <p>There are three types of methods:
 *
 * <ol>
 *   <li>Convert a generic gremlin vertex to a specific Java implementation
 *   <li>Load a gremlin vertex based on its id and convert it a specific Java implementation
 *   <li>Execute a gremlin query and convert the vertex/vertices to the strongly typed object
 * </ol>
 *
 * Each of these methods typically has a two arg and three arg version. The three arg version has an
 * overloaded {@code supplementalParam} argument that is currently overloaded. This parameter is
 * interpreted in different vertex specific ways.
 *
 * <p>Some examples of supplementalParam
 *
 * <ul>
 *   <li>{@link StandardConditionVertex.Builder} interprets this to decide which subclass to create
 *   <li>{@link VariableExpressionVertex.ForLoop} accepts the set of loop values
 *   <li>{@link LiteralExpressionVertex}, {@link MethodCallExpressionVertex}, {@link
 *       VariableExpressionVertex.Single}, {@link
 *       VariableExpressionVertex.SelfReferentialInstanceProperty}, {@link
 *       NewKeyValueObjectExpressionVertex} use it to declare the {@link ExpressionType} specific to
 *       themselves or the items they contain.
 *   <li>{@link CacheBehavior#NO_CACHE} is used to disable caching
 * </ul>
 *
 * TODO: Currently all of the supplementalParam use cases are independent enough that this single
 * parameter can be overloaded, but this is risky and confusing, convert supplementalParam to a
 * strongly typed object.
 */
public final class SFVertexFactory {
    private static final String CREATE_METHOD = "create";
    private static final Logger LOGGER = LogManager.getLogger(SFVertexFactory.class);

    public enum CacheBehavior {
        /**
         * Callers should pass this as a supplemental parameter when they don't want the cache to be
         * used. This should be used in cases where the caller intends to mutate the vertex, such as
         * in a builder.
         */
        NO_CACHE
    }

    /**
     * Convert the Gremlin {@code vertex} that was already loaded to its specific Java
     * implementation.
     */
    public static <T extends BaseSFVertex> T load(GraphTraversalSource g, Vertex vertex) {
        return load(g, vertex, null);
    }

    public static <T extends BaseSFVertex> T load(
            GraphTraversalSource g, Vertex vertex, Object supplementalParam) {
        GraphTraversal<Vertex, Vertex> traversal = g.V(vertex.id());
        return load(g, traversal, supplementalParam);
    }

    public static <T extends BaseSFVertex> T load(GraphTraversalSource g, Long id) {
        return load(g, id, null);
    }

    /**
     * Load the vertex identified by {@code id} and convert it to a Java specific implementation.
     */
    public static <T extends BaseSFVertex> T load(
            GraphTraversalSource g, Long id, Object supplementalParam) {
        GraphTraversal<Vertex, Vertex> traversal = g.V(id);
        return load(g, traversal, supplementalParam);
    }

    /**
     * Load the vertex returned by {@code traversal} and convert it to a Java specific
     * implementation. Use {@link #loadSingleOrNull(GraphTraversalSource, GraphTraversal)} if the
     * query could return zero vertices.s
     *
     * @throws NoSuchElementException if the query returns no vertices
     */
    public static <T extends BaseSFVertex> T load(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> traversal) {
        return load(g, traversal, null);
    }

    public static <T extends BaseSFVertex> T load(
            GraphTraversalSource g,
            GraphTraversal<Vertex, Vertex> traversal,
            Object supplementalParam) {
        T vertex = loadSingleOrNull(g, traversal, supplementalParam);

        if (vertex == null) {
            throw new NoSuchElementException("No vertex found: " + traversal);
        }

        return vertex;
    }

    /**
     * Load the vertex returned by {@code traversal} and convert it to a Java specific
     * implementation. This differs from {@link #load(GraphTraversalSource, GraphTraversal)} in that
     * it returns null when no vertex is found.
     *
     * @throws UnexpectedException if more than one vertex is found
     */
    @Nullable
    public static <T extends BaseSFVertex> T loadSingleOrNull(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> traversal) {
        return loadSingleOrNull(g, traversal, null);
    }

    public static <T extends BaseSFVertex> T loadSingleOrNull(
            GraphTraversalSource g,
            GraphTraversal<Vertex, Vertex> traversal,
            Object supplementalParam) {
        List<T> vertices = loadVertices(g, traversal, supplementalParam);

        if (vertices.size() > 1) {
            throw new UnexpectedException("Found more than one vertex. vertices=" + vertices);
        } else if (vertices.isEmpty()) {
            return null;
        }

        return vertices.get(0);
    }

    /**
     * Load the vertex returned by {@code traversal} and convert it to a list of Java specific
     * implementations.
     */
    public static <T extends BaseSFVertex> List<T> loadVertices(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> traversal) {
        return loadVertices(g, traversal, null);
    }

    public static <T extends BaseSFVertex> List<T> loadVertices(
            GraphTraversalSource g,
            GraphTraversal<Vertex, Vertex> traversal,
            Object overloadedSupplementalParam) {
        // TODO: The overloading of overloadedSupplementalParam as a Vertex parameter and
        // CacheBehavior can be improved
        final boolean useCache;
        final Object supplementalParam;
        if (overloadedSupplementalParam == CacheBehavior.NO_CACHE) {
            useCache = false;
            // Null out the parameter since it has no meaning to the vertices
            supplementalParam = null;
        } else {
            useCache = true;
            supplementalParam = overloadedSupplementalParam;
        }

        if (useCache) {
            // List of vertices returned from the query. This is a minimal object that only includes
            // the id and label
            final List<Vertex> vertices = traversal.toList();
            return VertexCacheProvider.get()
                    .get(
                            vertices,
                            supplementalParam,
                            new Function<Map<Object, Object>, T>() {
                                @Override
                                public T apply(Map<Object, Object> vertexProperties) {
                                    return load(vertexProperties, supplementalParam);
                                }
                            });
        } else {
            return traversal.elementMap().toList().stream()
                    .map(v -> (T) load(v, supplementalParam))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Convert the {@code vertexProperties} to a strongly typed Java vertex. This method uses the
     * label from the {@code vertexProperties} to find the correct vertex or vertex builder to
     * invoke. {@code supplementalParam} is passed to the vertex/vertex builder for further
     * specialization.
     */
    private static <T extends BaseSFVertex> T load(
            Map<Object, Object> vertexProperties, Object supplementalParam) {
        T result = null;

        String label = GremlinUtil.getLabel(vertexProperties);

        try {
            Optional<Class<?>> optClazz = ReflectionUtil.getClass(getVertexBuilderName(label));
            if (optClazz.isPresent()) {
                // Try to find a static method that is capable of a more complex instantiation,
                // these are typically used
                // when their isn't a direct 1:1 mapping between the graph and the type, for
                // instance LiteralExpression
                // nodes are converted into a more specific type that returns a strongly typed
                // value.
                // TODO: Some of these implementations could be replaced with a graph builder that
                // mutates the graph
                if (supplementalParam != null) {
                    Method m =
                            optClazz.get()
                                    .getDeclaredMethod(CREATE_METHOD, Map.class, Object.class);
                    result = (T) m.invoke(null, vertexProperties, supplementalParam);
                } else {
                    Method m = optClazz.get().getDeclaredMethod(CREATE_METHOD, Map.class);
                    result = (T) m.invoke(null, vertexProperties);
                }
            } else {
                optClazz = ReflectionUtil.getClass(getVertexClassName(label));
                if (optClazz.isPresent()) {
                    if (supplementalParam != null) {
                        Constructor constructor =
                                optClazz.get().getDeclaredConstructor(Map.class, Object.class);
                        result = (T) constructor.newInstance(vertexProperties, supplementalParam);
                    } else {
                        Constructor constructor = optClazz.get().getDeclaredConstructor(Map.class);
                        result = (T) constructor.newInstance(vertexProperties);
                    }
                }
            }
        } catch (NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            throw new UnexpectedException(ex.getMessage(), ex);
        }

        if (result == null) {
            if (supplementalParam != null) {
                throw new UnexpectedException(
                        "Any vertices that expect a supplementalParam need a specific implementation. label="
                                + label
                                + ", supplementalParam="
                                + supplementalParam);
            }
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Using default vertex. Unable to find class=" + label);
            }
            return (T) new DefaultVertex(vertexProperties);
        } else {
            return (T) result;
        }
    }

    /**
     * @return the Java class name that corresponds to the vertex with the given label.
     */
    private static String getVertexClassName(String label) {
        return "com.salesforce.graph.vertex." + label + "Vertex";
    }

    /**
     * @return the name of an inner Java class that contains more complex construction logic. Only
     *     more complex vertices hav a builder.
     */
    private static String getVertexBuilderName(String label) {
        return getVertexClassName(label) + "$Builder";
    }

    private SFVertexFactory() {}
}
