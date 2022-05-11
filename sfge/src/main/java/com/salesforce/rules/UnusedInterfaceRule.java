package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UnusedInterfaceRule extends AbstractStaticRule {
    private static UnusedInterfaceRule INSTANCE;

    private UnusedInterfaceRule() {
        super();
    }

    @Override
    protected List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
        List<Violation> violations = new ArrayList<>();

        List<BaseSFVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        eligibleVertices
                                .hasLabel(ASTConstants.NodeType.USER_INTERFACE)
                                .where(
                                        __.out(Schema.IMPLEMENTED_BY, Schema.EXTENDED_BY)
                                                .count()
                                                .is(P.eq(0))));

        for (BaseSFVertex vertex : vertices) {
            Violation v =
                    new Violation.StaticRuleViolation(
                            "Interface " + vertex.getDefiningType() + " has no implementations",
                            vertex);
            violations.add(v);
        }

        return violations;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code;
    }

    @Override
    protected String getDescription() {
        return "Identifies interfaces that have neither implementations nor extensions";
    }

    @Override
    protected String getCategory() {
        return CATEGORY.BEST_PRACTICES.name;
    }

    public static UnusedInterfaceRule getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UnusedInterfaceRule();
        }
        return INSTANCE;
    }
}
