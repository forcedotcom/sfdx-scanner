package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.TodoException;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.RuleThrowable;
import com.salesforce.rules.Violation;

/**
 * Represents information required to create a violation from {@link
 * com.salesforce.rules.MultipleMassSchemaLookupRule}.
 */
public class MultipleMassSchemaLookupInfo implements RuleThrowable {

    /**
     * vertex where the expensive Schema lookup happens TODO: Is this always only a
     * MethodCallExpression?
     */
    private final MethodCallExpressionVertex sinkVertex;

    /** origin of the path leading to the GGD operation * */
    private final SFVertex sourceVertex;

    /** Type of repetition that happens between source and sink * */
    private final MmslrUtil.RepetitionType repetitionType;

    /** Vertex where the repetition occurs */
    private final SFVertex repetitionVertex;

    public MultipleMassSchemaLookupInfo(
            SFVertex sourceVertex,
            MethodCallExpressionVertex sinkVertex,
            MmslrUtil.RepetitionType repetitionType,
            SFVertex repetitionVertex) {
        validateInput(repetitionType, repetitionVertex);
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.repetitionType = repetitionType;
        this.repetitionVertex = repetitionVertex;
    }

    private void validateInput(MmslrUtil.RepetitionType repetitionType, SFVertex repetitionVertex) {
        if (repetitionType == null) {
            throw new ProgrammingException("repetitionType cannot be null.");
        }

        if (repetitionVertex == null) {
            throw new ProgrammingException("repetitionVertex cannot be null.");
        }

        if (MmslrUtil.RepetitionType.MULTIPLE.equals(repetitionType)) {
            if (!(repetitionVertex instanceof MethodCallExpressionVertex)) {
                throw new ProgrammingException(
                        "Repetition of type MULTIPLE can only happen on MethodCallExpressions. repetitionVertex="
                                + repetitionVertex);
            }
        }

        if (MmslrUtil.RepetitionType.ANOTHER_PATH.equals(repetitionType)) {
            if (!(repetitionVertex instanceof MethodCallExpressionVertex
                    || repetitionVertex instanceof NewObjectExpressionVertex)) {
                throw new TodoException(
                        "Repetition of type ANOTHER_PATH not handled. repetitionVertex="
                                + repetitionVertex);
            }
        }
    }

    public Violation.PathBasedRuleViolation convert() {
        return new Violation.PathBasedRuleViolation(
                MassSchemaLookupInfoUtil.getMessage(this), sourceVertex, sinkVertex);
    }

    public MethodCallExpressionVertex getSinkVertex() {
        return sinkVertex;
    }

    public SFVertex getSourceVertex() {
        return sourceVertex;
    }

    public MmslrUtil.RepetitionType getRepetitionType() {
        return repetitionType;
    }

    public SFVertex getRepetitionVertex() {
        return repetitionVertex;
    }
}
