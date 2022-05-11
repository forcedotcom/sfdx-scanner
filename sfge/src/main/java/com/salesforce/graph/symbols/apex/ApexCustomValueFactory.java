package com.salesforce.graph.symbols.apex;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodCallApexValueBuilder;
import com.salesforce.graph.vertex.AbstractReferenceExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.ReferenceExpressionVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

/** Generates {@link ApexCustomValue}s from methods. */
public final class ApexCustomValueFactory {
    static final TreeSet<String> CUSTOM_SETTING_METHOD_NAMES;
    static final String GET_ALL = "getAll";
    static final String GET_INSTANCE = "getInstance";
    static final String GET_ORG_DEFAULTS = "getOrgDefaults";
    static final String GET_VALUES = "getValues";

    static {
        CUSTOM_SETTING_METHOD_NAMES = CollectionUtil.newTreeSet();
        CUSTOM_SETTING_METHOD_NAMES.add(ApexCustomValueFactory.GET_ALL);
        CUSTOM_SETTING_METHOD_NAMES.add(ApexCustomValueFactory.GET_INSTANCE);
        CUSTOM_SETTING_METHOD_NAMES.add(ApexCustomValueFactory.GET_ORG_DEFAULTS);
        CUSTOM_SETTING_METHOD_NAMES.add(ApexCustomValueFactory.GET_VALUES);
    }

    public static final MethodCallApexValueBuilder METHOD_CALL_BUILDER_FUNCTION =
            (g, vertex, symbols) -> {
                String methodName = vertex.getMethodName();

                if (CUSTOM_SETTING_METHOD_NAMES.contains(methodName)) {
                    AbstractReferenceExpressionVertex abstractReferenceExpression =
                            vertex.getReferenceExpression();
                    if (abstractReferenceExpression instanceof ReferenceExpressionVertex) {
                        ReferenceExpressionVertex referenceExpression =
                                (ReferenceExpressionVertex) abstractReferenceExpression;
                        String name = referenceExpression.getName();
                        if (referenceExpression
                                        .getReferenceType()
                                        .equals(ASTConstants.ReferenceType.METHOD)
                                && name.toLowerCase(Locale.ROOT)
                                        .endsWith(ASTConstants.TypeSuffix.SUFFIX_CUSTOM_OBJECT)) {
                            Typeable typeable = SyntheticTypedVertex.get(name);
                            ApexValueBuilder builder =
                                    ApexValueBuilder.getWithoutSymbolProvider().valueVertex(vertex);
                            if (referenceExpression.getNames().size() == 1) {
                                MethodCallExpressionVertex methodCallExpression =
                                        referenceExpression.getParent();
                                // These method calls take 0 or 1 parameters
                                if (GET_INSTANCE.equalsIgnoreCase(
                                                methodCallExpression.getMethodName())
                                        || GET_VALUES.equalsIgnoreCase(
                                                methodCallExpression.getMethodName())) {
                                    if (methodCallExpression.getParameters().size() <= 1) {
                                        return Optional.of(builder.buildCustomValue(typeable));
                                    }
                                } else if (GET_ORG_DEFAULTS.equalsIgnoreCase(
                                        methodCallExpression.getMethodName())) {
                                    if (methodCallExpression.getParameters().size() == 0) {
                                        return Optional.of(builder.buildCustomValue(typeable));
                                    }
                                } else if (GET_ALL.equalsIgnoreCase(
                                        methodCallExpression.getMethodName())) {
                                    if (methodCallExpression.getParameters().size() == 0) {
                                        builder.withStatus(ValueStatus.INDETERMINANT)
                                                .declarationVertex(
                                                        SyntheticTypedVertex.get(
                                                                ApexStandardLibraryUtil
                                                                        .getMapDeclaration(
                                                                                ApexStandardLibraryUtil
                                                                                        .Type
                                                                                        .STRING,
                                                                                name)));
                                        return Optional.of(builder.buildMap());
                                    }
                                }
                            }
                        }
                    }
                }

                return Optional.empty();
            };

    private ApexCustomValueFactory() {}
}
