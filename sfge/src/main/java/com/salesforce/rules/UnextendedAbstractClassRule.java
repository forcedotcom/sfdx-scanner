package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UnextendedAbstractClassRule extends AbstractStaticRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UnextendedAbstractClassRule";

    private UnextendedAbstractClassRule() {
        super();
    }

    public static UnextendedAbstractClassRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
        List<Violation> violations = new ArrayList<>();

        // Get all classes that have no extensions.
        List<UserClassVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        eligibleVertices
                                .hasLabel(ASTConstants.NodeType.USER_CLASS)
                                .where(__.out(Schema.EXTENDED_BY).count().is(P.eq(0))));
        for (UserClassVertex vertex : vertices) {
            if (!vertex.isGlobal() && vertex.isAbstract()) {
                // If the class is non-global and abstract, then it's a dead class and violates this
                // rule.
                violations.add(
                        new Violation.StaticRuleViolation(
                                String.format(
                                        UserFacingMessages.RuleViolationTemplates
                                                .UNEXTENDED_ABSTRACT_CLASS_RULE,
                                        vertex.getDefiningType()),
                                vertex));
            }
        }
        return violations;
    }

    private boolean classViolatesRule(UserClassVertex vertex) {
        // Non-global abstract/virtual classes are eligible for consideration by this method.
        // All others are excluded.
        return !vertex.isGlobal() && (vertex.isAbstract() || vertex.isVirtual());
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
        return UserFacingMessages.RuleDescriptions.UNEXTENDED_ABSTRACT_CLASS_RULE;
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
        private static final UnextendedAbstractClassRule INSTANCE =
                new UnextendedAbstractClassRule();
    }
}
