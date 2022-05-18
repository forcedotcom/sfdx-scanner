package com.salesforce.graph.vertex;

import java.util.Optional;

/** A vertex that may be used as a key in a {@link NewKeyValueObjectExpressionVertex} */
public interface KeyVertex {
    /**
     * The name of the key. For example, the key for the literal expression would be "Name" in the
     * following example.
     *
     * <p>Account a = new Account(Name = 'Acme Inc.');
     *
     * @return May return Optional.empty in cases where this vertex is used in a context where it is
     *     not a key
     */
    Optional<String> getKeyName();

    /** Only call {@link #getKeyName()} if this is {@link ExpressionType#KEY_VALUE} */
    ExpressionType getExpressionType();
}
