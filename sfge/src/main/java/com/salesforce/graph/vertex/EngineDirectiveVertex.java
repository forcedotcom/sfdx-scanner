package com.salesforce.graph.vertex;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.Map;
import java.util.TreeSet;

public class EngineDirectiveVertex extends BaseSFVertex {
    EngineDirectiveVertex(Map<Object, Object> properties) {
        super(properties);
    }

    @Override
    public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
        return visitor.visit(this, symbols);
    }

    @Override
    public boolean visit(SymbolProviderVertexVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
        visitor.afterVisit(this, symbols);
    }

    @Override
    public void afterVisit(SymbolProviderVertexVisitor visitor) {
        visitor.afterVisit(this);
    }

    public EngineDirective getEngineDirective() {
        final EngineDirectiveCommand engineDirectiveCommand =
                EngineDirectiveCommand.fromString(getString(Schema.NAME)).get();
        final TreeSet<String> ruleNames =
                CollectionUtil.newTreeSetOf(getStrings(Schema.RULE_NAMES));
        final String comment = getString(Schema.COMMENT);
        return EngineDirective.Builder.get(engineDirectiveCommand)
                .withRuleNames(ruleNames)
                .withComment(comment)
                .build();
    }
}
