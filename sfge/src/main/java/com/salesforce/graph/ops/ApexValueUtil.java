package com.salesforce.graph.ops;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.CircularReferenceException;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueBuilder;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.BinaryExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ParameterVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ApexValueUtil {
    private static final Logger LOGGER = LogManager.getLogger(ApexValueUtil.class);
    private static final String UNKNOWN = "UNKNOWN";

    /** Currently concatenates string values. TODO: Expand to other operations on Integers etc. */
    public static Optional<ApexValue<?>> applyBinaryExpression(
            BinaryExpressionVertex vertex, SymbolProvider symbols) {

        ApexValue<?> lhs = ScopeUtil.resolveToApexValue(symbols, vertex.getLhs()).orElse(null);
        checkNullAccess(lhs, vertex);

        ApexValue<?> rhs;

        if (vertex.getRhs() instanceof BinaryExpressionVertex) {
            rhs =
                    applyBinaryExpression((BinaryExpressionVertex) vertex.getRhs(), symbols)
                            .orElse(null);
        } else {
            rhs = ApexValueBuilder.get(symbols).valueVertex(vertex.getRhs()).build();
        }
        checkNullAccess(rhs, vertex);

        if (vertex.getOperator().equalsIgnoreCase(ASTConstants.OPERATOR_ADDITION)
                && lhs instanceof ApexStringValue
                && rhs instanceof ApexStringValue) {
            return performStringConcatenation(
                    (ApexStringValue) lhs, (ApexStringValue) rhs, vertex, symbols);
        }

        // TODO: Handle more binary expression scenarios

        if (LOGGER.isDebugEnabled()) {
            // This can be because either the binary expression has not been resolved or the are
            // non-strings
            LOGGER.debug("Unable to apply binary expression. vertex=" + vertex);
        }
        return Optional.empty();
    }

    private static void checkNullAccess(ApexValue<?> apexValue, BinaryExpressionVertex vertex) {
        // String value get represented by "null" when used in a concatenation scenario.
        //  Therefore, we exclude String values from null access checks.
        if (apexValue != null && !(apexValue instanceof ApexStringValue)) {
            if (apexValue.isNull()) {
                throw new NullValueAccessedException(apexValue, vertex);
            }
        }
    }

    private static Optional<ApexValue<?>> performStringConcatenation(
            ApexStringValue lhs,
            ApexStringValue rhs,
            BinaryExpressionVertex vertex,
            SymbolProvider symbols) {
        final String lhsValue =
                lhs.isValuePresent() ? lhs.getValue().get() : lhs.isNull() ? "null" : UNKNOWN;
        final String rhsValue =
                rhs.isValuePresent() ? rhs.getValue().get() : rhs.isNull() ? "null" : UNKNOWN;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Applying: lhs=" + lhs + ", rhs=" + rhs);
        }

        if (!lhsValue.equals(UNKNOWN) && !rhsValue.equals(UNKNOWN)) {
            return Optional.of(
                    ApexValueBuilder.get(symbols)
                            .valueVertex(vertex)
                            .buildString(lhsValue + rhsValue));
        }

        return Optional.empty();
    }

    public static boolean isDeterminant(Optional<ApexValue<?>> apexValue) {
        return isDeterminant(apexValue.orElse(null));
    }

    public static boolean isDeterminant(ApexValue<?> apexValue) {
        return apexValue != null && apexValue.isDeterminant();
    }

    public static boolean isIndeterminant(ApexValue<?> apexValue) {
        return !isDeterminant(apexValue);
    }

    /**
     * Recursively, Converts an ApexValue into a string if all of the constituents are strings.
     * Returns Optional.empty if any of the vertices aren't strings.
     */
    public static Optional<String> applyBinaryExpression(
            ApexValue<?> apexValue, SymbolProvider symbols) {
        ChainedVertex vertex = apexValue.getValueVertex().orElse(null);
        if (vertex instanceof BinaryExpressionVertex) {
            return apexValueToString(apexValue, symbols);
        } else if (vertex instanceof MethodCallExpressionVertex
                || vertex instanceof VariableExpressionVertex) {
            return getString(apexValue, apexValue.getValueVertex().get(), symbols);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> apexValueToString(
            ApexValue<?> apexValue, SymbolProvider symbols) {
        BinaryExpressionVertex vertex = (BinaryExpressionVertex) apexValue.getValueVertex().get();
        String operator = vertex.getOperator();
        // We only understand plus operators for strings
        if (!ASTConstants.OPERATOR_ADDITION.equalsIgnoreCase(operator)) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        List<ChainedVertex> expanded = expand(vertex, symbols);
        for (ChainedVertex chainedVertex : expanded) {
            String value = getString(apexValue, chainedVertex, symbols).orElse(null);
            if (value != null) {
                sb.append(value);
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(sb.toString());
    }

    private static Optional<String> getString(
            @Nullable ApexValue<?> apexValue, ChainedVertex chainedVertex, SymbolProvider symbols) {
        if (chainedVertex instanceof LiteralExpressionVertex.SFString) {
            return Optional.of(((LiteralExpressionVertex.SFString) chainedVertex).getLiteral());
        } else if (chainedVertex instanceof MethodCallExpressionVertex) {
            MethodCallExpressionVertex methodCallExpression =
                    (MethodCallExpressionVertex) chainedVertex;
            ApexValue<?> methodCallApexValue =
                    symbols.getReturnedValue(methodCallExpression).orElse(null);
            if (methodCallApexValue != null && methodCallApexValue.getValueVertex().isPresent()) {
                return getString(
                        methodCallApexValue, methodCallApexValue.getValueVertex().get(), symbols);
            }
        } else if (chainedVertex instanceof VariableExpressionVertex) {
            ChainedVertex resolved = apexValue.getValue(chainedVertex, symbols).orElse(null);
            if (resolved != null) {
                return getString(apexValue, resolved, symbols);
            }
        }

        return Optional.empty();
    }

    /**
     * Expands an BinaryExpression into its composite parts. Recursive because a
     * BinaryExpressionVertex may contain another BinaryExpressionVertex.
     */
    public static List<ChainedVertex> expand(
            BinaryExpressionVertex vertex, SymbolProvider symbolProvider) {
        List<ChainedVertex> result = new ArrayList<>();

        ChainedVertex lhs = vertex.getLhs();
        if (lhs.isResolvable()) {
            lhs = symbolProvider.getValue(lhs).orElse(lhs);
        }

        ChainedVertex rhs = vertex.getRhs();
        if (rhs.isResolvable()) {
            rhs = symbolProvider.getValue(rhs).orElse(rhs);
        }

        if (lhs instanceof BinaryExpressionVertex) {
            result.addAll(expand((BinaryExpressionVertex) lhs, symbolProvider));
        } else {
            result.add(lhs);
        }

        if (rhs instanceof BinaryExpressionVertex) {
            result.addAll(expand((BinaryExpressionVertex) rhs, symbolProvider));
        } else {
            result.add(rhs);
        }

        return result;
    }

    /**
     * Return synthetic values from Apex classes that only exist as stubs.
     *
     * @param builder that was configured with information such as {@link
     *     ApexValueBuilder#returnedFrom(ApexValue, InvocableVertex)}
     * @param method the standard method that was invoked
     * @throws UnexpectedException if {@code builder} hasn't been configured properly or {@code
     *     method} does not correspond to a standard Apex class
     */
    public static ApexValue<?> synthesizeReturnedValue(
            ApexValueBuilder builder, MethodVertex method) {
        if (!method.isStandardType()) {
            throw new UnexpectedException(method);
        }

        // Only invocable is guaranteed to be non-null. returnedFrom can be null for cases where the
        // value is returned
        // from methods such as UserInfo.getUserId()
        if (builder.getInvocable() == null) {
            throw new UnexpectedException("Builder is not configured properly");
        }

        return builder.declarationVertex(SyntheticTypedVertex.get(method.getReturnType()))
                .withStatus(ValueStatus.INDETERMINANT)
                .build();
    }

    /**
     * Create a synthetic values for method parameters. This is useful when the entry point to a
     * path is a method and we don't know which values will be passed to the method.
     */
    public static ApexValue<?> synthesizeMethodParameter(ParameterVertex parameter) {
        return ApexValueBuilder.getWithoutSymbolProvider()
                .declarationVertex(parameter)
                .withStatus(ValueStatus.INDETERMINANT)
                .build();
    }

    /** Derive a String value to represent an ApexValue to end users. */
    public static Optional<String> deriveUserFriendlyDisplayName(ApexValue<?> apexValue) {
        Optional<String> stringEquivalent = Optional.empty();

        if (apexValue != null) {
            final Optional<String> declaredType = apexValue.getDeclaredType();
            final Optional<Typeable> typeVertex = apexValue.getTypeVertex();
            final Optional<String> variableName = apexValue.getVariableName();

            if (declaredType.isPresent()) {
                // For example, Account in this snippet:
                // List<Account> accounts = [SELECT Id, Name FROM Account];
                stringEquivalent = declaredType;
            } else if (variableName.isPresent()) {
                // When we only know the variable name but not its value
                // For example, when we receive variable as a method parameter
                stringEquivalent = variableName;
            } else if (typeVertex.isPresent()) {
                // For example, Account in:
                // List<SObject> sObj = [SELECT Name from Account];
                stringEquivalent = Optional.of(typeVertex.get().getCanonicalType());
            }
        }

        return stringEquivalent;
    }

    /**
     * Called by ApexValues when adding a new contained value. Throws an exception if {@code
     * parentValue} is the same object as {@code parameter}. This indicates a bug in the scope
     * resolution.
     *
     * @throws CircularReferenceException if a circular reference is detected
     */
    public static void assertNotCircular(
            ApexValue<?> parentValue,
            ApexValue<?> parameter,
            InvocableWithParametersVertex vertex) {
        if (parentValue == parameter) {
            throw new CircularReferenceException(parentValue, vertex);
        }
    }

    /**
     * Get nearest string value(s) of a given apex value. A For loop could fetch multiple values.
     */
    public static TreeSet<String> convertApexValueToString(ApexValue<?> apexValue) {
        final TreeSet<String> stringValues = CollectionUtil.newTreeSet();

        if (apexValue instanceof ApexStringValue) {
            final ApexStringValue apexStringValue = (ApexStringValue) apexValue;
            final Optional<String> apexStringOptional = getTypeValue(apexStringValue);
            if (apexStringOptional.isPresent()) {
                stringValues.add(apexStringOptional.get());
            }
        } else if (apexValue instanceof ApexForLoopValue) {
            final ApexForLoopValue apexForLoopValue = (ApexForLoopValue) apexValue;
            final List<ApexValue<?>> forLoopValues = apexForLoopValue.getForLoopValues();
            for (ApexValue<?> forLoopValue : forLoopValues) {
                stringValues.addAll(convertApexValueToString(forLoopValue));
            }
        }

        return stringValues;
    }

    /**
     * Gets type that a {@link ApexStringValue} represents. This cannot replace {@link
     * ApexStringValue#getValue()} since this attempts to dig further based on vertex types.
     */
    private static Optional<String> getTypeValue(ApexStringValue apexValue) {
        Optional<String> apexStringOptional = apexValue.getValue();
        if (!apexStringOptional.isPresent()) {
            // TODO: Add some indication to this result that it is not exact and only an estimate
            final ChainedVertex valueVertex = apexValue.getValueVertex().orElse(null);
            final Typeable declarationVertex = apexValue.getDeclarationVertex().orElse(null);
            final InvocableVertex invocableVertex = apexValue.getInvocable().orElse(null);
            // TODO: Support strings that are generated by concatenating values
            if (valueVertex instanceof BinaryExpressionVertex) {
                final BinaryExpressionVertex binaryExpressionVertex =
                        (BinaryExpressionVertex) valueVertex;
                if (binaryExpressionVertex.getOperator().equals(ASTConstants.OPERATOR_ADDITION)) {
                    ChainedVertex lhs = binaryExpressionVertex.getLhs();
                    while (lhs instanceof BinaryExpressionVertex) {
                        lhs = ((BinaryExpressionVertex) lhs).getLhs();
                    }
                    throw new TodoException("Support string concatenation. lhs=" + lhs);
                }
            } else if (declarationVertex instanceof ParameterVertex) {
                // We started walking a method where the value isn't resolvable. Use the parameter
                // name
                final ParameterVertex parameter = (ParameterVertex) declarationVertex;
                apexStringOptional = Optional.of(parameter.getName());
            } else if (valueVertex instanceof VariableExpressionVertex) {
                // Example(unresolved property is assigned to a variable):
                // String fieldName = null;
                // for(FieldDefinition fd : fds) {
                // 		if (fd.DataType.contains('Something')) {
                //  		fieldName = fd.QualifiedApiName;
                //  	}
                //  }
                //  obj.put(fieldName, 'Acme Inc.');
                final VariableExpressionVertex variableExpression =
                        (VariableExpressionVertex) valueVertex;
                apexStringOptional = Optional.of(variableExpression.getFullName());
            } else if (valueVertex instanceof MethodCallExpressionVertex) {
                // Example(unresolved method is assigned to a variable):
                // String objectName = getObjectName();\n" +
                // SObject obj = Schema.getGlobalDescribe().get(objectName).newSObject();\n" +
                // apexStringOptional will be "getObjectName"
                final MethodCallExpressionVertex methodCallExpression =
                        (MethodCallExpressionVertex) valueVertex;
                apexStringOptional = Optional.of(methodCallExpression.getFullMethodName());
            } else if (invocableVertex instanceof MethodCallExpressionVertex) {
                // Example(unresolved method is used inline):
                // SObject obj =
                // Schema.getGlobalDescribe().get(UserInfo.getOrganizationName()).newSObject();
                // apexStringOptional will be "UserInfo.getOrganizationName"
                final MethodCallExpressionVertex methodCallExpression =
                        (MethodCallExpressionVertex) invocableVertex;
                apexStringOptional = Optional.of(methodCallExpression.getFullMethodName());
            } else {
                throw new UnexpectedException(apexValue);
            }
        }
        return apexStringOptional;
    }

    private ApexValueUtil() {}
}
