package com.salesforce.rules.fls.apex.operations;

import com.google.common.base.Objects;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.vertex.SFVertex;
import com.salesforce.rules.AbstractRule;
import com.salesforce.rules.RuleThrowable;
import com.salesforce.rules.Violation;

import java.util.Optional;
import java.util.TreeSet;

/** POJO that represents an FLS violation. Created by FLS rule when it detects a violation. */
public class FlsViolationInfo extends ObjectFieldInfo<FlsViolationInfo> implements RuleThrowable {
    // read/write operation
    private SFVertex sinkVertex;
    // origin of the path leading to the read/write operation
    private SFVertex sourceVertex;
    // Rule that detected this FLS violation. TODO: this may be redundant since we have only one FLS
    // rule right now
    private AbstractRule rule;
    // validation type that doesn't have the necessary check
    private final FlsConstants.FlsValidationType validationType;

    /**
     * Create new instance of {@link FlsViolationInfo}. vertex param is usually not available in
     * hand when this instance is created. Include it separately using {@link
     * FlsViolationInfo#setSinkVertex(SFVertex)}
     */
    public FlsViolationInfo(
            FlsConstants.FlsValidationType validationType,
            String objectName,
            TreeSet<String> fields,
            boolean isAllFields) {
        super(objectName, fields, isAllFields);

        this.validationType = validationType;
    }

    FlsViolationInfo(FlsViolationInfo other) {
        super(other);
        this.validationType = other.validationType;
        this.sinkVertex = other.sinkVertex;
        this.sourceVertex = other.sourceVertex;
        this.rule = other.rule;
    }

    @Override
    protected boolean _canMerge(FlsViolationInfo other) {
        // Validation types should match
        return this.validationType.equals(other.validationType)
                &&
                // If present, Sink vertex on violation should match
                (this.sinkVertex != null ? this.sinkVertex.equals(other.sinkVertex) : true)
                &&
                // If present, Source vertex on violation should match
                (this.sourceVertex != null ? this.sourceVertex.equals(other.sourceVertex) : true)
                &&
                // If present, Rule on violation should match
                // TODO: is this an overkill? What if we have other types of FLS rules that reuse
                // this?
                (this.rule != null ? this.rule.equals(other.rule) : true);
    }

    @Override
    protected FlsViolationInfo _merge(FlsViolationInfo other) {
        // If we are here, we have already confirmed that a merge is possible
        final FlsViolationInfo returnValue =
                new FlsViolationInfo(
                        this.validationType,
                        this.objectName,
                        CollectionUtil.newTreeSetOf(this.fields, other.fields),
                        this.allFields
                                || other.allFields // If one of them is allFields, the check needs
                        // to be on all fields
                        );
        returnValue.setSinkVertex(this.sinkVertex);
        returnValue.setSourceVertex(this.sourceVertex);
        returnValue.setRule(this.rule);
        return returnValue;
    }

    public Optional<SFVertex> getSinkVertex() {
        return Optional.ofNullable(sinkVertex);
    }

    public Optional<SFVertex> getSourceVertex() {
        return Optional.ofNullable(sourceVertex);
    }

    public FlsConstants.FlsValidationType getValidationType() {
        return validationType;
    }

    public Optional<AbstractRule> getRule() {
        return Optional.ofNullable(this.rule);
    }

    /**
     * Set sink vertex separately, since it is available only after an instance of {@link
     * FlsViolationInfo} has been created
     *
     * @param sinkVertex
     */
    public void setSinkVertex(SFVertex sinkVertex) {
        this.sinkVertex = sinkVertex;
    }

    /**
     * Set source vertex separately, since it is available only after an instance of {@link
     * FlsViolationInfo} has been created
     *
     * @param sourceVertex
     */
    public void setSourceVertex(SFVertex sourceVertex) {
        this.sourceVertex = sourceVertex;
    }

    public void setRule(AbstractRule rule) {
        this.rule = rule;
    }

    public String getMessageTemplate() {
        return UserFacingMessages.RuleViolationTemplates.MISSING_CRUD_FLS_CHECK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlsViolationInfo)) return false;
        FlsViolationInfo that = (FlsViolationInfo) o;
        return allFields == that.allFields
                && Objects.equal(sinkVertex, that.sinkVertex)
                && Objects.equal(sourceVertex, that.sourceVertex)
                && validationType == that.validationType
                && Objects.equal(objectName, that.objectName)
                && Objects.equal(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                sinkVertex, sourceVertex, validationType, objectName, fields, allFields);
    }

    @Override
    public String toString() {
        return "FlsViolationInfo{"
                + "sinkVertex="
                + sinkVertex
                + ", sourceVertex="
                + sourceVertex
                + ", rule="
                + rule
                + ", validationType="
                + validationType
                + ", objectName='"
                + objectName
                + '\''
                + ", fields="
                + fields
                + ", isAllFields="
                + allFields
                + '}';
    }

    @Override
    public FlsViolationInfo deepClone() {
        return new FlsViolationInfo(this);
    }

//    @Override
    public Violation convert() {
        final String violationMessage =
            FlsViolationMessageUtil.constructMessage(this);
        final Optional<SFVertex> sourceVertex = this.getSourceVertex();
        final Optional<SFVertex> sinkVertex = this.getSinkVertex();
        if (sinkVertex.isPresent()) {
            final Violation.RuleViolation ruleViolation =
                new Violation.PathBasedRuleViolation(
                    violationMessage, sourceVertex.get(), sinkVertex.get());
            ruleViolation.setPropertiesFromRule(rule);
            return ruleViolation;
        } else {
            throw new ProgrammingException(
                "Sink vertex not set in flsViolationInfo: "
                    + this);
        }
    }
}
