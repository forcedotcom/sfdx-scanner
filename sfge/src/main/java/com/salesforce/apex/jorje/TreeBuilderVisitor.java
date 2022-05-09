package com.salesforce.apex.jorje;

import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.ast.compilation.AnonymousClass;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.compilation.UserClassMethods;
import apex.jorje.semantic.ast.compilation.UserEnum;
import apex.jorje.semantic.ast.compilation.UserExceptionMethods;
import apex.jorje.semantic.ast.compilation.UserInterface;
import apex.jorje.semantic.ast.compilation.UserTrigger;
import apex.jorje.semantic.ast.condition.StandardCondition;
import apex.jorje.semantic.ast.expression.ArrayLoadExpression;
import apex.jorje.semantic.ast.expression.ArrayStoreExpression;
import apex.jorje.semantic.ast.expression.AssignmentExpression;
import apex.jorje.semantic.ast.expression.BinaryExpression;
import apex.jorje.semantic.ast.expression.BindExpressions;
import apex.jorje.semantic.ast.expression.BooleanExpression;
import apex.jorje.semantic.ast.expression.CastExpression;
import apex.jorje.semantic.ast.expression.ClassRefExpression;
import apex.jorje.semantic.ast.expression.EmptyReferenceExpression;
import apex.jorje.semantic.ast.expression.InstanceOfExpression;
import apex.jorje.semantic.ast.expression.JavaMethodCallExpression;
import apex.jorje.semantic.ast.expression.JavaVariableExpression;
import apex.jorje.semantic.ast.expression.LiteralExpression;
import apex.jorje.semantic.ast.expression.MapEntryNode;
import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.NestedExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.NewListInitExpression;
import apex.jorje.semantic.ast.expression.NewListLiteralExpression;
import apex.jorje.semantic.ast.expression.NewMapInitExpression;
import apex.jorje.semantic.ast.expression.NewMapLiteralExpression;
import apex.jorje.semantic.ast.expression.NewObjectExpression;
import apex.jorje.semantic.ast.expression.NewSetInitExpression;
import apex.jorje.semantic.ast.expression.NewSetLiteralExpression;
import apex.jorje.semantic.ast.expression.PackageVersionExpression;
import apex.jorje.semantic.ast.expression.PostfixExpression;
import apex.jorje.semantic.ast.expression.PrefixExpression;
import apex.jorje.semantic.ast.expression.ReferenceExpression;
import apex.jorje.semantic.ast.expression.SoqlExpression;
import apex.jorje.semantic.ast.expression.SoslExpression;
import apex.jorje.semantic.ast.expression.SuperMethodCallExpression;
import apex.jorje.semantic.ast.expression.SuperVariableExpression;
import apex.jorje.semantic.ast.expression.TernaryExpression;
import apex.jorje.semantic.ast.expression.ThisMethodCallExpression;
import apex.jorje.semantic.ast.expression.ThisVariableExpression;
import apex.jorje.semantic.ast.expression.TriggerVariableExpression;
import apex.jorje.semantic.ast.expression.VariableExpression;
import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.ast.member.Property;
import apex.jorje.semantic.ast.member.bridge.BridgeMethodCreator;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.ast.modifier.AnnotationParameter;
import apex.jorje.semantic.ast.modifier.ModifierGroup;
import apex.jorje.semantic.ast.modifier.ModifierNode;
import apex.jorje.semantic.ast.statement.BlockStatement;
import apex.jorje.semantic.ast.statement.BreakStatement;
import apex.jorje.semantic.ast.statement.CatchBlockStatement;
import apex.jorje.semantic.ast.statement.ContinueStatement;
import apex.jorje.semantic.ast.statement.DmlDeleteStatement;
import apex.jorje.semantic.ast.statement.DmlInsertStatement;
import apex.jorje.semantic.ast.statement.DmlMergeStatement;
import apex.jorje.semantic.ast.statement.DmlUndeleteStatement;
import apex.jorje.semantic.ast.statement.DmlUpdateStatement;
import apex.jorje.semantic.ast.statement.DmlUpsertStatement;
import apex.jorje.semantic.ast.statement.DoLoopStatement;
import apex.jorje.semantic.ast.statement.ElseWhenBlock;
import apex.jorje.semantic.ast.statement.ExpressionStatement;
import apex.jorje.semantic.ast.statement.FieldDeclaration;
import apex.jorje.semantic.ast.statement.FieldDeclarationStatements;
import apex.jorje.semantic.ast.statement.ForEachStatement;
import apex.jorje.semantic.ast.statement.ForLoopStatement;
import apex.jorje.semantic.ast.statement.IfBlockStatement;
import apex.jorje.semantic.ast.statement.IfElseBlockStatement;
import apex.jorje.semantic.ast.statement.ReturnStatement;
import apex.jorje.semantic.ast.statement.RunAsBlockStatement;
import apex.jorje.semantic.ast.statement.SwitchStatement;
import apex.jorje.semantic.ast.statement.ThrowStatement;
import apex.jorje.semantic.ast.statement.TryCatchFinallyBlockStatement;
import apex.jorje.semantic.ast.statement.TypeWhenBlock;
import apex.jorje.semantic.ast.statement.ValueWhenBlock;
import apex.jorje.semantic.ast.statement.VariableDeclaration;
import apex.jorje.semantic.ast.statement.VariableDeclarationStatements;
import apex.jorje.semantic.ast.statement.WhenCases;
import apex.jorje.semantic.ast.statement.WhileLoopStatement;
import apex.jorje.semantic.ast.visitor.AstVisitor;
import apex.jorje.semantic.ast.visitor.NoopScope;
import com.google.common.collect.ImmutableSet;
import com.salesforce.exception.UnexpectedException;
import java.util.Set;
import java.util.Stack;

/**
 * Responsible for visiting all of the nodes in the Jorje AST tree. The visitor creates new {@link
 * AstNodeWrapper} nodes and hooks up parent/child relationships. Some nodes are excluded from the
 * tree(See bottom of file).
 */
public final class TreeBuilderVisitor extends AstVisitor<NoopScope> {
    /** The root node such as a Class, Interface, or Enum */
    private final AstNodeWrapper<?> root;

    /**
     * Keeps track of the current visitation stack in order to keep track of which node is the
     * current parent
     */
    private final Stack<AstNodeWrapper<?>> nodeStack;

    TreeBuilderVisitor(AstNodeWrapper<?> root) {
        this.root = root;
        this.nodeStack = new Stack<>();
    }

    /** Create new children and push the node on the stack as the current parent */
    private boolean defaultVisit(AstNode node) {
        final AstNodeWrapper<?> vertex;
        if (root.getNode() == node) {
            nodeStack.push(root);
        } else {
            final AstNodeWrapper<?> parent = nodeStack.peek();
            // Create a new type for this node and set its parent to the top of the stack
            vertex = AstNodeWrapperFactory.getVertex(node, parent);
            parent.addChild(vertex);
            nodeStack.push(vertex);
        }
        return true;
    }

    /** Pop the current parent off the stack and make sure the nodes match. */
    private void defaultVisitEnd(AstNode node) {
        final AstNodeWrapper<?> popped = this.nodeStack.pop();
        if (node != popped.getNode()) {
            throw new UnexpectedException(
                    "expected="
                            + node
                            + ", expectedType="
                            + node.getClass().getSimpleName()
                            + ", actual="
                            + popped.getNode()
                            + ", actualType="
                            + popped.getNode().getClass().getSimpleName());
        }
    }

    /** Return true if the parent is of the specified type */
    private boolean isParentOfType(Class<? extends AstNodeWrapper<?>> clazz) {
        return !nodeStack.isEmpty() && clazz.isInstance(nodeStack.peek());
    }

    @Override
    public boolean visit(AnonymousClass node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(AnonymousClass node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(UserClass node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserClass node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(UserEnum node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserEnum node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(UserInterface node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserInterface node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(UserTrigger node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserTrigger node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ArrayLoadExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ArrayLoadExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ArrayStoreExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ArrayStoreExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(AssignmentExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(AssignmentExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(BinaryExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(BinaryExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(BooleanExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(BooleanExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ClassRefExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ClassRefExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(CastExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(CastExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(InstanceOfExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(InstanceOfExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(JavaMethodCallExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(JavaMethodCallExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(JavaVariableExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(JavaVariableExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(LiteralExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(LiteralExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ReferenceExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ReferenceExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(EmptyReferenceExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(EmptyReferenceExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(MethodCallExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(MethodCallExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewListInitExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewListInitExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewMapInitExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewMapInitExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewSetInitExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewSetInitExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewListLiteralExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewListLiteralExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewSetLiteralExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewSetLiteralExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewMapLiteralExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewMapLiteralExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewObjectExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewObjectExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(NewKeyValueObjectExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(NewKeyValueObjectExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(PackageVersionExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(PackageVersionExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(PostfixExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(PostfixExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(PrefixExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(PrefixExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(TernaryExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(TernaryExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(StandardCondition node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(StandardCondition node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(TriggerVariableExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(TriggerVariableExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(VariableExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(VariableExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(BlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(BlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(BreakStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(BreakStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ContinueStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ContinueStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlDeleteStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlDeleteStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlInsertStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlInsertStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlMergeStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlMergeStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlUndeleteStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlUndeleteStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlUpdateStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlUpdateStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DmlUpsertStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DmlUpsertStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(DoLoopStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(DoLoopStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ExpressionStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ExpressionStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ForEachStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ForEachStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ForLoopStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ForLoopStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(FieldDeclaration node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(FieldDeclaration node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(FieldDeclarationStatements node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(FieldDeclarationStatements node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(IfBlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(IfBlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(IfElseBlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(IfElseBlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ReturnStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ReturnStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(RunAsBlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(RunAsBlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ThrowStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ThrowStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(VariableDeclaration node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(VariableDeclaration node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatements node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(VariableDeclarationStatements node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(WhileLoopStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(WhileLoopStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(BindExpressions node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(BindExpressions node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(SoqlExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(SoqlExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(SoslExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(SoslExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(MapEntryNode node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(MapEntryNode node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(CatchBlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(CatchBlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(TryCatchFinallyBlockStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(TryCatchFinallyBlockStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(Property node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(Property node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(Field node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(Field node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(Parameter node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(Parameter node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(Method node, NoopScope scope) {
        // UserEnums contain Method nodes that have no implementation. UserEnum is unique in that
        // its methods don't have
        // implementations for the ApexStandardLibrary or User provided enums. Filtering out all
        // methods here prevents
        // the need to modify a lot of the code downstream that would normally match the methods for
        // the User enums and
        // attempt to invoke them.
        if (isParentOfType(UserEnumWrapper.class)) {
            return false;
        } else {
            return defaultVisit(node);
        }
    }

    @Override
    public void visitEnd(Method node, NoopScope scope) {
        // See #visit(Method, NoopScope) above
        if (!isParentOfType(UserEnumWrapper.class)) {
            defaultVisitEnd(node);
        }
    }

    @Override
    public boolean visit(UserClassMethods node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserClassMethods node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(UserExceptionMethods node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(UserExceptionMethods node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(Annotation node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(Annotation node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(AnnotationParameter node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(AnnotationParameter node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ModifierNode node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ModifierNode node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(SuperMethodCallExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(SuperMethodCallExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ThisMethodCallExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ThisMethodCallExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(SuperVariableExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(SuperVariableExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ThisVariableExpression node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ThisVariableExpression node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(SwitchStatement node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(SwitchStatement node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ValueWhenBlock node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ValueWhenBlock node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(WhenCases.LiteralCase node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(WhenCases.LiteralCase node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(WhenCases.IdentifierCase node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(WhenCases.IdentifierCase node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(TypeWhenBlock node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(TypeWhenBlock node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    @Override
    public boolean visit(ElseWhenBlock node, NoopScope scope) {
        return defaultVisit(node);
    }

    @Override
    public void visitEnd(ElseWhenBlock node, NoopScope scope) {
        defaultVisitEnd(node);
    }

    // **************************************************************************************************
    // BEGIN EXCLUDED NODES
    // **************************************************************************************************
    public static final Set<String> IGNORED_NODES =
            ImmutableSet.of(
                    // These nodes are excluded
                    ASTConstants.NodeType.getVertexLabel(BridgeMethodCreator.class),
                    ASTConstants.NodeType.getVertexLabel(NestedExpression.class));

    @Override
    public boolean visit(BridgeMethodCreator node, NoopScope scope) {
        // This node is not if interest to us
        return false;
    }

    @Override
    public void visitEnd(BridgeMethodCreator node, NoopScope scope) {
        // Intentionally left blank. #defaultVisit was not called from #visit
    }

    @Override
    public boolean visit(ModifierGroup node, NoopScope scope) {
        // ModifierGroup is not derived from AstNode
        return true;
    }

    @Override
    public void visitEnd(ModifierGroup node, NoopScope scope) {
        // ModifierGroup is not derived from AstNode
    }

    @Override
    public boolean visit(NestedExpression node, NoopScope scope) {
        // We are only interested in the child of the NestedExpression, return true so the child
        // will be visited.
        // The child will be re-parented to the NestedExpression's parent.
        return true;
    }

    @Override
    public void visitEnd(NestedExpression node, NoopScope scope) {
        // Intentionally left blank. #defaultVisit was not called from #visit
    }
}
