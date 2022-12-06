package com.salesforce.graph.visitor;

import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A visitor that will listen for method invocations and record the line number and value of
 * parameters at that point in time.
 *
 * <p>Example: new ApexValueAccumulator(Pair.of("System.debug", "s")); will listen for all
 * invocations of "System.debug" and recode the value of "s" at the line of invocation.
 *
 * @deprecated use SystemDebugAccumulator instead
 */
@Deprecated
public class ApexValueAccumulator extends AbstractAccumulator<ApexValue<?>> {
    /**
     * @param pairs of (MethodName, VariableName) to listen for.
     */
    public ApexValueAccumulator(Pair<String, String>... pairs) {
        super(pairs);
    }

    @Override
    protected Optional<ApexValue<?>> getValue(ChainedVertex parameter, SymbolProvider symbols) {
        VariableExpressionVertex variableExpressionVertex = (VariableExpressionVertex) parameter;
        String variableName = variableExpressionVertex.getName();
        return symbols.getApexValue(variableName);
    }
}
