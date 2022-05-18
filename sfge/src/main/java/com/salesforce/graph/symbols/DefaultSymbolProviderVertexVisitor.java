package com.salesforce.graph.symbols;

import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.ArrayLoadExpressionVertex;
import com.salesforce.graph.vertex.AssignmentExpressionVertex;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
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
import com.salesforce.graph.vertex.ReturnStatementVertex;
import com.salesforce.graph.vertex.SoqlExpressionVertex;
import com.salesforce.graph.vertex.StandardConditionVertex;
import com.salesforce.graph.vertex.SuperMethodCallExpressionVertex;
import com.salesforce.graph.vertex.SwitchStatementVertex;
import com.salesforce.graph.vertex.ThrowStatementVertex;
import com.salesforce.graph.vertex.TryCatchFinallyBlockStatementVertex;
import com.salesforce.graph.vertex.TypeWhenBlockVertex;
import com.salesforce.graph.vertex.Typeable;
import com.salesforce.graph.vertex.ValueWhenBlockVertex;
import com.salesforce.graph.vertex.VariableDeclarationStatementsVertex;
import com.salesforce.graph.vertex.VariableDeclarationVertex;
import com.salesforce.graph.vertex.VariableExpressionVertex;
import com.salesforce.graph.vertex.WhileLoopStatementVertex;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Visits all paths in a path before the {@link com.salesforce.graph.visitor.PathVertexVisitor}. */
@SuppressWarnings(
        "PMD.UnusedFormalParameter") // Future implementations may need the currently unused
// parameter
public final class DefaultSymbolProviderVertexVisitor
        implements SymbolProviderVertexVisitor,
                SymbolProvider,
                DeepCloneable<DefaultSymbolProviderVertexVisitor> {
    private static final Logger LOGGER =
            LogManager.getLogger(DefaultSymbolProviderVertexVisitor.class);
    private final GraphTraversalSource g;

    private final Stack<PathScopeVisitor> scopeStack;
    private final Stack<String> currentScopeLabel;

    public DefaultSymbolProviderVertexVisitor(GraphTraversalSource g) {
        this(g, null);
    }

    public DefaultSymbolProviderVertexVisitor(
            GraphTraversalSource g, PathScopeVisitor initialScope) {
        this.g = g;
        this.scopeStack = new Stack<>();
        if (initialScope != null) {
            this.scopeStack.push(initialScope);
        }
        this.currentScopeLabel = new Stack<>();
    }

    private DefaultSymbolProviderVertexVisitor(DefaultSymbolProviderVertexVisitor other) {
        this.g = other.g;
        this.scopeStack = CloneUtil.cloneStack(other.scopeStack);
        this.currentScopeLabel = CloneUtil.cloneStack(other.currentScopeLabel);
    }

    @Override
    public DefaultSymbolProviderVertexVisitor deepClone() {
        return new DefaultSymbolProviderVertexVisitor(this);
    }

    @Override
    public SymbolProvider getSymbolProvider() {
        return this;
    }

    @Override
    public Optional<ChainedVertex> getValue(String key) {
        return scopeStack.peek().getMutableSymbolProvider().getValue(key);
    }

    @Override
    public Optional<Typeable> getTypedVertex(String key) {
        return scopeStack.peek().getMutableSymbolProvider().getTypedVertex(key);
    }

    @Override
    public Optional<Typeable> getTypedVertex(List<String> keySequence) {
        return scopeStack.peek().getMutableSymbolProvider().getTypedVertex(keySequence);
    }

    @Override
    public Optional<ChainedVertex> getValue(ChainedVertex value) {
        return scopeStack.peek().getMutableSymbolProvider().getValue(value);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(VariableExpressionVertex var) {
        return scopeStack.peek().getMutableSymbolProvider().getApexValue(var);
    }

    @Override
    public Optional<ApexValue<?>> getApexValue(String key) {
        return scopeStack.peek().getMutableSymbolProvider().getApexValue(key);
    }

    @Override
    public Optional<ApexValue<?>> getReturnedValue(InvocableVertex vertex) {
        return scopeStack.peek().getMutableSymbolProvider().getReturnedValue(vertex);
    }

    @Override
    public ChainedVertex getValueAtTimeOfInvocation(InvocableVertex vertex, ChainedVertex value) {
        return scopeStack
                .peek()
                .getMutableSymbolProvider()
                .getValueAtTimeOfInvocation(vertex, value);
    }

    @Override
    public Optional<AbstractClassInstanceScope> getClosestClassInstanceScope() {
        return scopeStack.peek().getMutableSymbolProvider().getClosestClassInstanceScope();
    }

    @Override
    public SymbolProvider start(BaseSFVertex vertex) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start. vertex=" + vertex);
        }

        SymbolProvider started;
        if (scopeStack.isEmpty()) {
            String className = vertex.getDefiningType();
            if (StringUtils.isEmpty(className)) {
                throw new UnexpectedException(vertex);
            }

            if (vertex instanceof MethodVertex) {
                throw new UnexpectedException(vertex);
            }

            boolean isStatic;
            MethodVertex method = null;
            if (vertex instanceof FieldDeclarationVertex) {
                FieldDeclarationVertex fieldDeclaration = (FieldDeclarationVertex) vertex;
                isStatic = fieldDeclaration.isStatic();
            } else {
                // Handle the case where we have started analysis inside of a method that may take
                // parameters. The
                // initial scope consists of the method parameters set to indeterminant. And any
                // final variables that are assigned
                // inline to the class that implements the method.
                method = vertex.getParentMethod().get();
                isStatic = method.isStatic();
                // Parse final variables for any new class types. This will only cover final
                // variables that are set outside
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Starting with a class scope. className="
                                    + className
                                    + ", method="
                                    + method.getName()
                                    + ", line="
                                    + method.getBeginLine());
                }
            }

            AbstractClassScope classScope;
            // Create the correct class scope based on the method being invoked. Static methods only
            // have access to
            // static member variables and instance members have access to instance variables. The
            // correct class needs
            // to be used because the AST differs between the two cases.
            if (isStatic) {
                classScope =
                        ContextProviders.CLASS_STATIC_SCOPE
                                .get()
                                .getClassStaticScope(className)
                                .get();
            } else {
                classScope = ClassInstanceScope.get(g, className);
            }

            if (method != null) {
                MethodInvocationScope methodInvocationScope =
                        MethodUtil.getIndeterminantMethodInvocationScope(method);
                classScope.pushMethodInvocationScope(methodInvocationScope);
            }
            scopeStack.push(classScope);
            started = classScope;
        } else if (scopeStack.size() == 1 && scopeStack.peek() instanceof AbstractClassScope) {
            // TODO: Document why this is needed. Maybe it can be removed.
            // ApexClassInstanceValueTest#testJSONDeserializeFieldsAreIndeterminant fails if removed
            started = scopeStack.peek();
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting with an inherited scope");
            }
            InnerScope innerScope = new InnerScope(g, scopeStack.peek(), vertex);
            scopeStack.push(innerScope);
            started = innerScope;
        }

        return started;
    }

    @Override
    public PathScopeVisitor beforeMethodCall(InvocableVertex invocable, MethodVertex method) {
        // Get the parameters that will be pushed on the stack
        MethodInvocationScope methodInvocationScope =
                invocable.resolveInvocationParameters(
                        method, scopeStack.peek().getMutableSymbolProvider());

        // Check with the current scope to see if it can determine which scope implements the method
        PathScopeVisitor scopeThatImplementsMethod =
                scopeStack.peek().getImplementingScope(invocable, method).orElse(null);
        if (scopeThatImplementsMethod != null) {
            // The current scope was able to resolve the implementer of the method. Use that scope
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Using scope returned. scope=" + scopeThatImplementsMethod);
            }
        } else {
            // TODO: Efficiency. Cache these?
            // Nothing within the current scope implements the method. This must be a static call to
            // another
            // class. Create a new ClassMethodScopeVisitor
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating new scope. method=" + method);
            }
            scopeThatImplementsMethod =
                    ContextProviders.CLASS_STATIC_SCOPE
                            .get()
                            .getClassStaticScope(method.getDefiningType())
                            .get();
        }

        scopeStack.push(scopeThatImplementsMethod);
        scopeStack.peek().pushMethodInvocationScope(methodInvocationScope);

        return scopeThatImplementsMethod;
    }

    @Override
    public Optional<ApexValue<?>> afterMethodCall(InvocableVertex invocable, MethodVertex method) {
        PathScopeVisitor pathScopeVisitor = scopeStack.pop();
        MethodInvocationScope methodInvocationScope =
                pathScopeVisitor.popMethodInvocationScope(invocable);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "afterMethodCall. method="
                            + method
                            + ", returnValue="
                            + methodInvocationScope.getReturnValue());
        }
        return scopeStack
                .peek()
                .afterMethodCall(
                        invocable, method, methodInvocationScope.getReturnValue().orElse(null));
    }

    @Override
    public boolean visit(ArrayLoadExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(AssignmentExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(BaseSFVertex vertex) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Using generic BaseSFVertex for vertex=" + vertex.getClass().getSimpleName());
        }
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(BlockStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(CatchBlockStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(DmlDeleteStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(DmlInsertStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(DmlUndeleteStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(DmlUpdateStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(DmlUpsertStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ElseWhenBlockVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(EmptyReferenceExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ExpressionStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(FieldDeclarationStatementsVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(FieldDeclarationVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(FieldVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ForEachStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ForLoopStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(IdentifierCaseVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(IfBlockStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(IfElseBlockStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(LiteralCaseVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(LiteralExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(MethodCallExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(MethodVertex.ConstructorVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(MethodVertex.InstanceMethodVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    public boolean visit(ModifierNodeVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(NewKeyValueObjectExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(NewListLiteralExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(NewObjectExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ParameterVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(PrefixExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ReferenceExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ReturnStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(StandardConditionVertex.Negative vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(StandardConditionVertex.Positive vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(StandardConditionVertex.Unknown vertex) {
        throw new UnexpectedException(
                "Vertex should be resolved before walking paths. vertex=" + vertex);
    }

    @Override
    public boolean visit(SuperMethodCallExpressionVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(SwitchStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ThrowStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(TypeWhenBlockVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(ValueWhenBlockVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(VariableDeclarationVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(VariableDeclarationStatementsVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(VariableExpressionVertex.ForLoop vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(VariableExpressionVertex.Single vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public boolean visit(WhileLoopStatementVertex vertex) {
        preScopeVisit(vertex);
        boolean result = scopeStack.peek().visit(vertex);
        postScopeVisit(vertex);
        return result;
    }

    @Override
    public void afterVisit(ArrayLoadExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(AssignmentExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(BaseSFVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlDeleteStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlInsertStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUndeleteStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUpdateStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(DmlUpsertStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(FieldDeclarationVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(MethodCallExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewListInitExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewListLiteralExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewMapInitExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewMapLiteralExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewObjectExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewSetInitExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(NewSetLiteralExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(ReturnStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(SoqlExpressionVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(ThrowStatementVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(VariableDeclarationVertex vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    @Override
    public void afterVisit(VariableExpressionVertex.Single vertex) {
        preScopeAfterVisit(vertex);
        scopeStack.peek().afterVisit(vertex);
        postScopeAfterVisit(vertex);
    }

    private void preScopeVisit(BaseSFVertex vertex) {
        if (vertex.startsInnerScope()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Pushing inner scope. vertex=" + vertex);
            }
            currentScopeLabel.push(vertex.getLabel());
            PathScopeVisitor inheritedScope = this.scopeStack.peek();
            this.scopeStack.push(new InnerScope(g, inheritedScope, vertex));
        }
    }

    private void postScopeVisit(BaseSFVertex vertex) {}

    private void preScopeAfterVisit(BaseSFVertex vertex) {}

    private void postScopeAfterVisit(BaseSFVertex vertex) {}

    @Override
    public void pushScope(ClassStaticScope scope) {
        this.scopeStack.push(scope);
    }

    @Override
    public void popScope(ClassStaticScope scope) {
        PathScopeVisitor popped = this.scopeStack.pop();
        if (popped != scope) {
            // Ensure that the popped scope is what was expected
            throw new UnexpectedException("expected=" + scope + ", actual=" + popped);
        }
    }

    @Override
    public void popScope(BaseSFVertex vertex) {
        if (!vertex.getEndScopes().isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Popping inner scopes. vertex="
                                + vertex.getLabel()
                                + ", endScopes="
                                + vertex.getEndScopes());
            }
        }
        for (String endScope : vertex.getEndScopes()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Popping inner scope. endScope=" + endScope + ", vertex=" + vertex);
            }
            String poppedScope = currentScopeLabel.pop();
            if (!poppedScope.equals(endScope)) {
                throw new UnexpectedException(
                        "Scope mismatch. expected="
                                + poppedScope
                                + ", actual="
                                + endScope
                                + ", vertex="
                                + vertex);
            }
            this.scopeStack.pop();
        }
    }
}
