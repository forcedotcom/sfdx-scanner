package com.salesforce.graph.visitor;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;

public abstract class AbstractAccumulator<T> extends DefaultNoOpPathVertexVisitor {
    protected final TreeMap<String, Map<Integer, List<Optional<T>>>> results;
    protected final List<Pair<String, String>> methodVarNamesToAccumulate;

    public AbstractAccumulator(Pair<String, String>... pairs) {
        this.results = CollectionUtil.newTreeMap();
        this.methodVarNamesToAccumulate = new ArrayList<>(Arrays.asList(pairs));
    }

    protected abstract Optional<T> getValue(ChainedVertex parameter, SymbolProvider symbols);

    @Override
    public boolean visit(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        for (Pair<String, String> pair : methodVarNamesToAccumulate) {
            String methodName = pair.getLeft();
            String variableName = pair.getRight();
            if (methodName.equalsIgnoreCase(vertex.getFullMethodName())) {
                for (ChainedVertex parameter : vertex.getParameters()) {
                    VariableExpressionVertex variableExpressionVertex =
                            (VariableExpressionVertex) parameter;
                    if (variableExpressionVertex.getName().equalsIgnoreCase(variableName)) {
                        Optional<T> value = getValue(parameter, symbols);
                        Integer line = vertex.getBeginLine();
                        Map<Integer, List<Optional<T>>> variableResults =
                                results.computeIfAbsent(variableName, k -> new HashMap<>());
                        List<Optional<T>> results =
                                variableResults.computeIfAbsent(line, k -> new ArrayList<>());
                        results.add(value);
                    }
                }
            }
        }
        return false;
    }

    /**
     * A map of variable name to its value on a specific line when the method was called. This
     * implementation that the give line will only ever execute once.
     */
    public TreeMap<String, Map<Integer, Optional<T>>> getSingleResultPerLine() {
        TreeMap<String, Map<Integer, Optional<T>>> singletonResults = CollectionUtil.newTreeMap();

        for (Map.Entry<String, Map<Integer, List<Optional<T>>>> variableMap : results.entrySet()) {
            Map<Integer, List<Optional<T>>> lineMap = variableMap.getValue();
            Map<Integer, Optional<T>> singletonMap = new HashMap<>();
            for (Map.Entry<Integer, List<Optional<T>>> e : lineMap.entrySet()) {
                if (e.getValue().size() > 1) {
                    throw new UnexpectedException(
                            "Multiple results found on the same line, call getMultipleResultsPerLine");
                }
                singletonMap.put(e.getKey(), e.getValue().get(0));
            }
            singletonResults.put(variableMap.getKey(), singletonMap);
        }
        return singletonResults;
    }

    public Map<Integer, Optional<T>> getSingleResultPerLineByName(String name) {
        return getSingleResultPerLine().get(name);
    }

    /**
     * A map of variable name to its value on a specific line when the method was called. This
     * implementation that the give line will only ever execute once. A specific line can be
     * executed if you have two instances of the same class and call the same method on both
     * classes.
     */
    public TreeMap<String, Map<Integer, List<Optional<T>>>> getMultipleResultsPerLine() {
        return results;
    }

    public Map<Integer, List<Optional<T>>> getMultipleResultsPerLineByName(String name) {
        return getMultipleResultsPerLine().get(name);
    }
}
