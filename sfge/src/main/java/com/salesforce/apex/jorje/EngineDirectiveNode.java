package com.salesforce.apex.jorje;

import apex.jorje.data.Location;
import apex.jorje.parser.impl.HiddenToken;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.directive.EngineDirective;
import java.util.ArrayList;
import java.util.Map;

/**
 * This is a node created from an ApexComment. It will be added as the last child of the node that
 * the EngineDirective corresponds to.
 */
final class EngineDirectiveNode extends AbstractJorjeNode {
    private final EngineDirective engineDirective;
    private final HiddenToken hiddenToken;

    protected EngineDirectiveNode(
            EngineDirective engineDirective, HiddenToken hiddenToken, JorjeNode parent) {
        super(parent);
        this.engineDirective = engineDirective;
        this.hiddenToken = hiddenToken;
    }

    @Override
    protected void fillProperties(Map<String, Object> properties) {
        properties.put(Schema.NAME, getName());
        if (!engineDirective.getRuleNames().isEmpty()) {
            properties.put(Schema.RULE_NAMES, new ArrayList<>(engineDirective.getRuleNames()));
        }
        engineDirective.getComment().ifPresent(c -> properties.put(Schema.COMMENT, c));
    }

    @Override
    public String getName() {
        return engineDirective.getDirectiveToken().getToken();
    }

    @Override
    public void accept(JorjeNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getLabel() {
        return Schema.JorjeNodeType.ENGINE_DIRECTIVE;
    }

    @Override
    public String getDefiningType() {
        return getParent().get().getDefiningType();
    }

    @Override
    public Location getLocation() {
        return hiddenToken.getLocation();
    }
}
