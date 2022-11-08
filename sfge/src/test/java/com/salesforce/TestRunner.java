package com.salesforce;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.CloningSymbolProvider;
import com.salesforce.graph.symbols.DefaultSymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.ApexPathWalker;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.SystemDebugAccumulator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;

/**
 * Helper to write more fluid tests. Starts with sane defaults.
 *
 * <p>The simplest invocation is:
 *
 * <p>TestRunner.Result<SystemDebugAccumulator> result = TestRunner.walkPath(g, sourceCode);
 */
public final class TestRunner {
    public static final String DEFAULT_METHOD_NAME = "doSomething";

    private final GraphTraversalSource g;
    private final String[] sourceCode;
    private ApexPathExpanderConfig apexPathExpanderConfig;
    private String methodName;
    private Supplier<PathVertexVisitor> visitorSupplier;
    private Supplier<SymbolProviderVertexVisitor> symbolVisitorSupplier;
    private Function<SymbolProviderVertexVisitor, SymbolProvider> symbolProviderFunction;

    private TestRunner(GraphTraversalSource g, String[] sourceCode) {
        this.g = g;
        this.sourceCode = sourceCode;
        this.methodName = DEFAULT_METHOD_NAME;
        this.apexPathExpanderConfig = ApexPathUtil.getFullConfiguredPathExpanderConfig();
        this.visitorSupplier = () -> new SystemDebugAccumulator();
        this.symbolVisitorSupplier = () -> new DefaultSymbolProviderVertexVisitor(g);
        this.symbolProviderFunction =
                (symbolVisitor) -> new CloningSymbolProvider(symbolVisitor.getSymbolProvider());
    }

    public static TestRunner get(GraphTraversalSource g, String sourceCode) {
        return get(g, new String[] {sourceCode});
    }

    public static TestRunner get(GraphTraversalSource g, String[] sourceCode) {
        return new TestRunner(g, sourceCode);
    }

    /** Walk a single path with the default configuration */
    public static <T extends PathVertexVisitor> Result<T> walkPath(
            GraphTraversalSource g, String sourceCode) {
        return get(g, sourceCode).walkPath();
    }

    /** Walk a single path with the default configuration */
    public static <T extends PathVertexVisitor> Result<T> walkPath(
            GraphTraversalSource g, String[] sourceCode) {
        return get(g, sourceCode).walkPath();
    }

    /** Walk multiple paths with the default configuration */
    public static <T extends PathVertexVisitor> List<Result<T>> walkPaths(
            GraphTraversalSource g, String sourceCode) {
        return walkPaths(g, new String[] {sourceCode});
    }

    /** Walk multiple paths with the default configuration */
    public static <T extends PathVertexVisitor> List<Result<T>> walkPaths(
            GraphTraversalSource g, String[] sourceCode) {
        return get(g, sourceCode).walkPaths();
    }

    /** Get the paths without walking them */
    public static List<ApexPath> getPaths(GraphTraversalSource g, String sourceCode) {
        return getPaths(g, new String[] {sourceCode});
    }

    /** Get the paths without walking them */
    public static List<ApexPath> getPaths(GraphTraversalSource g, String[] sourceCode) {
        return get(g, sourceCode).getPaths();
    }

    /**
     * @param methodName - defaults to {@link #DEFAULT_METHOD_NAME}
     */
    public TestRunner withMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * @param apexPathExpanderConfig - defaults to {@link
     *     ApexPathUtil#getFullConfiguredPathExpanderConfig()}
     */
    public TestRunner withExpanderConfig(ApexPathExpanderConfig apexPathExpanderConfig) {
        this.apexPathExpanderConfig = apexPathExpanderConfig;
        return this;
    }

    /**
     * @param visitorSupplier - defaults to {@link SystemDebugAccumulator}
     */
    public TestRunner withPathVertexVisitor(Supplier<PathVertexVisitor> visitorSupplier) {
        this.visitorSupplier = visitorSupplier;
        return this;
    }

    /**
     * Used to specify which {@link SymbolProviderVertexVisitor} will be passed to {@link
     * ApexPathWalker#walkPath(GraphTraversalSource, ApexPath, PathVertexVisitor,
     * SymbolProviderVertexVisitor, SymbolProvider)}
     *
     * @param symbolVisitorSupplier - defaults to {@link DefaultSymbolProviderVertexVisitor}
     */
    public TestRunner withSymbolProviderVisitor(
            Supplier<SymbolProviderVertexVisitor> symbolVisitorSupplier) {
        this.symbolVisitorSupplier = symbolVisitorSupplier;
        return this;
    }

    /**
     * Used to specify which {@link SymbolProvider} will be passed to {@link
     * ApexPathWalker#walkPath(GraphTraversalSource, ApexPath, PathVertexVisitor,
     * SymbolProviderVertexVisitor, SymbolProvider)}
     *
     * @param symbolProviderFunction - defaults to {@link CloningSymbolProvider}
     */
    public TestRunner withSymbolProviderFunction(
            Function<SymbolProviderVertexVisitor, SymbolProvider> symbolProviderFunction) {
        this.symbolProviderFunction = symbolProviderFunction;
        return this;
    }

    /** Walk a single path. Throws assertion if more than one path is found */
    public <T extends PathVertexVisitor> Result<T> walkPath() {
        List<Result<T>> results = walkPaths();

        MatcherAssert.assertThat("Expected a single path", results, hasSize(equalTo(1)));

        return results.get(0);
    }

    public <T extends PathVertexVisitor> List<Result<T>> walkPaths() {
        final List<Result<T>> results = new ArrayList<>();
        final List<ApexPath> paths = getPaths();

        for (ApexPath path : paths) {
            final PathVertexVisitor visitor = visitorSupplier.get();
            final SymbolProviderVertexVisitor symbolVisitor = symbolVisitorSupplier.get();
            final SymbolProvider symbolProvider = symbolProviderFunction.apply(symbolVisitor);
            ApexPathWalker.walkPath(g, path, visitor, symbolVisitor, symbolProvider);
            results.add(new Result(path, visitor));
        }

        return results;
    }

    /** Get the paths without walking them */
    public List<ApexPath> getPaths() {
        TestUtil.Config config = TestUtil.Config.Builder.get(g, sourceCode).build();
        return TestUtil.getApexPaths(config, apexPathExpanderConfig, methodName);
    }

    /** The result of walking a particular path */
    public static final class Result<T> {
        private final ApexPath path;
        private final T visitor;

        private Result(ApexPath path, T visitor) {
            this.path = path;
            this.visitor = visitor;
        }

        public ApexPath getPath() {
            return path;
        }

        public T getVisitor() {
            return visitor;
        }

        @Override
        public String toString() {
            return "Result{" + "path=" + path + ", visitor=" + visitor + '}';
        }
    }
}
