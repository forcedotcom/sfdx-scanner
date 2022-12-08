package com.salesforce.rules.unusedmethod;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class RuleStateTracker {
    private final GraphTraversalSource g;
    /**
     * The set of methods on which analysis was performed. Helps us know whether a method returned
     * no violations because it was inspected and an invocation was found, or if it was simply never
     * inspected in the first place.
     */
    private final Set<MethodVertex> eligibleMethods;
    /**
     * The set of methods for which no invocation was found. At the end of execution, we'll generate
     * violations from each method in this set.
     */
    private final Set<MethodVertex> unusedMethods;
    /**
     * A map used to cache every method call expression in a given class. Minimizing redundant
     * queries is a very high priority for this rule.
     */
    private final Map<String, List<InvocableWithParametersVertex>> methodCallsByDefiningType;
    /**
     * A map used to cache every subclass of a given class. Minimizing redundant queries is a very
     * high priority for this rule.
     */
    private final Map<String, List<String>> subclassesByDefiningType;

    public RuleStateTracker(GraphTraversalSource g) {
        this.g = g;
        this.eligibleMethods = new HashSet<>();
        this.unusedMethods = new HashSet<>();
        this.methodCallsByDefiningType = CollectionUtil.newTreeMap();
        this.subclassesByDefiningType = CollectionUtil.newTreeMap();
    }

    /**
     * Mark the provided method vertex as a candidate for rule analysis.
     */
    public void trackEligibleMethod(MethodVertex methodVertex) {
        eligibleMethods.add(methodVertex);
    }

    /**
     * Mark the provided method as unused.
     */
    public void trackUnusedMethod(MethodVertex methodVertex) {
        unusedMethods.add(methodVertex);
    }

    /**
     * Get all of the methods that were found to be unused.
     */
    public Set<MethodVertex> getUnusedMethods() {
        return unusedMethods;
    }

    /**
     * Get the total number of methods deemed eligible for analysis.
     */
    public int getEligibleMethodCount() {
        return eligibleMethods.size();
    }

    /** Return a list of every method call occurring in the specified class. */
    List<InvocableWithParametersVertex> getMethodCallsByDefiningType(String definingType) {
        // First, check if we've already got anything for the desired type.
        // If so, we can just return that.
        if (this.methodCallsByDefiningType.containsKey(definingType)) {
            return this.methodCallsByDefiningType.get(definingType);
        }
        // Otherwise, we need to do a query.
        // Any node with one of these labels is a method call.
        List<String> targetLabels =
                Arrays.asList(
                        NodeType.METHOD_CALL_EXPRESSION,
                        NodeType.THIS_METHOD_CALL_EXPRESSION,
                        NodeType.SUPER_METHOD_CALL_EXPRESSION);
        List<InvocableWithParametersVertex> methodCalls =
                SFVertexFactory.loadVertices(
                        g, g.V().where(H.has(targetLabels, Schema.DEFINING_TYPE, definingType)));
        // Cache and return the results.
        this.methodCallsByDefiningType.put(definingType, methodCalls);
        return methodCalls;
    }

    /** Get all immediate subclasses of the provided classes. */
    List<String> getSubclasses(String... definingTypes) {
        List<String> results = new ArrayList<>();
        // For each type we were given...
        for (String definingType : definingTypes) {
            // If we've already got results for that type, we can just add those results to the
            // overall list.
            if (this.subclassesByDefiningType.containsKey(definingType)) {
                results.addAll(this.subclassesByDefiningType.get(definingType));
                continue;
            }
            // Otherwise, we need to do some querying.
            List<UserClassVertex> subclassVertices =
                    SFVertexFactory.loadVertices(
                            g,
                            g.V()
                                    .where(
                                            H.has(
                                                    NodeType.USER_CLASS,
                                                    Schema.DEFINING_TYPE,
                                                    definingType))
                                    .out(Schema.EXTENDED_BY));
            List<String> subclassNames =
                    subclassVertices.stream()
                            .map(UserClassVertex::getName)
                            .collect(Collectors.toList());
            this.subclassesByDefiningType.put(definingType, subclassNames);
            results.addAll(subclassNames);
        }
        return results;
    }
}
