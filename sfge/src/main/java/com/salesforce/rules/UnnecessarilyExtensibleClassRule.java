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

public class UnnecessarilyExtensibleClassRule extends AbstractStaticRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UnnecessarilyExtensibleClassRule";

    private static final String ABSTRACT = "abstract";
    private static final String VIRTUAL = "virtual";

    private UnnecessarilyExtensibleClassRule() {
        super();
    }

    public static UnnecessarilyExtensibleClassRule getInstance() {
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
            if (classViolatesRule(vertex)) {
                String definingType = vertex.getDefiningType();
                // `virtual` and `abstract` are mutually exclusive, so
                // if it's not one then it's definitely the other.
                String keyword = vertex.isAbstract() ? ABSTRACT : VIRTUAL;
                violations.add(
                        new Violation.StaticRuleViolation(
                                String.format(
                                        UserFacingMessages.RuleViolationTemplates
                                                .UNNECESSARILY_EXTENSIBLE_CLASS_RULE,
                                        keyword,
                                        definingType),
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
        return UserFacingMessages.RuleDescriptions.UNNECESSARILY_EXTENSIBLE_CLASS_RULE;
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
        private static final UnnecessarilyExtensibleClassRule INSTANCE =
                new UnnecessarilyExtensibleClassRule();
    }
}
