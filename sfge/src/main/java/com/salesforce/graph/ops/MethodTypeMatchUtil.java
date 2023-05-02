package com.salesforce.graph.ops;

import static com.salesforce.graph.ops.TypeableUtil.NOT_A_MATCH;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.symbols.AbstractClassInstanceScope;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexClassInstanceValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/** Performs type-related operations while matching method calls with method definitions. */
public final class MethodTypeMatchUtil {
    private MethodTypeMatchUtil() {}

    static boolean methodVariableTypeMatches(
            GraphTraversalSource g,
            MethodVertex method,
            String hostVariable,
            SymbolProvider symbols) {
        // Get the declaration of the variable, and make sure its type matches.
        Typeable declaration = symbols.getTypedVertex(hostVariable).orElse(null);

        // If there's a declaration vertex and it's of the exact right type, we're in the clear.
        if (declaration != null
                && declaration.getCanonicalType().equalsIgnoreCase(method.getDefiningType())) {
            return true;
        } else if (declaration != null) {
            // If there's a declaration vertex, but the types don't exactly match, then we need to
            // figure out whether
            // inheritance is relevant.
            // For that, we need the type where the method is defined, and we need the type of the
            // declared variable.
            String methodDefiningType = method.getDefiningType();
            String declaredType = declaration.getCanonicalType();

            List<BaseSFVertex> inheritedTypes =
                    SFVertexFactory.loadVertices(
                            g,
                            g.V()
                                    // Starting from the vertex that represents the method's
                                    // defining class/interface...
                                    .hasLabel(
                                            P.within(
                                                    ASTConstants.NodeType.USER_INTERFACE,
                                                    ASTConstants.NodeType.USER_CLASS))
                                    .where(
                                            CaseSafePropertyUtil.H.has(
                                                    Arrays.asList(
                                                            ASTConstants.NodeType.USER_CLASS,
                                                            ASTConstants.NodeType.USER_INTERFACE),
                                                    Schema.NAME,
                                                    methodDefiningType))
                                    // ...follow edges to any supertypes...
                                    .repeat(__.out(Schema.IMPLEMENTATION_OF, Schema.EXTENSION_OF))
                                    // ...until we run into the declared type of the host variable.
                                    .until(
                                            CaseSafePropertyUtil.H.has(
                                                    Arrays.asList(
                                                            ASTConstants.NodeType.USER_CLASS,
                                                            ASTConstants.NodeType.USER_INTERFACE),
                                                    Schema.NAME,
                                                    declaredType)));

            // If any vertices were returned by the above query, then the method's defining type
            // inherits from the variable's
            // declared type, in which case we're good. If not, return false.
            return !inheritedTypes.isEmpty();
        } else {
            // If there's no declaration, we can just return false.
            return false;
        }
    }

    static int parameterTypesMatch(
            MethodVertex method, InvocableWithParametersVertex invocable, SymbolProvider symbols) {
        int rank = 0;
        // For each of the method's parameters...
        for (int i = 0; i < method.getParameters().size(); i++) {
            ParameterVertex methodParameter = method.getParameters().get(i);
            ChainedVertex invokedParameter = invocable.getParameters().get(i);

            // Look at the child of the Post/Prefix expression to determine the parameter's type
            if (invokedParameter instanceof PostfixExpressionVertex
                    || invokedParameter instanceof PrefixExpressionVertex) {
                invokedParameter = invokedParameter.getOnlyChild();
            }

            // Choose one of the ternary results to match. Iteratively resolve in case there are
            // multiple
            // ternary vertices, i.e. s = x ? 'x' : y ? 'y' : 'z'
            while (invokedParameter instanceof TernaryExpressionVertex) {
                TernaryExpressionVertex ternaryExpression =
                        (TernaryExpressionVertex) invokedParameter;
                // Choose the first option.
                invokedParameter = ternaryExpression.getTrueValue();
            }

            Typeable typeable;
            // TODO: replace with visitor
            if (invokedParameter instanceof LiteralExpressionVertex.Null) {
                // Everything is considered a match to NULL
                continue;
            } else if (invokedParameter instanceof NewCollectionExpressionVertex) {
                if (!methodParameter
                        .getCanonicalType()
                        .toLowerCase()
                        .startsWith(
                                ((NewCollectionExpressionVertex) invokedParameter)
                                        .getTypePrefix())) {
                    return TypeableUtil.NOT_A_MATCH;
                }

                rank = getMatchRank(rank, (Typeable) invokedParameter, methodParameter);
            } else if (invokedParameter instanceof VariableExpressionVertex) {
                typeable =
                        getTypedVertex((VariableExpressionVertex) invokedParameter, symbols)
                                .orElse(null);
                rank = getMatchRank(rank, typeable, methodParameter);
            } else if (invokedParameter instanceof SoqlExpressionVertex) {
                typeable = (SoqlExpressionVertex) invokedParameter;
                rank = getMatchRank(rank, typeable, methodParameter);
            } else if (invokedParameter instanceof InvocableVertex) {
                typeable =
                        getTypeFromInvocableVertexReturnValue(
                                symbols, (InvocableVertex) invokedParameter);
                rank = getMatchRank(rank, typeable, methodParameter);
            } else if (invokedParameter instanceof BinaryExpressionVertex) {
                typeable =
                        ((BinaryExpressionVertex) invokedParameter)
                                .getTypedVertex(symbols)
                                .orElse(null);
                rank = getMatchRank(rank, typeable, methodParameter);
            } else if (invokedParameter instanceof Typeable) {
                typeable = (Typeable) invokedParameter;
                rank = getMatchRank(rank, typeable, methodParameter);
                // TODO: Why can't this be first?
                // If the invocable uses literal parameters of the wrong type, then it's not a
                // match.
            } else {
                typeable = getTypeFromSymbol(symbols, invokedParameter);
                rank = getMatchRank(rank, typeable, methodParameter);
            }

            // If we hit a NOT_A_MATCH at any point, return immediately
            if (rank == NOT_A_MATCH) {
                return rank;
            }
        }
        // If we couldn't find a reason to return false, then it's a match. Return true.
        return rank;
    }

    private static Typeable getTypeFromSymbol(
            SymbolProvider symbols, ChainedVertex invokedParameter) {
        String symbolicName = invokedParameter.getSymbolicName().orElse(null);
        if (symbolicName == null) {
            throw new UnexpectedException(invokedParameter);
        }
        // If the desired variable has no declaration, or is declared as the wrong type, then it's
        // not a match.
        Typeable typeable = symbols.getTypedVertex(symbolicName).orElse(null);
        return typeable;
    }

    private static Typeable getTypeFromInvocableVertexReturnValue(
            SymbolProvider symbols, InvocableVertex invocableVertex) {
        Typeable typeable = null;

        ApexValue<?> apexValue = symbols.getReturnedValue(invocableVertex).orElse(null);
        if (apexValue != null) {
            typeable = apexValue.getTypeVertex().orElse(null);
        }
        return typeable;
    }

    private static int getMatchRank(int rank, Typeable typeable, ParameterVertex methodParameter) {
        if (typeable == null || !typeable.matchesParameterType(methodParameter)) {
            return NOT_A_MATCH;
        }

        rank += typeable.rankParameterMatch(methodParameter);
        return rank;
    }

    /** Determine a variable expression's type if possible */
    private static Optional<Typeable> getTypedVertex(
            VariableExpressionVertex vertex, SymbolProvider symbols) {
        Typeable typeable;

        if (vertex instanceof VariableExpressionVertex.Standard) {
            typeable = (VariableExpressionVertex.Standard) vertex;
        } else {
            List<String> symbolicNameChain = vertex.getSymbolicNameChain();
            typeable = symbols.getTypedVertex(symbolicNameChain).orElse(null);
            Optional<ApexValue<?>> apexValue = Optional.empty();

            // Special casing FieldVertex since we don't get information about the class hierarchy
            // unless we have the actual class scope in hand.
            if (typeable instanceof FieldVertex) {
                apexValue = symbols.getApexValue(((FieldVertex) typeable).getName());
            }
            typeable = getDeclarationTypeWhenAvailable(typeable, apexValue);
        }

        return Optional.ofNullable(typeable);
    }

    /**
     * When matching method parameter types, we want to match based on declaration value rather than
     * the initialization value. For example, SObject sobj = new Account(); matchMe(sobj);
     *
     * <p>void matchMe(SObject s) - should match void matchMe(Account a) - should not match
     *
     * @return declaration type if available. If not, same as the value provided.
     */
    private static Typeable getDeclarationTypeWhenAvailable(
            Typeable typeable, Optional<ApexValue<?>> apexValue) {
        if (typeable instanceof ApexValue
                && ((ApexValue<?>) typeable).getDeclarationVertex().isPresent()) {
            Typeable declarationType = ((ApexValue<?>) typeable).getDeclarationVertex().get();
            if (!declarationType.getCanonicalType().equalsIgnoreCase(typeable.getCanonicalType())) {
                // Prioritize declaration type over value type
                typeable = declarationType;
            }
        }
        if (apexValue.isPresent() && apexValue.get() instanceof ApexClassInstanceValue) {
            final AbstractClassInstanceScope classType =
                    ((ApexClassInstanceValue) apexValue.get()).getClassInstanceScope();
            // If class type and the Typeable have the same canonical type, prefer class type
            // since we get more information about class hierarchy.
            if (classType.getCanonicalType().equalsIgnoreCase(typeable.getCanonicalType())) {
                typeable = classType;
            }
        } else if (typeable instanceof DeclarationVertex) {
            final ChainedVertex lhs = ((DeclarationVertex) typeable).getLhs();
            typeable = (lhs instanceof Typeable) ? (Typeable) lhs : typeable;
        }
        return typeable;
    }
}
