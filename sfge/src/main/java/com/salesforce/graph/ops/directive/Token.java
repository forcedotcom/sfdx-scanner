package com.salesforce.graph.ops.directive;

/** Represents the various parts of an engine directive */
public final class Token {
    public static final String DIRECTIVE_SEPARATOR = "-";
    public static final String ARRAY_SEPARATOR = ",";
    public static final String COMMA = ",";
    public static final String COMMENT_SEPARATOR = "--";
    public static final String DASH = "-";
    public static final String DISABLE = "disable";
    public static final String ENGINE = "sfge";
    public static final String NEXT_LINE = "next-line";
    public static final String SFGE_DISABLE = ENGINE + DIRECTIVE_SEPARATOR + DISABLE;
    public static final String SPACE = " ";
    public static final String STACK = "stack";

    private Token() {}
}
