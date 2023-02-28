package com.salesforce.rules;

/**
 * Abstract parent class for rules whose execution requires the construction and/or traversal of
 * {@link com.salesforce.graph.ApexPath} instances. Currently, this class has no methods, but exists
 * because it's beneficial to have all path-based rules share a common parent class.
 */
public abstract class AbstractPathBasedRule extends AbstractRule {}
