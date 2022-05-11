package com.salesforce.graph.symbols;

import com.salesforce.graph.symbols.apex.ApexValue;
import java.util.Map;
import java.util.Optional;

public interface ObjectProperties {
    /**
     * SObject obj = Schema.getGlobalDescribe().get('Account').newSObject();
     *
     * <p>Get the type of the declared variable. This may be less specific than the ConcreteType and
     * also available before any assignment has occurred.
     */
    Optional<String> getDeclaredType();

    /** SObject obj = Schema.getGlobalDescribe().get('Account').newSObject(); would return 'obj' */
    Optional<String> getVariableName();

    /**
     * Get all properties as ApexValues that were set during instantiation, via #put or, property
     * assignment.
     */
    Map<ApexValue<?>, ApexValue<?>> getApexValueProperties();

    /**
     * Get the property as an ApexValue that was set during instantiation, via #put or, property
     * assignment.
     */
    Optional<ApexValue<?>> getApexValue(ApexValue<?> key);

    /**
     * Get the property as an ApexValue that was set during instantiation, via #put or, property
     * assignment.
     */
    Optional<ApexValue<?>> getApexValue(String key);

    /**
     * Find an existing ApexValue in the map of properties. If no value can be found, try to create
     * a default.
     *
     * @return
     */
    public Optional<ApexValue<?>> getOrAddDefault(String key);

    /** Add an ApexValue to the map of of properties. */
    void putApexValue(ApexValue<?> key, ApexValue<?> value);

    /**
     * Adds a new property to the object which is indeterminant. Used to track state when properties
     * that that haven't been explicitly set are compared in if/else conditions
     *
     * @throws com.salesforce.exception.UnexpectedException if the key already exists. This should
     *     only be called in instances where the caller knows that the key doesn't exist.
     *     <p>and should be removed.
     */
    void putConstrainedApexValue(ApexValue<?> key, ApexValue<?> value);
}
