package com.salesforce.graph.symbols.apex;

import apex.jorje.semantic.symbol.type.TypeInfos;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.TypeableUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * The Id class, this class has overlap with a String.
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_id.htm
 */
public final class ApexIdValue extends ApexStandardValue<ApexIdValue>
        implements DeepCloneable<ApexIdValue> {
    public static final String TYPE = TypeInfos.ID.getApexName();

    public static String METHOD_ADD_ERROR = "addError";
    public static String METHOD_GET_S_OBJECT_TYPE = "getSObjectType";
    public static String METHOD_TO_15 = "to15";

    final ApexValue<?> apexValue;

    ApexIdValue(ApexValueBuilder builder) {
        this(null, builder);
    }

    ApexIdValue(@Nullable ApexValue<?> apexValue, ApexValueBuilder builder) {
        super(TYPE, builder);
        this.apexValue = apexValue;
    }

    private ApexIdValue(ApexIdValue other) {
        super(other);
        this.apexValue = CloneUtil.cloneApexValue(other.apexValue);
    }

    @Override
    public ApexIdValue deepClone() {
        return new ApexIdValue(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean matchesParameterType(Typeable parameterVertex) {
        return getCanonicalType().equalsIgnoreCase(parameterVertex.getCanonicalType())
                ||
                // Strings are also ids
                ApexStringValue.TYPE.equalsIgnoreCase(parameterVertex.getCanonicalType());
    }

    /**
     * Id value can match a method that takes String as parameter. Id parameter type is given higher
     * priority than String parameter type. String value can match a method that takes Id as
     * parameter. However, at runtime, Apex checks if the string value is in a valid Id format.
     */
    @Override
    public TypeableUtil.OrderedTreeSet getTypes() {
        final TypeableUtil.OrderedTreeSet typeHierarchy = new TypeableUtil.OrderedTreeSet();
        typeHierarchy.add(ApexIdValue.TYPE);
        typeHierarchy.add(ApexStringValue.TYPE);
        typeHierarchy.add(TypeInfos.OBJECT.getApexName());
        return typeHierarchy;
    }

    @Override
    @SuppressWarnings("PMD.EmptyIfStmt") // TODO: revisit
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        final ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        final String methodName = vertex.getMethodName();

        if (METHOD_ADD_ERROR.equalsIgnoreCase(methodName)) {
            // Intentionally left blank
        } else if (METHOD_GET_S_OBJECT_TYPE.equalsIgnoreCase(methodName)) {
            return Optional.of(builder.buildSObjectType(builder.deepClone().buildString()));
        } else if (METHOD_TO_15.equalsIgnoreCase(methodName)) {
            return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildString());
        } else {
            throw new UnimplementedMethodException(this, vertex);
        }

        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols) {
        return Optional.empty();
    }

    public Optional<ApexValue<?>> getValue() {
        return Optional.ofNullable(apexValue);
    }

    @Override
    public boolean isValuePresent() {
        return getValue().isPresent();
    }
}
