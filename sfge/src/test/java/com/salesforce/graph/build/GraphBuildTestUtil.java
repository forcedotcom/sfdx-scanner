package com.salesforce.graph.build;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.DefaultNoOpPathVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class GraphBuildTestUtil {
    static void buildGraph(GraphTraversalSource g, String sourceCode) {
        buildGraph(g, new String[] {sourceCode});
    }

    static void buildGraph(GraphTraversalSource g, String[] sourceCodes) {
        List<Util.CompilationDescriptor> compilations = new ArrayList<Util.CompilationDescriptor>();
        for (int i = 0; i < sourceCodes.length; i++) {
            compilations.add(
                    new Util.CompilationDescriptor(
                            "TestCode" + i, JorjeUtil.compileApexFromString(sourceCodes[i])));
        }

        VertexCacheProvider.get().initialize(g);
        CustomerApexVertexBuilder customerApexVertexBuilder =
                new CustomerApexVertexBuilder(g, compilations);

        for (GraphBuilder graphBuilder : new GraphBuilder[] {customerApexVertexBuilder}) {
            graphBuilder.build();
        }
    }

    /** Sanity method to walk all paths. Helps to ensure all of the push/pops are correct */
    static List<ApexPath> walkAllPaths(GraphTraversalSource g, String methodName) {
        MethodVertex methodVertex =
                SFVertexFactory.load(
                        g,
                        g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, methodName)
                                .not(has(Schema.IS_STANDARD, true)));
        List<ApexPath> paths = ApexPathUtil.getForwardPaths(g, methodVertex);

        for (ApexPath path : paths) {
            DefaultSymbolProviderVertexVisitor symbols = new DefaultSymbolProviderVertexVisitor(g);
            PathVertexVisitor visitor = new DefaultNoOpPathVertexVisitor();
            ApexPathWalker.walkPath(g, path, visitor, symbols);
        }

        return paths;
    }
}
