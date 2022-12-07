package com.salesforce.graph.ops;

import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.metainfo.MetaInfoCollectorProvider;
import java.util.*;
import java.util.stream.Collectors;

public final class PathEntryPointUtil {

    public static boolean isPathEntryPoint(MethodVertex methodVertex) {
        // Global methods are entry points.
        if (methodVertex.getModifierNode().isGlobal()) {
            return true;
        }
        // Methods that return PageReference objects are entry points.
        if (methodVertex.getReturnType().equalsIgnoreCase(Schema.PAGE_REFERENCE)) {
            return true;
        }
        // Certain annotations can designate a method as an entry point.
        String[] entryPointAnnotations =
                new String[] {
                    Schema.AURA_ENABLED,
                    Schema.NAMESPACE_ACCESSIBLE,
                    Schema.REMOTE_ACTION,
                    Schema.INVOCABLE_METHOD
                };
        for (String annotation : entryPointAnnotations) {
            if (methodVertex.hasAnnotation(annotation)) {
                return true;
            }
        }
        // Exposed methods on VF controllers are entry points.
        Set<String> vfControllers =
                MetaInfoCollectorProvider.getVisualForceHandler().getMetaInfoCollected().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
        if (vfControllers.contains(methodVertex.getDefiningType().toLowerCase())) {
            return true;
        }

        // InboundEmailHandler methods are entry points.
        // NOTE: This is a pretty cursory check and may struggle with nested inheritance. This isn't
        // likely to happen, but if it does, we can make the check more robust.
        Optional<UserClassVertex> parentClass = methodVertex.getParentClass();
        return parentClass.isPresent()
                && parentClass.get().getInterfaceNames().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
                        // Does the parent class implement InboundEmailHandler?
                        .contains(Schema.INBOUND_EMAIL_HANDLER.toLowerCase())
                //  Does the method return an InboundEmailResult?
                && methodVertex.getReturnType().equalsIgnoreCase(Schema.INBOUND_EMAIL_RESULT)
                // Is the method named handleInboundEmail?
                && methodVertex.getName().equalsIgnoreCase(Schema.HANDLE_INBOUND_EMAIL);
    }

    private PathEntryPointUtil() {}
}
