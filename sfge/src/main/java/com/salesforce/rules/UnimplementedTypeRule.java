package com.salesforce.rules;

import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.Schema;
import com.salesforce.graph.vertex.FieldWithModifierVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.graph.vertex.UserInterfaceVertex;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class UnimplementedTypeRule extends AbstractStaticRule {
    private static final String URL =
            "https://forcedotcom.github.io./sfdx-scanner/en/v3.x/salesforce-graph-engine/rules/#UnimplementedTypeRule";

    private static final String ABSTRACT_CLASS = "abstract class";
    private static final String INTERFACE = "interface";

    private UnimplementedTypeRule() {
        super();
    }

    public static UnimplementedTypeRule getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    protected List<Violation> _run(
            GraphTraversalSource g, GraphTraversal<Vertex, Vertex> eligibleVertices) {
        List<Violation> results = new ArrayList<>();

        // Get every class and interface that has no extensions or implementations.
        List<FieldWithModifierVertex> vertices =
                SFVertexFactory.loadVertices(
                        g,
                        eligibleVertices
                                .hasLabel(NodeType.USER_CLASS, NodeType.USER_INTERFACE)
                                .where(
                                        __.out(Schema.IMPLEMENTED_BY, Schema.EXTENDED_BY)
                                                .count()
                                                .is(P.eq(0))));
        for (FieldWithModifierVertex vertex : vertices) {
            // If the type is global, it's possible that it's meant to be implemented by other
            // packages, so we should skip it.
            if (vertex.isGlobal()) {
                continue;
            }
            // If the type is an interface or abstract class, it should throw a violation.
            boolean isInterface = vertex instanceof UserInterfaceVertex;
            boolean isAbstractClass = vertex instanceof UserClassVertex && vertex.isAbstract();
            if (isInterface || isAbstractClass) {
                String type = isInterface ? INTERFACE : ABSTRACT_CLASS;
                String msg =
                        String.format(
                                UserFacingMessages.RuleViolationTemplates.UNIMPLEMENTED_TYPE_RULE,
                                type,
                                vertex.getDefiningType());
                results.add(new Violation.StaticRuleViolation(msg, vertex));
            }
        }
        return results;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isExperimental() {
        return false;
    }

    @Override
    protected int getSeverity() {
        return SEVERITY.LOW.code;
    }

    @Override
    protected String getDescription() {
        return UserFacingMessages.RuleDescriptions.UNIMPLEMENTED_TYPE_RULE;
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
        private static final UnimplementedTypeRule INSTANCE = new UnimplementedTypeRule();
    }
}
