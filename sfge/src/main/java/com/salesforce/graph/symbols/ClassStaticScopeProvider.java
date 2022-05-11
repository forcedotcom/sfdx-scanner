package com.salesforce.graph.symbols;

import java.util.Optional;

/**
 * Allows ApexPathExpander and ApexPathWalker to store a reference to itself. Allows easy access to
 * classes that are participating in the path that is being walked. ClassInstances use this class to
 * obtain a reference the class that they use as their inherited scope.
 *
 * <p>Implemented by classes that maintain a set of ClassStaticScopes that should be shared while
 * walking a path.
 */
public interface ClassStaticScopeProvider {
    Optional<ClassStaticScope> getClassStaticScope(String className);
}
