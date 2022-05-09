package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import java.util.Stack;

/** Generic context that supports pushing and popping a specific type onto a stack */
public final class StackableContext<T> {
    /**
     * This is intentionally not static. The reference to the class instance itself will be set as a
     * static. See {@link ContextProviders}
     */
    private ThreadLocal<Stack<T>> THREAD_LOCAL = ThreadLocal.withInitial(() -> new Stack<>());

    StackableContext() {}

    public void pop() {
        THREAD_LOCAL.get().pop();
    }

    public T get() {
        if (THREAD_LOCAL.get().isEmpty()) {
            throw new UnexpectedException("Stack is empty");
        }
        return THREAD_LOCAL.get().peek();
    }

    public void push(T t) {
        THREAD_LOCAL.get().push(t);
    }

    public void assertEmpty() {
        if (!THREAD_LOCAL.get().isEmpty()) {
            throw new UnexpectedException("Stack is not empty");
        }
    }
}
