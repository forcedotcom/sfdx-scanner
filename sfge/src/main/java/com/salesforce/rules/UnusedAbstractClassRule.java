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

public class UnusedAbstractClassRule extends AbstractStaticRule {
    private static UnusedAbstractClassRule INSTANCE;

    private UnusedAbstractClassRule() {
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
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .where(
                                        __.and(
                                                __.out(Schema.CHILD)
                                                        .has(
                                                                ASTConstants.NodeType.MODIFIER_NODE,
                                                                Schema.ABSTRACT,
                                                                true)
                                                        .count()
                                                        .is(P.gt(0)),
                                                __.out(Schema.EXTENDED_BY).count().is(P.eq(0)))));

        for (BaseSFVertex vertex : vertices) {
            Violation v =
                    new Violation.StaticRuleViolation(
                            "Abstract class " + vertex.getDefiningType() + " is never extended",
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
        return "Identifies abstract classes that are never extended";
    }

    @Override
    protected String getCategory() {
        return CATEGORY.BEST_PRACTICES.name;
    }

    public static UnusedAbstractClassRule getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UnusedAbstractClassRule();
        }
        return INSTANCE;
    }
}
