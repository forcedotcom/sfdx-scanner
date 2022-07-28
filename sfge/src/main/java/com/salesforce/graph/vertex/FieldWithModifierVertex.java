package com.salesforce.graph.vertex;

import com.salesforce.apex.jorje.ASTConstants;
import java.util.Map;

public abstract class FieldWithModifierVertex extends ChainedVertex {
    FieldWithModifierVertex(Map<Object, Object> properties) {
        super(properties);
    }

    // TODO(Law of Demeter): This exposes an implementation detail versus providing an abstraction
    // of the model
    // Most of the items on the modifier node conceptually apply to the parent vertex, i.e(is this
    // method public).
    // The parent vertex can provide the methods that invoke the methods on the Modifier node
    // without exposing the modifier node.
    public ModifierNodeVertex getModifierNode() {
        return getOnlyChild(ASTConstants.NodeType.MODIFIER_NODE);
    }

    public boolean isPublic() {
        return getModifierNode().isPublic();
    }

    public boolean isProtected() {
        return getModifierNode().isProtected();
    }

    public boolean isPrivate() {
        return getModifierNode().isPrivate();
    }

    public boolean isStatic() {
        return getModifierNode().isStatic();
    }
}
