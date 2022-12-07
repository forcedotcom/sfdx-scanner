package com.salesforce.rules.unusedmethod;

import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.InvocableWithParametersVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.List;

public class CallValidator extends TypedVertexVisitor.DefaultNoOp<Boolean> {
    protected final MethodVertex invokedMethod;

    protected CallValidator(MethodVertex methodVertex) {
        this.invokedMethod = methodVertex;
    }

    /**
     * Override the default visit method to return false, since vertex types we're not prepared to
     * deal with should not be interpreted as usage of the target method.
     */
    @Override
    public Boolean defaultVisit(BaseSFVertex vertex) {
        return false;
    }

    public boolean isValidCall(InvocableWithParametersVertex vertex) {
        return vertex.accept(this);
    }

    protected boolean parametersAreValid(InvocableWithParametersVertex vertex) {
        // If the arity is wrong, then it's not a match, but rather a call to another overload of
        // the same method.
        // TODO: Long-term, we'll want to validate the parameters' types in addition to their count.
        List<ChainedVertex> parameters = vertex.getParameters();
        return parameters.size() == invokedMethod.getArity();
    }
}
