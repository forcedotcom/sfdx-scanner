package com.salesforce.graph.ops.expander;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Immutable;
import com.salesforce.graph.ops.expander.switches.ApexPathCaseStatementExcluder;
import com.salesforce.graph.symbols.AbstractClassScope;
import com.salesforce.graph.symbols.ClassInstanceScope;
import com.salesforce.graph.symbols.ClassStaticScope;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.DeserializedClassInstanceScope;
import com.salesforce.graph.vertex.VertexPredicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Configuration used to specialize the behavior of {@link ApexPathExpander} */
public class ApexPathExpanderConfig implements Immutable<ApexPathExpanderConfig> {
    private final boolean expandMethodCalls;
    private final List<ApexDynamicPathCollapser> dynamicPathCollapsers;
    private final List<ApexReturnValuePathCollapser> returnValueCollapsers;
    private final List<ApexPathCaseStatementExcluder> caseStatementExcluders;
    private final List<ApexPathStandardConditionExcluder> conditionExcluders;
    private final List<ApexValueConstrainer> valueConstrainers;
    private final List<VertexPredicate> vertexPredicates;
    private final DefaultSymbolProviderVertexVisitor defaultSymbolProviderVertexVisitor;
    private final String deserializedClassName;
    private final String instanceClassName;
    private final String staticClassName;

    private ApexPathExpanderConfig(Builder builder) {
        this.expandMethodCalls = builder.shouldExpandMethodCalls;
        this.dynamicPathCollapsers = Collections.unmodifiableList(builder.dynamicCollapsers);
        this.returnValueCollapsers = Collections.unmodifiableList(builder.returnValueCollapsers);
        this.caseStatementExcluders = Collections.unmodifiableList(builder.caseStatementExcluders);
        this.conditionExcluders = Collections.unmodifiableList(builder.conditionExcluders);
        this.valueConstrainers = Collections.unmodifiableList(builder.valueConstrainers);
        this.vertexPredicates = Collections.unmodifiableList(builder.vertexPredicates);
        this.deserializedClassName = builder.deserializedClassName;
        this.instanceClassName = builder.instanceClassName;
        this.staticClassName = builder.staticClassName;
        this.defaultSymbolProviderVertexVisitor = builder.defaultSymbolProviderVertexVisitor;
    }

    public boolean getExpandMethodCalls() {
        return this.expandMethodCalls;
    }

    public List<ApexDynamicPathCollapser> getDynamicCollapsers() {
        return this.dynamicPathCollapsers;
    }

    public List<ApexReturnValuePathCollapser> getReturnValueCollapsers() {
        return this.returnValueCollapsers;
    }

    public List<ApexPathCaseStatementExcluder> getCaseStatementExcluders() {
        return this.caseStatementExcluders;
    }

    public List<ApexPathStandardConditionExcluder> getConditionExcluders() {
        return this.conditionExcluders;
    }

    public List<ApexValueConstrainer> getValueConstrainers() {
        return this.valueConstrainers;
    }

    public List<VertexPredicate> getVertexPredicates() {
        return vertexPredicates;
    }

    /**
     * Instantiates a new {@link AbstractClassScope} subclass that matches the class passed to a
     * previous invocation of {@link Builder#with(AbstractClassScope)}. Returns an empty if {@code
     * #with} was not invoked.
     */
    public Optional<? extends AbstractClassScope> instantiateClassScope(GraphTraversalSource g) {
        if (deserializedClassName != null) {
            return Optional.of(DeserializedClassInstanceScope.get(g, instanceClassName));
        } else if (instanceClassName != null) {
            return Optional.of(ClassInstanceScope.get(g, instanceClassName));
        } else if (staticClassName != null) {
            return Optional.of(ClassStaticScope.get(g, staticClassName));
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> getInstanceClassName() {
        return Optional.ofNullable(instanceClassName);
    }

    public Optional<String> getStaticClassName() {
        return Optional.ofNullable(staticClassName);
    }

    public Optional<DefaultSymbolProviderVertexVisitor> getDefaultSymbolProviderVertexVisitor() {
        return Optional.ofNullable(defaultSymbolProviderVertexVisitor);
    }

    public static final class Builder {
        private boolean shouldExpandMethodCalls;
        private final List<ApexDynamicPathCollapser> dynamicCollapsers;
        private final List<ApexReturnValuePathCollapser> returnValueCollapsers;
        private final List<ApexPathCaseStatementExcluder> caseStatementExcluders;
        private final List<ApexPathStandardConditionExcluder> conditionExcluders;
        private final List<ApexValueConstrainer> valueConstrainers;
        private final List<VertexPredicate> vertexPredicates;
        private String deserializedClassName;
        private String instanceClassName;
        private String staticClassName;
        private DefaultSymbolProviderVertexVisitor defaultSymbolProviderVertexVisitor;

        private Builder() {
            this.dynamicCollapsers = new ArrayList<>();
            this.returnValueCollapsers = new ArrayList<>();
            this.caseStatementExcluders = new ArrayList<>();
            this.conditionExcluders = new ArrayList<>();
            this.valueConstrainers = new ArrayList<>();
            this.vertexPredicates = new ArrayList<>();
        }

        public static Builder get() {
            return new Builder();
        }

        /** Method call expressions will be resolved to the paths that implement the method */
        public Builder expandMethodCalls(boolean shouldExpandMethodCalls) {
            this.shouldExpandMethodCalls = shouldExpandMethodCalls;
            return this;
        }

        /** Additively add interest in certain vertices */
        public Builder withVertexPredicate(VertexPredicate vertexPredicate) {
            vertexPredicates.add(vertexPredicate);
            return this;
        }

        /** Additively add an {@link ApexDynamicPathCollapser} */
        public Builder with(ApexDynamicPathCollapser collapser) {
            dynamicCollapsers.add(collapser);
            return this;
        }

        /** Additively add an {@link ApexReturnValuePathCollapser} */
        public Builder with(ApexReturnValuePathCollapser collapser) {
            returnValueCollapsers.add(collapser);
            return this;
        }

        /** Additively add an {@link ApexPathCaseStatementExcluder} */
        public Builder with(ApexPathCaseStatementExcluder excluder) {
            caseStatementExcluders.add(excluder);
            return this;
        }

        /** Additively add an {@link ApexPathStandardConditionExcluder} */
        public Builder with(ApexPathStandardConditionExcluder excluder) {
            conditionExcluders.add(excluder);
            return this;
        }

        /** Additively add an {@link ApexValueConstrainer} */
        public Builder with(ApexValueConstrainer valueConstrainer) {
            valueConstrainers.add(valueConstrainer);
            return this;
        }

        public Builder with(AbstractClassScope classScope) {
            if (this.instanceClassName != null || this.staticClassName != null) {
                throw new UnexpectedException("Should only be called once");
            }
            if (classScope instanceof ClassInstanceScope) {
                this.instanceClassName = classScope.getClassName();
            } else if (classScope instanceof DeserializedClassInstanceScope) {
                this.deserializedClassName = classScope.getClassName();
            } else if (classScope instanceof ClassStaticScope) {
                this.staticClassName = classScope.getClassName();
            } else {
                throw new UnexpectedException(classScope);
            }
            return this;
        }

        public Builder with(DefaultSymbolProviderVertexVisitor defaultSymbolProviderVertexVisitor) {
            if (this.defaultSymbolProviderVertexVisitor != null) {
                throw new UnexpectedException(this);
            }
            this.defaultSymbolProviderVertexVisitor = defaultSymbolProviderVertexVisitor;
            return this;
        }

        public ApexPathExpanderConfig build() {
            if (!shouldExpandMethodCalls) {
                if (!dynamicCollapsers.isEmpty() || !returnValueCollapsers.isEmpty()) {
                    throw new IllegalStateException(
                            "Collapsers have no effect if paths aren't expanded");
                }
            }
            return new ApexPathExpanderConfig(this);
        }
    }
}
