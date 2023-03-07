package com.salesforce.rules.unusedmethod;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.vertex.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * A helper class for {@link com.salesforce.rules.UnusedMethodRule}, which tracks various elements
 * of state as the rule executes.
 */
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
     * A Set used to track every DefiningType for which we've cached values. Minimizing redundant
     * queries is a very high priority for this rule.
     */
    private final Set<String> cachedDefiningTypes;
    /**
     * A map used to cache every {@link MethodCallExpressionVertex} in a given class. Minimizing
     * redundant queries is a very high priority for this rule. <br>
     * Note: Expressions of this type represent invocations of non-constructor methods.
     */
    private final Map<String, List<MethodCallExpressionVertex>> methodCallExpressionsByDefiningType;
    /**
     * A map used to cache every {@link ThisMethodCallExpressionVertex} in a given class. Minimizing
     * redundant queries is a very high priority for this rule. <br>
     * Note: Expressions of this type represent invocations of the {@code this()} constructor
     * pattern.
     */
    private final Map<String, List<ThisMethodCallExpressionVertex>>
            thisMethodCallExpressionsByDefiningType;
    /**
     * A map used to cache every {@link SuperMethodCallExpressionVertex} in a given class.
     * Minimizing redundant queries is a very high priority for this rule. <br>
     * Note: Expressions of this type represent invocations of the {@code super()} constructor
     * pattern.
     */
    private final Map<String, List<SuperMethodCallExpressionVertex>>
            superMethodCallExpressionsByDefiningType;
    /**
     * A map used to cache every subclass of a given class. Minimizing redundant queries is a very
     * high priority for this rule.
     */
    private final Map<String, List<String>> subclassesByDefiningType;

    public RuleStateTracker(GraphTraversalSource g) {
        this.g = g;
        this.eligibleMethods = new HashSet<>();
        this.unusedMethods = new HashSet<>();
        this.cachedDefiningTypes = new HashSet<>();
        this.methodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.thisMethodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.superMethodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.subclassesByDefiningType = CollectionUtil.newTreeMap();
    }

    /** Mark the provided method vertex as a candidate for rule analysis. */
    public void trackEligibleMethod(MethodVertex methodVertex) {
        eligibleMethods.add(methodVertex);
    }

    /** Mark the provided method as unused. */
    public void trackUnusedMethod(MethodVertex methodVertex) {
        unusedMethods.add(methodVertex);
    }

    /** Get all of the methods that were found to be unused. */
    public Set<MethodVertex> getUnusedMethods() {
        return unusedMethods;
    }

    /** Get the total number of methods deemed eligible for analysis. */
    public int getEligibleMethodCount() {
        return eligibleMethods.size();
    }

    /**
     * Get every {@link MethodCallExpressionVertex} occurring in the class represented by {@code
     * definingType}.
     *
     * @param definingType
     * @return
     */
    List<MethodCallExpressionVertex> getMethodCallExpressionsByDefiningType(String definingType) {
        populateCachesForDefiningType(definingType);
        return this.methodCallExpressionsByDefiningType.get(definingType);
    }

    /**
     * Get every {@link ThisMethodCallExpressionVertex} occurring in the class represented by {@code
     * definingType}.
     *
     * @param definingType
     * @return
     */
    List<ThisMethodCallExpressionVertex> getThisMethodCallExpressionsByDefiningType(
            String definingType) {
        populateCachesForDefiningType(definingType);
        return this.thisMethodCallExpressionsByDefiningType.get(definingType);
    }

    /**
     * Get every {@link SuperMethodCallExpressionVertex} occurring in the class represented by
     * {@code definingType}.
     *
     * @param definingType
     * @return
     */
    List<SuperMethodCallExpressionVertex> getSuperMethodCallExpressionsByDefiningType(
            String definingType) {
        populateCachesForDefiningType(definingType);
        return this.superMethodCallExpressionsByDefiningType.get(definingType);
    }

    /**
     * Populate the various method call caches for the class represented by {@code definingType}.
     *
     * @param definingType
     */
    private void populateCachesForDefiningType(String definingType) {
        // If we've already populated the caches for this defining type, there's nothing to do.
        if (this.cachedDefiningTypes.contains(definingType)) {
            return;
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
        // Sort the results by type and cache them appropriately.
        List<MethodCallExpressionVertex> methodCallExpressions = new ArrayList<>();
        List<ThisMethodCallExpressionVertex> thisMethodCallExpressions = new ArrayList<>();
        List<SuperMethodCallExpressionVertex> superMethodCallExpressions = new ArrayList<>();
        for (InvocableWithParametersVertex invocable : methodCalls) {
            if (invocable instanceof MethodCallExpressionVertex) {
                methodCallExpressions.add((MethodCallExpressionVertex) invocable);
            } else if (invocable instanceof ThisMethodCallExpressionVertex) {
                thisMethodCallExpressions.add((ThisMethodCallExpressionVertex) invocable);
            } else if (invocable instanceof SuperMethodCallExpressionVertex) {
                superMethodCallExpressions.add((SuperMethodCallExpressionVertex) invocable);
            } else {
                throw new UnexpectedException(
                        "Unexpected InvocableWithParametersVertex implementation "
                                + invocable.getClass());
            }
        }
        methodCallExpressionsByDefiningType.put(definingType, methodCallExpressions);
        thisMethodCallExpressionsByDefiningType.put(definingType, thisMethodCallExpressions);
        superMethodCallExpressionsByDefiningType.put(definingType, superMethodCallExpressions);
        cachedDefiningTypes.add(definingType);
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
