package com.salesforce.apex.jorje;

import apex.jorje.parser.impl.HiddenToken;
import apex.jorje.parser.impl.HiddenTokens;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveParser;
import java.util.Optional;

/**
 * Parses {@link HiddenTokens.BlockComment} and {@link HiddenTokens.InlineComment}s into {@link
 * EngineDirective}s
 */
final class HiddenTokenToEngineDirective implements HiddenToken.Visitor<Void> {
    private static final String COMMENT_START_INDICATOR = "/*";
    private static final String COMMENT_END_INDICATOR = "*/";
    private static final String SINGLE_LINE_COMMENT_INDICATOR = "//";
    /** The label of the JorjeNode */
    private final String label;

    private EngineDirective engineDirective;

    HiddenTokenToEngineDirective(JorjeNode jorjeNode) {
        this.label = jorjeNode.getLabel();
    }

    // TODO: Do we need to handle /**?
    @Override
    public Void visit(HiddenTokens.BlockComment blockComment) {
        String comment = blockComment.getValue().trim();
        if (!COMMENT_START_INDICATOR.equals(comment.substring(0, 2))) {
            throw new UnexpectedException(blockComment);
        }
        if (!COMMENT_END_INDICATOR.equals(
                comment.substring(comment.length() - 2, comment.length()))) {
            throw new UnexpectedException(blockComment);
        }
        // Trim the /* and */
        comment = comment.substring(2);
        comment = comment.substring(0, comment.length() - 2);
        engineDirective = EngineDirectiveParser.getEngineDirective(comment, label).orElse(null);
        return null;
    }

    @Override
    public Void visit(HiddenTokens.InlineComment inlineComment) {
        String comment = inlineComment.getValue().trim();
        // Trim the //
        if (!SINGLE_LINE_COMMENT_INDICATOR.equals(comment.substring(0, 2))) {
            throw new UnexpectedException(inlineComment);
        }
        comment = comment.substring(2);
        engineDirective = EngineDirectiveParser.getEngineDirective(comment, label).orElse(null);
        return null;
    }

    public Optional<EngineDirective> getEngineDirectives() {
        return Optional.ofNullable(engineDirective);
    }
}
