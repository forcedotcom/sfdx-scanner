package com.salesforce.apex.jorje;

import apex.jorje.data.ast.BinaryOp;
import apex.jorje.data.ast.BooleanOp;
import apex.jorje.data.ast.PrefixOp;
import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.compilation.UserClassMethods;
import apex.jorje.semantic.ast.compilation.UserEnum;
import apex.jorje.semantic.ast.compilation.UserInterface;
import apex.jorje.semantic.ast.compilation.UserTrigger;
import apex.jorje.semantic.ast.condition.StandardCondition;
import apex.jorje.semantic.ast.expression.ArrayLoadExpression;
import apex.jorje.semantic.ast.expression.AssignmentExpression;
import apex.jorje.semantic.ast.expression.BinaryExpression;
import apex.jorje.semantic.ast.expression.BooleanExpression;
import apex.jorje.semantic.ast.expression.CastExpression;
import apex.jorje.semantic.ast.expression.ClassRefExpression;
import apex.jorje.semantic.ast.expression.EmptyReferenceExpression;
import apex.jorje.semantic.ast.expression.LiteralExpression;
import apex.jorje.semantic.ast.expression.MethodCallExpression;
import apex.jorje.semantic.ast.expression.NewKeyValueObjectExpression;
import apex.jorje.semantic.ast.expression.NewListLiteralExpression;
import apex.jorje.semantic.ast.expression.NewObjectExpression;
import apex.jorje.semantic.ast.expression.PostfixExpression;
import apex.jorje.semantic.ast.expression.PrefixExpression;
import apex.jorje.semantic.ast.expression.ReferenceExpression;
import apex.jorje.semantic.ast.expression.SoqlExpression;
import apex.jorje.semantic.ast.expression.SoslExpression;
import apex.jorje.semantic.ast.expression.ThisMethodCallExpression;
import apex.jorje.semantic.ast.expression.ThisVariableExpression;
import apex.jorje.semantic.ast.expression.TriggerVariableExpression;
import apex.jorje.semantic.ast.expression.VariableExpression;
import apex.jorje.semantic.ast.member.Field;
import apex.jorje.semantic.ast.member.Method;
import apex.jorje.semantic.ast.member.Parameter;
import apex.jorje.semantic.ast.member.Property;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.ast.modifier.AnnotationParameter;
import apex.jorje.semantic.ast.modifier.ModifierNode;
import apex.jorje.semantic.ast.statement.BlockStatement;
import apex.jorje.semantic.ast.statement.CatchBlockStatement;
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
import apex.jorje.semantic.ast.statement.SwitchStatement;
import apex.jorje.semantic.ast.statement.ThrowStatement;
import apex.jorje.semantic.ast.statement.TryCatchFinallyBlockStatement;
import apex.jorje.semantic.ast.statement.TypeWhenBlock;
import apex.jorje.semantic.ast.statement.ValueWhenBlock;
import apex.jorje.semantic.ast.statement.VariableDeclaration;
import apex.jorje.semantic.ast.statement.VariableDeclarationStatements;
import apex.jorje.semantic.ast.statement.WhenCases;
import apex.jorje.semantic.ast.statement.WhileLoopStatement;
import apex.jorje.semantic.symbol.type.TypeInfos;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains simple types that are derived from Jorje Java classes.
 *
 * <p>NOTE: Keep alphabetized where possible
 */
public final class ASTConstants {

    // This is prefixed to methods that represent Apex properties
    // private String a { get; } results int the following AST
    // <Method Arity='0' BeginColumn='19' BeginLine='2' CanonicalName='__sfdc_a' Constructor='false'
    // DefiningType='MyClass' EndLine='2' Image='__sfdc_a' MethodSignature='String __sfdc_a()'
    // RealLoc='true' ReturnType='String'>
    //    <ModifierNode Abstract='false' BeginColumn='19' BeginLine='2' DefiningType='MyClass'
    // EndLine='2' />
    // </Method>
    public static final String PROPERTY_METHOD_PREFIX = "__sfdc_";

    public static final String ANNOTATION_IS_TEST = "IsTest";

    public static final String TYPE_STRING = TypeInfos.STRING.getApexName();
    public static final String TYPE_VOID = TypeInfos.VOID.getApexName();

    public static final String OPERATOR_AND = BooleanOp.AND.toString();
    public static final String OPERATOR_DOUBLE_EQUAL = BooleanOp.DOUBLE_EQUAL.toString();
    public static final String OPERATOR_NEGATE = PrefixOp.NOT.toString();
    public static final String OPERATOR_NOT_EQUAL = BooleanOp.NOT_EQUAL.toString();
    public static final String OPERATOR_OR = BooleanOp.OR.toString();
    public static final String OPERATOR_ADDITION = BinaryOp.ADDITION.toString();

    /** Used as the vertex label in the graph */
    public static final class NodeType {
        public static String getVertexLabel(Class<? extends AstNode> clazz) {
            return clazz.getSimpleName();
        }

        public static final String ANNOTATION = getVertexLabel(Annotation.class);
        public static final String ANNOTATION_PARAMETER = getVertexLabel(AnnotationParameter.class);
        public static final String ARRAY_LOAD_EXPRESSION =
                getVertexLabel(ArrayLoadExpression.class);
        public static final String ASSIGNMENT_EXPRESSION =
                getVertexLabel(AssignmentExpression.class);
        public static final String BINARY_EXPRESSION = getVertexLabel(BinaryExpression.class);
        public static final String BOOLEAN_EXPRESSION = getVertexLabel(BooleanExpression.class);
        public static final String BLOCK_STATEMENT = getVertexLabel(BlockStatement.class);
        public static final String CAST_EXPRESSION = getVertexLabel(CastExpression.class);
        public static final String CATCH_BLOCK_STATEMENT =
                getVertexLabel(CatchBlockStatement.class);
        public static final String CLASS_REF_EXPRESSION = getVertexLabel(ClassRefExpression.class);
        public static final String ELSE_WHEN_BLOCK = getVertexLabel(ElseWhenBlock.class);
        public static final String EMPTY_REFERENCE_EXPRESSION =
                getVertexLabel(EmptyReferenceExpression.class);
        public static final String EXPRESSION_STATEMENT = getVertexLabel(ExpressionStatement.class);
        public static final String IDENTIFIER_CASE = getVertexLabel(WhenCases.IdentifierCase.class);
        public static final String IF_BLOCK_STATEMENT = getVertexLabel(IfBlockStatement.class);
        public static final String FIELD = getVertexLabel(Field.class);
        public static final String FIELD_DECLARATION = getVertexLabel(FieldDeclaration.class);
        public static final String FIELD_DECLARATION_STATEMENTS =
                getVertexLabel(FieldDeclarationStatements.class);
        public static final String LITERAL_CASE = getVertexLabel(WhenCases.LiteralCase.class);
        public static final String LITERAL_EXPRESSION = getVertexLabel(LiteralExpression.class);
        public static final String METHOD = getVertexLabel(Method.class);
        public static final String MODIFIER_NODE = getVertexLabel(ModifierNode.class);
        public static final String NEW_KEY_VALUE_OBJECT_EXPRESSION =
                getVertexLabel(NewKeyValueObjectExpression.class);
        public static final String NEW_LIST_LITERAL_EXPRESSION =
                getVertexLabel(NewListLiteralExpression.class);
        public static final String NEW_OBJECT_EXPRESSION =
                getVertexLabel(NewObjectExpression.class);
        public static final String PARAMETER = getVertexLabel(Parameter.class);
        public static final String POSTFIX_EXPRESSION = getVertexLabel(PostfixExpression.class);
        public static final String PREFIX_EXPRESSION = getVertexLabel(PrefixExpression.class);
        public static final String PROPERTY = getVertexLabel(Property.class);
        public static final String REFERENCE_EXPRESSION = getVertexLabel(ReferenceExpression.class);
        public static final String RETURN_STATEMENT = getVertexLabel(ReturnStatement.class);
        public static final String STANDARD_CONDITION = getVertexLabel(StandardCondition.class);
        public static final String THIS_METHOD_CALL_EXPRESSION =
                getVertexLabel(ThisMethodCallExpression.class);
        public static final String THIS_VARIABLE_EXPRESSION =
                getVertexLabel(ThisVariableExpression.class);
        public static final String THROW_STATEMENT = getVertexLabel(ThrowStatement.class);
        public static final String TRIGGER_VARIABLE_EXPRESSION =
                getVertexLabel(TriggerVariableExpression.class);
        public static final String TRY_CATCH_FINALLY_BLOCK_STATEMENT =
                getVertexLabel(TryCatchFinallyBlockStatement.class);
        public static final String TYPE_WHEN_BLOCK = getVertexLabel(TypeWhenBlock.class);
        public static final String USER_CLASS = getVertexLabel(UserClass.class);
        public static final String USER_CLASS_METHODS = getVertexLabel(UserClassMethods.class);
        public static final String USER_ENUM = getVertexLabel(UserEnum.class);
        public static final String USER_INTERFACE = getVertexLabel(UserInterface.class);
        public static final String USER_TRIGGER = getVertexLabel(UserTrigger.class);
        public static final String VALUE_WHEN_BLOCK = getVertexLabel(ValueWhenBlock.class);
        public static final String VARIABLE_DECLARATION = getVertexLabel(VariableDeclaration.class);
        public static final String VARIABLE_DECLARATION_STATEMENTS =
                getVertexLabel(VariableDeclarationStatements.class);
        public static final String VARIABLE_EXPRESSION = getVertexLabel(VariableExpression.class);

        public static final String IF_ELSE_BLOCK_STATEMENT =
                getVertexLabel(IfElseBlockStatement.class);
        public static final String SWITCH_STATEMENT = getVertexLabel(SwitchStatement.class);

        public static final String DML_DELETE_STATEMENT = getVertexLabel(DmlDeleteStatement.class);
        public static final String DML_INSERT_STATEMENT = getVertexLabel(DmlInsertStatement.class);
        public static final String DML_MERGE_STATEMENT = getVertexLabel(DmlMergeStatement.class);
        public static final String DML_UNDELETE_STATEMENT =
                getVertexLabel(DmlUndeleteStatement.class);
        public static final String DML_UPDATE_STATEMENT = getVertexLabel(DmlUpdateStatement.class);
        public static final String DML_UPSERT_STATEMENT = getVertexLabel(DmlUpsertStatement.class);
        public static final String METHOD_CALL_EXPRESSION =
                getVertexLabel(MethodCallExpression.class);
        public static final String SOQL_EXPRESSION = getVertexLabel(SoqlExpression.class);
        public static final String SOSL_EXPRESSION = getVertexLabel(SoslExpression.class);

        public static final String[] ROOT_VERTICES = {
            USER_CLASS, USER_ENUM, USER_INTERFACE, USER_TRIGGER
        };

        public static final String DO_LOOP_STATEMENT = DoLoopStatement.class.getSimpleName();
        public static final String FOR_EACH_STATEMENT = ForEachStatement.class.getSimpleName();
        public static final String WHILE_LOOP_STATEMENT = WhileLoopStatement.class.getSimpleName();
        public static final String FOR_LOOP_STATEMENT = ForLoopStatement.class.getSimpleName();

        /**
         * Labels that start an inner scope, they need to be handled specially. You also need to do
         * the following. // TODO There are too many manual steps
         *
         * <ol>
         *   <li>Override BaseVertex#startsInnerScope to return true
         *   <li>PathScopeVisitor#visit for the vertex type should return false
         * </ol>
         */
        public static final Set<String> START_INNER_SCOPE_LABELS =
                new HashSet<>(
                        Arrays.asList(
                                NodeType.BLOCK_STATEMENT,
                                NodeType.CATCH_BLOCK_STATEMENT,
                                NodeType.ELSE_WHEN_BLOCK,
                                NodeType.FOR_EACH_STATEMENT,
                                NodeType.FOR_LOOP_STATEMENT,
                                NodeType.IF_ELSE_BLOCK_STATEMENT,
                                NodeType.SWITCH_STATEMENT,
                                NodeType.TRY_CATCH_FINALLY_BLOCK_STATEMENT,
                                NodeType.TYPE_WHEN_BLOCK,
                                NodeType.VALUE_WHEN_BLOCK));

        /** Labels that indicate a path is terminated. Outgoing edges should not be created */
        public static final Set<String> TERMINAL_VERTEX_LABELS =
                new HashSet<>(Arrays.asList(NodeType.RETURN_STATEMENT, NodeType.THROW_STATEMENT));

        private NodeType() {}
    }

    public static final class ReferenceType {
        public static final String LOAD =
                apex.jorje.semantic.ast.expression.ReferenceType.LOAD.name();
        public static final String STORE =
                apex.jorje.semantic.ast.expression.ReferenceType.STORE.name();
        public static final String METHOD =
                apex.jorje.semantic.ast.expression.ReferenceType.METHOD.name();
        public static final String CLASS =
                apex.jorje.semantic.ast.expression.ReferenceType.CLASS.name();
        public static final String NONE =
                apex.jorje.semantic.ast.expression.ReferenceType.NONE.name();

        private ReferenceType() {}
    }

    public static final class TypePrefix {
        // All list types start with this prefix
        public static final String LIST = "list<";
        // All map types start with this prefix
        public static final String MAP = "map<";
        // All set types start with this prefix
        public static final String SET = "set<";

        private TypePrefix() {}
    }

    public static final class TypeSuffix {
        public static final String SUFFIX_CUSTOM_OBJECT = "__c";
        public static final String SUFFIX_METADATA_OBJECT = "__mdt";

        private TypeSuffix() {}
    }

    private ASTConstants() {}
}
