package com.salesforce.graph.build;

import com.salesforce.apex.StandardLibraryLoader;
import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/** Converts Apex standard objects into Gremlin Vertices. */
// TODO: Filter out parameters that are ignored such as line numbers, first child etc.
public class ApexStandardLibraryVertexBuilder extends AbstractApexVertexBuilder
        implements GraphBuilder {
    private static final Logger LOGGER =
            LogManager.getLogger(ApexStandardLibraryVertexBuilder.class);

    private static final String APEX_CORE = "ApexCore";
    // Note this is the same on *nix/Windows
    private static final String JAR_SEPARATOR = "/";
    private static final String PACKAGE_SEPARATOR = ".";

    private List<String> currentPackage;

    public ApexStandardLibraryVertexBuilder(GraphTraversalSource g) {
        super(g);
    }

    @Override
    public void build() {
        for (Map.Entry<List<String>, List<JorjeNode>> entry :
                StandardLibraryLoader.getPackageToCompilations().entrySet()) {
            currentPackage = entry.getKey();
            for (JorjeNode node : entry.getValue()) {
                final String synthesizedFile =
                        APEX_CORE
                                + JAR_SEPARATOR
                                + String.join(JAR_SEPARATOR, currentPackage)
                                + JAR_SEPARATOR
                                + node.getDefiningType();
                buildVertices(node, synthesizedFile);
            }
        }
    }

    @Override
    protected Object adjustPropertyValue(JorjeNode node, String key, Object value) {
        Object result = value;

        final boolean isRootVertexNameName =
                GremlinUtil.ROOT_VERTICES.contains(node.getLabel()) && key.equals(Schema.NAME);
        if (isRootVertexNameName || key.equals(Schema.DEFINING_TYPE)) {
            result = getFullName(currentPackage, (String) value);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Converted. key="
                                + key
                                + ", node="
                                + node.getClass().getSimpleName()
                                + ", original="
                                + value
                                + ", new="
                                + result);
            }
        }

        return result;
    }

    @Override
    protected Map<String, Object> getAdditionalProperties(JorjeNode node) {
        Map<String, Object> result = super.getAdditionalProperties(node);

        // Mark all nodes as being from the Standard library
        // TODO: Reduce number of nodes where this is set
        final Object previous = result.put(Schema.IS_STANDARD, true);
        if (previous != null) {
            throw new UnexpectedException(node);
        }

        return result;
    }

    /** Converts a list of strings to a dot separated string. */
    private static String getFullName(List<String> packages, String name) {
        final List<String> names = new ArrayList<>(packages);
        names.add(name);
        return String.join(PACKAGE_SEPARATOR, names);
    }
}
