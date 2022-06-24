package com.salesforce.graph;

/**
 * Contains names of vertex properties, edges etc. These values differ from AstConstants in that
 * they aren't derived from Jorje names.
 */
public class Schema {
    public static final String ABSTRACT = "Abstract";
    public static final String ARITY = "Arity";
    public static final String AURA_ENABLED = "AuraEnabled";
    public static final String BEGIN_COLUMN = "BeginColumn";
    public static final String BEGIN_LINE = "BeginLine";
    public static final String CHILD_INDEX = "childIdx";
    public static final String COMMENT = "Comment";
    public static final String CONSTRUCTOR = "Constructor";
    public static final String DEFINING_TYPE = "DefiningType";
    public static final String END_LINE = "EndLine";
    public static final String END_SCOPES = "EndScopes";
    public static final String EXPRESSION_TYPE = "ExpressionType";
    public static final String EXTENDED_BY = "ExtendedBy";
    public static final String EXTENSION_OF = "ExtensionOf";
    public static final String FILE_NAME = "FileName";
    public static final String FIRST_CHILD = "FirstChild";
    public static final String FULL_METHOD_NAME = "FullMethodName";
    public static final String GLOBAL = "Global";
    public static final String HAS_GETTER_METHOD_BLOCK = "HasGetterMethodBlock";
    public static final String HAS_SETTER_METHOD_BLOCK = "HasSetterMethodBlock";
    public static final String IDENTIFIER = "Identifier";
    public static final String IMPLEMENTATION_OF = "ImplementationOf";
    public static final String IMPLEMENTED_BY = "ImplementedBy";
    public static final String INTERFACE_NAMES = "InterfaceNames";
    /** True if this vertex is part of the Apex Standard Library */
    public static final String IS_STANDARD = "IsStandard";
    /**
     * True if this vertex is a test method or class.
     * https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/apex_classes_annotation_isTest.htm
     */
    public static final String IS_TEST = "IsTest";

    public static final String KEY_NAME = "KeyName";
    public static final String LAST_CHILD = "LastChild";
    public static final String LITERAL_TYPE = "LiteralType";
    public static final String METHOD_NAME = "MethodName";
    public static final String MODIFIERS = "Modifiers";
    public static final String NAME = "Name";
    public static final String NAMES = "Names";
    public static final String NAMESPACE_ACCESSIBLE = "NamespaceAccessible";
    public static final String OPERATOR = "Operator";
    public static final String REFERENCE_TYPE = "ReferenceType";
    public static final String REMOTE_ACTION = "RemoteAction";
    public static final String RETURN_TYPE = "ReturnType";
    public static final String RULE_NAMES = "RulesNames";
    public static final String STATIC = "Static";
    public static final String SUPER_CLASS_NAME = "SuperClassName";
    public static final String SUPER_INTERFACE_NAME = "SuperInterfaceName";
    public static final String TYPE = "Type";
    /** Contains type for statements such as MyClass.class */
    public static final String TYPE_REF = "TypeRef";

    public static final String VALUE = "Value";
    public static final String QUERY = "Query";

    public static final class JorjeNodeType {
        public static final String ENGINE_DIRECTIVE = "EngineDirective";
    }

    // Edge properties
    public static final String CFG_PATH = "CfgPath";
    public static final String CHILD = "Child";
    public static final String PARENT = "Parent";
    public static final String NEXT_SIBLING = "NextSibling";

	/** Mark a vertex as synthetic */
	public static final String IS_SYNTHETIC = "IsSynthetic";
	/** Indicates if a method is a synthetic static block method */
	public static final String IS_STATIC_BLOCK_METHOD = "IsStaticBlockMethod";
	/** Indicates if a method is a synthetic static block invoker method */
	public static final String IS_STATIC_BLOCK_INVOKER_METHOD = "IsStaticBlockInvokerMethod";
	/** Indicates if a MethodCallExpression is a synthetic invocation of static block from invoker method*/
	public static final String IS_STATIC_BLOCK_INVOCATION = "IsStaticBlockInvocation";
}
