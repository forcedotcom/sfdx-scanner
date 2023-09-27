package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.ast.modifier.ModifierOrAnnotation;
import apex.jorje.semantic.symbol.member.method.MethodInfo;
import apex.jorje.semantic.symbol.type.ModifierTypeInfos;
import com.salesforce.graph.Schema;
import java.util.Map;

final class MethodWrapper extends AstNodeWrapper<Method> {
    MethodWrapper(Method node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final MethodInfo methodInfo = getNode().getMethodInfo();
        properties.put(Schema.ARITY, methodInfo.getParameters().size());
        final String[] definingTypes = getDefiningType().split("\\.");
        // Handle differences in #getCanonicalName and #getName. #getCanonicalName sometimes is the
        // same as the defining type. In this case we want to use #getName as the method name.
        // TODO: Investigate further
        if (methodInfo.getName().equals(definingTypes[definingTypes.length - 1])) {
            properties.put(Schema.NAME, methodInfo.getCanonicalName());
        } else {
            properties.put(Schema.NAME, methodInfo.getName());
        }
        if (isTestMethod()) {
            // Only add if it is present
            properties.put(Schema.IS_TEST, true);
        }
        if (isImplicitDefaultConstructor()) {
            // Only add if necessary.
            properties.put(Schema.IS_IMPLICIT, true);
        }
        properties.put(Schema.CONSTRUCTOR, methodInfo.isConstructor());
        properties.put(Schema.RETURN_TYPE, methodInfo.getReturnType().getApexName());
    }

    private boolean isTestMethod() {
        final Method method = getNode();
        final MethodInfo methodInfo = method.getMethodInfo();
        final ModifierOrAnnotation modifierOrAnnotation =
                methodInfo.getModifiers().get(ModifierTypeInfos.TEST_METHOD);

        if (modifierOrAnnotation != null) {
            // public void testMethod foo();
            return true;
        } else {
            for (Annotation annotation : method.getModifiers().getAnnotations()) {
                if (annotation
                        .getType()
                        .getApexName()
                        .equalsIgnoreCase(ASTConstants.ANNOTATION_IS_TEST)) {
                    // @IsTest public void foo();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isImplicitDefaultConstructor() {
        final MethodInfo methodInfo = getNode().getMethodInfo();
        return methodInfo.isConstructor()
                && methodInfo.getParameters().isEmpty()
                && !methodInfo.getGenerated().isUserDefined();
    }
}
