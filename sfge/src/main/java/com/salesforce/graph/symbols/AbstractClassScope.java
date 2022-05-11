package com.salesforce.graph.symbols;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.TypeableUtil.OrderedTreeSet;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.ValueStatus;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.FieldDeclarationVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * Responsible for assigning in-line class variable assignments. Subclasses are responsible for
 * deciding how values are assigned and resolved.
 */
@SuppressWarnings(
        "PMD.EmptyIfStmt") // TODO: consider using visitor pattern to avoid empty if statements
public abstract class AbstractClassScope extends PathScopeVisitor implements Typeable {
    public enum State {
        /** No initialization has begun */
        UNINITIALIZED,
        /**
         * Some level of initialization has begun, but has not been completed, the work which was
         * done before transitioning to this state should not be executed again
         */
        INITIALIZING,
        /** The class is completely initialized */
        INITIALIZED
    }

    private static final Logger LOGGER = LogManager.getLogger(AbstractClassScope.class);

    protected final UserClassVertex userClass;
    /**
     * Keeps track of final fields that were initialized inline. Attempting to set them again
     * results in an exception. This is an incomplete solution, it doesn't handle final fields
     * initialized in constructors. The purpose of this map is to sanity check the sfge code, it is
     * not intended to catch invalid apex code.
     */
    protected final TreeSet<String> finalFieldsInitializedInline;

    protected final TreeMap<String, ChainedVertex> finalFields;
    protected final TreeMap<String, ChainedVertex> nonFinalFields;
    protected final TreeMap<String, Typeable> declaredFields;
    protected final TreeMap<String, ApexValue<?>> apexValues;
    protected final OrderedTreeSet types;
    /**
     * Keeps track of which fields have setter methods. The assignment of these methods need special
     * handling.
     */
    protected final TreeSet<String> fieldsWithSetterBlock;

    /** The current state of the class */
    private State state;

    protected AbstractClassScope(GraphTraversalSource g, UserClassVertex userClass) {
        super(g);
        this.userClass = userClass;
        this.finalFieldsInitializedInline = CollectionUtil.newTreeSet();
        this.finalFields = CollectionUtil.newTreeMap();
        this.nonFinalFields = CollectionUtil.newTreeMap();
        this.declaredFields = CollectionUtil.newTreeMap();
        this.apexValues = CollectionUtil.newTreeMap();
        this.fieldsWithSetterBlock = CollectionUtil.newTreeSet();
        this.types = CollectionUtil.newOrderedTreeSet();
        this.state = State.UNINITIALIZED;
    }

    protected AbstractClassScope(AbstractClassScope other) {
        super(other);
        this.userClass = other.userClass;
        this.finalFieldsInitializedInline =
                CloneUtil.cloneTreeSet(other.finalFieldsInitializedInline);
        this.finalFields = CloneUtil.cloneTreeMap(other.finalFields);
        this.nonFinalFields = CloneUtil.cloneTreeMap(other.nonFinalFields);
        this.declaredFields = CloneUtil.cloneTreeMap(other.declaredFields);
        this.apexValues = CloneUtil.cloneTreeMap(other.apexValues);
        this.fieldsWithSetterBlock = CloneUtil.cloneTreeSet(other.fieldsWithSetterBlock);
        this.types = CloneUtil.clone(other.types);
        this.state = other.state;
    }

    public final String getClassName() {
        return userClass.getDefiningType();
    }

    @Override
    public final String getCanonicalType() {
        return getClassName();
    }

    @Override
    public final OrderedTreeSet getTypes() {
        if (types.isEmpty()) {
            types.add(getCanonicalType());
            String superClassName = userClass.getSuperClassName().orElse(null);
            if (superClassName != null) {
                ClassInstanceScope superClassInstance =
                        ClassInstanceScope.getOptional(g, superClassName).orElse(null);
                if (superClassInstance != null) {
                    types.addAll(superClassInstance.getTypes().getAll());
                } else {
                    types.add(superClassName);
                }
            }
            // TODO: Need to support inherited interfaces, they aren't currently copied into the JIT
            final ArrayList<String> interfaceNames = new ArrayList<>(userClass.getInterfaceNames());
            types.addAll(interfaceNames);

            // Every instance in Apex is inherited from Object class
            types.add(ApexStandardLibraryUtil.Type.OBJECT);
        }
        return types;
    }

    @Override
    public final boolean matchesParameterType(Typeable parameterVertex) {
        OrderedTreeSet types = getTypes();
        return types.contains(parameterVertex.getCanonicalType());
    }

    /**
     * Return true if this class is a static context. Used to filter vertices instead of using an
     * instanceof call
     */
    public abstract boolean isStatic();

    /**
     * @return the Field vertices in the order that they appear in source, including base classes
     */
    public final List<FieldVertex> getFields() {
        return getFields(g, userClass, isStatic());
    }

    private static List<FieldVertex> getFields(
            GraphTraversalSource g, UserClassVertex userClass, boolean isStatic) {
        List<FieldVertex> results = new ArrayList<>();

        String superClassName = userClass.getSuperClassName().orElse(null);
        if (superClassName != null) {
            UserClassVertex superClass = ClassUtil.getUserClass(g, superClassName).orElse(null);
            if (superClass != null) {
                results.addAll(getFields(g, superClass, isStatic));
            }
        }

        results.addAll(
                SFVertexFactory.loadVertices(
                        g,
                        g.V(userClass.getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.FIELD)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .where(
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                                .has(Schema.STATIC, isStatic))));

        // fields with synthetic getters and setters
        results.addAll(
                SFVertexFactory.loadVertices(
                        g,
                        g.V(userClass.getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.PROPERTY)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.FIELD)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .where(
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                                .has(Schema.STATIC, isStatic))));

        return results;
    }

    public void setState(State state) {
        if (this.state == state) {
            throw new ProgrammingException(this);
        }
        this.state = state;
    }

    public State getState() {
        return state;
    }

    /**
     * Special case called when updating a synthetic property that references itself in the method.
     */
    public ApexValue<?> updateProperty(String key, ApexValue<?> value) {
        return apexValues.put(key, value);
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        Optional<Typeable> result = super.getTypedVertex(key);

        if (result.isPresent()) {
            return result;
        } else if (declaredFields.containsKey(key)) {
            result = Optional.of(declaredFields.get(key));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        Optional<ChainedVertex> result = super.getValue(key);

        // TODO: Handle this
        if (!result.isPresent()) {
            if (finalFields.containsKey(key)) {
                result = Optional.ofNullable(finalFields.get(key));
            } else if (nonFinalFields.containsKey(key)) {
                result = Optional.ofNullable(nonFinalFields.get(key));
            }
        }

        return result;
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        Optional<ApexValue<?>> result = super.getApexValue(key);

        if (!result.isPresent() && apexValues.containsKey(key)) {
            result = Optional.ofNullable(apexValues.get(key));
        }

        return result;
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex) {
        ChainedVertex rhs = vertex.getRhs().orElse(null);
        if (rhs instanceof InvocableVertex) {
            VariableExpressionVertex lhs = vertex.getLhs();
            final String key = lhs.getName();
            setPendingAssignment(key, (InvocableVertex) rhs);
        }
        return true;
    }

    // TODO this has some overlap with VariableDeclaration and AssignmentExpression
    @Override
    public void afterVisit(FieldDeclarationVertex vertex) {
        VariableExpressionVertex lhs = vertex.getLhs();
        final String key = lhs.getName();

        // Account a = new Account();
        // a.Name = 'Acme Inc.';
        // Anything that has a dot
        boolean complexAssignment = !lhs.getChainedNames().isEmpty();
        String apexValueKey;
        if (complexAssignment) {
            if (lhs.getChainedNames().size() > 1) {
                throw new TodoException(
                        "Handle multiple levels. vertex=" + vertex + " , lhs=" + lhs);
            }
            apexValueKey = lhs.getChainedNames().get(0);
        } else {
            apexValueKey = key;
        }

        ChainedVertex rhs = vertex.getRhs().orElse(null);
        ApexValue<?> apexValue = getApexValue(apexValueKey).orElse(null);

        if (apexValue == null) {
            // This should have been set when the FieldVertex was visited
            throw new UnexpectedException(vertex);
        } else if (rhs instanceof MethodCallExpressionVertex) {
            // Handled by #afterVisit
            // TODO: Test
        } else if (rhs instanceof NewObjectExpressionVertex) {
            // Handled by #afterVisit
            // TODO: Test
        } else if (rhs != null) {
            // This is a simple assignment such as "s = 'foo';"
            ApexValue<?> newApexValue = null;
            if (rhs instanceof VariableExpressionVertex) {
                newApexValue = getApexValue((VariableExpressionVertex) rhs).orElse(null);
            }
            if (newApexValue == null) {
                Typeable typeable = apexValue.getTypeVertex().get();
                newApexValue = getApexValueForDeclaration(typeable, rhs);
            }
            updateVariable(key, newApexValue, this);
        }

        if (rhs != null) {
            rhs = resolve(rhs);
        }
        if (finalFields.containsKey(key)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found final field for FieldDeclarationVertex. name=" + key);
            }
            if (rhs != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found rhs for final field. name=" + key + ", value=" + rhs);
                }
                finalFields.put(key, rhs);
                finalFieldsInitializedInline.add(key);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No rhs for final field. name=" + key);
                }
            }
        } else if (nonFinalFields.containsKey(key)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found non-final field for FieldDeclarationVertex. name=" + key);
            }
            if (rhs != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Found rhs for final field. name=" + key + ", value=" + rhs);
                }
                nonFinalFields.put(key, rhs);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No rhs for final field. name=" + key);
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No final field for FieldDeclarationVertex. name=" + key);
            }
        }
    }

    @Override
    public boolean visit(FieldVertex vertex) {
        String key = vertex.getName();
        // TODO: Only support private fields? The current code relies on the class
        // context to evaluate correctly.
        if (vertex.getModifierNode().isFinal()) {
            finalFields.put(key, null);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Initializing final variable. name=" + key);
            }
        } else {
            nonFinalFields.put(key, null);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Initializing non-final variable. name=" + key);
            }
        }

        declaredFields.put(key, vertex);
        // Set the value to uninitialized. This will be updated based on the assignment while
        // visiting the
        // FieldDeclarationVertex. UNINITIALIZED variables are implicitly null.
        ApexValue<?> apexValue =
                getApexValueBuilderForDeclaration(vertex, null)
                        .withStatus(getDefaultFieldStatus())
                        .build();
        apexValues.put(key, apexValue);
        if (vertex.hasSetterBlock()) {
            fieldsWithSetterBlock.add(key);
        }
        return true;
    }

    /**
     * Return the default status for fields that have not been assigned to. Most of the cases this
     * is {@link ValueStatus#UNINITIALIZED}, but in the case of deserialized classes it will be
     * {@link ValueStatus#INDETERMINANT}
     */
    protected ValueStatus getDefaultFieldStatus() {
        return ValueStatus.UNINITIALIZED;
    }

    /**
     * Resolves a vertex to a more specific value if possible, this version only operates on the
     * ApexValues that this class is aware of because this method is only called for inline field
     * assignments, not constructor assignments.
     */
    protected ChainedVertex resolve(ChainedVertex value) {
        if (value.isResolvable()) {
            String symbolicName = value.getSymbolicName().orElse(null);
            if (symbolicName != null && apexValues.containsKey(symbolicName)) {
                ApexValue<?> apexValue = apexValues.get(symbolicName);
                if (apexValue.getValueVertex().isPresent()) {
                    return apexValue.getValueVertex().get();
                }
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{"
                + "userClass="
                + userClass
                + "} "
                + super.toString();
    }
}
