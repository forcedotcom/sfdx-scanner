package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents Schema.DescribeFieldResult See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_fields_describe.htm
 */
public final class DescribeFieldResult extends ApexStandardValue<DescribeFieldResult>
        implements DeepCloneable<DescribeFieldResult> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_S_OBJECT_RESULT;

    public static final String METHOD_GET_MAP = "getMap";
    private static final String METHOD_GET_NAME = "getName";
    private static final String METHOD_GET_PICKLIST_VALUES = "getPicklistValues";
    private static final String METHOD_GET_REFERENCE_TO = "getReferenceTo";
    private static final String METHOD_GET_S_OBJECT_FIELD = "getSObjectField";

    private final DescribeSObjectResult describeSObjectResult;
    private final ApexValue<?> fieldName;

    /** Do not call directly. Use {@link ApexValueBuilder} */
    @SuppressWarnings("PMD.NullAssignment") // Final variables are set non-null values in the other
    // constructor
    public DescribeFieldResult(ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_FIELD_RESULT, builder);
        this.describeSObjectResult = null;
        this.fieldName = null;
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeFieldResult(
            DescribeSObjectResult describeSObjectResult,
            String fieldName,
            ApexValueBuilder builder) {
        this(
                describeSObjectResult,
                ApexValueBuilder.getWithoutSymbolProvider().buildString(fieldName),
                builder);
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeFieldResult(
            DescribeSObjectResult describeSObjectResult,
            ApexStringValue fieldName,
            ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_FIELD_RESULT, builder);
        this.describeSObjectResult = describeSObjectResult;
        this.fieldName = fieldName;
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeFieldResult(
            DescribeSObjectResult describeSObjectResult,
            ApexForLoopValue fieldName,
            ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_FIELD_RESULT, builder);
        this.describeSObjectResult = describeSObjectResult;
        this.fieldName = fieldName;
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeFieldResult(
            DescribeSObjectResult describeSObjectResult,
            ApexSingleValue fieldName,
            ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_FIELD_RESULT, builder);
        this.describeSObjectResult = describeSObjectResult;
        this.fieldName = fieldName;
    }

    private DescribeFieldResult(DescribeFieldResult other) {
        super(other);
        this.describeSObjectResult = CloneUtil.clone(other.describeSObjectResult);
        this.fieldName = CloneUtil.cloneApexValue(other.fieldName);
    }

    @Override
    public DescribeFieldResult deepClone() {
        return new DescribeFieldResult(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    public Optional<DescribeSObjectResult> getDescribeSObjectResult() {
        return Optional.ofNullable(describeSObjectResult);
    }

    public Optional<SObjectType> getSObjectType() {
        if (describeSObjectResult != null) {
            return describeSObjectResult.getSObjectType();
        } else {
            return Optional.empty();
        }
    }

    public Optional<ApexValue<?>> getFieldName() {
        return Optional.ofNullable(fieldName);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols) {
        // TODO: Pass a builder to this method. All of the methods have the same setup
        ApexValueBuilder builder =
                ApexValueBuilder.get(symbols)
                        .returnedFrom(this, invocableExpression)
                        .methodVertex(method);
        String methodName = method.getName();

        if (METHOD_GET_NAME.equalsIgnoreCase(methodName)) {
            if (fieldName != null && fieldName.isDeterminant()) {
                if (fieldName instanceof ApexStringValue) {
                    ApexStringValue apexStringValue = (ApexStringValue) fieldName;
                    if (apexStringValue.isValuePresent()) {
                        return Optional.of(builder.buildString(apexStringValue.getValue().get()));
                    }
                }
            }
            return Optional.of(builder.buildString());
        } else if (METHOD_GET_PICKLIST_VALUES.equalsIgnoreCase(methodName)) {
            return Optional.of(builder.buildList());
        } else if (METHOD_GET_REFERENCE_TO.equalsIgnoreCase(methodName)) {
            String type =
                    ApexStandardLibraryUtil.getListDeclaration(
                            ApexStandardLibraryUtil.Type.SCHEMA_S_OBJECT_TYPE);
            Typeable typeable = SyntheticTypedVertex.get(type);
            return Optional.of(builder.declarationVertex(typeable).buildList());
        } else if (METHOD_GET_S_OBJECT_FIELD.equalsIgnoreCase(methodName)) {
            if (fieldName instanceof SObjectField) {
                return Optional.of(
                        ((SObjectField) fieldName).deepCloneForReturn(this, invocableExpression));
            } else {
                return Optional.of(
                        builder.buildSObjectField(
                                describeSObjectResult.getSObjectType().get(), fieldName));
            }
        } else {
            return Optional.of(ApexValueUtil.synthesizeReturnedValue(builder, method));
        }
    }

    @Override
    public String toString() {
        return "DescribeFieldResult{"
                + "describeSObjectResult="
                + describeSObjectResult
                + ", fieldName="
                + fieldName
                + "} "
                + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DescribeFieldResult that = (DescribeFieldResult) o;
        return Objects.equals(describeSObjectResult, that.describeSObjectResult)
                && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(describeSObjectResult, fieldName);
    }
}
