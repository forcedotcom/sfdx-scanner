package com.salesforce.graph.symbols;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ClassUtil;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.FieldDeclarationVertex;
import com.salesforce.graph.vertex.NewObjectExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class ClassInstanceScope extends AbstractClassInstanceScope
        implements DeepCloneable<ClassInstanceScope> {
    private ClassInstanceScope(GraphTraversalSource g, UserClassVertex userClass) {
        super(g, userClass);
    }

    private ClassInstanceScope(ClassInstanceScope other) {
        super(other);
    }

    @Override
    public ClassInstanceScope deepClone() {
        return DeepCloneContextProvider.cloneIfAbsent(this, () -> new ClassInstanceScope(this));
    }

    /** Return a ClassInstanceScope if the class exists in source */
    public static Optional<ClassInstanceScope> getOptional(
            GraphTraversalSource g, String className) {
        UserClassVertex userClass = ClassUtil.getUserClass(g, className).orElse(null);
        if (userClass != null) {
            return Optional.of(new ClassInstanceScope(g, userClass));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<ClassInstanceScope> getOptional(
            GraphTraversalSource g, NewObjectExpressionVertex newObjectExpression) {
        UserClassVertex userClass = ClassUtil.getUserClass(g, newObjectExpression).orElse(null);
        if (userClass != null) {
            return Optional.of(new ClassInstanceScope(g, userClass));
        } else {
            return Optional.empty();
        }
    }

    public static ClassInstanceScope get(GraphTraversalSource g, String className) {
        return getOptional(g, className).orElseThrow(() -> new UnexpectedException(className));
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

        results.addAll(
                SFVertexFactory.loadVertices(
                        g,
                        g.V(userClass.getId())
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.FIELD_DECLARATION_STATEMENTS)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)
                                .out(Schema.CHILD)
                                .hasLabel(ASTConstants.NodeType.FIELD_DECLARATION)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc)));

        return results;
    }

    /**
     * Returns a path that represents the instance properties defined by the class. The following
     * example would contain a path for 'MyClass' that contains the Field and FieldDeclarations for
     * 's'. {@code
     *
     *    public class MyClass {
     *        private String s = 'Hello';
     *    }
     * }
     *
     * @return path that represents the instance properties defined by the class or empty if the
     *     class does not declare any instance fields
     */
    public static Optional<ApexPath> getInitializationPath(
            GraphTraversalSource g, String classname) {
        ClassInstanceScope classInstanceScope = ClassInstanceScope.get(g, classname);
        List<BaseSFVertex> vertices = new ArrayList<>();
        vertices.addAll(classInstanceScope.getFields());
        vertices.addAll(getFieldDeclarations(g, classInstanceScope.userClass));
        if (vertices.isEmpty()) {
            return Optional.empty();
        } else {
            ApexPath apexPath = new ApexPath(null);
            apexPath.addVertices(vertices);
            return Optional.of(apexPath);
        }
    }
}
