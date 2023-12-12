package com.salesforce.graph.symbols.apex;

import static com.salesforce.graph.symbols.apex.ApexStringValueFactory.UNRESOLVED_ARGUMENT_PREFIX;

import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.expander.NullValueAccessedException;
import com.salesforce.graph.symbols.DefaultNoOpScope;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BinaryExpressionVertex;
import com.salesforce.graph.vertex.BooleanExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.NamedVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Create new ApexVertexValue for values that have vertices. Not all ApexValues have vertices
public abstract class ApexValue<T extends ApexValue> implements DeepCloneable<T> {

    private static final Logger LOGGER = LogManager.getLogger(ApexValue.class);

    /**
     * Vertex that declares the type of the ApexValue. This may be null in cases where the ApexValue
     * is never assigned to a variable, such as when the ApexValue is part of a return statement.
     */
    private Typeable declarationVertex; // TODO: Make final

    /**
     * The vertex that represents the value being assigned or returned. TODO: This property is not
     * used by ApexClassInstance. Consider changing the hierarchy
     */
    protected ChainedVertex valueVertex; // TODO: Make final

    /**
     * Contains resolution of any values that were a child of {@link #valueVertex}. The map would
     * contain the resolution of the 'a' Variable expression in the following example. public String
     * getValue() { String a = 'Foo'; return a; }
     */
    private final HashMap<ChainedVertex, ChainedVertex> resolvedValues;

    /**
     * Contains the ApexValue that returned this value from a method.{@code returnedFrom} would
     * equal the ApexValue that represents MyObject__c.SObjectType if this ApexValue referred to the
     * {@code describe} variable.
     *
     * <p>>Schema.DescribeSObjectResult describe = MyObject__c.SObjectType.getDescribe();
     */
    private final ApexValue<?> returnedFrom;

    /**
     * Contains the InvocableWithParametersVertex whose invocation returned this value.{@code
     * returnedFrom} would equal the MethodCallExpressionVertex that represents getDescribe() if
     * this ApexValue referred to the {@code describe} variable.
     *
     * <p>>Schema.DescribeSObjectResult describe = MyObject__c.SObjectType.getDescribe();
     */
    private final InvocableVertex invocable;

    /**
     * Contains the MethodVertex whose path returned this value.{@code returnedFrom} would equal the
     * MethodVertex that represents SObjectType#getDescribe if this ApexValue referred to the {@code
     * describe} variable.
     *
     * <p>>Schema.DescribeSObjectResult describe = MyObject__c.SObjectType.getDescribe();
     */
    private final MethodVertex method;

    /** Tracks the set of positive constraints that have been placed on this value */
    protected final HashSet<Constraint> positiveConstraints;

    /** Tracks the set of negative constraints that have been placed on this value */
    protected final HashSet<Constraint> negativeConstraints;

    /** See {@link ValueStatus} */
    private final ValueStatus status;

    protected ApexValue(ApexValueBuilder builder) {
        this.status = builder.getStatus();
        this.declarationVertex = builder.getDeclarationVertex();
        this.returnedFrom =
                (ApexValue<?>) CloneUtil.clone((DeepCloneable) builder.getReturnedFrom());
        this.invocable = builder.getInvocable();
        this.method = builder.getMethod();
        this.resolvedValues = new HashMap<>();
        this.positiveConstraints = new HashSet<>(builder.getPositiveConstraints());
        this.negativeConstraints = new HashSet<>(builder.getNegativeConstraints());
        setValueVertex(builder);
    }

    protected ApexValue(ApexValue<?> other) {
        this(other, other.returnedFrom, other.invocable);
    }

    protected ApexValue(
            ApexValue<?> other,
            @Nullable ApexValue<?> returnedFrom,
            @Nullable InvocableVertex invocable) {
        this.status = other.status;
        this.declarationVertex = other.declarationVertex;
        this.returnedFrom = returnedFrom;
        this.invocable = invocable;
        this.method = other.method;
        this.valueVertex = other.valueVertex;
        this.resolvedValues = CloneUtil.cloneHashMap(other.resolvedValues);
        this.positiveConstraints = CloneUtil.cloneHashSet(other.positiveConstraints);
        this.negativeConstraints = CloneUtil.cloneHashSet(other.negativeConstraints);
    }

    public abstract Optional<String> getDefiningType();

    public abstract <U> U accept(ApexValueVisitor<U> visitor);

    public final ValueStatus getStatus() {
        return status;
    }

    public boolean isValuePresent() {
        throw new TodoException("Subclass should override if appropriate");
    }

    public final boolean isValueNotPresent() {
        return !isValuePresent();
    }

    public final boolean isDeterminant() {
        return !isIndeterminant();
    }

    public final boolean isIndeterminant() {
        return ValueStatus.INDETERMINANT.equals(status);
    }

    public final boolean isInitialized() {
        return ValueStatus.INITIALIZED.equals(status);
    }

    public final boolean isUninitialized() {
        return ValueStatus.UNINITIALIZED.equals(status);
    }

    /**
     * Add a new positive constraint
     *
     * @throws UnexpectedException if the value is already a negative constraint
     */
    public final void addPositiveConstraint(Constraint constraint) {
        if (negativeConstraints.contains(constraint)) {
            throw new UnexpectedException("Conflicting constraint. constraint=" + constraint);
        }
        positiveConstraints.add(constraint);
    }

    /**
     * @return true if the constraint is contained in either the positive or negative set
     */
    public final boolean hasEitherConstraint(Constraint constraint) {
        return hasPositiveConstraint(constraint) || hasNegativeConstraint(constraint);
    }

    public final boolean hasPositiveConstraint(Constraint constraint) {
        return positiveConstraints.contains(constraint);
    }

    public final Set<Constraint> getPositiveConstraints() {
        return Collections.unmodifiableSet(positiveConstraints);
    }

    /**
     * Add a new negative constraint
     *
     * @throws UnexpectedException if the value is already a positive constraint
     */
    public final void addNegativeConstraint(Constraint constraint) {
        if (positiveConstraints.contains(constraint)) {
            throw new UnexpectedException("Conflicting constraint. constraint=" + constraint);
        }
        negativeConstraints.add(constraint);
    }

    public final boolean hasNegativeConstraint(Constraint constraint) {
        return negativeConstraints.contains(constraint);
    }

    public final Set<Constraint> getNegativeConstraints() {
        return Collections.unmodifiableSet(negativeConstraints);
    }

    /**
     * Return whether this value should be treated as true or false in cases such as "if
     * (apexValue){}"
     */
    public Optional<Boolean> asTruthyBoolean() {
        return Optional.empty();
    }

    public boolean isNotNull() {
        return !isNull();
    }

    /**
     * Returns null when the ApexValue corresponds to a null value. It was either explicitly set to
     * null or it is a an uninitialized variable which will be initialized to null by the system.
     */
    public boolean isNull() {
        boolean returnValue =
                valueVertex instanceof LiteralExpressionVertex.Null
                        // A null constraint could also indicate that the value is null
                        || (positiveConstraints.contains(Constraint.Null)
                                && !negativeConstraints.contains(Constraint.Null));

        if (!returnValue) {
            if (ValueStatus.UNINITIALIZED.equals(status)) {
                // Uninitialized values are initialized to null by the compiler
                returnValue = true;
            } else if (ValueStatus.INDETERMINANT.equals(status)) {
                returnValue = false;
            }
        }

        return returnValue;
    }

    /**
     * Apply the given method call and return an optional result. For instance map.get('my_key');
     * would result in an invocation of ApexMapValue returning the current apex value stored in
     * 'my_key'
     */
    public abstract Optional<ApexValue<?>> apply(
            MethodCallExpressionVertex vertex, SymbolProvider symbols);

    public Optional<InvocableVertex> getInvocable() {
        return Optional.ofNullable(invocable);
    }

    public Optional<MethodVertex> getMethod() {
        return Optional.ofNullable(method);
    }

    public Optional<ChainedVertex> getValue(ChainedVertex value) {
        return Optional.ofNullable(resolvedValues.get(value));
    }

    /**
     * Traverses the {@link #returnedFrom} relations returning a chain with this ApexValue as the
     * last element in the list.
     */
    public List<ApexValue<?>> getChain() {
        List<ApexValue<?>> result = new ArrayList<>();

        ApexValue<?> next = this;

        while (next != null) {
            result.add(0, next);
            next = next.returnedFrom;
        }

        return result;
    }

    public Optional<ApexValue<?>> getReturnedFrom() {
        return Optional.ofNullable(returnedFrom);
    }

    public Optional<Typeable> getTypeVertex() {
        if (this instanceof Typeable) {
            return Optional.of((Typeable) this);
        } else if (declarationVertex != null) {
            return Optional.of(declarationVertex);
        } else if (valueVertex instanceof Typeable) {
            return Optional.of((Typeable) valueVertex);
        } else {
            return Optional.empty();
        }
    }

    public void setDeclarationVertex(Typeable declarationVertex) {
        if (this.declarationVertex != null && !this.declarationVertex.equals(declarationVertex)) {
            throw new UnexpectedException(this);
        }
        this.declarationVertex = declarationVertex;
    }

    public Optional<Typeable> getDeclarationVertex() {
        return Optional.ofNullable(declarationVertex);
    }

    public Optional<ChainedVertex> getValueVertex() {
        return Optional.ofNullable(valueVertex);
    }

    public Optional<String> getDeclaredType() {
        if (declarationVertex == null) {
            return Optional.empty();
        } else {
            return Optional.of(declarationVertex.getCanonicalType());
        }
    }

    // TODO: get a more suitable name that doesn't have a conflict
    public Optional<String> getValueVertexType() {
        if (valueVertex instanceof Typeable) {
            return Optional.of(((Typeable) valueVertex).getCanonicalType());
        }
        return Optional.empty();
    }

    public Optional<String> getVariableName() {
        if (declarationVertex instanceof NamedVertex) {
            return Optional.of(((NamedVertex) declarationVertex).getName());
        } else if (valueVertex instanceof NamedVertex) {
            return Optional.ofNullable(((NamedVertex) valueVertex).getName());
        } else {
            return Optional.empty();
        }
    }

    /** Resolves the value if a more specific value exists. */
    public Optional<ChainedVertex> getValue(ChainedVertex value, SymbolProvider symbols) {
        Optional<ChainedVertex> result = symbols.getValue(value);
        if (result.isPresent()) {
            return result;
        } else {
            return getValue(value);
        }
    }

    private void setValueVertex(ApexValueBuilder builder) {
        setValueVertex(builder.getValueVertex(), builder.getSymbolProvider());
    }

    protected final void setValueVertex(
            @Nullable ChainedVertex valueVertex, SymbolProvider symbolProvider) {
        this.valueVertex = valueVertex;
        // Reassigning, clear out any previously resolved values
        this.resolvedValues.clear();
        this.resolvedValues.putAll(_getResolvedValues(symbolProvider));
    }

    protected static Optional<String> convertParameterToString(
            ChainedVertex chainedVertex, SymbolProvider symbols) {
        return ApexValueBuilder.get(symbols).valueVertex(chainedVertex).buildString().getValue();
    }

    protected static Optional<Integer> convertParameterToInteger(
            ChainedVertex chainedVertex, SymbolProvider symbols) {
        return ApexValueBuilder.get(symbols).valueVertex(chainedVertex).buildInteger().getValue();
    }

    private HashMap<ChainedVertex, ChainedVertex> _getResolvedValues(SymbolProvider symbols) {
        HashMap<ChainedVertex, ChainedVertex> values = new HashMap<>();

        if (valueVertex != null) {
            ChainedVertex resolvedVertex = symbols.getValue(valueVertex).orElse(null);
            if (resolvedVertex != null) {
                values.put(valueVertex, resolvedVertex);
            } else if (valueVertex instanceof BinaryExpressionVertex) {
                BinaryExpressionVertex binaryExpression = (BinaryExpressionVertex) valueVertex;
                // We expand without the current scope so that we can add our resolution
                List<ChainedVertex> vertices =
                        ApexValueUtil.expand(binaryExpression, DefaultNoOpScope.getInstance());
                for (ChainedVertex expanded : vertices) {
                    ChainedVertex resolvedExpanded = symbols.getValue(expanded).orElse(null);
                    if (resolvedExpanded != null) {
                        values.put(expanded, resolvedExpanded);
                    }
                }
            } else if (valueVertex instanceof InvocableVertex) {
                for (InvocableVertex invocable : ((InvocableVertex) valueVertex).firstToList()) {
                    for (int i = 0; i < invocable.getParameters().size(); i++) {
                        ChainedVertex parameter = invocable.getParameters().get(i);
                        ChainedVertex resolvedParameter = symbols.getValue(parameter).orElse(null);
                        if (resolvedParameter != null) {
                            values.put(parameter, resolvedParameter);
                        }
                    }
                }
            }
        }

        return values;
    }

    /**
     * Find the parent or grandparent BooleanExpressionVertex that participates in a short circuit.
     * Short circuits occur when the first part of the boolean expression evaluates to a value that
     * makes the second part of the boolean comparison unnecessary.
     *
     * <p>if (x == null || x.isEmpty()) // Short circuits after first comparison if x is null
     *
     * <p>if (x != null && !x.isEmpty()) // Short circuits after first comparison if x is null
     */
    private Optional<BooleanExpressionVertex> findBooleanForShortCircuitComparison(
            MethodCallExpressionVertex vertex) {
        BaseSFVertex parent = vertex.getParent();
        BaseSFVertex grandParent = parent.getParent();
        // The method's parent is the second part of a BooleanExpressionVertex
        if (parent instanceof BooleanExpressionVertex
                && grandParent instanceof BooleanExpressionVertex
                && parent.getChildIndex() == 1) {
            // if (o == null || o.size == 0)
            return Optional.of((BooleanExpressionVertex) grandParent);
        } else if (parent instanceof BooleanExpressionVertex
                && !(grandParent instanceof BooleanExpressionVertex)
                && vertex.getChildIndex() == 1) {
            // if (o == null || o.isEmpty())
            return Optional.of((BooleanExpressionVertex) parent);
        }
        return Optional.empty();
    }

    /**
     * @return true if {@link LiteralExpressionVertex.Null} is being compared to an ApexValue on the
     *     left hand side of a boolean OR expression that will short circuit if the ApexValue is
     *     null.
     */
    private boolean isShortCircuitOr(
            BooleanExpressionVertex booleanExpression, SymbolProvider symbols) {
        if (booleanExpression != null && booleanExpression.isOperatorOr()) {
            BaseSFVertex firstChild = booleanExpression.getChild(0);
            // Verify that the first part of the grandparent's boolean is of the form o == null,
            // null == o,
            // and o resolves to this ApexValue
            if (firstChild instanceof BooleanExpressionVertex) {
                BooleanExpressionVertex firstChildBoolean = (BooleanExpressionVertex) firstChild;
                if (firstChildBoolean.isOperatorEquals()) {
                    BaseSFVertex lhs = firstChildBoolean.getLhs();
                    BaseSFVertex rhs = firstChildBoolean.getRhs();
                    BaseSFVertex comparisonVertex = null;
                    if (lhs instanceof LiteralExpressionVertex.Null) {
                        comparisonVertex = rhs;
                    } else if (rhs instanceof LiteralExpressionVertex.Null) {
                        comparisonVertex = lhs;
                    }
                    if (comparisonVertex instanceof VariableExpressionVertex) {
                        ApexValue<?> apexValue =
                                symbols.getApexValue((VariableExpressionVertex) comparisonVertex)
                                        .orElse(null);
                        // Reference equality is intentional
                        return apexValue == this;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return true if {@link LiteralExpressionVertex.Null} is being compared to an ApexValue on the
     *     left hand side of a boolean AND expression that will short circuit if the ApexValue is
     *     null.
     */
    private boolean isShortCircuitAnd(
            BooleanExpressionVertex booleanExpression, SymbolProvider symbols) {
        if (booleanExpression != null && booleanExpression.isOperatorAnd()) {
            BaseSFVertex firstChild = booleanExpression.getChild(0);
            // Verify that the first part of the grandparent's boolean is of the form o != null,
            // null != o,
            // and o resolves to this ApexValue
            if (firstChild instanceof BooleanExpressionVertex) {
                BooleanExpressionVertex firstChildBoolean = (BooleanExpressionVertex) firstChild;
                if (firstChildBoolean.isOperatorNotEquals()) {
                    BaseSFVertex lhs = firstChildBoolean.getLhs();
                    BaseSFVertex rhs = firstChildBoolean.getRhs();
                    BaseSFVertex comparisonVertex = null;
                    if (lhs instanceof LiteralExpressionVertex.Null) {
                        comparisonVertex = rhs;
                    } else if (rhs instanceof LiteralExpressionVertex.Null) {
                        comparisonVertex = lhs;
                    }
                    if (comparisonVertex instanceof VariableExpressionVertex) {
                        ApexValue<?> apexValue =
                                symbols.getApexValue((VariableExpressionVertex) comparisonVertex)
                                        .orElse(null);
                        // Reference equality is intentional
                        return apexValue == this;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Detect cases where the runtime would throw a NullPointer exception because a method was
     * called on a null value. Exclude this path by throwing a{@link NullValueAccessedException}.
     *
     * @throws NullValueAccessedException if this would have caused an NPE at runtime, effectively
     *     removing an invalid path
     */
    public void checkForNullAccess(MethodCallExpressionVertex vertex, SymbolProvider symbols) {
        if (declarationVertex instanceof FieldVertex) {
            // Don't check for null in cases of field vertices. There are instances where the sfge
            // engine thinks the
            // value is null, but we can't guarantee that. 1. We don't always walk all constructors,
            // the constructor
            // might have set the value. 2. The aura or vf page might have set this value, but the
            // engine doesn't have
            // visibility into this.
            return;
        }

        if (isNull()) {
            // Don't throw an exception if this is the part of a short circuited or statement
            // if (o == null || o.isEmpty()) 		// Don't throw
            // if (o == null || o.size() == 0) 		// Don't throw
            // if (o != null && !o.isEmpty()) 		// Don't throw
            // if (o != null && o.size() != 0) 		// Don't throw
            // if (o.isEmpty)						// throw
            // System.println(o.size());			// throw
            BooleanExpressionVertex booleanExpression =
                    findBooleanForShortCircuitComparison(vertex).orElse(null);
            if (isShortCircuitOr(booleanExpression, symbols)
                    || isShortCircuitAnd(booleanExpression, symbols)) {
                // TODO: Normally this statement would not execute at runtime because it would have
                // short circuited. Improve this to avoid visiting the short circuited vertices at
                // all
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "Ignoring access to null object that would short circuit at runtime");
                }
                return;
            }
            throw new NullValueAccessedException(this, vertex);
        }
    }

    /**
     * Detect cases where the runtime would throw a NullPointer exception because a null value was
     * passed to a method. Exclude this path by throwing a{@link NullValueAccessedException}.
     *
     * @throws NullValueAccessedException if this would have caused an NPE at runtime, effectively
     *     removing an invalid path
     */
    public void checkForUseAsNullParameter(MethodCallExpressionVertex vertex) {
        if (declarationVertex instanceof FieldVertex) {
            // See comment in same code block for #checkForNullAccess
            return;
        }

        if (isNull()) {
            throw new NullValueAccessedException(this, vertex);
        }
    }

    @Override
    public String toString() {
        return "ApexValue("
                + this.getClass().getSimpleName()
                + ") {"
                + "status="
                + status
                + ", declarationVertex="
                + declarationVertex
                + ", valueVertex="
                + valueVertex
                + ", resolvedValues="
                + resolvedValues
                + ", returnedFrom="
                + returnedFrom
                + ", invocableExpression="
                + invocable
                + ", method="
                + method
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApexValue<?> apexValue = (ApexValue<?>) o;
        return Objects.equals(declarationVertex, apexValue.declarationVertex)
                && Objects.equals(valueVertex, apexValue.valueVertex)
                && Objects.equals(resolvedValues, apexValue.resolvedValues)
                && Objects.equals(returnedFrom, apexValue.returnedFrom)
                && Objects.equals(invocable, apexValue.invocable)
                && Objects.equals(method, apexValue.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                declarationVertex, valueVertex, resolvedValues, returnedFrom, invocable, method);
    }

    // TODO: Move to util
    /**
     * @throws UnexpectedException if vertex parameter size is not {@code expectedParameterSize }.
     */
    public static void validateParameterSize(InvocableVertex vertex, int expectedParameterSize) {
        if (vertex.getParameters().size() != expectedParameterSize) {
            throw new UnexpectedException(vertex);
        }
    }

    /**
     * @throws UnexpectedException if vertex parameter size is not one of {@code
     *     expectedParameterSizes}. Used when a method has overloads with different numbers of
     *     arguments.
     */
    public static void validateParameterSizes(
            InvocableVertex vertex, int... expectedParameterSizes) {
        for (int expectedParameterSize : expectedParameterSizes) {
            if (vertex.getParameters().size() == expectedParameterSize) {
                return;
            }
        }
        throw new UnexpectedException(vertex);
    }
}
