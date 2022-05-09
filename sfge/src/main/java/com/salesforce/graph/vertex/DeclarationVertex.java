package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.salesforce.graph.Schema;
import java.util.Map;
import java.util.Optional;

public abstract class DeclarationVertex extends BaseSFVertex {
    private final LazyVertex<VariableExpressionVertex> lhs;
    private final LazyOptionalVertex<ChainedVertex> rhs;

    DeclarationVertex(Map<Object, Object> properties) {
        super(properties);
        this.lhs = _getLhs();
        this.rhs = _getRhs();
    }

    public VariableExpressionVertex getLhs() {
        return lhs.get();
    }

    public Optional<ChainedVertex> getRhs() {
        return rhs.get();
    }

    // The left hand side is always the last child, the same applies for VariableDeclarationVertex
    // private final String s = 'Foo';
    // <FieldDeclaration BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='b' Location='(3, 26, 86, 87)' Name='b' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //      <LiteralExpression BeginColumn='30' BeginLine='3' Boolean='false' Decimal='false'
    // DefiningType='MyClass' Double='false' EndColumn='38' EndLine='3' FindBoundary='false'
    // Image='Foo' Integer='false' LiteralType='STRING' Location='(3, 30, 90, 99)' Long='false'
    // Name='' Namespace='' Null='false' RealLoc='true' SingleLine='true' String='true' />
    //      <VariableExpression BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='s' Location='(3, 26, 86, 87)' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //          <EmptyReferenceExpression BeginColumn='26' BeginLine='3' DefiningType=''
    // EndColumn='26' EndLine='3' FindBoundary='false' Image='' Location='no location' Namespace=''
    // RealLoc='false' SingleLine='false' />
    //      </VariableExpression>
    // </FieldDeclaration>
    private LazyVertex<VariableExpressionVertex> _getLhs() {
        return new LazyVertex<>(
                () -> g().V(getId()).out(Schema.CHILD).has(Schema.LAST_CHILD, true));
    }

    // The right hand side is is the first child if there are two children, else, their is not a
    // rhs, , the same applies for VariableDeclarationVertex
    // private final String s = 'Foo';
    // <FieldDeclaration BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='b' Location='(3, 26, 86, 87)' Name='b' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //      <LiteralExpression BeginColumn='30' BeginLine='3' Boolean='false' Decimal='false'
    // DefiningType='MyClass' Double='false' EndColumn='38' EndLine='3' FindBoundary='false'
    // Image='Foo' Integer='false' LiteralType='STRING' Location='(3, 30, 90, 99)' Long='false'
    // Name='' Namespace='' Null='false' RealLoc='true' SingleLine='true' String='true' />
    //      <VariableExpression BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='s' Location='(3, 26, 86, 87)' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //          <EmptyReferenceExpression BeginColumn='26' BeginLine='3' DefiningType=''
    // EndColumn='26' EndLine='3' FindBoundary='false' Image='' Location='no location' Namespace=''
    // RealLoc='false' SingleLine='false' />
    //      </VariableExpression>
    // </FieldDeclaration>
    // private final String s;
    // <FieldDeclaration BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='b' Location='(3, 26, 86, 87)' Name='b' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //      <VariableExpression BeginColumn='26' BeginLine='3' DefiningType='MyClass' EndColumn='26'
    // EndLine='3' FindBoundary='false' Image='s' Location='(3, 26, 86, 87)' Namespace=''
    // RealLoc='true' SingleLine='true'>
    //          <EmptyReferenceExpression BeginColumn='26' BeginLine='3' DefiningType=''
    // EndColumn='26' EndLine='3' FindBoundary='false' Image='' Location='no location' Namespace=''
    // RealLoc='false' SingleLine='false' />
    //      </VariableExpression>
    // </FieldDeclaration>
    private LazyOptionalVertex<ChainedVertex> _getRhs() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .has(Schema.FIRST_CHILD, true)
                                .not(has(Schema.LAST_CHILD, true)));
    }
}
