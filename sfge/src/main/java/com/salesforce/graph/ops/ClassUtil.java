package com.salesforce.graph.ops;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.cache.CaseInsensitiveCacheKey;
import com.salesforce.graph.cache.VertexCacheKey;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ClassRefExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Utilities related to {@link UserClassVertex} */
public final class ClassUtil {
    /**
     * Attempt to obtain more qualified type than {@code type} for cases where an inner class is
     * being referenced from within its parent class or a sibling inner class without fully
     * qualifying the name.
     *
     * <pre>
     *     public class OuterClass {
     *         	public class InnerClass1 {
     * 			public void method1(){
     * 				InnerClass2.method2();	// Will resolve the method call expression to OuterClass.InnerClass2
     * 			}
     *         	}
     *         	public class InnerClass2 {
     * 			public void method1() {
     * 				InnerClass1.method1();	// Will resolve the method call expression to OuterClass.InnerClass1
     * 			}
     *         	}
     * 	 	public void doSomething() {
     * 			InnerClass1.method1();	// Will resolve the method call expression to OuterClass.InnerClass1
     * 			InnerClass2.method2();	// Will resolve the method call expression to OuterClass.InnerClass2
     * 		}
     *     }
     * </pre>
     *
     * @return a more specific type if possible else empty
     */
    public static Optional<String> getMoreSpecificClassName(BaseSFVertex vertex, String type) {
        // A type with a '.' is already fully qualified
        if (!type.contains(".")) {
            // Split the vertex's definingType to find the outer-most class
            final String definingType = vertex.getDefiningType().split("\\.")[0];
            // Combine the values if they aren't equal
            if (!definingType.equalsIgnoreCase(type)) {
                return Optional.of(definingType + "." + type);
            }
        }
        return Optional.empty();
    }

    /**
     * Cache key for {@link #getUserClass(GraphTraversalSource, String)}. This method may be called
     * many times and can be expensive.
     */
    private static final class UserClassByClassNameCacheKey extends CaseInsensitiveCacheKey {
        private UserClassByClassNameCacheKey(String className) {
            super(className);
        }
    }

    /**
     * @return the UserClassVertex identified by {@code className} if it exists. The caller is
     *     responsible for resolving ambiguities caused by non-qualified inner class access. See
     *     {@link #getMoreSpecificClassName(BaseSFVertex, String)}
     */
    public static Optional<UserClassVertex> getUserClass(GraphTraversalSource g, String className) {
        final UserClassByClassNameCacheKey key = new UserClassByClassNameCacheKey(className);
        return VertexCacheProvider.get()
                .get(
                        key,
                        () -> {
                            UserClassVertex result =
                                    SFVertexFactory.loadSingleOrNull(
                                            g,
                                            g.V().where(
                                                            H.has(
                                                                    ASTConstants.NodeType
                                                                            .USER_CLASS,
                                                                    Schema.DEFINING_TYPE,
                                                                    className)));
                            return result != null ? result : BaseSFVertex.NULL_VALUE;
                        });
    }

    /**
     * Cache key for {@link #getUserClass(GraphTraversalSource, BaseSFVertex)}. This method may be
     * called many times and can be expensive.
     */
    private static final class UserClassByReferenceVertexCacheKey extends VertexCacheKey {
        private UserClassByReferenceVertexCacheKey(BaseSFVertex vertex) {
            super(vertex);
        }
    }

    /**
     * Finds the {@link UserClassVertex} referred to by {@code vertex}.
     *
     * @return the UserClassVertex identified by {@code vertex} if it exists. {@code vertex }.
     */
    public static <V extends BaseSFVertex & Typeable> Optional<UserClassVertex> getUserClass(
            GraphTraversalSource g, V vertex) {
        // TODO: Create a common hierarchy for these two vertex types
        if (!(vertex instanceof NewObjectExpressionVertex)
                && !(vertex instanceof ClassRefExpressionVertex)) {
            throw new UnexpectedException(vertex);
        }

        final UserClassByReferenceVertexCacheKey key =
                new UserClassByReferenceVertexCacheKey(vertex);
        return VertexCacheProvider.get()
                .get(
                        key,
                        () -> {
                            final String className = vertex.getCanonicalType();
                            UserClassVertex result =
                                    SFVertexFactory.loadSingleOrNull(
                                            g,
                                            g.V().where(
                                                            H.has(
                                                                    ASTConstants.NodeType
                                                                            .USER_CLASS,
                                                                    Schema.DEFINING_TYPE,
                                                                    className)));
                            if (result == null) {
                                final String fullClassName =
                                        getMoreSpecificClassName(vertex, className).orElse(null);
                                if (fullClassName != null) {
                                    result =
                                            SFVertexFactory.loadSingleOrNull(
                                                    g,
                                                    g.V().where(
                                                                    H.has(
                                                                            ASTConstants.NodeType
                                                                                    .USER_CLASS,
                                                                            Schema.DEFINING_TYPE,
                                                                            fullClassName)));
                                }
                            }
                            return result != null ? result : BaseSFVertex.NULL_VALUE;
                        });
    }

    private ClassUtil() {}
}
