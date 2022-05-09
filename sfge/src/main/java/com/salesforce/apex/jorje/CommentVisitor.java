package com.salesforce.apex.jorje;

import apex.jorje.data.Location;
import apex.jorje.parser.impl.HiddenToken;
import com.salesforce.graph.ops.directive.EngineDirective;
import java.util.Map;
import java.util.NavigableMap;

/** Visits all nodes in the tree looking for comments that correspond to {@link EngineDirective}s */
final class CommentVisitor extends JorjeNodeVisitor {
    private final NavigableMap<Integer, HiddenToken> hiddenTokenMap;

    /**
     * @param hiddenTokenMap that was accumulated the Apex was parsed. This is a map of line number
     *     to comment
     */
    CommentVisitor(NavigableMap<Integer, HiddenToken> hiddenTokenMap) {
        this.hiddenTokenMap = hiddenTokenMap;
    }

    @Override
    public void defaultVisit(AstNodeWrapper<?> wrapper) {
        processHiddenTokens(wrapper);
    }

    private void processHiddenTokens(JorjeNode jorjeNode) {
        final Location location = jorjeNode.getLocation();
        final int nodeStartIndex = location.getStartIndex();
        final Map.Entry<Integer, HiddenToken> commentEntry =
                hiddenTokenMap.lowerEntry(nodeStartIndex);
        if (commentEntry != null) {
            final HiddenToken hiddenToken = commentEntry.getValue();
            if (isSingleLineComment(hiddenToken)) {
                final int tokenLine = hiddenToken.getLocation().getLine();
                // The comment applies to the node if the node is on the line after the comment
                if (location.getLine() == tokenLine + 1) {
                    // Try to convert the HiddenToken to an EngineDirective
                    final HiddenTokenToEngineDirective hiddenTokenToEngineDirective =
                            new HiddenTokenToEngineDirective(jorjeNode);
                    hiddenToken.accept(hiddenTokenToEngineDirective);
                    hiddenTokenToEngineDirective
                            .getEngineDirectives()
                            .ifPresent(
                                    engineDirective -> {
                                        EngineDirectiveNode engineDirectiveNode =
                                                new EngineDirectiveNode(
                                                        engineDirective, hiddenToken, jorjeNode);
                                        jorjeNode.addChild(engineDirectiveNode);
                                    });
                }
            }
        }
    }

    /** @return true if the comment is on a single line */
    private static boolean isSingleLineComment(HiddenToken hiddenToken) {
        final String[] comments = hiddenToken.getValue().split(PositionInformation.END_LINE);
        return comments.length == 1;
    }
}
