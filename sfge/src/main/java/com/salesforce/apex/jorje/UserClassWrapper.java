package com.salesforce.apex.jorje;

import apex.jorje.data.ast.TypeRef;
import apex.jorje.semantic.ast.compilation.UserClass;
import apex.jorje.semantic.ast.modifier.Annotation;
import apex.jorje.semantic.symbol.type.CodeUnitDetails;
import com.salesforce.graph.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class UserClassWrapper extends AstNodeWrapper<UserClass> implements TopLevelWrapper {
    UserClassWrapper(UserClass node, @Nullable AstNodeWrapper<?> parent) {
        super(node, parent);
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        final UserClass node = getNode();
        properties.put(Schema.NAME, getName());

        final CodeUnitDetails codeUnitDetails = node.getDefiningType().getCodeUnitDetails();
        final List<String> interfaceNames = new ArrayList<>();

        for (TypeRef typeRef : codeUnitDetails.getInterfaceTypeRefs()) {
            // Outer.Inner is split into separate names that need to be joined
            interfaceNames.add(typeRefToString(typeRef));
        }
        if (!interfaceNames.isEmpty()) {
            properties.put(Schema.INTERFACE_NAMES, interfaceNames);
        }

        if (codeUnitDetails.getSuperTypeRef().isPresent()) {
            final String superClassName = codeUnitDetails.getSuperTypeRef().get().toString();
            properties.put(Schema.SUPER_CLASS_NAME, superClassName);
        }

        for (Annotation annotation : node.getModifiers().getModifiers().getAnnotations()) {
            if (annotation
                    .getType()
                    .getApexName()
                    .equalsIgnoreCase(ASTConstants.ANNOTATION_IS_TEST)) {
                properties.put(Schema.IS_TEST, true);
                break;
            }
        }
    }
}
