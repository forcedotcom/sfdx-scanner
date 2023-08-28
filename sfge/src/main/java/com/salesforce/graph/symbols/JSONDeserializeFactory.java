package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.SystemNames;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ClassRefExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.VertexPredicateVisitor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Special case factory that handles the various JSON#deserializeMethods */
public final class JSONDeserializeFactory {
    private static final Logger LOGGER = LogManager.getLogger(JSONDeserializeFactory.class);

    private static final String METHOD_PREFIX = "JSON.";
    private static final String METHOD_DESERIALIZE = METHOD_PREFIX + "deserialize";
    private static final String METHOD_DESERIALIZE_STRICT = METHOD_PREFIX + "deserializeStrict";

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                final String fullMethodName = vertex.getFullMethodName();

                if (METHOD_DESERIALIZE.equalsIgnoreCase(fullMethodName)
                        || METHOD_DESERIALIZE_STRICT.equalsIgnoreCase(fullMethodName)) {
                    // This results in a CastExpression that wraps a MethodCallExpression. The
                    // second parameter is always
                    // a ClassRefExpressionVertex that denotes the Type
                    // (MyObject__c)JSON.deserialize(asJson, MyObject__c.class);

                    ApexValue.validateParameterSize(vertex, 2);
                    final ClassRefExpressionVertex classRefExpression =
                            (ClassRefExpressionVertex) vertex.getParameters().get(1);
                    final String type = classRefExpression.getCanonicalType();
                    // A value initialized from JSON.deserialized is guaranteed to be initialized.
                    // Hence, we'll mark all apex values created from Factory as DETERMINANT.
                    final ApexValueBuilder builder =
                            ApexValueBuilder.get(symbols)
                                    .withStatus(ValueStatus.INITIALIZED)
                                    .returnedFrom(null, vertex);

                    if (type.endsWith(SystemNames.SUFFIX_CUSTOM_OBJECT)) {
                        // __c is either a CustomObject or CustomSetting
                        return Optional.of(deserializeCustomObjectOrSetting(builder, type));
                    } else if (ApexStandardLibraryUtil.isStandardObject(type)) {
                        // Account, Contact etc.
                        final SyntheticTypedVertex typeable = SyntheticTypedVertex.get(type);
                        return Optional.of(builder.declarationVertex(typeable).buildUnknownType());
                    } else {
                        // Attempt to map it to an ApexClass
                        Optional<ApexValue<?>> classInstance =
                                deserializeClassInstance(g, builder, classRefExpression);
                        if (classInstance.isPresent()) {
                            return classInstance;
                        }
                    }

                    // Allow the builder to build any other types
                    return Optional.of(
                            builder.declarationVertex(SyntheticTypedVertex.get(type)).build());
                }
                return Optional.empty();
            };

    private static ApexValue<?> deserializeCustomObjectOrSetting(
            ApexValueBuilder builder, String type) {
        final SyntheticTypedVertex typeable = SyntheticTypedVertex.get(type);
        if (MetadataInfoProvider.get().isCustomSetting(type)) {
            return builder.buildCustomValue(typeable);
        } else {
            return builder.declarationVertex(typeable).buildUnknownType();
        }
    }

    /**
     * TODO: This should be moved someplace more generic. Create a new class instance and walk its
     * instantiation paths It currently only handles simple paths which don't have any method calls.
     * Limitations
     *
     * <ol>
     *   <li>Excludes any classes where the initialization path contains method calls
     *   <li>Does not walk the constructor, this assumes that the properties set in the constructor
     *       would made indeterminant by the deserialization of the JSON
     * </ol>
     */
    private static Optional<ApexValue<?>> deserializeClassInstance(
            GraphTraversalSource g,
            ApexValueBuilder apexValueBuilder,
            ClassRefExpressionVertex classRefExpression) {
        DeserializedClassInstanceScope classInstanceScope =
                DeserializedClassInstanceScope.getOptional(g, classRefExpression).orElse(null);
        if (classInstanceScope != null) {
            List<ApexPath> instantiationPaths = getClassInstantiationPaths(g, classInstanceScope);
            if (pathsAreValid(instantiationPaths)) {
                if (!instantiationPaths.isEmpty()) {
                    walkPath(g, instantiationPaths.get(0), classInstanceScope);
                }
                return Optional.of(
                        apexValueBuilder.buildApexClassInstanceValue(classInstanceScope));
            }
        }

        return Optional.empty();
    }

    private static final void walkPath(
            GraphTraversalSource g,
            ApexPath path,
            DeserializedClassInstanceScope classInstanceScope) {
        PathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
        SymbolProviderVertexVisitor symbols =
                new DefaultSymbolProviderVertexVisitor(g, classInstanceScope);
        ApexPathWalker.walkPath(g, path, visitor, symbols);
    }

    /**
     * @return if there there are 0 or, there is 1 paths and the path does not include any
     *     MethodCallExpressionVertices
     */
    private static boolean pathsAreValid(List<ApexPath> paths) {
        if (paths.isEmpty()) {
            return true;
        } else if (paths.size() > 1) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("More than one path found. paths=" + paths);
            }
            return false;
        } else {
            ApexPath path = paths.get(0);
            // The path is invalid if it contains method calls
            return !path.getApexPathMetaInfo().isPresent();
        }
    }

    private static List<ApexPath> getClassInstantiationPaths(
            GraphTraversalSource g, DeserializedClassInstanceScope classInstanceScope) {
        MethodVertex.ConstructorVertex constructor =
                MethodUtil.getNoArgConstructor(g, classInstanceScope.getClassName()).orElse(null);

        if (constructor != null) {
            ApexPathExpanderConfig expanderConfig =
                    ApexPathExpanderConfig.Builder.get()
                            .withVertexPredicate(MethodCallExpressionPredicateLazyHolder.INSTANCE)
                            .build();
            return ApexPathUtil.getForwardPaths(g, constructor, expanderConfig);
        } else {
            return Collections.emptyList();
        }
    }

    /** Indicates that a path contains {@link MethodCallExpressionVertex} */
    private static final class MethodCallExpressionPredicate implements VertexPredicate {
        @Override
        public boolean test(BaseSFVertex vertex, SymbolProvider provider) {
            return vertex instanceof MethodCallExpressionVertex;
        }

        @Override
        public void accept(VertexPredicateVisitor visitor) {
            throw new UnexpectedException(visitor);
        }
    }

    private static final class MethodCallExpressionPredicateLazyHolder {
        private static final MethodCallExpressionPredicate INSTANCE =
                new MethodCallExpressionPredicate();
    }

    private JSONDeserializeFactory() {}
}
