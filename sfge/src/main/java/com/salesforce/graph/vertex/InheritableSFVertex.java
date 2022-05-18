package com.salesforce.graph.vertex;

import java.util.Optional;

public interface InheritableSFVertex extends SFVertex {
    Optional<String> getSuperClassName();
}
