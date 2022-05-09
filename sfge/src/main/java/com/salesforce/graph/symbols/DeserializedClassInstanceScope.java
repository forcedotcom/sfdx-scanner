package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.ClassRefExpressionVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Represents an Apex class that has been created via JSON.deserialize. Current limitations
 *
 * <ol>
 *   <li>Does not delegate to the class static scope
 * </ol>
 *
 * Differences from {@link ClassInstanceScope}
 *
 * <ol>
 *   <li>Non-assigned properties are initialized to indeterminant
 * </ol>
 *
 * TODO: Decide if we should ignore inline assignments
 */
public final class DeserializedClassInstanceScope extends AbstractClassInstanceScope
        implements DeepCloneable<DeserializedClassInstanceScope> {
    private DeserializedClassInstanceScope(GraphTraversalSource g, UserClassVertex userClass) {
        super(g, userClass);
    }

    private DeserializedClassInstanceScope(DeserializedClassInstanceScope other) {
        super(other);
    }

    @Override
    public DeserializedClassInstanceScope deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(
                this, () -> new DeserializedClassInstanceScope(this));
    }

    /**
     * Return a DeserializedClassInstanceScope if the class exists in source, attempt to match inner
     * classes
     */
    public static Optional<DeserializedClassInstanceScope> getOptional(
            GraphTraversalSource g, ClassRefExpressionVertex classRefExpression) {
        UserClassVertex userClass = ClassUtil.getUserClass(g, classRefExpression).orElse(null);
        if (userClass != null) {
            return Optional.of(new DeserializedClassInstanceScope(g, userClass));
        } else {
            return Optional.empty();
        }
    }

    public static DeserializedClassInstanceScope get(GraphTraversalSource g, String className) {
        UserClassVertex userClass =
                ClassUtil.getUserClass(g, className)
                        .orElseThrow(() -> new UnexpectedException(className));
        return new DeserializedClassInstanceScope(g, userClass);
    }

    @Override
    protected Optional<PathScopeVisitor> getInheritedScope() {
        // TODO: This is an oversimplification.
        // Assume that JSONSerialized classes are simple types without static variables, this isn't
        // true, but will work for now
        return Optional.empty();
    }

    @Override
    protected ValueStatus getDefaultFieldStatus() {
        return ValueStatus.INDETERMINANT;
    }
}
