package com.salesforce.graph.ops;

import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import java.util.Optional;

public final class LiteralUtil {
    public static Optional<String> toString(ChainedVertex vertex) {
        String result = null;

        if (vertex instanceof LiteralExpressionVertex.SFString) {
            result = ((LiteralExpressionVertex.SFString) vertex).getLiteral();
        }

        return Optional.ofNullable(result);
    }

    private LiteralUtil() {}
}
