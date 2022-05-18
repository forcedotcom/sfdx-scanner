package com.salesforce.graph.vertex;

/**
 * Literal and Variable expressions appear as simple types, or as key value pairs. In the case of
 * key value pairs, their Name property is the key in the key value pair. For example: {@code
 * Account a = new Account(Phone = '415-555-1212'); } Creates a NewKeyValueObjectExpressionVertex
 * with a LiteralExpression child that has the following properties {@code <LiteralExpression
 * Image='415-555-1212' Name='Phone' /> } TODO: This is a leaky abstraction, would be good to hide
 * it. Consider creating an entirely new class
 */
public enum ExpressionType {
    KEY_VALUE,
    SIMPLE,
    UNKNOWN
}
