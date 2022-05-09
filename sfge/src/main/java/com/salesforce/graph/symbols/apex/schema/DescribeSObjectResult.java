package com.salesforce.graph.symbols.apex.schema;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Represents the object returned from SObjectType#getDescribe() */
public final class DescribeSObjectResult extends ApexStandardValue<DescribeSObjectResult>
        implements DeepCloneable<DescribeSObjectResult> {
    public static final String TYPE = ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_S_OBJECT_RESULT;

    private static final String FIELDS = "fields";
    private static final String FIELD_SETS = "fieldSets";
    private static final String METHOD_GET_MAP = "getMap";
    private static final String METHOD_GET_NAME = "getName";
    private static final String METHOD_GET_RECORD_TYPE_INFOS = "getRecordTypeInfos";
    private static final String METHOD_GET_RECORD_TYPE_INFOS_BY_DEVELOPER_NAME =
            "getRecordTypeInfosByDeveloperName";
    private static final String METHOD_GET_RECORD_TYPE_INFOS_BY_NAME = "getRecordTypeInfosByName";
    private static final String METHOD_GET_S_OBJECT_TYPE = "getSObjectType";

    private final SObjectType sObjectType;

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeSObjectResult(ApexValueBuilder builder) {
        this(null, builder);
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public DescribeSObjectResult(SObjectType sObjectType, ApexValueBuilder builder) {
        super(ApexStandardLibraryUtil.Type.SCHEMA_DESCRIBE_S_OBJECT_RESULT, builder);
        this.sObjectType = sObjectType;
    }

    private DescribeSObjectResult(DescribeSObjectResult other) {
        super(other);
        this.sObjectType = CloneUtil.clone(other.sObjectType);
    }

    @Override
    public DescribeSObjectResult deepClone() {
        return new DescribeSObjectResult(this);
    }

    @Override
    public <U> U accept(ApexValueVisitor<U> visitor) {
        return visitor.visit(this);
    }

    // TODO: Is this ever empty, I think it's always defined but may be indeterminant
    public Optional<SObjectType> getSObjectType() {
        return Optional.ofNullable(sObjectType);
    }

    @Override
    public Optional<ApexValue<?>> apply(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        ApexValueBuilder builder = ApexValueBuilder.get(symbols).returnedFrom(this, vertex);
        // These are actually methods called on properties. We use the Full method name to establish
        // which
        // property is being indexed into.
        // TODO: Support properties
        String methodName = vertex.getMethodName();
        List<String> chainedNames = vertex.getChainedNames();

        if (METHOD_GET_MAP.equalsIgnoreCase(methodName) && !chainedNames.isEmpty()) {
            String mapType = chainedNames.get(chainedNames.size() - 1);
            // objectDescribe.fields.getMap()
            if (FIELDS.equalsIgnoreCase(mapType)) {
                validateParameterSize(vertex, 0);
                if (sObjectType != null) {
                    return Optional.of(builder.buildApexFieldDescribeMapValue(sObjectType));
                } else {
                    return Optional.of(
                            builder.buildApexFieldDescribeMapValue(
                                    builder.deepClone().buildSObjectType()));
                }
                // objectDescribe.fieldSets.getMap()
            } else if (FIELD_SETS.equalsIgnoreCase(mapType)) {
                validateParameterSize(vertex, 0);
                if (sObjectType != null) {
                    return Optional.of(builder.buildApexFieldSetDescribeMapValue(sObjectType));
                } else {
                    return Optional.of(
                            builder.buildApexFieldSetDescribeMapValue(
                                    builder.deepClone().buildSObjectType()));
                }
            } else {
                throw new UnexpectedException(vertex);
            }
        }

        return Optional.empty();
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

        if (METHOD_GET_NAME.equalsIgnoreCase(methodName)) {
            if (sObjectType != null && ApexValueUtil.isDeterminant(sObjectType.getType())) {
                ApexValue<?> value = sObjectType.getType().get();
                if (value instanceof ApexStringValue) {
                    ApexStringValue apexStringValue = (ApexStringValue) value;
                    if (apexStringValue.isValuePresent()) {
                        return Optional.of(builder.buildString(apexStringValue.getValue().get()));
                    }
                }
            }
            return Optional.of(builder.withStatus(ValueStatus.INDETERMINANT).buildString());
        } else if (METHOD_GET_RECORD_TYPE_INFOS.equalsIgnoreCase(methodName)) {
            String type =
                    ApexStandardLibraryUtil.getListDeclaration(
                            ApexStandardLibraryUtil.Type.SCHEMA_RECORD_TYPE_INFO);
            Typeable typeable = SyntheticTypedVertex.get(type);
            builder.declarationVertex(typeable).withStatus(ValueStatus.INDETERMINANT);
            return Optional.of(builder.buildList());
        } else if (METHOD_GET_RECORD_TYPE_INFOS_BY_DEVELOPER_NAME.equalsIgnoreCase(methodName)
                || METHOD_GET_RECORD_TYPE_INFOS_BY_NAME.equalsIgnoreCase(methodName)) {
            String type =
                    ApexStandardLibraryUtil.getMapDeclaration(
                            ApexStandardLibraryUtil.Type.STRING,
                            ApexStandardLibraryUtil.Type.SCHEMA_RECORD_TYPE_INFO);
            Typeable typeable = SyntheticTypedVertex.get(type);
            builder.declarationVertex(typeable).withStatus(ValueStatus.INDETERMINANT);
            return Optional.of(builder.buildMap());
        } else if (METHOD_GET_S_OBJECT_TYPE.equalsIgnoreCase(methodName)) {
            if (sObjectType != null) {
                return Optional.of(sObjectType.deepCloneForReturn(this, invocableExpression));
            } else {
                return Optional.of(builder.buildSObjectType());
            }
        } else {
            return Optional.of(ApexValueUtil.synthesizeReturnedValue(builder, method));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DescribeSObjectResult that = (DescribeSObjectResult) o;
        return Objects.equals(sObjectType, that.sObjectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sObjectType);
    }
}
