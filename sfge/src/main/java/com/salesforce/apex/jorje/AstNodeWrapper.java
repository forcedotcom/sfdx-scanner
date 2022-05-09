package com.salesforce.apex.jorje;

import apex.jorje.data.Location;
import apex.jorje.data.Locations;
import apex.jorje.data.ast.TypeRef;
import apex.jorje.data.ast.TypeRefs;
import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.symbol.type.TypeInfo;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.ops.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides structure and property information to populate the graph. Nodes that need additional
 * properties should create a new subclass named &lt;OriginalJorjeName&gt;Wrapper to add node
 * specific properties.
 */
public abstract class AstNodeWrapper<T extends AstNode> extends AbstractJorjeNode {
    private static final Logger LOGGER = LogManager.getLogger(AstNodeWrapper.class);
    private final T astNode;

    protected AstNodeWrapper(T astNode, @Nullable JorjeNode parent) {
        super(parent);
        this.astNode = astNode;
    }

    @Override
    public final String getLabel() {
        return astNode.getClass().getSimpleName();
    }

    @Override
    public final String getDefiningType() {
        try {
            final TypeInfo typeInfo = astNode.getDefiningType();
            return typeInfo.getApexName();
        } catch (UnsupportedOperationException ex) {
            // Some nodes such as EmptyReferenceExpression don't support TypeInfo, delegate to the
            // parent
            return getParent().get().getDefiningType();
        }
    }

    @Override
    public String getName() {
        final String[] apexNames = getNode().getDefiningType().getApexName().split("\\.");
        return apexNames[apexNames.length - 1];
    }

    @Override
    public Location getLocation() {
        try {
            Location location = astNode.getLoc();
            if (location != null && Locations.isReal(location)) {
                return location;
            }
        } catch (Exception ignore) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Ignored exception", ignore);
            }
        }
        // Some nodes such as UserClassMethods don't support location, delegate to the parent
        return getParent().get().getLocation();
    }

    final T getNode() {
        return astNode;
    }

    /**
     * Multiple levels of packages are represented as an array. This converts them to a dotted
     * string. i.e. [Schema, SObjectResult] is converted to Schema.SObjectResult.
     */
    protected static String typeRefToString(TypeRef typeRef) {
        // Outer.Inner is split into separate names that need to be joined
        final String name =
                typeRef.getNames().stream().map(t -> t.getValue()).collect(Collectors.joining("."));
        if (typeRef.getTypeArguments().isEmpty()) {
            return name;
        } else {
            final List<TypeRef> typeArguments = typeRef.getTypeArguments();
            if (typeRef instanceof TypeRefs.ArrayTypeRef && typeArguments.size() == 1) {
                // Arrays are distinguished by their class
                return ApexStandardLibraryUtil.getListDeclaration(
                        typeRefToString(typeArguments.get(0)));
            } else {
                // Generics such as List<String> or Database.Batchable<sObject>
                final StringBuilder sb = new StringBuilder();
                sb.append(name).append('<');
                final List<String> argumentsAsStrings = new ArrayList<>();
                for (TypeRef argument : typeArguments) {
                    argumentsAsStrings.add(typeRefToString(argument));
                }
                sb.append(String.join(",", argumentsAsStrings)).append('>');
                return normalizeType(sb.toString());
            }
        }
    }

    /** Removes all spaces from {@code type} */
    protected static String normalizeType(String type) {
        return StringUtil.stripAllSpaces(type);
    }
}
