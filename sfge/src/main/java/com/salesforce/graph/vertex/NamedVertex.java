package com.salesforce.graph.vertex;

/** Implemented by vertices that can be named, such as a UserClass or Variable. */
public interface NamedVertex {
    /** @return the name that identifies this vertex */
    String getName();
}
