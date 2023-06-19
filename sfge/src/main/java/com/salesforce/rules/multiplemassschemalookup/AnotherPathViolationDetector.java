package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.TodoException;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.ops.methodpath.SinkCentricDuplicateMethodCallDetector;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects multiple invocations of expensive schema operations that happen through different path
 * invocations.
 */
public class AnotherPathViolationDetector extends SinkCentricDuplicateMethodCallDetector {

    private static final Logger LOGGER = LogManager.getLogger(AnotherPathViolationDetector.class);

    /** Represents the path entry point that this visitor is walking */
    private final SFVertex sourceVertex;

    /** Collects violation information */
    private final HashSet<MultipleMassSchemaLookupInfo> violations;

    public AnotherPathViolationDetector(
            DefaultSymbolProviderVertexVisitor symbolVisitor,
            SFVertex sourceVertex,
            MethodCallExpressionVertex sinkVertex) {
        super(symbolVisitor, sinkVertex);
        this.sourceVertex = sourceVertex;
        this.violations = new HashSet<>();
    }

    @Override
    protected boolean shouldIgnoreMethod(InvocableVertex vertex) {
        if (vertex instanceof MethodCallExpressionVertex) {
            // Ignore expensive method calls in this check
            return MmslrUtil.isSchemaExpensiveMethod((MethodCallExpressionVertex) vertex);
        }
        return false;
    }

    /**
     * @return Violations collected by the rule.
     */
    Set<MultipleMassSchemaLookupInfo> getViolations() {
        if (!(sinkVertex instanceof MethodCallExpressionVertex)) {
            throw new ProgrammingException(
                    "MultipleMassSchemaLookupRule does not have non-method call sinks. sinkVertex="
                            + sinkVertex);
        }
        final MethodCallExpressionVertex sinkMethodCallVertex =
                (MethodCallExpressionVertex) sinkVertex;

        for (String methodCallKey : methodCallToInvocationOccurrence.keys()) {
            Collection<InvocableVertex> invocableVertices =
                    methodCallToInvocationOccurrence.get(methodCallKey);
            if (invocableVertices.size() > 1) {
                for (InvocableVertex invocableVertex : invocableVertices) {
                    if (!(invocableVertex instanceof SFVertex)) {
                        throw new TodoException(
                                "Invocable vertex is not an instance of SFVertex. invocableVertex="
                                        + invocableVertex);
                    }
                    violations.add(
                            MmslrUtil.newViolation(
                                    sourceVertex,
                                    sinkMethodCallVertex,
                                    MmslrUtil.RepetitionType.ANOTHER_PATH,
                                    (SFVertex) invocableVertex));
                }
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Only one invocation of " + methodCallKey + " detected.");
                }
            }
        }
        return violations;
    }
}
