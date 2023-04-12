package com.salesforce.graph.ops.directive;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.graph.EnumUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents instructions given to the engine via a string. This could be to enable/disable certain
 * rules. These are currently communicated via Apex comments but could be provided by other means
 * such as configuration files or environment variables.
 */
public enum EngineDirectiveCommand {
    /**
     * Statically disable the engine in a context such as a Class. This value can be used in static
     * contexts and does not require a path based rule.
     */
    DISABLE(Token.SFGE_DISABLE, NodeType.USER_CLASS),

    /**
     * Statically disable the engine for the next line of code only. This value can be used in
     * static contexts and does not require a path based rule.
     */
    DISABLE_NEXT_LINE(
            Token.SFGE_DISABLE + Token.DIRECTIVE_SEPARATOR + Token.NEXT_LINE,
            NodeType.DML_DELETE_STATEMENT,
            NodeType.DML_INSERT_STATEMENT,
            NodeType.DML_MERGE_STATEMENT,
            NodeType.DML_UNDELETE_STATEMENT,
            NodeType.DML_UPDATE_STATEMENT,
            NodeType.DML_UPSERT_STATEMENT,
            NodeType.EXPRESSION_STATEMENT,
            NodeType.METHOD,
            NodeType.VARIABLE_EXPRESSION,
            NodeType.VARIABLE_DECLARATION_STATEMENTS),

    /**
     * Dynamically disable the engine until the corresponding item returns
     *
     * <p>
     *
     * <p>This command is valid in the following contexts
     *
     * <ul>
     *   <li>{@link com.salesforce.graph.vertex.MethodVertex} - applies from the point that the
     *       method is entered until the last statement in the method, including any invocations of
     *       other methods.
     *   <li>{@link com.salesforce.graph.vertex.MethodCallExpressionVertex} - applies from the point
     *       that the method identified by the expression is invoked until the call returns,
     *       including any invocations of other methods.
     * </ul>
     */
    DISABLE_STACK(
            Token.SFGE_DISABLE + Token.DIRECTIVE_SEPARATOR + Token.STACK,
            NodeType.METHOD,
            NodeType.METHOD_CALL_EXPRESSION);

    /** first token in the string such as "sfge-disable" */
    private final String token;

    /** The list of vertex types that this directive can be applied to */
    private final Set<String> validVertexTypes;

    EngineDirectiveCommand(String token, String... vertexTypes) {
        this.token = token;
        this.validVertexTypes =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(vertexTypes)));
    }

    /** Allows case insensitive conversion from string to EngineDirectiveCommand */
    private static final TreeMap<String, EngineDirectiveCommand> TOKEN_MAP =
            EnumUtil.getEnumTreeMap(EngineDirectiveCommand.class, EngineDirectiveCommand::getToken);

    /**
     * Return the {@link EngineDirectiveCommand} whose {@link #token} is case-insensitively
     * equivalentl to {@code token}
     */
    public static Optional<EngineDirectiveCommand> fromString(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TOKEN_MAP.get(token));
    }

    public boolean isAnyDisable() {
        return token.startsWith(Token.SFGE_DISABLE);
    }

    public boolean isDisable() {
        return this == DISABLE;
    }

    public boolean isDisableNextLine() {
        return this == DISABLE_NEXT_LINE;
    }

    public boolean isDisableStack() {
        return this == DISABLE_STACK;
    }

    public String getToken() {
        return token;
    }

    public String toString() {
        return token;
    }

    /**
     * @return true if this directive is valid for the given label
     */
    public boolean isValidForLabel(String label) {
        return validVertexTypes.contains(label);
    }
}
