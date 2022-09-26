package com.salesforce.graph.ops.expander.switches;

import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.graph.ops.expander.ApexPathExcluder;
import com.salesforce.graph.ops.expander.PathExcludedException;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexCustomValue;
import com.salesforce.graph.symbols.apex.ApexEnumValue;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexIntegerValue;
import com.salesforce.graph.symbols.apex.ApexLongValue;
import com.salesforce.graph.symbols.apex.ApexSimpleValue;
import com.salesforce.graph.symbols.apex.ApexSingleValue;
import com.salesforce.graph.symbols.apex.ApexStringValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ApexValueVisitor;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.WhenBlockVertex;

/**
 * Switch statements cause N path forks where N is the number of "when" + "when else" statements in
 * the switch. Paths can be excluded if the expression is determinant and the path goes through a
 * "when/when else" which doesn't cover the expression.
 *
 * <p>This class does not have an interface, because it is not foreseen that more than one
 * implementation will be required
 */
@SuppressWarnings(
        "PMD.PreserveStackTrace") // Stacktrace is intentionally modified for new exception while
// handling case statements
public final class ApexPathCaseStatementExcluder implements ApexPathExcluder {
    public void exclude(WhenBlockVertex vertex, SymbolProvider symbols)
            throws PathExcludedException {
        final ChainedVertex switchExpression = vertex.getExpressionVertex();
        final ApexValue<?> apexValue =
                ScopeUtil.resolveToApexValue(symbols, switchExpression).orElse(null);
        if (apexValue != null && apexValue.isDeterminant()) {
            ApexValueVisitor<Void> visitor =
                    new ApexValueVisitor.DefaultThrow<Void>() {
                        // Switch using enums
                        @Override
                        public Void visit(ApexEnumValue value) {
                            CaseVertexExcluder excluder = new CaseVertexExcluder(value);
                            vertex.accept(excluder);
                            return null;
                        }

                        // Switch inside a for loop
                        @Override
                        public Void visit(ApexForLoopValue value) {
                            // We don't know the actual value, we can't exclude any paths
                            return null;
                        }

                        // Switch using sObjects
                        @Override
                        public Void visit(ApexSingleValue value) {
                            SObjectWhenBlockExcluder excluder = new SObjectWhenBlockExcluder(value);
                            vertex.accept(excluder);
                            return null;
                        }

                        @Override
                        public Void visit(ApexCustomValue value) {
                            SObjectWhenBlockExcluder excluder = new SObjectWhenBlockExcluder(value);
                            vertex.accept(excluder);
                            return null;
                        }

                        // Switch using literals
                        @Override
                        public Void visit(ApexIntegerValue value) {
                            return visitSimpleValue(value);
                        }

                        @Override
                        public Void visit(ApexLongValue value) {
                            return visitSimpleValue(value);
                        }

                        @Override
                        public Void visit(ApexStringValue value) {
                            return visitSimpleValue(value);
                        }

                        private Void visitSimpleValue(ApexSimpleValue<?, ?> value) {
                            CaseVertexExcluder excluder = new CaseVertexExcluder(value);
                            vertex.accept(excluder);
                            return null;
                        }
                    };
            try {
                // The visitor will throw a CaseStatementExcludedException if the case identified by
                // vertex doesn't make
                // sense for the apexValue
                apexValue.accept(visitor);
            } catch (CaseStatementExcludedException caseStatementExcludedException) {
                throw new PathExcludedException(this, vertex, vertex.getExpressionVertex());
            }
        }
    }

    public static ApexPathCaseStatementExcluder getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use
        private static final ApexPathCaseStatementExcluder INSTANCE =
                new ApexPathCaseStatementExcluder();
    }

    private ApexPathCaseStatementExcluder() {}

    /** Thrown by ExcludeCaseStatementVertexVisitor if the when block should be excluded */
    static final class CaseStatementExcludedException extends SfgeRuntimeException {
        CaseStatementExcludedException() {
            super();
        }
    }
}
