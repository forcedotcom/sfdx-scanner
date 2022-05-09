package com.salesforce.graph.ops.directive;

import com.salesforce.collections.CollectionUtil;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts strings to {@link EngineDirective}s. These strings currently come from Apex comments,
 * but could also come from a configuration file in the future.
 */
public class EngineDirectiveParser {
    /** Finds space-comma, comma-space combinations. Used to condense arrays. */
    private static final Pattern COMMA_WITH_SPACES = Pattern.compile("\\s*,\\s*");

    /**
     * Convert the string to an {@link EngineDirective} if possible
     *
     * @param valueParam with the comment delimiters stripped
     * @param label label of the vertex that the comment precedes. {@link EngineDirectiveCommand}
     *     instances can only be associated with specific types of vertices.
     * @return an EngineDirective if one can be found, else empty
     */
    public static Optional<EngineDirective> getEngineDirective(String valueParam, String label) {
        final String value = normalizeValue(valueParam);
        final TreeSet<String> ruleNames = CollectionUtil.newTreeSet();
        EngineDirective firstEngineDirective = null;

        final int commentSeparatorIndex = value.indexOf(Token.COMMENT_SEPARATOR);
        final String directive;
        final String comment;
        if (commentSeparatorIndex == -1) {
            directive = value;
            comment = null;
        } else {
            directive = normalizeValue(value.substring(0, commentSeparatorIndex));
            // The comment can have any number of "-" characters after the first two. Iteratively
            // remove them
            String rawComment =
                    value.substring(commentSeparatorIndex + Token.COMMENT_SEPARATOR.length());
            while (rawComment.startsWith(Token.DASH)) {
                rawComment = rawComment.substring(1);
            }
            comment = normalizeValue(rawComment);
        }
        final String[] arrayElements = directive.split(Token.ARRAY_SEPARATOR);
        for (int i = 0; i < arrayElements.length; i++) {
            final String arrayElement = arrayElements[i];
            if (i == 0) {
                firstEngineDirective = parseFirstArrayElement(arrayElement, label).orElse(null);
                if (firstEngineDirective == null) {
                    return Optional.empty();
                }
                ruleNames.addAll(firstEngineDirective.getRuleNames());
            } else {
                ruleNames.add(arrayElement);
            }
        }
        return Optional.of(
                EngineDirective.Builder.get(firstEngineDirective.getDirectiveToken())
                        .withRuleNames(ruleNames)
                        .withComment(comment)
                        .build());
    }

    /**
     * Parses the first element of a directive array into the a EngineDirective. "sfge-disable
     * Rule1, Rule2" returns an EngineDirective with directiveToken = "sfge-disable", RuleNames =
     * {"Rule1"}
     */
    private static Optional<EngineDirective> parseFirstArrayElement(String value, String label) {
        final String[] parameters = value.split(Token.SPACE);
        if (parameters.length <= 2) {
            final String directive = parameters[0];
            final EngineDirectiveCommand directiveToken =
                    EngineDirectiveCommand.fromString(directive).orElse(null);
            if (directiveToken != null && directiveToken.isValidForLabel(label)) {
                EngineDirective.Builder builder = EngineDirective.Builder.get(directiveToken);
                if (parameters.length == 2) {
                    builder.withRuleName(parameters[1]);
                }
                return Optional.of(builder.build());
            }
        }
        return Optional.empty();
    }

    /**
     * Replaces tabs with spaces, consolidates multiple continuous spaces to a single space,
     * replaces ", " with "," and " ," with ","
     */
    static String normalizeValue(String rawValueParam) {
        String rawValue = rawValueParam.trim();
        rawValue = StringUtils.normalizeSpace(rawValue);
        rawValue = COMMA_WITH_SPACES.matcher(rawValue).replaceAll(Token.COMMA);
        return rawValue;
    }

    private EngineDirectiveParser() {}
}
