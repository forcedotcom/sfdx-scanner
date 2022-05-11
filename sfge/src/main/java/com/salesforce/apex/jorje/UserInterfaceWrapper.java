package com.salesforce.apex.jorje;

import apex.jorje.data.ast.TypeRef;
import apex.jorje.semantic.ast.compilation.UserInterface;
import apex.jorje.semantic.symbol.type.CodeUnitDetails;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UserInterfaceWrapper extends AstNodeWrapper<UserInterface>
        implements TopLevelWrapper {
    UserInterfaceWrapper(UserInterface node, AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.NAME, getName());

        final CodeUnitDetails codeUnitDetails = getNode().getDefiningType().getCodeUnitDetails();
        if (!codeUnitDetails.getInterfaceTypeRefs().isEmpty()) {
            final List<TypeRef> typeRefs = codeUnitDetails.getInterfaceTypeRefs();
            if (typeRefs.size() != 1) {
                throw new UnexpectedException(typeRefs);
            }
            final List<String> interfaceNames = new ArrayList<>();
            for (TypeRef typeRef : codeUnitDetails.getInterfaceTypeRefs()) {
                interfaceNames.add(typeRefToString(typeRef));
            }
            if (interfaceNames.size() != 1) {
                throw new UnexpectedException(interfaceNames);
            }
            final String superInterfaceName = interfaceNames.get(0);
            properties.put(Schema.SUPER_INTERFACE_NAME, superInterfaceName);
        }
    }
}
