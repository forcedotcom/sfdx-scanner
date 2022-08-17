package com.salesforce.graph.symbols.apex.system;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.exception.UnimplementedMethodException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.AbstractSanitizableValue;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexSoqlValue;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.symbols.apex.MethodBasedSanitization;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.rules.fls.apex.operations.FlsConstants.StripInaccessibleAccessType;
import java.util.List;
import java.util.Optional;

/**
 * Represents the LHS of {@code SObjectAccessDecision accessDecision =
 * Security.stripInaccessible(AccessType.CREATEABLE, [list of sobjects]); }
 */
public final class SObjectAccessDecision extends ApexStandardValue<SObjectAccessDecision>
        implements MethodBasedSanitization.SanitizerDecision, DeepCloneable<SObjectAccessDecision> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SYSTEM_S_OBJECT_ACCESS_DECISION;

    private static final String METHOD_GET_RECORDS = "getRecords";
    private static final String METHOD_GET_MODIFIED_INDEXES = "getModifiedIndexes";
    private static final String METHOD_GET_REMOVED_FIELDS = "getRemovedFields";

    private final StripInaccessibleAccessType accessType;
    private final ApexEnumValue accessTypeEnumValue;
    private final AbstractSanitizableValue sanitizableValue;

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public SObjectAccessDecision(
            ApexEnumValue accessTypeEnumValue,
            AbstractSanitizableValue sanitizableValue,
            ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SYSTEM_S_OBJECT_ACCESS_DECISION, builder);
        this.accessTypeEnumValue = accessTypeEnumValue;
        this.sanitizableValue = sanitizableValue;
        this.accessType =
                StripInaccessibleAccessType.getAccessType(this.accessTypeEnumValue).orElse(null);
    }

    public SObjectAccessDecision(ApexValueBuilder builder) {
        // Indeterminant situation
        this(null, null, builder);
    }

    private SObjectAccessDecision(SObjectAccessDecision other) {
        super(other);
        this.accessType = other.accessType;
        this.accessTypeEnumValue = CloneUtil.cloneApexValue(other.accessTypeEnumValue);
        this.sanitizableValue = CloneUtil.cloneApexValue(other.sanitizableValue);
    }

    @Override
    public SObjectAccessDecision deepClone() {
        return new SObjectAccessDecision(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        return Optional.empty();
    }

    @Override
    public Optional<MethodBasedSanitization.SanitizerAccessType> getAccessType() {
        return Optional.ofNullable(this.accessType);
    }

    public Optional<AbstractSanitizableValue<?>> getSanitizableValue() {
        return Optional.ofNullable(this.sanitizableValue);
    }

    @Override
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols) {
        ApexValueBuilder builder =
                ApexValueBuilder.get(symbols)
                        .returnedFrom(this, invocableExpression)
                        .methodVertex(method);
        String methodName = method.getName();

        if (METHOD_GET_RECORDS.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 0);

            // getRecords() always returns a List. But the subtype depends on sanitizableValue's
            // type
            AbstractSanitizableValue<?> sanitizedValue =
                    buildSanitizedValue(builder, this.sanitizableValue);
            sanitizedValue.markSanitized(
                    MethodBasedSanitization.SanitizerMechanism.STRIP_INACCESSIBLE, this);

            return Optional.of(sanitizedValue);
        } else if (METHOD_GET_MODIFIED_INDEXES.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 0);

            builder.declarationVertex(SyntheticTypedVertex.get("Set<Integer>"));
            builder.withStatus(ValueStatus.INDETERMINANT);
            return Optional.of(builder.buildSet());

        } else if (METHOD_GET_REMOVED_FIELDS.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 0);

            builder.declarationVertex(SyntheticTypedVertex.get("Map<String,Set<String>>"));
            builder.withStatus(ValueStatus.INDETERMINANT);
            return Optional.of(builder.buildMap());
        }

        throw new UnimplementedMethodException(this, method);
    }

    private static AbstractSanitizableValue<?> buildSanitizedValue(
            ApexValueBuilder builder, AbstractSanitizableValue sanitizableValue) {
        final Optional<Typeable> sanitizableValueDeclarationVertex =
                sanitizableValue.getDeclarationVertex();
        final Object sanitizableValueVertex = sanitizableValue.getValueVertex().orElse(null);
        AbstractSanitizableValue<?> sanitizedValue;

        if (sanitizableValue instanceof ApexListValue) {

            if (sanitizableValueVertex instanceof SoqlExpressionVertex) {
                // Lists originally created from SoqlExpressions, need to be marked Indeterminant
                // We derive the type from the Soql object
                sanitizedValue =
                        buildSanitizedValue(builder, (SoqlExpressionVertex) sanitizableValueVertex);
            } else {
                sanitizedValue =
                        builder.declarationVertex(
                                        sanitizableValueDeclarationVertex.orElse(
                                                SyntheticTypedVertex.get("List<SObject>")))
                                .buildList();
                // Add items from original list, but since we don't know if field information
                // would've changed,
                // we need some special handling
                final ApexListValue sanitizableListValue = (ApexListValue) sanitizableValue;
                final List<ApexValue<?>> listValues = sanitizableListValue.getValues();
                for (ApexValue<?> value : listValues) {
                    final ApexSingleValue newValue =
                            ApexValueBuilder.getWithoutSymbolProvider()
                                    .withStatus(ValueStatus.INDETERMINANT)
                                    .buildSObjectInstance(
                                            value.getTypeVertex()
                                                    .orElse(SyntheticTypedVertex.get("SObject")));
                    ((ApexListValue) sanitizedValue).add(newValue);
                }
            }
        } else if (sanitizableValue instanceof ApexSoqlValue) {
            sanitizedValue =
                    buildSanitizedValue(builder, (SoqlExpressionVertex) sanitizableValueVertex);
        } else if (sanitizableValue instanceof ApexSingleValue
                || sanitizableValue instanceof ApexCustomValue) {
            sanitizedValue =
                    builder.declarationVertex(SyntheticTypedVertex.get("List<SObject>"))
                            .withStatus(ValueStatus.INDETERMINANT)
                            .buildList();
        } else {
            throw new UnexpectedException(
                    "ApexValue type not handled for stripInaccessible call: " + sanitizableValue);
        }
        return sanitizedValue;
    }

    private static AbstractSanitizableValue<?> buildSanitizedValue(
            ApexValueBuilder builder, SoqlExpressionVertex soqlExpressionVertex) {
        AbstractSanitizableValue<?> sanitizedValue;
        final String canonicalType = soqlExpressionVertex.getCanonicalType();
        final String listType = ApexStandardLibraryUtil.getListDeclaration(canonicalType);
        sanitizedValue =
                builder.declarationVertex(SyntheticTypedVertex.get(listType))
                        .withStatus(ValueStatus.INDETERMINANT)
                        .buildList();
        return sanitizedValue;
    }
}
