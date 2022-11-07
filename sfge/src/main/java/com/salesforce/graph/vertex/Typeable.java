package com.salesforce.graph.vertex;

import com.salesforce.graph.ops.TypeableUtil;

/** Represents a vertex that has a specific type, such as 'String' or 'SObject' */
// TODO: Move comparsion of two Typeable objects into a static helper, I don't think that transitive
// equality always
// holds for current classes that implement Typeable
public interface Typeable {
    /**
     * @return string representation of the canonical type. This may differ from the actual
     *     declaration. See {@link
     *     com.salesforce.graph.ops.ApexStandardLibraryUtil#getCanonicalName} to understand when it
     *     may differ.
     */
    String getCanonicalType();

    /**
     * @return all types that this object supports. This is typically used for subclasses, classes
     *     that implement interfaces, and interfaces that extend other interfaces.
     */
    default TypeableUtil.OrderedTreeSet getTypes() {
        final String type = getCanonicalType();
        return TypeableUtil.getTypeHierarchy(type);
    }

    /**
     * @return true if this vertex is a match for parameterVertex
     */
    default boolean matchesParameterType(Typeable parameterVertex) {
        final TypeableUtil.OrderedTreeSet types = getTypes();
        final String type = parameterVertex.getCanonicalType();
        return types.contains(type);
    }

    /**
     * @return the rank of current type's match against parameter's type. Lower the number, greater
     *     the match. -1 indicates not-a-match.
     */
    default int rankParameterMatch(Typeable parameterVertex) {
        return TypeableUtil.rankParameterMatch(this, parameterVertex);
    }
}
