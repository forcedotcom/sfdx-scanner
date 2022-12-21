package com.salesforce.graph.symbols;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.exception.UserActionException;
import com.salesforce.graph.ops.ApexClassUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.symbols.apex.*;
import com.salesforce.graph.vertex.ArrayLoadExpressionVertex;
import com.salesforce.graph.vertex.AssignmentExpressionVertex;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BinaryExpressionVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.CastExpressionVertex;
import com.salesforce.graph.vertex.CatchBlockStatementVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.DmlDeleteStatementVertex;
import com.salesforce.graph.vertex.DmlInsertStatementVertex;
import com.salesforce.graph.vertex.DmlUndeleteStatementVertex;
import com.salesforce.graph.vertex.DmlUpdateStatementVertex;
import com.salesforce.graph.vertex.DmlUpsertStatementVertex;
import com.salesforce.graph.vertex.ElseWhenBlockVertex;
import com.salesforce.graph.vertex.EmptyReferenceExpressionVertex;
import com.salesforce.graph.vertex.ExpressionStatementVertex;
import com.salesforce.graph.vertex.FieldDeclarationStatementsVertex;
import com.salesforce.graph.vertex.FieldDeclarationVertex;
import com.salesforce.graph.vertex.FieldVertex;
import com.salesforce.graph.vertex.ForEachStatementVertex;
import com.salesforce.graph.vertex.ForLoopStatementVertex;
import com.salesforce.graph.vertex.IdentifierCaseVertex;
import com.salesforce.graph.vertex.IfBlockStatementVertex;
import com.salesforce.graph.vertex.IfElseBlockStatementVertex;
import com.salesforce.graph.vertex.InvocableVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.LiteralCaseVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.ModifierNodeVertex;
import com.salesforce.graph.vertex.NewKeyValueObjectExpressionVertex;
import com.salesforce.graph.vertex.NewListInitExpressionVertex;
import com.salesforce.graph.vertex.NewListLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewMapInitExpressionVertex;
import com.salesforce.graph.vertex.NewMapLiteralExpressionVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.NewSetInitExpressionVertex;
import com.salesforce.graph.vertex.NewSetLiteralExpressionVertex;
import com.salesforce.graph.vertex.ParameterVertex;
import com.salesforce.graph.vertex.PrefixExpressionVertex;
import com.salesforce.graph.vertex.ReferenceExpressionVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.SuperMethodCallExpressionVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import com.salesforce.graph.vertex.ThisMethodCallExpressionVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import com.salesforce.graph.vertex.TryCatchFinallyBlockStatementVertex;
import com.salesforce.graph.vertex.TypeWhenBlockVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableDeclarationVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.vertex.WhileLoopStatementVertex;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

@SuppressWarnings(
        "PMD") // TODO: This class needs multiple changes and refactoring - leaving as is for now
public abstract class PathScopeVisitor extends BaseScopeVisitor<PathScopeVisitor> {
    private static final Logger LOGGER = LogManager.getLogger(PathScopeVisitor.class);

    /** We push an empty stack so that we don't have to constantly check if the stack is empty */
    private static final int DEFAULT_STACK_DEPTH = 1;

    protected final GraphTraversalSource g;

    /** Keeps track of the return values from any method calls */
    private final Stack<Map<InvocableVertex, ApexValue<?>>> invocableResults;

    /**
     * This method stack is used to resolve chained methods that may not be saved to an interim
     * variable. An example of this would be MySingleton.getInstance().getName();.
     * MySingleton.getInstance() returns an ApexClassInstanceValue. This value is stored as the
     * value, where the #getName MethodCallExpression is the key. This allows the correct method to
     * be called when #getName is visited.
     */
    private final Stack<Map<InvocableVertex, ApexValue<?>>> chainedApexValueStack;

    /**
     * It is used to hold values assigned via method calls or assignments. Account a = new
     * Account(); a.Name = 'Acme Inc.';
     *
     * <p>or method assignment Account a = new Account(); a.put('Name', 'Acme Inc.'); or method
     * calls Account a = AccountFactory.getAccount(); a.put('Name', 'Acme Inc.');
     */
    // TODO: This doesn't work for instance variables
    protected final Stack<TreeMap<String, ApexValue<?>>> apexValueStack;

    /** Method values passed to the currently executing methods as parameters */
    protected final Stack<MethodInvocationScope> methodInvocationStack;

    /** Keep track of assignments that contain invocable vertices. These need to be deferred. */
    private final HashMap<InvocableVertex, String> pendingInvocableAssignments;

    protected PathScopeVisitor(GraphTraversalSource g) {
        this(g, null);
    }

    public PathScopeVisitor(GraphTraversalSource g, PathScopeVisitor inheritedScope) {
        super(inheritedScope);
        this.g = g;

        this.invocableResults = new Stack<>();
        this.chainedApexValueStack = new Stack<>();
        this.methodInvocationStack = new Stack<>();
        this.apexValueStack = new Stack<>();
        this.pendingInvocableAssignments = new HashMap<>();

        // Push some default stacks to avoid the need to constantly check the for empty stacks.
        // An empty stack sometimes triggers contextual information, those stacks aren't included
        // here.
        for (int i = 0; i < DEFAULT_STACK_DEPTH; i++) {
            invocableResults.push(new HashMap<>());
            chainedApexValueStack.push(new HashMap<>());
            apexValueStack.push(CollectionUtil.newTreeMap());
        }
    }

    protected PathScopeVisitor(PathScopeVisitor other) {
        super(other, /*ignored*/ false);
        this.g = other.g;
        this.invocableResults = CloneUtil.cloneStack(other.invocableResults);
        this.chainedApexValueStack = CloneUtil.cloneStack(other.chainedApexValueStack);
        this.methodInvocationStack = CloneUtil.cloneStack(other.methodInvocationStack);
        this.apexValueStack = CloneUtil.cloneStack(other.apexValueStack);
        this.pendingInvocableAssignments =
                CloneUtil.cloneHashMap(other.pendingInvocableAssignments);
    }

    @Override
    public MutableSymbolProvider getMutableSymbolProvider() {
        return this;
    }

    @Override
    public ApexValue<?> updateVariable(
            String key, ApexValue<?> value, SymbolProvider currentScope) {

        // TODO: Remove ChainedVertex, only track ApexValue
        if (apexValueStack.peek().containsKey(key)) {
            ApexValue<?> previousValue = apexValueStack.peek().get(key);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Updating variable. key="
                                + key
                                + ",  value="
                                + value
                                + ", previousValue="
                                + previousValue);
            }
            if (!value.getTypeVertex().isPresent() && previousValue.getTypeVertex().isPresent()) {
                // Copy over the typed vertex from the original value if the new value doesn't have
                // one
                value.setDeclarationVertex(previousValue.getTypeVertex().get());
            }
            return apexValueStack.peek().put(key, value);
        } else if (!methodInvocationStack.isEmpty()
                && methodInvocationStack.peek().getTypedVertex(key).isPresent()) {
            // This happens when a method is overwriting a variable that was passed to it
            // TODO: Why is it "this" instead of currentScope
            return methodInvocationStack.peek().updateVariable(key, value, this);
        } else if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().updateVariable(key, value, currentScope);
        } else {
            throw new UnexpectedException(
                    "Undefined variable. this=" + this + ", key=" + key + ", value=" + value);
        }
    }

    private ApexValue<?> updateInvocableResults(InvocableVertex vertex, ApexValue<?> apexValue) {
        // TODO: Look into removing vertex from chainedApexValueStack
        return invocableResults.peek().put(vertex, apexValue);
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        Optional<ChainedVertex> result;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getValue. key=" + key);
        }
        Optional<ApexValue<?>> apexValue = getApexValue(key);
        if (apexValue.isPresent()) {
            result = apexValue.get().getValueVertex();
        } else if (!methodInvocationStack.isEmpty()
                && methodInvocationStack.peek().getTypedVertex(key).isPresent()) {
            result = methodInvocationStack.peek().getValue(key);
        } else if (getInheritedScope().isPresent()) {
            result = getInheritedScope().get().getValue(key);
        } else {
            result = Optional.empty();
        }

        if (result.isPresent()) {
            ChainedVertex value = result.get();
            // TODO: Test
            if (value instanceof VariableExpressionVertex.Single) {
                VariableExpressionVertex.Single variableExpression =
                        (VariableExpressionVertex.Single) value;
                if (variableExpression.getSymbolicName().get().equalsIgnoreCase(key)) {
                    // The variable retrieved is the same as the key asked for. It can't be resolved
                    // any more
                    return result;
                }
            } else if (value instanceof MethodCallExpressionVertex) {
                value = getMethodResults((MethodCallExpressionVertex) value).orElse(value);
                return Optional.of(value);
            }

            String symbolicName = value.getSymbolicName().orElse(null);
            // ForLoopVariableExpressionVertex has to be handled specially because its name is the
            // same as the thing it
            // resolves to
            if (key.equals(symbolicName) && !(value instanceof VariableExpressionVertex.ForLoop)) {
                // This can happen in cases such as
                // s = s.toLowerCase();
                return result;
            }

            Optional<ChainedVertex> resolved = getValue(result.get());
            if (resolved.isPresent()) {
                // It was resolved further
                return resolved;
            } else {
                return result;
            }
        } else {
            return result;
        }
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        if (key.contains(".")) {
            return getTypedVertex(Arrays.asList(key.split("\\.")));
        }

        Optional<Typeable> result;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getDeclaration. key=" + key);
        }
        if (apexValueStack.peek().containsKey(key)) {
            result = apexValueStack.peek().get(key).getTypeVertex();
        } else if (!methodInvocationStack.isEmpty()
                && methodInvocationStack.peek().getTypedVertex(key).isPresent()) {
            result = methodInvocationStack.peek().getTypedVertex(key);
        } else if (getInheritedScope().isPresent()) {
            result = getInheritedScope().get().getTypedVertex(key);
        } else {
            result = Optional.empty();
        }

        return result;
    }

    @Override
    public Optional<Typeable> getTypedVertex(List<String> keySequence) {
        if (keySequence.isEmpty()) {
            return Optional.empty();
        } else if (keySequence.size() == 1) {
            return getTypedVertex(keySequence.get(0));
        } else if (keySequence.size() > 2) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "TODO: PathScopeVisitor.getTypedVertex() can currently only support chains of length 2 or lower. keySequence="
                                + keySequence);
            }
            return Optional.empty();
        } else {
            String variableName = keySequence.get(0);
            String fieldName = keySequence.get(1);

            // Try to find the field declaration of the class. This covers cases where the path is
            // being walked but
            // all paths have not been resolved. In this case there won't be a class instance, but
            // we can find the
            // variable declaration and introspect the fields of the class.
            Typeable typeable = getTypedVertex(variableName).orElse(null);
            if (typeable != null) {
                FieldVertex fieldVertex =
                        ApexClassUtil.getField(g, typeable, fieldName).orElse(null);
                return Optional.ofNullable(fieldVertex);
            }

            if (getInheritedScope().isPresent()) {
                return getInheritedScope().get().getTypedVertex(keySequence);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        Optional<ApexValue<?>> result;

        if (apexValueStack.peek().containsKey(key)) {
            result = Optional.ofNullable(apexValueStack.peek().get(key));
            if (result.isPresent()) {
                return result;
            }
        }

        if (!methodInvocationStack.isEmpty()) {
            result = methodInvocationStack.peek().getApexValue(key);
            if (result.isPresent()) {
                return result;
            }
        }

        if (getInheritedScope().isPresent()) {
            result = getInheritedScope().get().getApexValue(key);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<ApexValue<?>> getReturnedValue(InvocableVertex vertex) {
        final ApexValue<?> result;

        // Order is important here. invocableResults can have a more recent result after the item is
        // invoked
        // chainedApexValueStack will contain a placeholder value before the invocable is invoked.
        // TODO: Look into removing from chainedApexValueStack when invocableResults is updated
        if (invocableResults.peek().containsKey(vertex)) {
            result = invocableResults.peek().get(vertex);
        } else if (chainedApexValueStack.peek().containsKey(vertex)) {
            result = chainedApexValueStack.peek().get(vertex);
        } else if (getInheritedScope().isPresent()) {
            result = getInheritedScope().get().getReturnedValue(vertex).orElse(null);
        } else {
            result = null;
        }

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(VariableExpressionVertex var) {
        String symbolicName = var.getSymbolicName().orElseThrow(() -> new UnexpectedException(var));

        ApexValue<?> result = getApexValue(symbolicName).orElse(null);

        // Handle references to static class references
        String[] keys = var.getFullName().split("\\.");
        if (keys.length > 1) {
            if (keys.length > 2) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            "TODO: PathScopeVisitor.getApexValue() can currently only support chains of length 2 or lower. keySequence="
                                    + Arrays.toString(keys));
                }
                return Optional.empty();
            }
            final String propertyName = keys[1];
            // Attempt to get the property on an ObjectProperties ApexValue, a member on a class
            // instnance, or if it is
            // a static property on a class
            if (result instanceof ObjectProperties) {
                ObjectProperties objectProperties = (ObjectProperties) result;
                return objectProperties.getApexValue(propertyName);
            } else if (result instanceof ApexClassInstanceValue) {
                ApexClassInstanceValue apexClassInstanceValue = (ApexClassInstanceValue) result;
                Optional<ApexValue<?>> classInstanceValue =
                        apexClassInstanceValue.getClassInstanceScope().getApexValue(propertyName);
                if (classInstanceValue.isPresent()) {
                    return classInstanceValue;
                }
            } else if (result == null) {
                ClassStaticScope classStaticScope =
                        ContextProviders.CLASS_STATIC_SCOPE
                                .get()
                                .getClassStaticScope(symbolicName)
                                .orElse(null);
                if (classStaticScope != null) {
                    return classStaticScope.getApexValue(propertyName);
                }
            }

            // We were unable to resolve the secondary property
            return Optional.empty();
        } else {
            return Optional.ofNullable(result);
        }
    }

    /**
     * Retrieves a value that was the result of an invocable vertex. This method must be called
     * before {@link #afterVisit(NewObjectExpressionVertex)}
     */
    protected Optional<ApexValue<?>> getChainedApexValue(InvocableWithParametersVertex vertex) {
        if (chainedApexValueStack.peek().containsKey(vertex)) {
            return Optional.ofNullable(chainedApexValueStack.peek().get(vertex));
        } else if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().getChainedApexValue(vertex);
        } else {
            return Optional.empty();
        }
    }

    private Optional<ChainedVertex> getMethodResults(MethodCallExpressionVertex value) {
        if (invocableResults.peek().containsKey(value)) {
            ApexValue<?> returnValue = invocableResults.peek().get(value);
            if (returnValue instanceof ApexSingleValue) {
                if (returnValue.getValueVertex().isPresent()) {
                    ChainedVertex result = returnValue.getValueVertex().get();
                    result = returnValue.getValue(result).orElse(result);
                    return Optional.of(result);
                }
            } else if (returnValue != null) {
                // TODO: This method should return an ApexValue
            }
        } else if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().getMethodResults(value);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ChainedVertex> getValue(ChainedVertex value) {
        ChainedVertex result = ScopeUtil.recursivelyResolve(this, value);
        // Reference equality is intentional
        if (result == value) {
            // It can't be resolved further
            return Optional.empty();
        } else {
            if (result instanceof MethodCallExpressionVertex) {
                result = ((MethodCallExpressionVertex) result).getFirst();
            }
            return Optional.of(result);
        }
    }

    @Override
    public ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value) {
        if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().getValueAtTimeOfInvocation(vertex, value);
        } else {
            return value;
        }
    }

    @Override
    public void pushMethodInvocationScope(MethodInvocationScope methodInvocationScope) {
        invocableResults.push(new HashMap<>());
        chainedApexValueStack.push(new HashMap<>());
        apexValueStack.push(CollectionUtil.newTreeMap());
        if (methodInvocationScope == null) {
            throw new UnexpectedException(this);
        }
        methodInvocationStack.push(methodInvocationScope);
    }

    @Override
    public MethodInvocationScope popMethodInvocationScope(@Nullable InvocableVertex invocable) {
        if (invocableResults.size() < (DEFAULT_STACK_DEPTH + 1)) {
            // The stack has been popped too many times
            throw new UnexpectedException(invocable);
        }
        invocableResults.pop();
        chainedApexValueStack.pop();
        apexValueStack.pop();
        return methodInvocationStack.pop();
    }

    @Override
    public Optional<PathScopeVisitor> getImplementingScope(
            InvocableVertex invocable, MethodVertex method) {
        if (invocable instanceof MethodCallExpressionVertex) {
            if (method.isStatic()) {
                ClassStaticScope classStaticScope =
                        ContextProviders.CLASS_STATIC_SCOPE
                                .get()
                                .getClassStaticScope(method.getDefiningType())
                                .get();
                return Optional.of(classStaticScope);
            }
            MethodCallExpressionVertex methodCallExpression =
                    (MethodCallExpressionVertex) invocable;
            String symbolicName = methodCallExpression.getSymbolicName().orElse(null);
            // Try to find the method on an object instance
            if (symbolicName != null) {
                AbstractClassInstanceScope classScope =
                        resolveClassInstanceScope(symbolicName).orElse(null);
                return Optional.ofNullable(classScope);
            } else if (chainedApexValueStack.peek().containsKey(invocable)) {
                ApexValue<?> apexValue = chainedApexValueStack.peek().get(invocable);
                if (apexValue instanceof ApexClassInstanceValue) {
                    return Optional.of(
                            ((ApexClassInstanceValue) apexValue).getClassInstanceScope());
                } else {
                    // A value was returned, but it's not a class that was resolved. Execute with
                    // the current class scope
                    return Optional.of(this);
                }
            }
        } else if (invocable instanceof NewObjectExpressionVertex) {
            NewObjectExpressionVertex newObjectExpression = (NewObjectExpressionVertex) invocable;
            // Create a new class scope with the constructors from the invocation
            ClassInstanceScope classScope =
                    ClassInstanceScope.getOptional(g, newObjectExpression).orElse(null);
            if (classScope != null) {
                final ApexValueBuilder apexValueBuilder =
                        ApexValueBuilder.get(this)
                                .returnedFrom(null, invocable)
                                .valueVertex(newObjectExpression);

                // Set declaration vertex if available
                // For example,
                // SObject obj = new Account();
                // Method parameter matching will need declaration type information
                final BaseSFVertex parent = newObjectExpression.getParent();
                if (parent instanceof Typeable) {
                    apexValueBuilder.declarationVertex((Typeable) parent);
                } else if (parent instanceof FieldDeclarationVertex) {
                    final FieldDeclarationVertex fieldDeclaration = (FieldDeclarationVertex) parent;
                    final String fieldName = fieldDeclaration.getLhs().getName();
                    final Optional<FieldVertex> field =
                            ApexClassUtil.getField(
                                    g, newObjectExpression.getDefiningType(), fieldName);
                    if (field.isPresent()) {
                        apexValueBuilder.declarationVertex(field.get());
                    } else {
                        throw new TodoException(
                                "FieldVertex not found for class member "
                                        + fieldName
                                        + " with value "
                                        + newObjectExpression);
                    }

                } else {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(
                                "Parent type of NewObjectInitExpression is not handled: " + parent);
                    }
                }

                final ApexClassInstanceValue apexClassInstance =
                        apexValueBuilder.buildApexClassInstanceValue(classScope);

                chainedApexValueStack.peek().put(invocable, apexClassInstance);
                // Store this in case it is passed as a parameter to a method
                updateInvocableResults(invocable, apexClassInstance);
                return Optional.of(classScope);
            }
        } else if (invocable instanceof SuperMethodCallExpressionVertex
                || invocable instanceof ThisMethodCallExpressionVertex) {
            PathScopeVisitor inheritedScope = getInheritedScope().orElse(null);
            if (!(inheritedScope instanceof ClassInstanceScope)) {
                throw new UnexpectedException(this);
            }
            return Optional.of(inheritedScope);
        } else if (invocable instanceof VariableExpressionVertex.Single) {
            VariableExpressionVertex.Single variableExpression =
                    (VariableExpressionVertex.Single) invocable;
            String[] keys = variableExpression.getFullName().split("\\.");
            if (keys.length > 2) {
                // TODO: this is possible, it just needs to iteratively call getApexValue on each
                // value
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                            "TODO: PathScopeVisitor.getApexValue() can currently only support chains of length 2 or lower. keySequence="
                                    + Arrays.toString(keys));
                }
                return Optional.empty();
            }
            ApexValue<?> apexValue = getApexValue(keys[0]).orElse(null);
            if (apexValue instanceof ApexClassInstanceValue) {
                // Refers to "c" in the following case
                // MyClass c = new MyClass();
                // String s = c.aString;
                return Optional.of(((ApexClassInstanceValue) apexValue).getClassInstanceScope());
            }
        } else {
            throw new UnexpectedException(invocable);
        }

        // It is executing on the current class
        return Optional.of(this);
    }

    @Override
    public Optional<ApexValue<?>> afterMethodCall(
            InvocableVertex invocable, MethodVertex method, ApexValue<?> returnValue) {
        InvocableVertex next = invocable.getNext().orElse(null);

        // Standard methods only have stubs that represent their signature. We must call
        // #executeMethod on the ApexValue
        // that represents the StandardType. This will return the proxy object that we will treat as
        // the return value.
        // NewObjectExpressionVertex is excluded because the constructor exists and we want it to
        // follow the standard
        // class instance instantiation path
        if (!(invocable instanceof NewObjectExpressionVertex) && method.isStandardType()) {
            if (returnValue != null) {
                // Values are never returned for standard methods
                throw new UnexpectedException(invocable);
            }

            ApexValue<?> apexValue =
                    MethodUtil.getApexValue((ChainedVertex) invocable, this).orElse(null);

            // Execute the method on the standard type
            if (apexValue instanceof ApexStandardValue) {
                Optional<ApexValue<?>> standardValue =
                        ((ApexStandardValue) apexValue)
                                .executeMethod(
                                        (InvocableWithParametersVertex) invocable, method, this);
                returnValue = standardValue.orElse(null);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "afterMethodCall. StandardReturnValue. apexValue="
                                    + apexValue
                                    + ", returnValue="
                                    + returnValue);
                }
            } else if (apexValue == null || apexValue instanceof ApexClassInstanceValue) {
                ApexValueBuilder builder =
                        ApexValueBuilder.get(this).returnedFrom(apexValue, invocable);
                returnValue = ApexValueUtil.synthesizeReturnedValue(builder, method);
            }
        }

        // Only store the results of the method call expression if the next value is not present
        // The value will be stored whenever the entire chain completes
        if (next == null) {
            if (returnValue != null) {
                String key = pendingInvocableAssignments.remove(invocable);
                if (key != null) {
                    if (key.contains(".")) {
                        // TODO: this needs to happen for all the other places that use
                        // pendingAssignments.remove
                        String[] keys = key.split("\\.");
                        if (keys.length != 2) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(
                                        "TODO: Handle multiple levels. vertex="
                                                + invocable
                                                + " , keys="
                                                + keys);
                            }
                        } else {
                            ApexValue<?> object = getApexValue(keys[0]).orElse(null);
                            ApexStringValue fieldName =
                                    ApexValueBuilder.getWithoutSymbolProvider()
                                            .buildString(keys[1]);
                            if (object instanceof ApexClassInstanceValue) {
                                ApexClassInstanceValue apexClassInstanceValue =
                                        (ApexClassInstanceValue) object;
                                apexClassInstanceValue
                                        .getClassInstanceScope()
                                        .updateInstanceApexValue(key, returnValue);
                            } else if (object instanceof ObjectProperties) {
                                ObjectProperties objectProperties = (ObjectProperties) object;
                                objectProperties.putApexValue(fieldName, returnValue);
                            }
                        }
                    } else {
                        updateVariable(key, returnValue, this);
                    }
                }
            }
        } else if (returnValue == null) {
            // Handle chained NewObjectExpression such as new MyClass().getValue()
            returnValue = chainedApexValueStack.peek().get(invocable);
            chainedApexValueStack.peek().put(next, returnValue);
        } else {
            // Handle chained method calls such as MySingleton.getInstance().getName();
            chainedApexValueStack.peek().put(next, returnValue);
        }
        if (returnValue != null) {
            updateInvocableResults(invocable, returnValue);
        }
        return Optional.ofNullable(returnValue);
    }

    @Override
    public boolean visit(ArrayLoadExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(AssignmentExpressionVertex vertex) {
        trackVisited(vertex);

        VariableExpressionVertex lhs = vertex.getLhs();
        final String key = lhs.getFullName();

        ChainedVertex rhs = vertex.getRhs();
        if (rhs instanceof InvocableVertex) {
            setPendingAssignment(key, (InvocableVertex) rhs);
        }

        return true;
    }

    @Override
    public void afterVisit(ArrayLoadExpressionVertex vertex) {
        final ChainedVertex array = vertex.getArray();
        final Integer index = vertex.getIndexAsInteger(this).orElse(null);
        final ApexValue<?> apexValue;

        if (array instanceof MethodCallExpressionVertex) {
            apexValue = getReturnedValue((MethodCallExpressionVertex) array).orElse(null);
        } else if (array instanceof VariableExpressionVertex.Single) {
            apexValue = getApexValue((VariableExpressionVertex) array).orElse(null);
        } else {
            throw new UnexpectedException(vertex);
        }

        final String key = pendingInvocableAssignments.remove(vertex);

        ApexValue<?> loadedValue = null;

        if (ApexValueUtil.isDeterminant(apexValue)) {
            if (apexValue instanceof ApexListValue) {
                ApexListValue apexListValue = (ApexListValue) apexValue;
                if (isSimpleIncrementingTypeForLoop(vertex)) {
                    // This is an array load within a for loop. Convert to an ApexForLoopValue
                    loadedValue = ApexValueBuilder.get(this).buildForLoopValue(apexListValue);
                } else if (index != null) {
                    Optional<ForLoopStatementVertex> optForLoopStatement =
                            vertex.getFirstParentOfType(ASTConstants.NodeType.FOR_LOOP_STATEMENT);

                    if (!optForLoopStatement.isPresent()
                            && index < apexListValue.getValues().size()) {
                        // Only load the value if we aren't within a for loop. This avoids us
                        // accidentally loading a
                        // single member of the list from within a for loop
                        loadedValue = apexListValue.getValues().get(index);
                    } else {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(
                                    "Ignoring ArrayLoadExpressionVertex outside of index. vertex="
                                            + vertex
                                            + ", size="
                                            + apexListValue.getValues().size()
                                            + ", index="
                                            + index);
                        }
                        loadedValue = getIndeterminantArrayLoadValue(apexValue);
                    }
                }
            } else if (apexValue instanceof ApexSoqlValue) {
                // Soql values are always indeterminant since we don't know what was loaded
                loadedValue = getIndeterminantArrayLoadValue(apexValue);
            } else if (apexValue instanceof ApexForLoopValue) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("TODO: vertex=" + vertex + ", apexValue=" + apexValue);
                }
            } else {
                throw new UnexpectedException(vertex);
            }
        }

        if (loadedValue == null) {
            loadedValue = getIndeterminantArrayLoadValue(apexValue);
        }

        if (key != null) {
            // TODO: This logic is repeated in too many places
            if (key.contains(".")) {
                final String[] keys = key.split("\\.");
                if (keys.length != 2) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(
                                "TODO: Handle multiple levels. vertex="
                                        + vertex
                                        + " , keys="
                                        + keys);
                    }
                    return;
                }
                final ApexValue<?> apexValueForAssignment = getApexValue(keys[0]).orElse(null);
                final String propertyName = keys[1];
                if (apexValueForAssignment instanceof ApexClassInstanceValue) {
                    final ApexClassInstanceValue apexClassInstanceValue =
                            (ApexClassInstanceValue) apexValueForAssignment;
                    apexClassInstanceValue
                            .getClassInstanceScope()
                            .updateVariable(propertyName, loadedValue, this);
                } else if (apexValueForAssignment instanceof ObjectProperties) {
                    final ObjectProperties objectProperties =
                            (ObjectProperties) apexValueForAssignment;
                    final ApexStringValue stringKey =
                            ApexValueBuilder.getWithoutSymbolProvider().buildString(propertyName);
                    objectProperties.putApexValue(stringKey, loadedValue);
                }
            } else {
                updateVariable(key, loadedValue, this);
            }
        } else {
            updateInvocableResults(vertex, loadedValue);
        }
    }

    /**
     * Returns the value of {@link ForLoopStatementVertex#isSupportedSimpleIncrementingType(String,
     * String)} if {@code vertex} is within a for loop. @return value from {@link
     * ForLoopStatementVertex#isSupportedSimpleIncrementingType(String, String)} if
     * isSupportedSimpleIncrementingType can be called, else false.
     */
    private boolean isSimpleIncrementingTypeForLoop(ArrayLoadExpressionVertex vertex) {
        Optional<ForLoopStatementVertex> optForLoopStatement =
                vertex.getFirstParentOfType(ASTConstants.NodeType.FOR_LOOP_STATEMENT);
        ForLoopStatementVertex forLoopStatement = optForLoopStatement.orElse(null);
        if (forLoopStatement != null) {
            ChainedVertex arrayVertex = vertex.getArray();
            ChainedVertex indexVertex = vertex.getIndex();
            if (arrayVertex instanceof VariableExpressionVertex
                    && indexVertex instanceof VariableExpressionVertex.Single) {
                VariableExpressionVertex arrayVariable = (VariableExpressionVertex) arrayVertex;
                VariableExpressionVertex indexVariable = (VariableExpressionVertex) indexVertex;
                return forLoopStatement.isSupportedSimpleIncrementingType(
                        arrayVariable.getName(), indexVariable.getName());
            }
        }
        return false;
    }

    /** Generates a single indeterminant value based on the type contained in {@code apexValue} */
    private ApexValue<?> getIndeterminantArrayLoadValue(@Nullable ApexValue<?> apexValue) {
        final Typeable listType;
        if (apexValue instanceof ApexListValue) {
            ApexListValue apexListValue = (ApexListValue) apexValue;
            listType = apexListValue.getListType().orElse(null);
        } else if (apexValue instanceof ApexForLoopValue) {
            ApexForLoopValue apexForLoopValue = (ApexForLoopValue) apexValue;
            listType = apexForLoopValue.getTypeVertex().orElse(null);
        } else if (apexValue instanceof ApexSoqlValue) {
            ApexSoqlValue apexSoqlValue = (ApexSoqlValue) apexValue;
            String objectName = apexSoqlValue.getDefiningType().orElse(null);
            if (objectName != null) {
                listType = SyntheticTypedVertex.get(objectName);
            } else {
                listType = null;
            }
        } else if (apexValue == null) {
            listType = null;
        } else {
            throw new UnexpectedException(apexValue);
        }

        ApexValueBuilder builder = ApexValueBuilder.get(this).withStatus(ValueStatus.INDETERMINANT);
        if (listType != null) {
            return builder.declarationVertex(listType).build();
        } else {
            return builder.buildUnknownType();
        }
    }

    @Override
    public void afterVisit(AssignmentExpressionVertex vertex) {
        VariableExpressionVertex lhs = vertex.getLhs();
        ChainedVertex rhs = vertex.getRhs();
        final String key = lhs.getName();

        // Account a = new Account();
        // a.Name = 'Acme Inc.';
        // Anything that has a dot
        boolean lhsComplexAssignment = !lhs.getChainedNames().isEmpty();

        String apexValueKey;
        if (lhsComplexAssignment) {
            if (lhs.getChainedNames().size() > 1) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("TODO: Handle multiple levels. vertex=" + vertex + " , lhs=" + lhs);
                }
                return;
            }
            apexValueKey = lhs.getChainedNames().get(0);
        } else {
            apexValueKey = key;
        }

        ApexValue<?> apexValue;
        if (lhs.isThisReference()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Looking for closest instance scope. lhs=" + lhs);
            }
            apexValue =
                    getClosestClassInstanceScope().get().getInstanceApexValue(apexValueKey).get();
        } else {
            apexValue = getApexValue(apexValueKey).orElse(null);
        }

        if (apexValue == null && lhs instanceof VariableExpressionVertex.Single) {
            // Handle cases such as getCustomSetting().CustomField__c = true;
            // <VariableExpression BeginColumn='31' BeginLine='3' DefiningType='MyClass' EndLine='3'
            // Image='CustomField__c' KeyName='' RealLoc='true'>
            //    <ReferenceExpression BeginColumn='31' BeginLine='3' Context=''
            // DefiningType='MyClass' EndLine='3' Image='' Names='[]' RealLoc='false'
            // ReferenceType='STORE' SafeNav='false'>
            //        <MethodCallExpression BeginColumn='8' BeginLine='3' DefiningType='MyClass'
            // EndLine='3' FullMethodName='getCustomSetting' KeyName=''
            // MethodName='getCustomSetting' RealLoc='true'>
            //        </MethodCallExpression>
            //    </ReferenceExpression>
            // </VariableExpression>
            VariableExpressionVertex.Single variableExpression =
                    (VariableExpressionVertex.Single) lhs;
            if (variableExpression.getReferenceExpression() instanceof ReferenceExpressionVertex) {
                ReferenceExpressionVertex referenceExpression =
                        (ReferenceExpressionVertex) variableExpression.getReferenceExpression();
                if (referenceExpression
                        .getReferenceType()
                        .equals(ASTConstants.ReferenceType.STORE)) {
                    List<BaseSFVertex> children = referenceExpression.getChildren();
                    if (children.size() == 1
                            && children.get(0) instanceof MethodCallExpressionVertex) {
                        apexValue =
                                getReturnedValue((MethodCallExpressionVertex) children.get(0))
                                        .orElse(null);
                        if (apexValue != null) {
                            lhsComplexAssignment = true;
                        }
                    }
                }
            }
        }

        // contacts[i].CustomField__c = 'something';
        ArrayLoadExpressionVertex arrayLoadExpression =
                lhs.getArrayLoadExpressionVertex().orElse(null);
        if (arrayLoadExpression != null) {
            ChainedVertex array = arrayLoadExpression.getArray();
            if (array instanceof VariableExpressionVertex) {
                ApexValue<?> apexValueArray =
                        getApexValue((VariableExpressionVertex) array).orElse(null);
                if (apexValueArray instanceof ComplexAssignable) {
                    // TODO: Assign should take an ApexValue
                    ((ComplexAssignable) apexValueArray).assign(lhs, rhs, this);
                }
            }
        } else if (rhs instanceof MethodCallExpressionVertex) {
            // Handled by #afterVisit(MethodCallExpressionVertex)
        } else if (rhs instanceof NewObjectExpressionVertex) {
            // Handled by #afterVisit(NewObjectExpressionVertex)
        } else if (rhs instanceof VariableExpressionVertex.Single
                && invocableResults.peek().containsKey(rhs)) {
            // Handled by #afterMethodCall
        } else if (apexValue == null) {
            // This can happen when traversing starts at a method and that method body overwrites a
            // method parameter.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ApexValue is null. vertex=" + vertex);
            }
            if (!lhsComplexAssignment) {
                apexValue =
                        ApexValueBuilder.get(this).valueVertex(rhs).buildOptional().orElse(null);
                if (apexValue != null) {
                    updateVariable(apexValueKey, apexValue, this);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "ApexValue was null. vertex="
                                        + vertex
                                        + ", initializeValue="
                                        + apexValue);
                    }
                }
            }
        } else {
            ApexValue<?> newApexValue = getApexPropertyValue(rhs).orElse(null);

            if (newApexValue == null) {
                // This is a simple assignment such as "s = 'foo';"
                newApexValue =
                        getApexValueForDeclaration(apexValue.getTypeVertex().orElse(null), rhs);
            }

            if (lhs instanceof VariableExpressionVertex.SelfReferentialInstanceProperty) {
                // SelfReferentialProperty needs special handling to distinguish assignment within
                // the property method
                // body and assignment from within an instance method.
                Optional<AbstractClassInstanceScope> closestScope = getClosestClassInstanceScope();
                if (!closestScope.isPresent()) {
                    throw new UnexpectedException(
                            "this="
                                    + this
                                    + ", key="
                                    + key
                                    + ", newApexValue="
                                    + newApexValue
                                    + ", vertex="
                                    + vertex);
                }
                closestScope.get().updateProperty(key, newApexValue);
            } else if (lhs instanceof VariableExpressionVertex.SelfReferentialStaticProperty) {
                ClassStaticScope classStaticScope =
                        ContextProviders.CLASS_STATIC_SCOPE
                                .get()
                                .getClassStaticScope(lhs.getDefiningType())
                                .get();
                classStaticScope.updateProperty(key, newApexValue);
            } else if (lhs.isThisReference()) {
                Optional<AbstractClassInstanceScope> closestScope = getClosestClassInstanceScope();
                if (!closestScope.isPresent()) {
                    throw new UnexpectedException(
                            "this="
                                    + this
                                    + ", key="
                                    + key
                                    + ", newApexValue="
                                    + newApexValue
                                    + ", vertex="
                                    + vertex);
                }
                closestScope.get().updateInstanceApexValue(key, newApexValue);
            } else {
                if (lhsComplexAssignment) {
                    if (apexValue instanceof ApexClassInstanceValue) {
                        ((ApexClassInstanceValue) apexValue)
                                .getClassInstanceScope()
                                .updateInstanceApexValue(key, newApexValue);
                    } else if (apexValue instanceof ComplexAssignable) {
                        ((ComplexAssignable) apexValue).assign(lhs, rhs, this);
                    } else {
                        throw new UnexpectedException(
                                "apexValue=" + apexValue + ", vertex=" + vertex);
                    }
                } else {
                    updateVariable(key, newApexValue, this);
                }
            }
        }
    }

    /**
     * Set the pending assignment to the last MethodCallExpression so that the assignment only
     * occurs after the last method has been invoked. For example, we only want the #getName method
     * in the following code to set the assignment. MySingleton.getInstance().getName();
     */
    protected String setPendingAssignment(String key, InvocableVertex invocable) {
        return pendingInvocableAssignments.put(invocable.getLast(), key);
    }

    @Override
    public boolean visit(BaseSFVertex vertex) {
        trackVisited(vertex);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Using default for vertex=" + vertex.getClass().getSimpleName());
        }
        return true;
    }

    @Override
    public boolean visit(BlockStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(CatchBlockStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(DmlDeleteStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(DmlInsertStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(DmlUndeleteStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(DmlUpdateStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(DmlUpsertStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(ElseWhenBlockVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(EmptyReferenceExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(ExpressionStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(FieldDeclarationStatementsVertex vertex) {
        // This should be parsed by a different scope
        throw new UnexpectedException(vertex);
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex) {
        // This should be parsed by a different scope
        throw new UnexpectedException(vertex);
    }

    @Override
    public boolean visit(FieldVertex vertex) {
        // This should be parsed by a different scope
        throw new UnexpectedException(vertex);
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(IdentifierCaseVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(IfBlockStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(IfElseBlockStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(LiteralCaseVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(LiteralExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    private Optional<AbstractClassInstanceScope> resolveClassInstanceScope(String key) {
        ApexValue<?> apexValue = getApexValue(key).orElse(null);

        if (apexValue instanceof ApexClassInstanceValue) {
            // This could be an ApexSingleValue in the case that the declaration and assignment are
            // on separate lines.
            // In that case we will return an empty since the classInstance hasn't been created yet.
            return Optional.of(((ApexClassInstanceValue) apexValue).getClassInstanceScope());
        }

        return Optional.empty();
    }

    @Override
    public boolean visit(MethodCallExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex) {
        // This needs to occur in #afterVisit because the MethodCallExpressionVertex may take other
        // method calls as parameters
        ApexValue<?> newApexValue = handleApexValueMethod(vertex).orElse(null);
        if (newApexValue == null) {
            newApexValue = ApexStandardLibraryUtil.getStandardType(g, vertex, this).orElse(null);
        }

        String key = pendingInvocableAssignments.remove(vertex);
        if (key != null) {
            if (key.contains(".")) {
                // TODO: this needs to happen for all the other places that use
                // pendingAssignments.remove
                String[] keys = key.split("\\.");
                if (keys.length != 2) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("TODO: key=" + key + ", vertex=" + vertex);
                    }
                } else {
                    ApexValue<?> value = getApexValue(keys[0]).orElse(null);
                    if (value instanceof ApexClassInstanceValue) {
                        if (newApexValue == null) {
                            // The method call did not resolve to a method. Assign a value based the
                            // result of
                            // #getApexValueForDeclaration
                            Typeable typeable = getTypedVertex(key).orElse(null);
                            if (typeable != null) {
                                newApexValue =
                                        ((ApexClassInstanceValue) value)
                                                .getClassInstanceScope()
                                                .getApexValueForDeclaration(typeable, vertex);
                            }
                        }
                        if (newApexValue != null) {
                            ((ApexClassInstanceValue) value)
                                    .getClassInstanceScope()
                                    .updateInstanceApexValue(key, newApexValue);
                        }
                    }
                }
            } else {
                if (newApexValue == null) {
                    // The method call did not resolve to a method. Assign a value based the result
                    // of
                    // #getApexValueForDeclaration
                    Typeable typeable = getTypedVertex(key).orElse(null);
                    if (typeable != null) {
                        newApexValue = getApexValueForDeclaration(typeable, vertex);
                    }
                }
                if (newApexValue != null) {
                    // Update the variable with the new value
                    updateVariable(key, newApexValue, this);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Removing method that wasn't resolved. key=" + key + ", vertex=" + vertex);
            }
        }

        // Store the ApexValue in the method results map if it hasn't already been stored by
        // #afterMethodCall
        // This occurs when the method is implemented by an ApexValue, but doesn't have a method in
        // a .cls file, i.e. Map#get
        if (newApexValue != null && !invocableResults.peek().containsKey(vertex)) {
            updateInvocableResults(vertex, newApexValue);
        }

        // Store the ApexValue in the chainedApexValueStack if it hasn't already been stored by
        // #afterMethodCall.
        // This occurs when the method is implemented by an ApexValue, but doesn't have a method in
        // a .cls file, i.e. Map#get
        InvocableVertex next = vertex.getNext().orElse(null);
        if (next != null && !chainedApexValueStack.peek().containsKey(next)) {
            // Schema.getGlobalDescribe().get('Foo__c').getDescribe();
            chainedApexValueStack.peek().put(next, newApexValue);
        }

        // Remove the chained method call
        if (chainedApexValueStack.peek().containsKey(vertex)) {
            chainedApexValueStack.peek().remove(vertex);
        }
    }

    @Override
    public void afterVisit(NewListInitExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    @Override
    public void afterVisit(NewListLiteralExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    @Override
    public void afterVisit(NewMapInitExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    @Override
    public void afterVisit(NewMapLiteralExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    @Override
    public void afterVisit(NewObjectExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
        // Adding new instance creation value to invocableResults. This one-off addition is needed
        // only for NewObjectExpressionVertex since it creates an ApexValue that needs to be
        // available
        // for lookup.
        if (!invocableResults.contains(vertex)) {
            updateInvocableResults(
                    vertex,
                    ApexValueBuilder.get(this)
                            .declarationVertex(vertex)
                            .valueVertex(vertex)
                            .build());
        }
    }

    @Override
    public void afterVisit(NewSetInitExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    @Override
    public void afterVisit(NewSetLiteralExpressionVertex vertex) {
        updateInvocableWithParametersVertex(vertex);
    }

    /**
     * Updates the value of an {@link InvocableWithParametersVertex} after it has been visited. This
     * is required because the vertex may accept other InvocableWithParametersVertices or
     * MethodCallExpressions that are only resolvable after the vertex is visited.
     */
    private void updateInvocableWithParametersVertex(InvocableWithParametersVertex vertex) {
        // This needs to occur in #afterVisit because the InvocableWithParametersVertex may take
        // other method calls as parameters
        String key = pendingInvocableAssignments.remove(vertex);
        if (key != null) {
            String[] keys = null;
            if (key.contains(".")) {
                keys = key.split("\\.");
                if (keys.length != 2) {
                    throw new TodoException(vertex);
                }
            }
            final ApexValue<?> newApexValue;
            if (chainedApexValueStack.peek().containsKey(vertex)) {
                // Find the ApexClassInstance that was instantiated and apply it to the variable
                newApexValue = chainedApexValueStack.peek().get(vertex);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Assigning ApexClassInstance. key=" + key + ", vertex=" + vertex);
                }
            } else {
                // Assign a new ApexValue to the variable. This happens when the new object doesn't
                // correspond to a
                // class instance
                Typeable typeable = getTypedVertex(key).orElse(null);
                if (typeable == null) {
                    // TODO: This is occurring when the rhs is a new List/Map/Set, write test
                    if (vertex instanceof Typeable) {
                        typeable = (Typeable) vertex;
                    } else {
                        throw new UnexpectedException(vertex);
                    }
                }
                newApexValue =
                        getApexValueBuilderForDeclaration(typeable, vertex)
                                .withStatus(ValueStatus.INITIALIZED)
                                .build();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Assigning ApexValue. key=" + key + ", vertex=" + vertex);
                }
            }

            if (keys != null) {
                // TODO: this needs to happen for all the other places that use
                // pendingAssignments.remove
                ApexValue<?> value = getApexValue(keys[0]).orElse(null);
                if (value instanceof ApexClassInstanceValue) {
                    ((ApexClassInstanceValue) value)
                            .getClassInstanceScope()
                            .updateInstanceApexValue(key, newApexValue);
                }
            } else {
                updateVariable(key, newApexValue, this);
            }
        }

        // Remove the chained method call
        if (chainedApexValueStack.peek().containsKey(vertex)) {
            chainedApexValueStack.peek().remove(vertex);
        }
    }

    @Override
    public void afterVisit(ThrowStatementVertex vertex) {}

    /**
     * Call {@link ApexValue#apply(MethodCallExpressionVertex, SymbolProvider)} if the method call
     * is being performed on an ApexValue.
     *
     * @return any value returned from the invocation
     */
    private Optional<ApexValue<?>> handleApexValueMethod(MethodCallExpressionVertex vertex) {
        ApexValue<?> apexValue = null;

        InvocableWithParametersVertex previous = vertex.getPrevious().orElse(null);
        if (previous instanceof SoqlExpressionVertex) {
            // Handle chained SOQL expressions that were not assigned to a variable
            // i.e. return [SELECT Id FROM MyObject__c].isEmpty();
            apexValue = ApexValueBuilder.get(this).valueVertex(previous).buildSoql();
        } else if (previous != null) {
            // The method is being called on a chain
            // SObjectType sot = Schema.getGlobalDescribe().get('Account');
            apexValue = getReturnedValue(previous).orElse(null);
        } else {
            String symbolicName = vertex.getSymbolicName().orElse(null);
            if (symbolicName != null) {
                if (vertex.isThisReference()) {
                    // Handles method call that is prefixed with "this", i.e. this.aList.size()
                    apexValue =
                            getClosestClassInstanceScope()
                                    .get()
                                    .getInstanceApexValue(symbolicName)
                                    .get();
                } else {
                    // The method is being called on a variable
                    // Map<String, Schema.SObjectType> gd = Schema.getGlobalDescribe()
                    // SObjectType sot = gd.get('Account');
                    apexValue = getApexValue(symbolicName).orElse(null);
                }
            }
        }

        if (apexValue != null) {
            // This will check for null access and throw an exception if this would result in a
            // RuntimeException during
            // production execution
            apexValue.checkForNullAccess(vertex, this);
            return apexValue.apply(vertex, this);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean visit(MethodVertex.ConstructorVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(MethodVertex.InstanceMethodVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(ModifierNodeVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(NewKeyValueObjectExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(NewListLiteralExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(NewObjectExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(ParameterVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(PrefixExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(ReferenceExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(StandardConditionVertex.Unknown vertex) {
        // This should have been resolved already
        throw new UnexpectedException(vertex);
    }

    @Override
    public boolean visit(SuperMethodCallExpressionVertex vertex) {
        return true;
    }

    @Override
    public boolean visit(SwitchStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(ThrowStatementVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(TypeWhenBlockVertex vertex) {
        // Instance of switch statements upcast the switch expression and introduce a new variable.
        // TODO: This is an incomplete solution. It should create a new value that has the more
        // specific type
        // and delegates all calls to the original value. This requires a new ApexValue type and
        // lots of changes to the
        // hierarchy. It is not unique to this use case. It also exists when an object is upcast and
        // assigned
        final ChainedVertex switchExpression = vertex.getExpressionVertex();
        final ApexValue<?> apexValue =
                ScopeUtil.resolveToApexValue(this, switchExpression).orElse(null);
        final String variableName = vertex.getVariableName();
        apexValueStack.peek().put(variableName, apexValue);
        ;
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(ValueWhenBlockVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationVertex vertex) {
        trackVisited(vertex);

        final String key = vertex.getName();
        if (apexValueStack.peek().containsKey(key)) {
            // The variable was defined multiple times
            throw new UserActionException(
                    UserFacingMessages.VARIABLE_DECLARED_MULTIPLE_TIMES,
                    vertex.getFileName(),
                    vertex.getDefiningType(),
                    vertex.getBeginLine());
        }

        ChainedVertex rhs = vertex.getRhs().orElse(null);
        ApexValue<?> apexValue = getApexPropertyValue(rhs).orElse(null);

        if (apexValue == null) {
            apexValue = getApexValueForDeclaration(vertex, rhs);
        }

        InvocableVertex invocableVertex = null;
        if (rhs instanceof InvocableVertex) {
            invocableVertex = (InvocableVertex) rhs;
        } else if (rhs instanceof CastExpressionVertex) {
            // InnerClass ic = (InnerClass)JSON.deserialize(asJson, InnerClass.class)
            CastExpressionVertex castExpression = (CastExpressionVertex) rhs;
            if (castExpression.getOnlyChild() instanceof InvocableVertex) {
                invocableVertex = castExpression.getOnlyChild();
            }
        }

        if (invocableVertex != null) {
            String previous = setPendingAssignment(key, invocableVertex);
            if (previous != null) {
                throw new UnexpectedException(vertex);
            }
        }

        apexValueStack.peek().put(key, apexValue);
        return true;
    }

    @Override
    public void afterVisit(VariableDeclarationVertex vertex) {
        // TODO: More consistent handling of Declaration and Assignment to avoid duplication
        ChainedVertex rhs = vertex.getRhs().orElse(null);
        if (rhs instanceof BinaryExpressionVertex) {
            final String key = vertex.getName();
            ApexValue<?> apexValue = getApexValueForDeclaration(vertex, rhs);
            updateVariable(key, apexValue, this);
        }
    }

    @Override
    public boolean visit(VariableDeclarationStatementsVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public boolean visit(VariableExpressionVertex.ForLoop vertex) {
        trackVisited(vertex);
        String key = vertex.getName();
        ApexValue<?> apexValue = getApexValue(key).get();

        ApexValue<?> newValue =
                ApexValueBuilder.get(this)
                        .declarationVertex(apexValue.getTypeVertex().get())
                        .valueVertex(vertex)
                        .build();
        apexValueStack.peek().put(key, newValue);
        return true;
    }

    @Override
    public boolean visit(VariableExpressionVertex.Single vertex) {
        return visitVariableExpression(vertex);
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex) {
        trackVisited(vertex);
        return false;
    }

    @Override
    public void afterVisit(VariableExpressionVertex.Single vertex) {}

    private boolean visitVariableExpression(VariableExpressionVertex vertex) {
        trackVisited(vertex);
        return true;
    }

    @Override
    public void afterVisit(BaseSFVertex vertex) {}

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex) {}

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex) {}

    @Override
    public void afterVisit(FieldDeclarationVertex vertex) {
        // This should be parsed by a different scope
        throw new UnexpectedException(vertex);
    }

    @Override
    public void afterVisit(SoqlExpressionVertex vertex) {}

    /**
     * Find the closest scope with a non-empty {@link #methodInvocationStack}. This is the scope
     * that should have return values assigned to it.
     */
    protected Optional<MethodInvocationScope> getClosestMethodInvocationScope() {
        if (!methodInvocationStack.isEmpty()) {
            return Optional.of(methodInvocationStack.peek());
        } else if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().getClosestMethodInvocationScope();
        } else {
            throw new UnexpectedException(this);
        }
    }

    /**
     * Find the scope that is a ClassInstanceScope. This is the scope that should resolve "this"
     * values
     */
    @Override
    public Optional<AbstractClassInstanceScope> getClosestClassInstanceScope() {
        if (getInheritedScope().isPresent()) {
            return getInheritedScope().get().getClosestClassInstanceScope();
        } else {
            return Optional.empty();
        }
    }

    protected void trackVisited(BaseSFVertex vertex) {
        // Intentionally left blank.
    }

    protected ApexValueBuilder getApexValueBuilderForDeclaration(
            Typeable vertex, ChainedVertex rhs) {
        return ApexValueBuilder.get(this)
                .declarationVertex(vertex)
                // Declarations with an assignment are set to INDETERMINANT, this will be updated if
                // the rhs is executed.
                // String x; 					ApexValue.Status.UNINITIALIZED
                // String x = SomeMethod();		ApexValue.Status.INDETERMINANT
                .withStatus(rhs == null ? ValueStatus.UNINITIALIZED : ValueStatus.INDETERMINANT)
                .valueVertex(rhs);
    }

    protected ApexValue<?> getApexValueForDeclaration(Typeable vertex, ChainedVertex rhs) {
        return getApexValueBuilderForDeclaration(vertex, rhs).build();
    }

    /**
     * Handle cases where the right hand side of an assignment or declaration is a Apex Property
     * getter MyClass c = new MyClass(); c.aString = 'Goodbye'; String s = c.aString;
     */
    protected Optional<ApexValue<?>> getApexPropertyValue(ChainedVertex vertex) {
        if (vertex instanceof VariableExpressionVertex.Single) {
            VariableExpressionVertex variableExpression = (VariableExpressionVertex) vertex;
            ApexValue<?> apexValue =
                    getApexValue((VariableExpressionVertex.Single) vertex).orElse(null);
            if (apexValue instanceof ApexClassInstanceValue) {
                boolean complexAssignment = !vertex.getChainedNames().isEmpty();
                if (complexAssignment) {
                    if (vertex.getChainedNames().size() > 1) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("TODO: Handle multiple levels. vertex=" + vertex);
                        }
                    } else {
                        String apexValueKey = variableExpression.getName();
                        return ((ApexClassInstanceValue) apexValue)
                                .getClassInstanceScope()
                                .getApexValue(apexValueKey);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{"
                + ", methodParametersStack="
                + methodInvocationStack
                + '}';
    }
}
