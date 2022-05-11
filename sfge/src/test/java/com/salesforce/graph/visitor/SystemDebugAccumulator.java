package com.salesforce.graph.visitor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;

/**
 * Captures all values passed to System.debug. Partitioned by Pair&lt;DefiningType, BeginLine&gt;
 *
 * <p>See {@link com.salesforce.TestRunner}
 */
public class SystemDebugAccumulator extends DefaultNoOpPathVertexVisitor {
    protected final ListMultimap<Pair<String, Integer>, Optional<ApexValue<?>>> results;

    public SystemDebugAccumulator() {
        // Use a LinkedHashMap so that the values are returned in the order they were added
        this.results = LinkedListMultimap.create();
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if ("System.debug".equalsIgnoreCase(vertex.getFullMethodName())) {
            ChainedVertex parameter = vertex.getParameters().get(0);
            Optional<ApexValue<?>> apexValue = ScopeUtil.resolveToApexValue(symbols, parameter);
            Pair<String, Integer> key = Pair.of(vertex.getDefiningType(), vertex.getBeginLine());
            results.put(key, apexValue);
        }
    }

    public List<Optional<ApexValue<?>>> getResults(String definingType, Integer lineNumber) {
        return new ArrayList(results.get(Pair.of(definingType, lineNumber)));
    }

    public List<Optional<ApexValue<?>>> getAllResults() {
        return new ArrayList(results.values());
    }

    public int resultSize() {
        return results.values().size();
    }

    /** Asserts that there is only a single Optional result and returns it */
    public Optional<ApexValue<?>> getOptionalSingletonResult() {
        MatcherAssert.assertThat(getAllResults(), hasSize(equalTo(1)));
        return getAllResults().get(0);
    }

    /**
     * Get the present result found at {@code index}. Equivalent to getAllResults().get(index).get()
     */
    public <T extends ApexValue<?>> T getResult(int index) {
        return (T) getAllResults().get(index).get();
    }

    /** Get the optional result found at {@code index}. Equivalent to getAllResults().get(index) */
    public <T extends ApexValue<?>> Optional<T> getOptionalResult(int index) {
        return (Optional<T>) getAllResults().get(index);
    }

    /** Asserts there is only one present result and returns it. */
    public <T extends ApexValue<?>> T getSingletonResult() {
        MatcherAssert.assertThat(getAllResults(), hasSize(equalTo(1)));
        return (T) getResult(0);
    }

    @Override
    public String toString() {
        return "SystemDebugAccumulator{" + "results=" + results + "}";
    }
}
