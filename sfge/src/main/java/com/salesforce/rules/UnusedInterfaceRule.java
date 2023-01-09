package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserInterfaceVertex;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UnusedInterfaceRule extends AbstractStaticRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UnusedInterfaceRule";
    private static final String DESCRIPTION =
            "Identifies interfaces that are declared but never implemented or extended";
    private static final String VIOLATION_TEMPLATE =
            "Implement or delete unimplemented interface %s";

    private UnusedInterfaceRule() {
        super();
    }

    public static UnusedInterfaceRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
        List<Violation> violations = new ArrayList<>();

        List<UserInterfaceVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        eligibleVertices
                                .hasLabel(ASTConstants.NodeType.USER_INTERFACE)
                                .where(
                                        __.out(Schema.IMPLEMENTED_BY, Schema.EXTENDED_BY)
                                                .count()
                                                .is(P.eq(0))));

        for (UserInterfaceVertex vertex : vertices) {
            // Global interfaces should be considered used regardless, since their purpose
            // is to be visible to other packages.
            if (vertex.isGlobal()) {
                continue;
            }
            Violation v =
                    new Violation.StaticRuleViolation(
                            String.format(VIOLATION_TEMPLATE, vertex.getDefiningType()), vertex);
            violations.add(v);
        }

        return violations;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code;
    }

    @Override
    protected String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String getCategory() {
        return CATEGORY.PERFORMANCE.name;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    private static final class LazyHolder {
        // Postpone initialization until first use.
        private static final UnusedInterfaceRule INSTANCE = new UnusedInterfaceRule();
    }
}
