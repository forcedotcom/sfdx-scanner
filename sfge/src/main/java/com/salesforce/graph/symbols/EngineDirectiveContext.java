package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.directive.EngineDirective;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/** Keeps track of {@link EngineDirective}s as methods are called while walking a path */
public final class EngineDirectiveContext implements DeepCloneable<EngineDirectiveContext> {
    private final Stack<List<EngineDirective>> stack;

    public EngineDirectiveContext() {
        this.stack = new Stack<>();
    }

    private EngineDirectiveContext(EngineDirectiveContext other) {
        this.stack = CloneUtil.cloneStack(other.stack);
    }

    @Override
    public EngineDirectiveContext deepClone() {
        return new EngineDirectiveContext(this);
    }

    /** Called before invoking a MethodCallExpression or entering a Method */
    public void push(List<EngineDirective> engineDirectives) {
        stack.push(engineDirectives);
    }

    /** Called after invoking a MethodCallExpression or exiting a Method */
    public void pop(List<EngineDirective> engineDirectives) {
        List<EngineDirective> popped = stack.pop();
        if (!popped.equals(engineDirectives)) {
            throw new UnexpectedException("Mismatch");
        }
    }

    public void clear() {
        this.stack.clear();
    }

    /**
     * @return the unique set of {@link EngineDirective}s that are currently in scope. This is the
     *     accumulation of the entire stack.
     */
    public Set<EngineDirective> getEngineDirectives() {
        Set<EngineDirective> result = new HashSet<>();

        for (List<EngineDirective> directives : stack) {
            result.addAll(directives);
        }

        return result;
    }
}
