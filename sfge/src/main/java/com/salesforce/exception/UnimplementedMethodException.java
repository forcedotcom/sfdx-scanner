package com.salesforce.exception;

import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;

/** Thrown by ApexValues when they have not implemented a specific method. */
public final class UnimplementedMethodException extends SfgeRuntimeException {
    private static final String MESSAGE_FORMAT = "%s:%s, vertex=%s";

    public UnimplementedMethodException(ApexValue<?> apexValue, MethodCallExpressionVertex vertex) {
        super(getMessage(apexValue, vertex.getMethodName(), vertex));
    }

    public UnimplementedMethodException(ApexValue<?> apexValue, MethodVertex vertex) {
        super(getMessage(apexValue, vertex.getName(), vertex));
    }

    private static String getMessage(
            ApexValue<?> apexValue, String methodName, ChainedVertex vertex) {
        return String.format(
                MESSAGE_FORMAT, apexValue.getClass().getSimpleName(), methodName, vertex);
    }
}
