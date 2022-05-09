package com.salesforce.graph.symbols.apex;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.apex.schema.DescribeFieldResult;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.FieldSet;
import com.salesforce.graph.symbols.apex.schema.FieldSetMember;
import com.salesforce.graph.symbols.apex.schema.SObjectField;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.symbols.apex.system.SObjectAccessDecision;
import com.salesforce.graph.symbols.apex.system.SystemSchema;

/**
 * A visitor that allows for distinct return values. Use this class to avoid "instanceof" pattern.
 */
public abstract class ApexValueVisitor<T> {
    /** {@link #defaultVisit} is a no-op that returns null */
    public abstract static class DefaultNoOp<T> extends ApexValueVisitor<T> {
        @Override
        public T defaultVisit(ApexValue<?> value) {
            return null;
        }
    }

    /** {@link #defaultVisit} throws an exception */
    public abstract static class DefaultThrow<T> extends ApexValueVisitor<T> {
        @Override
        public T defaultVisit(ApexValue<?> value) {
            throw new UnexpectedException(value);
        }
    }

    abstract T defaultVisit(ApexValue<?> value);

    public T visit(ApexBooleanValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexClassInstanceValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexCustomValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexDecimalValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexDoubleValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexEnumValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexFieldDescribeMapValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexFieldSetDescribeMapValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexFieldSetListValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexForLoopValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexGlobalDescribeMapValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexIdValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexIntegerValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexListValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexLongValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexMapValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexSetValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexSingleValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexSoqlValue value) {
        return defaultVisit(value);
    }

    public T visit(ApexStringValue value) {
        return defaultVisit(value);
    }

    public T visit(DescribeFieldResult value) {
        return defaultVisit(value);
    }

    public T visit(DescribeSObjectResult value) {
        return defaultVisit(value);
    }

    public T visit(FieldSet value) {
        return defaultVisit(value);
    }

    public T visit(FieldSetMember value) {
        return defaultVisit(value);
    }

    public T visit(SObjectAccessDecision value) {
        return defaultVisit(value);
    }

    public T visit(SObjectField value) {
        return defaultVisit(value);
    }

    public T visit(SObjectType value) {
        return defaultVisit(value);
    }

    public T visit(SystemSchema value) {
        return defaultVisit(value);
    }
}
