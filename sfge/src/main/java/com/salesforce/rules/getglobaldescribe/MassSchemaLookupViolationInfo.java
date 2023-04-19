package com.salesforce.rules.getglobaldescribe;

import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.RuleThrowable;
import com.salesforce.rules.Violation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;

public class MassSchemaLookupViolationInfo implements RuleThrowable {
    /**
     * Type enum to indicate the category of GetGlobalDescribe performance degrade.
     */
    enum Type{
        INVOKED_IN_A_LOOP(UserFacingMessages.GetGlobalDescribeTemplates.INVOKED_IN_A_LOOP_TEXT, UserFacingMessages.GetGlobalDescribeTemplates.INVOKED_IN_A_LOOP_ADDITIONAL_INFO),
        INVOKED_MULTIPLE_TIMES(UserFacingMessages.GetGlobalDescribeTemplates.INVOKED_MULTIPLE_TIMES_TEXT, UserFacingMessages.GetGlobalDescribeTemplates.INVOKED_MULTIPLE_TIMES_ADDITIONAL_INFO);

        private String text;
        private String additionalInfoTemplate;

        Type(String text, String additionalInfoTemplate) {
            this.text = text;
            this.additionalInfoTemplate = additionalInfoTemplate;
        }

        public String getText() {
            return text;
        }

        public String getAdditionalInfoTemplate() {
            return additionalInfoTemplate;
        }
    }

    /**
     * Schema.getGlobalDescribe() operation
     * TODO: Is this always only a MethodCallExpression?
     **/
    private final MethodCallExpressionVertex sinkVertex;

    /** origin of the path leading to the GGD operation **/
    private SFVertex sourceVertex;

    /* Combination of type of issue and the vertex where this occurs */
    private final Set<Pair<Type, SFVertex>> occurrences;

    public MassSchemaLookupViolationInfo(SFVertex sourceVertex, MethodCallExpressionVertex sinkVertex) {
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;
        this.occurrences = new HashSet<>();
    }

    public void addOccurrence(Type type, SFVertex vertex) {
        occurrences.add(Pair.of(type, vertex));
    }


//    @Override
    public Violation convert() {
        return new Violation.PathBasedRuleViolation(this.getMessage(), sourceVertex, sinkVertex);
    }

    private String getMessage() {
        final String message = "Expensive operation %s invoked multiple times [%s]";
        return String.format(message,
            sinkVertex.getFullMethodName(),
            getOccurrences());
    }

    private String getOccurrences() {
        // TODO: get an approved format

        final StringBuilder occurrencesBuilder = new StringBuilder();
        for (Pair<Type, SFVertex> occurrence : occurrences) {
            occurrencesBuilder.append(occurrence.getLeft() + ": " + occurrence.getRight() + ",");
        }
        if (occurrencesBuilder.length() > 0) {
            return occurrencesBuilder.substring(0, occurrencesBuilder.length() - 1);
        }
        return "";
    }

}
