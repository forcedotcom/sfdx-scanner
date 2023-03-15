package com.salesforce.rules.unusedmethod;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil.H;
import com.salesforce.graph.ops.ApexClassUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.ops.TraversalUtil;
import com.salesforce.graph.vertex.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

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
    /**
     * A map used to cache every inner class of a given class. Minimizing redundant queries is a
     * very high priority for this rule.
     */
    private final Map<String, List<UserClassVertex>> innerClassesByDefiningType;

    public RuleStateTracker(GraphTraversalSource g) {
        this.g = g;
        this.eligibleMethods = new HashSet<>();
        this.unusedMethods = new HashSet<>();
        this.cachedDefiningTypes = new HashSet<>();
        this.methodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.thisMethodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.superMethodCallExpressionsByDefiningType = CollectionUtil.newTreeMap();
        this.subclassesByDefiningType = CollectionUtil.newTreeMap();
        this.innerClassesByDefiningType = CollectionUtil.newTreeMap();
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
        populateMethodCallCachesForDefiningType(definingType);
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
        populateMethodCallCachesForDefiningType(definingType);
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
        populateMethodCallCachesForDefiningType(definingType);
        return this.superMethodCallExpressionsByDefiningType.get(definingType);
    }

    /**
     * Populate the various method call caches for the class represented by {@code definingType}. Do
     * all of them in the same method because it's exceedingly likely that we'll need all of them at
     * one point or another.
     *
     * @param definingType
     */
    private void populateMethodCallCachesForDefiningType(String definingType) {
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
                throw new TodoException(
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
                            .map(UserClassVertex::getDefiningType)
                            .collect(Collectors.toList());
            this.subclassesByDefiningType.put(definingType, subclassNames);
            results.addAll(subclassNames);
        }
        return results;
    }

    /** Get any inner classes that reside in {@code definingType}. */
    List<UserClassVertex> getInnerClasses(String definingType) {
        if (!this.innerClassesByDefiningType.containsKey(definingType)) {
            this.innerClassesByDefiningType.put(
                    definingType, ClassUtil.getInnerClassesOf(g, definingType));
        }
        return this.innerClassesByDefiningType.get(definingType);
    }

    /**
     * Indicates whether the specified class has/inherits a method with the specified signature
     *
     * @param definingType - A class name
     * @param signature - The signature of a method
     * @return True if class has method, else false
     */
    boolean classInheritsMatchingMethod(String definingType, String signature) {
        return MethodUtil.getMethodWithSignature(g, definingType, signature, true).isPresent();
    }

    /**
     * Get all {@link MethodCallExpressionVertex} instances representing invocations of a method
     * named {@code methodName} on a thing called {@code referencedType}.
     */
    List<MethodCallExpressionVertex> getInvocationsOnType(
            String referencedType, String methodName) {
        // Start off with all MethodCallExpressionVertex instances whose contained
        // ReferenceExpression matches the referenced type.
        List<MethodCallExpressionVertex> results =
                SFVertexFactory.loadVertices(
                        g,
                        TraversalUtil.traverseInvocationsOf(
                                g, new ArrayList<>(), referencedType, methodName));

        // Inner types can be referenced by their outer/sibling types by just their inner name
        // rather than the full name. This means we need to do more, but what that "more" is
        // depends on whether we're looking an inner or outer type.
        boolean typeIsInner = referencedType.contains(".");
        if (typeIsInner) {
            // If the type is inner, then we need to add inner-name-only references occurring in
            // other classes.
            String[] portions = referencedType.split("\\.");
            String outerType = portions[0];
            String innerType = portions[1];
            List<MethodCallExpressionVertex> additionalResults =
                    SFVertexFactory.loadVertices(
                            g,
                            TraversalUtil.traverseInvocationsOf(
                                            g, new ArrayList<>(), innerType, methodName)
                                    .where(
                                            __.or(
                                                    H.has(
                                                            NodeType.METHOD_CALL_EXPRESSION,
                                                            Schema.DEFINING_TYPE,
                                                            outerType),
                                                    H.hasStartingWith(
                                                            NodeType.METHOD_CALL_EXPRESSION,
                                                            Schema.DEFINING_TYPE,
                                                            outerType + "."))));
            results.addAll(additionalResults);
        } else {
            // If the type isn't an inner type, then it's possible some of the references we found
            // are actually inner-name-only references to inner types. So we need to remove those.
            results =
                    results.stream()
                            .filter(
                                    v -> {
                                        // Convert the result's definingType into an outer type by
                                        // getting everything before the first period.
                                        String outerType = v.getDefiningType().split("\\.")[0];
                                        // Get any inner classes for the outer type and cast their
                                        // names to lowercase.
                                        Set<String> innerClassNames =
                                                getInnerClasses(outerType).stream()
                                                        .map(i -> i.getName().toLowerCase())
                                                        .collect(Collectors.toSet());
                                        // If the set lacks an entry for our referenced type, then
                                        // there's no conflicting inner class and we can keep this
                                        // result.
                                        return !innerClassNames.contains(
                                                referencedType.toLowerCase());
                                    })
                            .collect(Collectors.toList());
        }
        return results;
    }

    /**
     * Returns the {@link BaseSFVertex} where the value referenced in the specified method call is
     * declared. <br>
     * E.g., for {@code someObject.someMethod()}, returns the declaration of {@code someObject}.
     */
    Optional<BaseSFVertex> getDeclarationOfReferencedValue(MethodCallExpressionVertex methodCall) {
        // Step 1: Get the name of the thing being referenced.
        List<String> referenceNameList = methodCall.getReferenceExpression().getNames();
        // Step 2: The method call must happen in the context of another method, a field
        // declaration, or both. Determine which.
        Optional<MethodVertex> parentMethodOptional = methodCall.getParentMethod();
        Optional<FieldDeclarationVertex> parentFieldDeclarationOptional =
                methodCall.getFieldDeclaration();
        // Theoretically one or both of those optionals should be present. If not, throw an
        // exception.
        if (!parentMethodOptional.isPresent() && !parentFieldDeclarationOptional.isPresent()) {
            throw new UnexpectedException(
                    "Cannot determine context for method call " + methodCall.toMinimalString());
        }
        // Step 3: If we're in a method, check for variables and parameters.
        if (parentMethodOptional.isPresent()) {
            MethodVertex parentMethod = parentMethodOptional.get();
            // Step 3A: Check for variables.
            List<VariableDeclarationVertex> declaredVariables =
                    MethodUtil.getVariableDeclarations(g, parentMethod);
            for (VariableDeclarationVertex declaredVariable : declaredVariables) {
                // NOTE: A known bug exists here. We merely check that the variable exists,
                //       without verifying that its declaration occurs before the method call.
                //       So `SomeClass someClass = SomeClass.getInstance();` will incorrectly
                //       pass this if-check.
                if (declaredVariable.getName().equalsIgnoreCase(referenceNameList.get(0))) {
                    return Optional.of(declaredVariable);
                }
            }

            // Step 3B: Check whether any of the method's parameters match the first referenced
            //          name.
            List<ParameterVertex> params = parentMethod.getParameters();
            for (ParameterVertex param : params) {
                if (param.getName().equalsIgnoreCase(referenceNameList.get(0))) {
                    return Optional.of(param);
                }
            }
        }

        // Step 4: Check whether any properties on the class match the first referenced name.
        Optional<FieldVertex> fieldOptional =
                ApexClassUtil.getField(g, methodCall.getDefiningType(), referenceNameList.get(0));
        // If there's a property, we need to make sure it's visible to our current context.
        if (fieldOptional.isPresent()) {
            // Determine whether we're in a static context or an instance context.
            boolean isContextStatic =
                    parentMethodOptional
                            .map(FieldWithModifierVertex::isStatic)
                            .orElseGet(() -> parentFieldDeclarationOptional.get().isStatic());
            // Instance context has access to both static and instance properties, while static
            // context only has access to static properties.
            if (!isContextStatic || fieldOptional.get().isStatic()) {
                return Optional.of(fieldOptional.get());
            }
        }

        // Step 5: If, after all of that, we still haven't found anything, just return an empty
        // Optional and let the caller figure out what to do with it.
        return Optional.empty();
    }
}
