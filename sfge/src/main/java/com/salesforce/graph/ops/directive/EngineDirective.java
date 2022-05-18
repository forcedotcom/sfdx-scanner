package com.salesforce.graph.ops.directive;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.CloneUtil;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Combination of an {@link EngineDirectiveCommand} and the rule names that it applies to. An empty
 * set of rule names implies that it applies to all rules.
 */
public final class EngineDirective {
    private final EngineDirectiveCommand engineDirectiveCommand;
    /** Use a tree set to make this case insensitive */
    private final TreeSet<String> ruleNames;

    private final String comment;

    private final int hash;

    private EngineDirective(Builder builder) {
        this.engineDirectiveCommand = builder.engineDirectiveCommand;
        this.ruleNames = CloneUtil.cloneTreeSet(builder.ruleNames);
        this.comment = builder.comment;
        this.hash =
                Objects.hash(
                        this.engineDirectiveCommand,
                        this.ruleNames,
                        StringUtils.toRootLowerCase(this.comment));
    }

    public EngineDirectiveCommand getDirectiveToken() {
        return engineDirectiveCommand;
    }

    public boolean isAnyDisable() {
        return engineDirectiveCommand.isAnyDisable();
    }

    public boolean isDisableStack() {
        return engineDirectiveCommand.isDisableStack();
    }

    public Set<String> getRuleNames() {
        return Collections.unmodifiableSet(ruleNames);
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    /**
     * @return true if the rule name was specified in the directive, or no rule names were specified
     */
    public boolean matchesRule(String ruleName) {
        return ruleNames.isEmpty() || ruleNames.contains(ruleName);
    }

    /** #equals and #hashCode have been overridden to make the comment case-insensitive */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineDirective that = (EngineDirective) o;
        return engineDirectiveCommand == that.engineDirectiveCommand
                && Objects.equals(ruleNames, that.ruleNames)
                && StringUtils.equalsIgnoreCase(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "EngineDirective{"
                + "engineDirectiveCommand="
                + engineDirectiveCommand
                + ", ruleNames="
                + ruleNames
                + ", comment='"
                + comment
                + '\''
                + '}';
    }

    public static final class Builder {
        private final EngineDirectiveCommand engineDirectiveCommand;
        private final TreeSet<String> ruleNames;
        private String comment;

        private Builder(EngineDirectiveCommand engineDirectiveCommand) {
            this.engineDirectiveCommand = engineDirectiveCommand;
            this.ruleNames = CollectionUtil.newTreeSet();
        }

        public static Builder get(EngineDirectiveCommand engineDirectiveCommand) {
            return new Builder(engineDirectiveCommand);
        }

        public Builder withRuleNames(TreeSet<String> ruleNames) {
            if (!this.ruleNames.isEmpty()) {
                throw new UnexpectedException(this.ruleNames);
            }
            this.ruleNames.addAll(ruleNames);
            return this;
        }

        public Builder withRuleName(String ruleName) {
            if (!this.ruleNames.isEmpty()) {
                throw new UnexpectedException(this.ruleNames);
            }
            this.ruleNames.add(ruleName);
            return this;
        }

        public Builder withComment(String comment) {
            if (this.comment != null) {
                throw new UnexpectedException(this.comment);
            }
            this.comment = comment;
            return this;
        }

        public EngineDirective build() {
            return new EngineDirective(this);
        }
    }
}
