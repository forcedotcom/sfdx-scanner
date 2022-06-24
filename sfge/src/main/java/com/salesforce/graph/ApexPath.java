package com.salesforce.graph;

import com.salesforce.Collectible;
import com.salesforce.NullCollectible;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.DuplicateKeyException;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.expander.RecursionDetectedException;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Represents a set of vertices that will be executed in a particular order. It references other
 * paths that are invoked via method calls.
 */
@NotThreadSafe
@SuppressWarnings("PMD.NullAssignment")
public final class ApexPath implements DeepCloneable<ApexPath>, Collectible<ApexPath> {
    public static final NullCollectible<ApexPath> NULL_VALUE =
            new NullCollectible<>(ApexPath.class);

    /** Used to give each object a unique id */
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    /** The method that contains the vertices found in this path */
    private final MethodVertex methodVertex;

    /** List of vertices contained in {@link #methodVertex} */
    private final List<BaseSFVertex> vertices;

    /**
     * The path that corresponds to an instance initialization path when this path is the top most
     * path and {@link #methodVertex} points to an instance method.
     */
    private final ApexPath instanceInitializationPath;

    /**
     * The path that corresponds to a constructor when this path is the top most path and {@link
     * #methodVertex} points to an instance method.
     */
    private ApexPath constructorPath;

    /**
     * Map of instance creation to the instantiation path for that instance. The following example
     * would contain a key for 'new MyClass()' that points to the path that initializes the instance
     * value 's'.
     *
     * <pre>{@code
     * public class MyClass {
     *     private String s = 'Hello';
     * }
     *
     * public class UseMyClass() {
     *     public void doSomething() {
     *         MyClass c = new MyClass();
     *     }
     * }
     *
     * }</pre>
     */
    private final HashMap<NewObjectExpressionVertex, Collectible<ApexPath>>
            newInstanceToInitializationPath;

    /**
     * Map of instance creation to the instantiation path for that instance. The following example
     * would contain a key for 'MyClass' that points to the path that initializes the instance value
     * 's'.
     *
     * <pre>{@code
     * public class MyClass {
     *     private static String s = 'Hello';
     *     public static void logSomething() {
     *         System.debug(s);
     *     }
     * }
     *
     * public class UseMyClass() {
     *     public void doSomething() {
     *         MyClass.logSomething();
     *     }
     * }
     *
     * }</pre>
     */
    private final TreeMap<String, Collectible<ApexPath>> staticClassNameToInitializationPath;

    /**
     * Maintain relationship to the ApexPath that should be traversed when the
     * InvocableWithParameters vertex is encountered
     */
    private final HashMap<InvocableVertex, ApexPath> invocableVertexToPath;

    /** Easy way to find the vertex that relates to a invocable call */
    private final HashMap<InvocableVertex, BaseSFVertex> invocableVertexToTopLevelVertex;

    /**
     * True if this path ends in an exception. Note that this is slightly different than the {@link
     * #endsInException()} method. The method will return true if any path in the data structure has
     * a true value for {@link #pathEndsInException}
     */
    private boolean pathEndsInException;

    /**
     * Record the method {@link MethodCallExpressionVertex) that would have caused recursion.
     */
    private MethodCallExpressionVertex vertexThatCausesRecursion;

    /**
     * Id that is stable across cloning. This is the value that should be maintained in maps etc.
     */
    private final Long stableId;

    /** Contains summary information about vertices in the path. */
    private ApexPathVertexMetaInfo apexPathVertexMetaInfo;

    public ApexPath(@Nullable MethodVertex methodVertex) {
        this.stableId = ID_GENERATOR.incrementAndGet();
        this.methodVertex = methodVertex;
        this.vertices = new ArrayList<>();
        this.constructorPath = null;
        this.newInstanceToInitializationPath = new HashMap<>();
        this.staticClassNameToInitializationPath = CollectionUtil.newTreeMap();
        this.invocableVertexToPath = new HashMap<>();
        this.invocableVertexToTopLevelVertex = new HashMap<>();
        this.vertexThatCausesRecursion = null;
        this.pathEndsInException = false;
        if (methodVertex != null) {
            // This will use the cache, it's ok to use the full graph
            final GraphTraversalSource g = VertexCacheProvider.get().getFullGraph();
            final String className = methodVertex.getDefiningType();
            if (methodVertex.isStatic()) {
                this.instanceInitializationPath = null;
            } else {
                this.instanceInitializationPath =
                        ClassInstanceScope.getInitializationPath(g, className).orElse(null);
            }
        } else {
            this.instanceInitializationPath = null;
        }
        this.apexPathVertexMetaInfo = null;
    }

    private ApexPath(ApexPath other) {
        this.stableId = other.stableId;
        this.methodVertex = other.methodVertex;
        this.vertices = CloneUtil.cloneArrayList(other.vertices);
        this.constructorPath = CloneUtil.clone(other.constructorPath);
        this.newInstanceToInitializationPath =
                CloneUtil.cloneHashMap(other.newInstanceToInitializationPath);
        this.staticClassNameToInitializationPath =
                CloneUtil.cloneTreeMap(other.staticClassNameToInitializationPath);
        this.invocableVertexToPath = CloneUtil.cloneHashMap(other.invocableVertexToPath);
        this.invocableVertexToTopLevelVertex =
                CloneUtil.cloneHashMap(other.invocableVertexToTopLevelVertex);
        if (other.vertexThatCausesRecursion != null) {
            // Paths that end in recursion should never be cloned
            throw new ProgrammingException(this);
        }
        this.vertexThatCausesRecursion = null;
        // Paths that end in an exception will be cloned if the topmost path is the path with the
        // exception
        this.pathEndsInException = other.pathEndsInException;
        this.instanceInitializationPath = CloneUtil.clone(other.instanceInitializationPath);
        this.apexPathVertexMetaInfo = CloneUtil.clone(other.apexPathVertexMetaInfo);
    }

    @Override
    public ApexPath deepClone() {
        return new ApexPath(this);
    }

    public Optional<ApexPathVertexMetaInfo> getApexPathMetaInfo() {
        return Optional.ofNullable(apexPathVertexMetaInfo);
    }

    public void setApexPathMetaInfo(ApexPathVertexMetaInfo apexPathVertexMetaInfo) {
        if (this.apexPathVertexMetaInfo != null) {
            throw new UnexpectedException(this);
        }
        this.apexPathVertexMetaInfo = apexPathVertexMetaInfo;
    }

    public Map<InvocableVertex, ApexPath> getInvocableVertexToPaths() {
        return Collections.unmodifiableMap(this.invocableVertexToPath);
    }

    public Optional<Collectible<ApexPath>> getNewInstanceToInitializationPath(
            NewObjectExpressionVertex newObjectExpression) {
        return Optional.ofNullable(newInstanceToInitializationPath.get(newObjectExpression));
    }

    public Optional<Collectible<ApexPath>> getStaticInitializationPath(String className) {
        return Optional.ofNullable(staticClassNameToInitializationPath.get(className));
    }

    public void putStaticInitializationPath(String className, Collectible<ApexPath> apexPath) {
        Collectible<ApexPath> previous =
                staticClassNameToInitializationPath.put(className, apexPath);
        if (previous != null) {
            throw new DuplicateKeyException(className, previous, apexPath);
        }
    }

    public void setConstructorPath(ApexPath constructorPath) {
        this.constructorPath = constructorPath;
    }

    public Optional<ApexPath> getConstructorPath() {
        return Optional.ofNullable(constructorPath);
    }

    public Optional<ApexPath> getInstanceInitializationPath() {
        return Optional.ofNullable(instanceInitializationPath);
    }

    /** Find the path which corresponds to the stable id. */
    public ApexPath getPathWithStableId(Long stableId) {
        ApexPath result = _getPathWithStableId(stableId).orElse(null);
        if (result == null) {
            throw new NoSuchElementException("id=" + stableId + ", this=" + this);
        }
        return result;
    }

    public int resolvedInvocableCallCountInCurrentMethod() {
        return this.invocableVertexToPath.size();
    }

    /**
     * This method will return an empty optional when the path refers to a class instantiation.
     * These paths don't have a corresponding method.
     */
    public Optional<MethodVertex> getMethodVertex() {
        return Optional.ofNullable(this.methodVertex);
    }

    public Long getStableId() {
        return this.stableId;
    }

    public void addVertices(List<BaseSFVertex> vertices) {
        if (this.vertices.isEmpty()
                &&
                // Method Path
                !(vertices.get(0) instanceof BlockStatementVertex)
                &&
                // Class Instantiation Path
                !(vertices.get(0) instanceof FieldVertex)
				&&
				// Static blocks
				!(vertices.get(0) instanceof MethodCallExpressionVertex)) {
            throw new UnexpectedException(vertices);
        }
        this.vertices.addAll(vertices);
    }

    public List<BaseSFVertex> verticesInCurrentMethod() {
        return Collections.unmodifiableList(vertices);
    }

    public int size() {
        return this.vertices.size();
    }

    public int totalNumberOfVertices() {
        int total = 0;

        for (ApexPath path : invocableVertexToPath.values()) {
            total += path.totalNumberOfVertices();
        }

        total += vertices.size();

        return total;
    }

    public BaseSFVertex firstVertex() {
        return vertices.get(0);
    }

    public BaseSFVertex lastVertex() {
        return vertices.get(vertices.size() - 1);
    }

    public void putInvocableExpression(Pair<BaseSFVertex, InvocableVertex> pair, ApexPath path) {
        BaseSFVertex topLevelVertex = pair.getLeft();
        InvocableVertex invocableExpression = pair.getRight();
        invocableVertexToTopLevelVertex.put(invocableExpression, topLevelVertex);

        // This path should not be appended to if one of its method calls ends in an exception or is
        // recursive
        if (pathEndsInException || vertexThatCausesRecursion != null) {
            throw new UnexpectedException(this);
        }

        ApexPath previous = invocableVertexToPath.put(invocableExpression, path);
        if (previous != null) {
            throw new DuplicateKeyException(invocableExpression, previous, path);
        }
    }

    public void putNewObjectExpression(
            NewObjectExpressionVertex newObjectExpression, Collectible<ApexPath> path) {
        Collectible<ApexPath> previous =
                newInstanceToInitializationPath.put(newObjectExpression, path);
        if (previous != null) {
            throw new DuplicateKeyException(newObjectExpression, previous, path);
        }
    }

    public void putPathEndsInException(BaseSFVertex vertex) {
        verifyContainsVertex(vertex);
        pathEndsInException = true;
    }

    /**
     * Verify that #putPathEndsInException was called on the correct path.
     *
     * <p>This method works around the issue that #putPathEndsInException is called with
     * StandardCondition$Unknown instances but the path contains StandardCondition$Negative or
     * StandardCondition$Positive classes.
     */
    private void verifyContainsVertex(BaseSFVertex topLevelVertex) {
        if (vertices.contains(topLevelVertex)) {
            return;
        } else if (topLevelVertex instanceof StandardConditionVertex.Unknown) {
            for (BaseSFVertex vertex : vertices) {
                if (vertex instanceof StandardConditionVertex
                        && vertex.getId().equals(topLevelVertex.getId())) {
                    return;
                }
            }
        }

        throw new UnexpectedException(topLevelVertex);
    }

    public void putPathEndsInRecursion(Pair<BaseSFVertex, MethodCallExpressionVertex> pair) {
        BaseSFVertex topLevelVertex = pair.getLeft();
        if (!vertices.contains(topLevelVertex)) {
            throw new UnexpectedException(this);
        }

        vertexThatCausesRecursion = pair.getRight();
    }

    public ThrowStatementVertex getThrowStatement() {
        if (lastVertex() instanceof ThrowStatementVertex) {
            return (ThrowStatementVertex) lastVertex();
        }

        for (ApexPath path : invocableVertexToPath.values()) {
            ThrowStatementVertex exception = path.getThrowStatement();
            if (exception != null) {
                return exception;
            }
        }

        return null;
    }

    public Optional<ApexPath> resolveInvocableCall(InvocableVertex invocable)
            throws RecursionDetectedException {
        ApexPath result = invocableVertexToPath.get(invocable);
        if (vertexThatCausesRecursion != null && vertexThatCausesRecursion.equals(invocable)) {
            if (result == null) {
                throw new UnexpectedException(this);
            }
            throw new RecursionDetectedException(result, vertexThatCausesRecursion);
        }
        return Optional.ofNullable(result);
    }

    /** @return true if any paths related to this path ends in an exception. */
    public boolean endsInException() {
        if (pathEndsInException) {
            return true;
        }

        if (this.vertices.isEmpty()) {
            throw new UnexpectedException(this);
        }

        if (lastVertex() instanceof ThrowStatementVertex) {
            return true;
        }

        for (ApexPath path : invocableVertexToPath.values()) {
            if (path.endsInException()) {
                return true;
            }
        }

        return false;
    }

    public Optional<ThrowStatementVertex> getThrowStatementVertex() {
        if (lastVertex() instanceof ThrowStatementVertex) {
            return Optional.of((ThrowStatementVertex) lastVertex());
        }

        for (ApexPath path : invocableVertexToPath.values()) {
            Optional<ThrowStatementVertex> result = path.getThrowStatementVertex();
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public ApexPath getCollectible() {
        return this;
    }

    /** Users should not call #equals on this. It would be too expensive. */
    @Override
    public boolean equals(Object o) {
        throw new UnexpectedException("Equality should never be checked on this item.");
    }

    @Override
    public int hashCode() {
        throw new UnexpectedException("Equality should never be checked on this item.");
    }

    @Override
    public String toString() {
        return "ApexPath{"
                + "stableId="
                + stableId
                + ", methodVertex="
                + methodVertex
                + ", vertices="
                + vertices
                + '}';
    }

    private Optional<ApexPath> _getPathWithStableId(Long stableId) {
        if (this.stableId.equals(stableId)) {
            return Optional.of(this);
        }

        for (Collectible<ApexPath> path : newInstanceToInitializationPath.values()) {
            if (path.getCollectible() == null) {
                continue;
            }
            Optional<ApexPath> result = path.getCollectible()._getPathWithStableId(stableId);
            if (result.isPresent()) {
                return result;
            }
        }

        for (Collectible<ApexPath> path : staticClassNameToInitializationPath.values()) {
            if (path.getCollectible() == null) {
                continue;
            }
            Optional<ApexPath> result = path.getCollectible()._getPathWithStableId(stableId);
            if (result.isPresent()) {
                return result;
            }
        }

        for (ApexPath path : invocableVertexToPath.values()) {
            Optional<ApexPath> result = path._getPathWithStableId(stableId);
            if (result.isPresent()) {
                return result;
            }
        }

        if (constructorPath != null && constructorPath.getCollectible() != null) {
            Optional<ApexPath> result =
                    constructorPath.getCollectible()._getPathWithStableId(stableId);
            if (result.isPresent()) {
                return result;
            }
        }

        if (instanceInitializationPath != null
                && instanceInitializationPath.getCollectible() != null) {
            Optional<ApexPath> result =
                    instanceInitializationPath.getCollectible()._getPathWithStableId(stableId);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
