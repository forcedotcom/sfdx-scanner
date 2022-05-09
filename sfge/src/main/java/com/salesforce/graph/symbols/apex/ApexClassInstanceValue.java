package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;

/** Represents a class instance that was created via a NewObjectExpressionVertex */
public final class ApexClassInstanceValue extends ApexValue<ApexClassInstanceValue>
        implements DeepCloneable<ApexClassInstanceValue>, Typeable {
    private final AbstractClassInstanceScope abstractClassInstanceScope;

    ApexClassInstanceValue(
            AbstractClassInstanceScope abstractClassInstanceScope, ApexValueBuilder builder) {
        super(builder);
        if (abstractClassInstanceScope == null) {
            throw new UnexpectedException(this);
        }
        this.abstractClassInstanceScope = abstractClassInstanceScope;
    }

    private ApexClassInstanceValue(ApexClassInstanceValue other) {
        super(other);
        this.abstractClassInstanceScope = CloneUtil.clone(other.abstractClassInstanceScope);
    }

    @Override
    public ApexClassInstanceValue deepClone() {
        return new ApexClassInstanceValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public AbstractClassInstanceScope getClassInstanceScope() {
        return abstractClassInstanceScope;
    }

    @Override
    public Optional<String> getDefiningType() {
        return Optional.of(getClassInstanceScope().getClassName());
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        return Optional.empty();
    }

    @Override
    public String getCanonicalType() {
        return abstractClassInstanceScope.getCanonicalType();
    }

    public TypeableUtil.OrderedTreeSet getTypes() {
        return abstractClassInstanceScope.getTypes();
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        return abstractClassInstanceScope.matchesParameterType(parameterVertex);
    }
}
