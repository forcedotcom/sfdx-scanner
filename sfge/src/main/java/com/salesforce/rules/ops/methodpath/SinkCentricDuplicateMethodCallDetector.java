package com.salesforce.rules.ops.methodpath;

import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects more than one invocation of a sink vertex through different paths. It ignores duplicate
 * methods that did not invoke the sink vertex in question.
 */
public abstract class SinkCentricDuplicateMethodCallDetector
        extends AbstractDuplicateMethodCallDetector {

    private static final Logger LOGGER =
            LogManager.getLogger(SinkCentricDuplicateMethodCallDetector.class);

    private final DefaultSymbolProviderVertexVisitor symbolVisitor;

    /** Sink vertex that requires multiple path invocation. */
    protected final SFVertex sinkVertex;

    /** Indicates if the sink vertex has been visited or not. */
    private boolean isSinkVisited = false;

    private final List<String> methodsInStackDuringSinkVisit;

    public SinkCentricDuplicateMethodCallDetector(
            DefaultSymbolProviderVertexVisitor symbolVisitor,
            MethodCallExpressionVertex sinkVertex) {
        this.symbolVisitor = symbolVisitor;
        this.sinkVertex = sinkVertex;
        this.methodsInStackDuringSinkVisit = new ArrayList<>();
    }

    @Override
    protected void performPreAction(InvocableVertex vertex) {
        if (vertex instanceof MethodCallExpressionVertex && sinkVertex.equals(vertex)) {
            isSinkVisited = true;
            // Note down all the methods in stack when sink was visited
            methodsInStackDuringSinkVisit.addAll(symbolVisitor.getMethodCallStack());
        }
    }

    @Override
    protected void performActionForDetectedDuplication(String key, InvocableVertex vertex) {
        boolean discardDuplicateMethod = false;

        if (!isSinkVisited) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Discarding method records since they did not include visiting the sink: "
                                + methodCallToInvocationOccurrence.get(key));
            }
            discardDuplicateMethod = true;
        } else if (!methodsInStackDuringSinkVisit.contains(key)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Discarding method that was not in the call-stack while visiting the sink: "
                                + methodCallToInvocationOccurrence.get(key));
            }
            discardDuplicateMethod = true;
        }

        if (discardDuplicateMethod) {
            methodCallToInvocationOccurrence.removeAll(key);
        }
    }
}
