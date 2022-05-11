package com.salesforce.graph.vertex;

/**
 * Any vertex that contains an operator such as {@code ++}, {@code --}, {@code ==}, {@code <=} etc.
 */
public interface OperatorVertex {
    String getOperator();
}
