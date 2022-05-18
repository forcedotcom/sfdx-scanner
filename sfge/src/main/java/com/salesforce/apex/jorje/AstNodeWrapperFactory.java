package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.AstNode;
import com.salesforce.graph.ops.ReflectionUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Wraps a Jorje {@link AstNode} in its equivalent {@link AstNodeWrapper} implementation. */
final class AstNodeWrapperFactory {
    static <T extends AstNodeWrapper> T getVertex(AstNode node, AstNodeWrapper<?> parent) {
        final Class<?> clazz = ReflectionUtil.getClass(getWrapperClassName(node)).orElse(null);
        if (clazz != null) {
            try {
                final Constructor constructor =
                        clazz.getDeclaredConstructor(node.getClass(), AstNodeWrapper.class);
                return (T) constructor.newInstance(node, parent);
            } catch (InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException
                    | NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        return (T) new DefaultWrapper(node, parent);
    }

    private static String getWrapperClassName(AstNode node) {
        return "com.salesforce.apex.jorje." + node.getClass().getSimpleName() + "Wrapper";
    }

    private AstNodeWrapperFactory() {}
}
