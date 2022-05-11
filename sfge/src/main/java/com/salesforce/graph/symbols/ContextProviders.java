package com.salesforce.graph.symbols;

/**
 * Static access to all Context base classes. This pattern allows reuse of {@link StackableContext}s
 * for different providers.
 */
public final class ContextProviders {
    public static final StackableContext<ClassStaticScopeProvider> CLASS_STATIC_SCOPE =
            new StackableContext<>();

    public static final StackableContext<EngineDirectiveContextProvider> ENGINE_DIRECTIVE_CONTEXT =
            new StackableContext<>();

    private ContextProviders() {}
}
