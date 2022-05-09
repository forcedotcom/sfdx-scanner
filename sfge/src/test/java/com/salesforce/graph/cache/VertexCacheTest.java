package com.salesforce.graph.cache;

import static com.salesforce.matchers.OptionalMatcher.optEqualTo;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.CollectibleObject;
import com.salesforce.TestUtil;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.JustInTimeGraph;
import com.salesforce.graph.JustInTimeGraphProvider;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VertexCacheTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    /**
     * This test loads the MyParentClass and MySubClass and UserClassVertices twice. The first load
     * causes a cache miss, the second does not.
     */
    @Test
    public void testGetById() {
        String[] sourceCode = {
            "public class MyParentClass {\n"
                    + "	public void doSomething() {\n"
                    + "   	System.debug('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MySubClass extends MyParentClass {\n" + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);

        // Overwrite the existing cache provider because the VertexBuilders have already loaded
        // vertices into the cache
        VertexCacheTestProvider.initializeForTest();
        final VertexCache vertexCache = VertexCacheProvider.get();
        vertexCache.initialize(g);

        final List<Vertex> vertices =
                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                        .not(has(Schema.IS_STANDARD, true))
                        // Guarantee vertex order
                        .order()
                        .by(Schema.NAME)
                        .toList();
        MatcherAssert.assertThat(vertices, hasSize(equalTo(2)));
        final Vertex parentClass = vertices.get(0);
        final Vertex subClass = vertices.get(1);

        IdCacheMissFunction cacheMissFunction;
        List<BaseSFVertex> userClassVertices;

        // The first load SHOULD cause a cache miss
        cacheMissFunction = new IdCacheMissFunction(g);
        userClassVertices = vertexCache.get(vertices, null, cacheMissFunction);
        MatcherAssert.assertThat(userClassVertices, hasSize(equalTo(2)));
        MatcherAssert.assertThat(cacheMissFunction.getCacheMisses(), hasSize(equalTo(2)));
        // The cache misses happen in the order that the vertices were requested
        MatcherAssert.assertThat(
                cacheMissFunction.getCacheMisses(), contains(parentClass.id(), subClass.id()));
        MatcherAssert.assertThat(userClassVertices.get(0).getId(), equalTo(parentClass.id()));
        MatcherAssert.assertThat(userClassVertices.get(1).getId(), equalTo(subClass.id()));

        // The second load SHOULD NOT cause a cache miss
        cacheMissFunction = new IdCacheMissFunction(g);
        userClassVertices = vertexCache.get(vertices, null, cacheMissFunction);
        MatcherAssert.assertThat(userClassVertices, hasSize(equalTo(2)));
        MatcherAssert.assertThat(cacheMissFunction.getCacheMisses(), hasSize(equalTo(0)));
        MatcherAssert.assertThat(userClassVertices.get(0).getId(), equalTo(parentClass.id()));
        MatcherAssert.assertThat(userClassVertices.get(1).getId(), equalTo(subClass.id()));
    }

    /**
     * This test demonstrates storing the result of a query using a {@link VertexCacheKey}. It
     * demonstrates two different outcomes for a query that returns the parent class for a given
     * UserClassVertex.
     *
     * <ol>
     *   <li>Stores a NON-NULL result: The query for the parent class of "MySubClass" finds the
     *       "MyParentClass" vertex. A mapping of MyVertexCacheKey(MySubClass)->MyParentClass vertex
     *       is stored in the cache.
     *   <li>Store a NULL result: The query for the parent of "MyParentClass" does not find a
     *       vertex. A mapping of MyVertexCacheKey(MyParentClass)->{@link
     *       UserClassVertex#NULL_VALUE} is stored in the cache.
     * </ol>
     */
    @Test
    public void testGetByVertexCacheKey() {
        String[] sourceCode = {
            "public class MyParentClass {\n"
                    + "	public void doSomething() {\n"
                    + "   	System.debug('Hello');\n"
                    + "   }\n"
                    + "}\n",
            "public class MySubClass extends MyParentClass {\n" + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);
        UserClassVertex parentClassVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyParentClass"));
        UserClassVertex subclassVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MySubClass"));

        // Unlike #testGetById we we can use the existing cache because we are using custom cache
        // keys
        final VertexCache vertexCache = VertexCacheProvider.get();

        VertexCache.CacheKey cacheKey;
        ParentClassSupplier parentClassSupplier;
        Optional<UserClassVertex> foundVertex;

        // Attempt to find the parent class of the subclassVertex. This SHOULD find a vertex
        // The first load SHOULD cause a cache miss
        cacheKey = new MyVertexCacheKey(subclassVertex);
        parentClassSupplier = new ParentClassSupplier(g, subclassVertex);
        foundVertex = vertexCache.get(cacheKey, parentClassSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(true));
        MatcherAssert.assertThat(parentClassSupplier.getCacheMisses(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(foundVertex, optEqualTo(parentClassVertex));

        // The second load SHOULD NOT cause a cache miss
        parentClassSupplier = new ParentClassSupplier(g, subclassVertex);
        foundVertex = vertexCache.get(cacheKey, parentClassSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(true));
        MatcherAssert.assertThat(parentClassSupplier.getCacheMisses(), hasSize(equalTo(0)));
        MatcherAssert.assertThat(foundVertex, optEqualTo(parentClassVertex));

        // Attempt to find the parent class of the parentClassVertex. This SHOULD NOT find a vertex
        // The first load SHOULD cause a cache miss
        cacheKey = new MyVertexCacheKey(parentClassVertex);
        parentClassSupplier = new ParentClassSupplier(g, parentClassVertex);
        foundVertex = vertexCache.get(cacheKey, parentClassSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(false));
        MatcherAssert.assertThat(parentClassSupplier.getCacheMisses(), hasSize(equalTo(1)));

        // The second load SHOULD NOT cause a cache miss
        cacheKey = new MyVertexCacheKey(parentClassVertex);
        parentClassSupplier = new ParentClassSupplier(g, parentClassVertex);
        foundVertex = vertexCache.get(cacheKey, parentClassSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(false));
        MatcherAssert.assertThat(parentClassSupplier.getCacheMisses(), hasSize(equalTo(0)));
    }

    /**
     * This test demonstrates storing the result of a query using a {@link CaseInsensitiveCacheKey}.
     * It demonstrates two different outcomes for a query that returns parent class for a given
     * string.
     *
     * <ol>
     *   <li>Stores a NON-NULL result: The query for the class named "MyClass" finds the "MyClass"
     *       vertex. A mapping of MyCaseInsensitiveCacheKey("MyClass")->MyClass vertex is stored in
     *       the cache.
     *   <li>Store a NULL result: The query for the class named "DoesNotExist" does not find a
     *       vertex. A mapping of MyCaseInsensitiveCacheKey("DoesNotExist")->{@link
     *       UserClassVertex#NULL_VALUE} is stored in the cache.
     * </ol>
     */
    @Test
    public void testGetByCaseInsensitiveCacheKey() {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "	public void doSomething() {\n"
                    + "   	System.debug('Hello');\n"
                    + "   }\n"
                    + "}\n"
        };

        TestUtil.buildGraph(g, sourceCode);
        UserClassVertex myClassVertex =
                SFVertexFactory.load(
                        g,
                        g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .has(Schema.NAME, "MyClass"));

        // Unlike #testGetById we we can use the existing cache because we are using custom cache
        // keys
        final VertexCache vertexCache = VertexCacheProvider.get();

        VertexCache.CacheKey cacheKey;
        ClassByNameSupplier classByNameSupplier;
        Optional<UserClassVertex> foundVertex;

        // Attempt to find the class named "MyClass". This SHOULD find a vertex.
        // The first load SHOULD cause a cache miss
        cacheKey = new MyCaseInsensitiveCacheKey("MyClass");
        classByNameSupplier = new ClassByNameSupplier(g, "MyClass");
        foundVertex = vertexCache.get(cacheKey, classByNameSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(true));
        MatcherAssert.assertThat(classByNameSupplier.getCacheMisses(), hasSize(equalTo(1)));
        MatcherAssert.assertThat(foundVertex, optEqualTo(myClassVertex));

        // The second load SHOULD NOT cause a cache miss. Use a upper case to show that the cache
        // key is case
        // insensitive
        classByNameSupplier = new ClassByNameSupplier(g, "MYCLASS");
        foundVertex = vertexCache.get(cacheKey, classByNameSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(true));
        MatcherAssert.assertThat(classByNameSupplier.getCacheMisses(), hasSize(equalTo(0)));
        MatcherAssert.assertThat(foundVertex, optEqualTo(myClassVertex));

        // Attempt to find the class named "DoesNotExist". This SHOULD NOT find a vertex.
        // The first load SHOULD cause a cache miss
        cacheKey = new MyCaseInsensitiveCacheKey("DoesNotExist");
        classByNameSupplier = new ClassByNameSupplier(g, "DoesNotExist");
        foundVertex = vertexCache.get(cacheKey, classByNameSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(false));
        MatcherAssert.assertThat(classByNameSupplier.getCacheMisses(), hasSize(equalTo(1)));

        // The second load SHOULD NOT cause a cache miss
        cacheKey = new MyCaseInsensitiveCacheKey("DoesNotExist");
        classByNameSupplier = new ClassByNameSupplier(g, "DoesNotExist");
        foundVertex = vertexCache.get(cacheKey, classByNameSupplier);
        MatcherAssert.assertThat(foundVertex.isPresent(), equalTo(false));
        MatcherAssert.assertThat(classByNameSupplier.getCacheMisses(), hasSize(equalTo(0)));
    }

    /**
     * This test verifies that the {@link VertexCache#get(VertexCache.CacheKey, Supplier)} method
     * seeds the JustInTimeGraph with the {@link Schema#DEFINING_TYPE} of the value returned from
     * the method. This is necessary for the following scenario.
     *
     * <ol>
     *   <li>Thread1 Stores Vertex V in the cache using a cache key K
     *   <li>Thread2 Retrieves V from the cache using K
     *   <li>Thread2 executes a query that uses the ID of V. For example List&lt;BaseSFVertex&gt;
     *       children = SFVertexFactory.loadVertices(g, g.V(V.getId().out(Schema.CHILD));
     * </ol>
     *
     * Thread2's graph has never called {@link CaseSafePropertyUtil.H#has(String, String, String)},
     * this is the most common mechanism for copying a vertex from the full graph to the
     * JustInTimeGraph. This requires another mechanism to seed Thread2's JustInTimeGraph with the
     * DefiningType of V. The solution is that {@link
     * com.salesforce.graph.cache.AbstractVertexCacheImpl#get(VertexCache.CacheKey, Supplier)}
     * invokes {@link JustInTimeGraph#loadUserClass(String)} for any vertices returned from this
     * method.
     */
    @Test
    public void testCacheHitSeedsJustInTimeGraph() throws Exception {
        String[] sourceCode = {
            "public class MyClass {\n"
                    + "    public static void doSomething() {\n"
                    + "       MyOtherClass c = new MyOtherClass();\n"
                    + "       c.logSomething(x);\n"
                    + "	}\n"
                    + "}\n",
            "public class MyOtherClass {\n"
                    + "    public String s;\n"
                    + "    public MyOtherClass() {\n"
                    + "    	this.s = 'Hello';\n"
                    + "    }\n"
                    + "    public void logSomething() {\n"
                    + "    	System.debug(s);\n"
                    + "    }\n"
                    + "}\n"
        };

        final GraphTraversalSource fullGraph = GraphUtil.getGraph();
        TestUtil.buildGraph(fullGraph, sourceCode);

        final List<UserClassVertex> vertices =
                SFVertexFactory.loadVertices(
                        fullGraph,
                        fullGraph
                                .V()
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .not(has(Schema.IS_STANDARD, true)));
        MatcherAssert.assertThat(vertices, hasSize(equalTo(2)));

        Optional<UserClassVertex> userClassVertex;
        userClassVertex = ClassUtil.getUserClass(fullGraph, "MyClass");
        MatcherAssert.assertThat(userClassVertex.isPresent(), equalTo(true));

        userClassVertex = ClassUtil.getUserClass(fullGraph, "MyOtherClass");
        MatcherAssert.assertThat(userClassVertex.isPresent(), equalTo(true));

        final ClassInstanceScope classInstanceScope =
                ClassInstanceScope.get(fullGraph, "MyOtherClass");
        // This results in a query that uses the MyOtherClass id
        final List<FieldVertex> fieldVertices = classInstanceScope.getFields();
        MatcherAssert.assertThat(fieldVertices, hasSize(equalTo(1)));

        // Retain the cache for the thread
        final VertexCache originalVertexCache = VertexCacheProvider.get();

        Runnable runnable =
                () -> {
                    try {
                        // Use the VertexCache from the main thread. This simulates the production
                        // case where the VertexCache
                        // is a singleton shared by all threads.
                        VertexCacheTestProvider.initializeForTest(originalVertexCache);

                        final GraphTraversalSource g =
                                JustInTimeGraphProvider.create(fullGraph, "MyClass");
                        // At this point in time the graph only contains MyClass. This method call
                        // loads the "MyOtherClass"
                        // UserClassVertex from the cache. AbstractVertexCacheImpl invokes
                        // JustInTimeGraph#loadUserClass("loadUserClass") before returning the
                        // cached result.
                        final ClassInstanceScope classInstanceScopeJITG =
                                ClassInstanceScope.get(g, "MyOtherClass");
                        // This method call results in a query that relies on the vertex id. The
                        // call will not seed the JITG
                        // with "MyOtherClass".
                        final List<FieldVertex> fieldVerticesJITG =
                                classInstanceScopeJITG.getFields();
                        // fieldVerticesJITG would be empty if the JITG was not correctly seeded
                        MatcherAssert.assertThat(fieldVerticesJITG, hasSize(equalTo(1)));
                    } finally {
                        JustInTimeGraphProvider.remove();
                    }
                };

        // Execute the thread and wait for it to complete
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(runnable);
        future.get();
    }

    /**
     * Class that allows us to keep track of cache misses. It delegates to the {@link
     * SFVertexFactory.CacheBehavior#NO_CACHE} version of {@link
     * SFVertexFactory#load(GraphTraversalSource, Long, Object)} when a cache miss occurs.
     */
    private static final class IdCacheMissFunction
            implements Function<Map<Object, Object>, BaseSFVertex> {
        private final GraphTraversalSource g;
        private final List<Long> cacheMisses;

        private IdCacheMissFunction(GraphTraversalSource g) {
            this.g = g;
            this.cacheMisses = new ArrayList<>();
        }

        @Override
        public BaseSFVertex apply(Map<Object, Object> objectObjectMap) {
            final Long id = (Long) objectObjectMap.get(T.id);
            cacheMisses.add(id);
            // Load it via the factory, indicating that it should not use the cache
            return SFVertexFactory.load(g, g.V(id), SFVertexFactory.CacheBehavior.NO_CACHE);
        }

        private List<Long> getCacheMisses() {
            return cacheMisses;
        }
    }

    /** Example supplier that finds the parent class of the provided vertex. */
    private static final class ParentClassSupplier implements Supplier<CollectibleObject> {
        private final GraphTraversalSource g;
        private final UserClassVertex vertex;
        private List<CollectibleObject> cacheMisses;

        private ParentClassSupplier(GraphTraversalSource g, UserClassVertex vertex) {
            this.g = g;
            this.vertex = vertex;
            this.cacheMisses = new ArrayList<>();
        }

        /**
         * Suppliers must return a NullCollectible if the result is null. This is because the value
         * is stored in a map that doesn't support nulls. See {@link CollectibleObject} for details.
         */
        @Override
        public CollectibleObject get() {
            final UserClassVertex userClassVertex;
            final String superclassName = vertex.getSuperClassName().orElse(null);
            if (superclassName != null) {
                userClassVertex =
                        SFVertexFactory.loadSingleOrNull(
                                g,
                                g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                        .has(Schema.NAME, superclassName));
            } else {
                userClassVertex = null;
            }
            CollectibleObject result =
                    userClassVertex != null ? userClassVertex : UserClassVertex.NULL_VALUE;
            cacheMisses.add(result);
            return result;
        }

        private List<CollectibleObject> getCacheMisses() {
            return cacheMisses;
        }
    }

    /** Example supplier that finds the UserClassVertex identified by the given class name */
    private static final class ClassByNameSupplier implements Supplier<CollectibleObject> {
        private final GraphTraversalSource g;
        private final String className;
        private List<CollectibleObject> cacheMisses;

        private ClassByNameSupplier(GraphTraversalSource g, String className) {
            this.g = g;
            this.className = className;
            this.cacheMisses = new ArrayList<>();
        }

        /**
         * Suppliers must return a NullCollectible if the result is null. This is because the value
         * is stored in a map that doesn't support nulls. See {@link CollectibleObject} for details.
         */
        @Override
        public CollectibleObject get() {
            final UserClassVertex userClassVertex =
                    SFVertexFactory.loadSingleOrNull(
                            g,
                            g.V().hasLabel(ASTConstants.NodeType.USER_CLASS)
                                    .has(Schema.NAME, className));
            CollectibleObject result =
                    userClassVertex != null ? userClassVertex : UserClassVertex.NULL_VALUE;
            cacheMisses.add(result);
            return result;
        }

        private List<CollectibleObject> getCacheMisses() {
            return cacheMisses;
        }
    }

    private static final class MyVertexCacheKey extends VertexCacheKey {
        protected MyVertexCacheKey(BaseSFVertex vertex) {
            super(vertex);
        }
    }

    private static final class MyCaseInsensitiveCacheKey extends CaseInsensitiveCacheKey {
        protected MyCaseInsensitiveCacheKey(String key) {
            super(key);
        }
    }
}
