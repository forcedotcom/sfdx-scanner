package com.salesforce.graph.symbols.apex.system;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexListValue;
import com.salesforce.graph.symbols.apex.ApexStandardValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Named SystemSchema to avoid conflict with {@link com.salesforce.graph.Schema} TODO: Rename
 * com.salesforce.graph.Schema to com.salesforce.graph.GraphSchema Implementation of System.Schema
 * class See
 * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_schema.htm
 */
public final class SystemSchema extends ApexStandardValue<SystemSchema>
        implements DeepCloneable<SystemSchema> {
    private static final String METHOD_GET_GLOBAL_DESCRIBE = "getGlobalDescribe";
    private static final String METHOD_DESCRIBE_S_OBJECTS = "describeSObjects";

    private SystemSchema() {
        super(
                ApexStandardLibraryUtil.Type.SYSTEM_SCHEMA,
                ApexValueBuilder.getWithoutSymbolProvider());
    }

    /** Do not call directly. Use {@link ApexValueBuilder} */
    public static SystemSchema getInstance() {
        return SystemSchema.LazyHolder.INSTANCE;
    }

    @Override
    public SystemSchema deepClone() {
        // Immutable, return itself
        return this;
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
    public Optional<ApexValue<?>> executeMethod(
            InvocableWithParametersVertex invocableExpression,
            MethodVertex method,
            SymbolProvider symbols) {
        ApexValueBuilder builder =
                ApexValueBuilder.get(symbols)
                        .returnedFrom(this, invocableExpression)
                        .valueVertex(invocableExpression)
                        .methodVertex(method);
        String methodName = method.getName();

        List<ChainedVertex> parameters = invocableExpression.getParameters();
        if (METHOD_DESCRIBE_S_OBJECTS.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 1);
            ChainedVertex parameter = parameters.get(0);
            List<DescribeSObjectResult> describeSObjectResults = new ArrayList<>();
            ApexValue<?> apexValue = ScopeUtil.resolveToApexValue(symbols, parameter).orElse(null);
            if (apexValue instanceof ApexListValue) {
                ApexListValue apexListValue = (ApexListValue) apexValue;
                ChainedVertex valueVertex = apexValue.getValueVertex().orElse(null);
                if (valueVertex instanceof NewListLiteralExpressionVertex) {
                    for (ApexValue<?> value : apexListValue.getValues()) {
                        SObjectType sObjectType = builder.deepClone().buildSObjectType(value);
                        describeSObjectResults.add(
                                builder.deepClone().buildDescribeSObjectResult(sObjectType));
                    }
                }
            }
            // The list is indeterminant if we could not resolve the values passed to the method
            ValueStatus status =
                    describeSObjectResults.isEmpty()
                            ? ValueStatus.INDETERMINANT
                            : ValueStatus.INITIALIZED;
            ApexListValue listValue = builder.withStatus(status).buildList();
            for (DescribeSObjectResult describeSObjectResult : describeSObjectResults) {
                listValue.add(describeSObjectResult);
            }
            return Optional.of(listValue);
        } else if (METHOD_GET_GLOBAL_DESCRIBE.equalsIgnoreCase(methodName)) {
            validateParameterSize(invocableExpression, 0);
            return Optional.of(builder.buildApexGlobalDescribeMap());
        } else {
            throw new UnexpectedException(this);
        }
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final SystemSchema INSTANCE = new SystemSchema();
    }
}
