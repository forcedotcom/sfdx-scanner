package com.salesforce.graph.symbols;

import static com.salesforce.apex.jorje.ASTConstants.NodeType;

import com.salesforce.Collectible;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BlockStatementVertex;
import com.salesforce.graph.vertex.FieldDeclarationVertex;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;

/** Used when invoking a static method on a class. */
public final class ClassStaticScope extends AbstractClassScope
        implements DeepCloneable<ClassStaticScope> {
    private ClassStaticScope(GraphTraversalSource g, UserClassVertex userClass) {
        super(g, userClass);
    }

    private ClassStaticScope(ClassStaticScope other) {
        super(other);
    }

    @Override
    public ClassStaticScope deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ClassStaticScope(this));
    }

    /** Return a ClassStaticScope if the class exists in source */
    public static Optional<ClassStaticScope> getOptional(GraphTraversalSource g, String className) {
        UserClassVertex userClass = ClassUtil.getUserClass(g, className).orElse(null);
        if (userClass != null) {
            return Optional.of(new ClassStaticScope(g, userClass));
        } else {
            return Optional.empty();
        }
    }

    public static ClassStaticScope get(GraphTraversalSource g, String className) {
        return getOptional(g, className).orElseThrow(() -> new UnexpectedException(className));
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public ApexValue<?> updateVariable(
            String key, ApexValue<?> value, SymbolProvider currentScope) {
        if (fieldsWithSetterBlock.contains(key)) {
            // Ignore this, it will be updated in #updateProperty
            return null;
        } else if (finalFields.containsKey(key) || nonFinalFields.containsKey(key)) {
            return apexValues.put(key, value);
        } else {
            return super.updateVariable(key, value, currentScope);
        }
    }

    private static List<FieldDeclarationVertex> getFieldDeclarations(
            GraphTraversalSource g, UserClassVertex userClass) {
        List<FieldDeclarationVertex> results = new ArrayList<>();

        String superClassName = userClass.getSuperClassName().orElse(null);
        if (superClassName != null) {
            UserClassVertex superClass = ClassUtil.getUserClass(g, superClassName).orElse(null);
            if (superClass != null) {
                results.addAll(getFieldDeclarations(g, superClass));
            }
        }

        // Static field declarations happen inside of static constructors
        results.addAll(
                SFVertexFactory.loadVertices(
                        g,
                        g.V(userClass.getId())
                                .out(Schema.CHILD)
                                .hasLabel(NodeType.METHOD)
                                .has(Schema.NAME, MethodUtil.STATIC_CONSTRUCTOR_CANONICAL_NAME)
                                // This is a static initializer block, not a constructor
                                .has(Schema.CONSTRUCTOR, false)
                                .has(Schema.ARITY, 0)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .out(Schema.CHILD)
                                .hasLabel(NodeType.FIELD_DECLARATION_STATEMENTS)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .out(Schema.CHILD)
                                .hasLabel(NodeType.FIELD_DECLARATION)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)));

        return results;
    }

	private static List<MethodCallExpressionVertex> getStaticBlocks(
		GraphTraversalSource g, UserClassVertex userClass) {
		List<MethodCallExpressionVertex> results = new ArrayList<>();

		String superClassName = userClass.getSuperClassName().orElse(null);
		if (superClassName != null) {
			UserClassVertex superClass = ClassUtil.getUserClass(g, superClassName).orElse(null);
			if (superClass != null) {
				results.addAll(getStaticBlocks(g, superClass));
			}
		}

		results.addAll(SFVertexFactory.loadVertices(
			g,
			g.V(userClass.getId())
				.out(Schema.CHILD)
				.hasLabel(NodeType.METHOD)
				.has(Schema.IS_STATIC_BLOCK_INVOKER_METHOD, true)
				.order(Scope.global)
				.by(Schema.CHILD_INDEX, Order.asc)
				.out(Schema.CHILD)
				.hasLabel(ASTConstants.NodeType.BLOCK_STATEMENT)
				.order(Scope.global)
				.by(Schema.CHILD_INDEX, Order.asc)
				.out(Schema.CHILD)
				.hasLabel(NodeType.EXPRESSION_STATEMENT)
				.out(Schema.CHILD)
				.hasLabel(NodeType.METHOD_CALL_EXPRESSION)));

		return results;
	}

	/**
     * Returns a path that represents the static properties defined by the class. The following
     * example would contain a path for 'MyClass' that contains the Field and FieldDeclarations for
     * 's'. {@code
     *
     *    public class MyClass {
     *        private static String s = 'Hello';
     *    }
     * }
     *
     * @return path that represents the static properties defined by the class or empty if the class
     *     does not declare any instance fields
     */
    public static Optional<ApexPath> getInitializationPath(
            GraphTraversalSource g, String classname) {
        ClassStaticScope classStaticScope = ClassStaticScope.get(g, classname);
        List<BaseSFVertex> vertices = new ArrayList<>();
        vertices.addAll(classStaticScope.getFields());
        vertices.addAll(getFieldDeclarations(g, classStaticScope.userClass));
		vertices.addAll(getStaticBlocks(g, classStaticScope.userClass));
        if (vertices.isEmpty()) {
            return Optional.empty();
        } else {
            ApexPath apexPath = new ApexPath(null);
            apexPath.addVertices(vertices);
            return Optional.of(apexPath);
        }
    }

//	private static MethodCallExpressionVertex createSyntheticInvocation(MethodVertex.StaticBlockVertex methodVertex) {
//		final HashMap<Object, Object> map = new HashMap<>();
//		map.put(T.id, Long.valueOf(-1));
//		map.put(T.label, NodeType.METHOD_CALL_EXPRESSION);
//		map.put(Schema.METHOD_NAME, methodVertex.getName());
//		map.put(Schema.DEFINING_TYPE, methodVertex.getDefiningType());
//		map.put(Schema.STATIC, Boolean.valueOf(true));
//		return new MethodCallExpressionVertex(map);
//	}
}
