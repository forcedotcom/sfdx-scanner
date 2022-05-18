package com.salesforce.graph.vertex;

import com.salesforce.graph.Schema;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;

public class AssignmentExpressionVertex extends TODO_FIX_HIERARCHY_ChainedVertex
        implements OperatorVertex {
    private final LazyVertex<VariableExpressionVertex> lhs;
    private final LazyVertex<ChainedVertex> rhs;

    AssignmentExpressionVertex(Map<Object, Object> properties) {
        super(properties);
        this.lhs = _getLhs();
        this.rhs = _getRhs();
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return visitor.visit(this, symbols);
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
        visitor.afterVisit(this, symbols);
    }

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {
        visitor.afterVisit(this);
    }

    @Override
    public String getOperator() {
        return getString(Schema.OPERATOR);
    }

    // <AssignmentExpression BeginColumn='8' BeginLine='6' DefiningType='MyOtherClass' EndColumn='9'
    // EndLine='6' FindBoundary='false' Image='' Location='(6, 8, 130, 132)' Namespace=''
    // Operator='=' RealLoc='true' SingleLine='true'>
    //      <VariableExpression BeginColumn='8' BeginLine='6' DefiningType='MyOtherClass'
    // EndColumn='9' EndLine='6' FindBoundary='false' Image='m2' Location='(6, 8, 130, 132)'
    // Namespace='' RealLoc='true' SingleLine='true'>
    //          <EmptyReferenceExpression BeginColumn='8' BeginLine='6' DefiningType=''
    // EndColumn='9' EndLine='6' FindBoundary='false' Image='' Location='no location' Namespace=''
    // RealLoc='false' SingleLine='false' />
    //      </VariableExpression>
    //      <NewObjectExpression BeginColumn='13' BeginLine='6' DefiningType='MyOtherClass'
    // EndColumn='15' EndLine='6' FindBoundary='false' Image='' Location='(6, 13, 135, 138)'
    // Namespace='' RealLoc='true' SingleLine='true' Type='MyClass' />
    // </AssignmentExpression>
    private LazyVertex<VariableExpressionVertex> _getLhs() {
        return new LazyVertex<>(
                () -> g().V(getId()).out(Schema.CHILD).has(Schema.FIRST_CHILD, true));
    }

    private LazyVertex<ChainedVertex> _getRhs() {
        return new LazyVertex<>(
                () -> g().V(getId()).out(Schema.CHILD).has(Schema.LAST_CHILD, true));
    }

    public VariableExpressionVertex getLhs() {
        return lhs.get();
    }

    public ChainedVertex getRhs() {
        return rhs.get();
    }
}
