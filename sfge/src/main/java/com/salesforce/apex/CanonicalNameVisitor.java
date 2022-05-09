package com.salesforce.apex;

import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.apex.jorje.JorjeNodeVisitor;
import com.salesforce.apex.jorje.UserClassWrapper;
import com.salesforce.apex.jorje.UserEnumWrapper;
import com.salesforce.apex.jorje.UserInterfaceWrapper;
import com.salesforce.apex.jorje.UserTriggerWrapper;
import com.salesforce.exception.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Creates a mapping from an unqualified class name to its fully qualified name i.e.
 * SObjectField->Schema.SObjectField, and a mapping from an unqualified enum to its fully qualified
 * name i.e. DisplayType->Schema.DisplayType
 */
final class CanonicalNameVisitor extends JorjeNodeVisitor {
    private static final String PACKAGE_SEPARATOR = ".";

    private final List<String> packages;
    private final TreeMap<String, String> canonicalNames;

    /**
     * @param packages directory names that the Apex class was loaded from, i.e. {@code Schema} for
     *     the {@code Schema.SObjectField} apex class.
     * @param canonicalNames map where canonical names are stored
     */
    CanonicalNameVisitor(List<String> packages, TreeMap<String, String> canonicalNames) {
        this.packages = packages;
        this.canonicalNames = canonicalNames;
    }

    @Override
    public void defaultVisit(JorjeNode node) {
        throw new UnexpectedException("Unexpected visit method called. node=" + node);
    }

    @Override
    public void visit(UserClassWrapper wrapper) {
        initializeCanonicalName(wrapper);
    }

    @Override
    public void visit(UserEnumWrapper wrapper) {
        initializeCanonicalName(wrapper);
    }

    @Override
    public void visit(UserInterfaceWrapper wrapper) {
        initializeCanonicalName(wrapper);
    }

    @Override
    public void visit(UserTriggerWrapper wrapper) {
        initializeCanonicalName(wrapper);
    }

    private void initializeCanonicalName(AstNodeWrapper<?> wrapper) {
        // Map the short name to the full name
        final String name = wrapper.getName();
        final String fullName = getFullName(packages, name);
        canonicalNames.put(name, fullName);
        canonicalNames.put(fullName, fullName);
    }

    /** Converts a list of strings to a dot separated string. */
    private static String getFullName(List<String> packages, String name) {
        final List<String> names = new ArrayList<>(packages);
        names.add(name);
        return String.join(PACKAGE_SEPARATOR, names);
    }
}
