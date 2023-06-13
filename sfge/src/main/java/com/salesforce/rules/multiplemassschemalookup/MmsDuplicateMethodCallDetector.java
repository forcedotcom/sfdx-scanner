package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.exception.TodoException;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.ops.methodpath.AbstractDuplicateMethodCallDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MmsDuplicateMethodCallDetector extends AbstractDuplicateMethodCallDetector {

    private static final Logger LOGGER = LogManager.getLogger(MmsDuplicateMethodCallDetector.class);

    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Represents the mass schema lookup vertex that we're looking earlier calls for. */
    private final MethodCallExpressionVertex sinkVertex;

    /** Collects violation information */
    private final HashSet<MultipleMassSchemaLookupInfo> violations;

//    /** Indicates if the sink vertex has been visited or not. */
//    private boolean isSinkVisited = false;

    public MmsDuplicateMethodCallDetector(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.violations = new HashSet<>();
    }

    @Override
    protected boolean shouldIgnoreMethod(InvocableVertex vertex) {
        if (vertex instanceof MethodCallExpressionVertex) {
            // Ignore expensive method calls in this check
            return MmslrUtil.isSchemaExpensiveMethod((MethodCallExpressionVertex) vertex);
        }
        return !shouldContinue();
    }

    @Override
    protected void performActionForDetectedDuplication(InvocableVertex vertex) {
//        if (!(vertex instanceof SFVertex)) {
//            throw new TodoException("Invocable vertex is not an instance of SFVertex. vertex=" + vertex);
//        }
//        violations.add(MmslrUtil.newViolation(sourceVertex, sinkVertex, MmslrUtil.RepetitionType.ANOTHER_PATH, (SFVertex) vertex));
    }

    /**
     * @return Violations collected by the rule.
     */
    Set<MultipleMassSchemaLookupInfo> getViolations() {
        for (String methodCallKey: methodCallToInvocationOccurrence.keys()) {
            Collection<InvocableVertex> invocableVertices = methodCallToInvocationOccurrence.get(methodCallKey);
            if (invocableVertices.size() > 1) {
                for (InvocableVertex invocableVertex: invocableVertices) {
                    if (!(invocableVertex instanceof SFVertex)) {
                        throw new TodoException("Invocable vertex is not an instance of SFVertex. invocableVertex=" + invocableVertex);
                    }
                    violations.add(MmslrUtil.newViolation(sourceVertex, sinkVertex, MmslrUtil.RepetitionType.ANOTHER_PATH, (SFVertex) invocableVertex));
                }
            } else {
                LOGGER.warn("Only one invocation of " + methodCallKey + " detected.");
            }
        }
        return violations;
    }

    /**
     * Decides whether the rule should continue collecting violations
     *
     * @return true if the rule visitor should continue.
     */
    private boolean shouldContinue() {
//        return !isSinkVisited;
//        return violations.isEmpty();
        return true;
    }
}
