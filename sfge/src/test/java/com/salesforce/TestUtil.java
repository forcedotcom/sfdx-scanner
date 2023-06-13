package com.salesforce;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.Gson;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.ApexStandardLibraryVertexBuilder;
import com.salesforce.graph.build.Util;
import com.salesforce.graph.ops.ApexPathUtil;
import com.salesforce.graph.ops.ApexValueUtil;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.ops.LiteralUtil;
import com.salesforce.graph.ops.SerializerUtil;
import com.salesforce.graph.ops.expander.ApexPathExpanderConfig;
import com.salesforce.graph.symbols.DefaultNoOpScope;
import com.salesforce.graph.symbols.apex.ApexForLoopValue;
import com.salesforce.graph.symbols.apex.ApexSimpleValue;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.symbols.apex.schema.DescribeSObjectResult;
import com.salesforce.graph.symbols.apex.schema.SObjectType;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.BinaryExpressionVertex;
import com.salesforce.graph.vertex.ChainedVertex;
import com.salesforce.graph.vertex.LiteralExpressionVertex;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import com.salesforce.rules.PathBasedRuleRunner;
import com.salesforce.rules.StaticRule;
import com.salesforce.rules.Violation;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.TestInfo;

public final class TestUtil {
    /** Set this variable in your environment to render the AST as json. */
    private static final String RENDER_JSON_ENV_VAR_NAME = "SFGE_RENDER_JSON";

    private static final boolean RENDER_JSON =
            Boolean.parseBoolean(System.getenv().getOrDefault(RENDER_JSON_ENV_VAR_NAME, "false"));

    /** Set this variable in your environment to render the AST as xml. */
    public static final String RENDER_XML_ENV_VAR_NAME = "SFGE_RENDER_XML";

    private static final boolean RENDER_XML =
            Boolean.parseBoolean(System.getenv().getOrDefault(RENDER_XML_ENV_VAR_NAME, "true"));

    /** Support for multi threaded tests requires its own GraphTraversalSource */
    private static final ThreadLocal<GraphTraversalSource> THREAD_LOCAL_GRAPHS =
            new ThreadLocal<>();

    private static final Logger LOGGER = LogManager.getLogger(TestUtil.class);
    public static final String FILENAME_PREFIX = "TestCode";
    public static final String FIRST_FILE = FILENAME_PREFIX + "0";

    /**
     * Populating the standard library is expensive. Use this method to obtain a graph that has been
     * pre-populated with the standard library classes. This graph is shared across multiple tests,
     * callers of this method should never repopulate the apex standard classes of this graph. See
     * {@link #getUniqueGraph()} if the standard apex classes will be modified by a test.
     */
    public static GraphTraversalSource getGraph() {
        return getInMemoryGraph();
    }

    /**
     * Obtain a unique graph that will not be shared across tests. Callers of this method are free
     * to modify the graph, it will never be reused across different test methods.
     */
    public static GraphTraversalSource getUniqueGraph() {
        return GraphUtil.getGraph();
    }

    /**
     * Populating the standard library is expensive. Perform this one time and keep the graph around
     * between tests. Remove any non-standard vertices if the graph already exists. This will clear
     * vertices between tests.
     */
    private static GraphTraversalSource getInMemoryGraph() {
        GraphTraversalSource g = THREAD_LOCAL_GRAPHS.get();
        if (g == null) {
            g = GraphUtil.getGraph();
            new ApexStandardLibraryVertexBuilder(g).build();
            THREAD_LOCAL_GRAPHS.set(g);
        } else {
            // Delete any vertices that were added by previous tests
            g.V().not(has(Schema.IS_STANDARD, true)).drop().iterate();
            // This isn't strictly necessary since the cache is based on id and I don't believe
            // tinkerpop reuses ids.
            // It is safer in case ids are ever reused.
        }
        return g;
    }

    public static void buildGraph(GraphTraversalSource g, String sourceCode) {
        buildGraph(g, new String[] {sourceCode}, RENDER_XML);
    }

    public static void buildGraph(GraphTraversalSource g, String sourceCode, boolean renderXml) {
        buildGraph(g, new String[] {sourceCode}, renderXml);
    }

    public static void buildGraphForFls(
            GraphTraversalSource g, String sourceCode, boolean renderXml) {
        buildGraphForFls(g, new String[] {sourceCode}, renderXml);
    }

    public static void buildGraph(GraphTraversalSource g, String... sourceCode) {
        buildGraph(Config.Builder.get(g, sourceCode).renderXml(RENDER_XML).build());
    }

    public static void buildGraph(GraphTraversalSource g, String[] sourceCode, boolean renderXml) {
        buildGraph(Config.Builder.get(g, sourceCode).renderXml(renderXml).build());
    }

    public static void buildGraphForFls(
            GraphTraversalSource g, String[] sourceCode, boolean renderXml) {
        buildGraph(Config.Builder.get(g, sourceCode).renderXml(renderXml).build());
    }

    public static void buildGraph(Config config) {
        List<AstNodeWrapper<?>> compilations = new ArrayList<>();
        for (String source : config.sourceCode) {
            AstNodeWrapper<?> compilation = JorjeUtil.compileApexFromString(source);
            compilations.add(compilation);
        }
        Util.buildGraph(getUtilConfig(config, compilations));

        // Render all top level vertices to stdout if requested
        if (config.renderJson || config.renderXml) {
            List<BaseSFVertex> vertices =
                    SFVertexFactory.loadVertices(
                            config.g,
                            config.g.V()
                                    // Work around hasLabel not accepting lists
                                    .hasLabel(
                                            ASTConstants.NodeType.ROOT_VERTICES[0],
                                            ASTConstants.NodeType.ROOT_VERTICES)
                                    // Only render customer Apex
                                    .hasNot(Schema.IS_STANDARD)
                                    // The presence of a filename indicates it's an outer class
                                    .has(Schema.FILE_NAME));
            for (BaseSFVertex vertex : vertices) {
                if (config.renderJson) {
                    System.out.print(SerializerUtil.Json.serialize(vertex));
                }
                if (config.renderXml) {
                    System.out.print(SerializerUtil.Xml.serialize(vertex));
                }
            }
        }
    }

    /** Find a method that isn't ambiguously defined. */
    public static MethodVertex getMethodVertex(
            GraphTraversalSource g, String definingType, String methodName) {
        return SFVertexFactory.load(
                g,
                g.V()
                        .hasLabel(ASTConstants.NodeType.METHOD)
                        .has(Schema.DEFINING_TYPE, definingType)
                        .has(Schema.NAME, methodName));
    }

    /**
     * @deprecated NOT Deprecated, but warning that this won't currently work for items that don't
     *     have a direct mapping such as Strings, Integers. This is because the
     *     clazz.getSimpleName() returns the concrete type, which doesn't exist in the AST.
     */
    @Deprecated
    public static <T extends BaseSFVertex> T getVertexOnLine(
            GraphTraversalSource g, Class<? extends BaseSFVertex> clazz, int beginLine) {
        String nodeType =
                clazz.getSimpleName().substring(0, clazz.getSimpleName().lastIndexOf("Vertex"));
        return getVertexOnLine(g, nodeType, beginLine);
    }

    /**
     * @deprecated NOT Deprecated, but warning that this won't currently work for items that don't
     *     have a direct mapping such as Strings, Integers. This is because the
     *     clazz.getSimpleName() returns the concrete type, which doesn't exist in the AST.
     */
    @Deprecated
    public static <T extends BaseSFVertex> T getVertexOnLine(
            GraphTraversalSource g,
            Class<? extends BaseSFVertex> clazz,
            int beginLine,
            int endLine) {
        List<T> vertices = getVerticesOnLine(g, clazz, beginLine);

        vertices =
                vertices.stream()
                        .filter(v -> v.getEndLine().equals(endLine))
                        .collect(Collectors.toList());

        if (vertices.size() != 1) {
            throw new UnexpectedException(vertices);
        }

        return (T) vertices.get(0);
    }

    /**
     * @deprecated NOT Deprecated, but warning that this won't currently work for items that don't
     *     have a direct mapping such as Strings, Integers. This is because the
     *     clazz.getSimpleName() returns the concrete type, which doesn't exist in the AST.
     */
    public static <T extends BaseSFVertex> List<T> getVerticesOnLine(
            GraphTraversalSource g, Class<? extends BaseSFVertex> clazz, int beginLine) {
        String nodeType =
                clazz.getSimpleName().substring(0, clazz.getSimpleName().lastIndexOf("Vertex"));
        return getVerticesOnLine(g, nodeType, beginLine);
    }

    /** Return the single vertex of the given type and line Throws if found size != 1 */
    public static <T extends BaseSFVertex> T getVertexOnLine(
            GraphTraversalSource g, String nodeType, int beginLine) {
        List<T> vertices = getVerticesOnLine(g, nodeType, beginLine);

        if (vertices.size() != 1) {
            throw new UnexpectedException(vertices);
        }

        return (T) vertices.get(0);
    }

    public static <T extends BaseSFVertex> List<T> getVertices(
            GraphTraversalSource g, String label) {
        return SFVertexFactory.loadVertices(g, g.V().hasLabel(label).order(Scope.local));
    }

    public static <T extends BaseSFVertex> List<T> getVerticesOnLine(
            GraphTraversalSource g, String nodeType, int beginLine) {
        List<T> vertices = getVertices(g, nodeType);
        return vertices.stream()
                .filter(v -> !v.isStandardType())
                .filter(v -> v.getBeginLine().equals(beginLine))
                .collect(Collectors.toList());
    }

    public static void assertNoViolation(
            GraphTraversalSource g, String sourceCode, StaticRule rule) {
        assertNoViolation(g, sourceCode, rule, false);
    }

    public static List<Violation> getViolations(
            GraphTraversalSource g,
            String sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName) {
        return getViolations(g, new String[] {sourceCode}, rule, definingType, methodName);
    }

    public static List<Violation> getViolations(
            GraphTraversalSource g,
            String[] sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName) {
        return getViolations(g, sourceCode, rule, definingType, methodName, true);
    }

    /**
     * Pass this as the <code>sourceCode<code> value to indicate that the test has already built the graph. This is
     * necessary in cases where the violation line number must be calculated after the graph has been built.
     */
    public static String[] USE_EXISTING_GRAPH = new String[] {"USE_EXISTING_GRAPH"};

    public static List<Violation> getViolations(
            GraphTraversalSource g,
            String[] sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName,
            Boolean renderXml) {

        return getViolations(g, sourceCode, definingType, methodName, renderXml, rule);
    }

    public static List<Violation> getViolations(
            GraphTraversalSource g,
            String[] sourceCode,
            String definingType,
            String methodName,
            Boolean renderXml,
            AbstractPathBasedRule... rules) {
        if (USE_EXISTING_GRAPH != sourceCode) {
            Config.Builder builder = Config.Builder.get(g, sourceCode);
            if (renderXml != null) {
                builder.renderXml(renderXml);
            }
            Config config = builder.build();
            buildGraph(config);
        }

        final MethodVertex methodVertex = getMethodVertex(g, definingType, methodName);

        final PathBasedRuleRunner ruleRunner =
                new PathBasedRuleRunner(g, Arrays.asList(rules), methodVertex);
        final List<Violation> violations = new ArrayList<>(ruleRunner.runRules());

        return violations;
    }

    public static void assertNoViolation(
            GraphTraversalSource g,
            String sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName) {
        assertNoViolation(g, new String[] {sourceCode}, rule, definingType, methodName);
    }

    public static void assertNoViolation(
            GraphTraversalSource g,
            String[] sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName) {
        List<Violation> violations = getViolations(g, sourceCode, rule, definingType, methodName);
        MatcherAssert.assertThat(
                "Unexpected violations encountered: " + violations,
                violations,
                hasSize(equalTo(0)));
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName,
            int... expectedLineNumbers) {
        return assertViolations(
                g, new String[] {sourceCode}, rule, definingType, methodName, expectedLineNumbers);
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String[] sourceCode,
            AbstractPathBasedRule rule,
            String definingType,
            String methodName,
            int... expectedLineNumbers) {
        List<Violation> violations = getViolations(g, sourceCode, rule, definingType, methodName);
        MatcherAssert.assertThat(
                "Unexpected number of violations: " + violations,
                violations,
                hasSize(equalTo(expectedLineNumbers.length)));
        LOGGER.info("Found violations: " + violations);

        for (int i = 0; i < expectedLineNumbers.length; i++) {
            Violation violation = violations.get(i);
            MatcherAssert.assertThat(
                    violation.getSourceLineNumber(), equalTo(expectedLineNumbers[i]));
        }

        return violations;
    }

    public static void assertNoViolation(
            GraphTraversalSource g, String sourceCode, StaticRule rule, boolean renderXml) {
        assertNoViolation(g, new String[] {sourceCode}, rule, renderXml);
    }

    public static void assertNoViolation(
            GraphTraversalSource g, String[] sourceCodes, StaticRule rule) {
        assertNoViolation(g, sourceCodes, rule, true);
    }

    public static void assertNoViolation(Config config, StaticRule rule) {
        buildGraph(config);

        List<Violation> violations = rule.run(config.g);
        MatcherAssert.assertThat(
                "Unexpected violations encountered: " + violations,
                violations,
                hasSize(equalTo(0)));
    }

    public static void assertNoViolation(
            GraphTraversalSource g, String[] sourceCode, StaticRule rule, boolean renderXml) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(renderXml).build();
        assertNoViolation(config, rule);
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String[] sourceCodes,
            StaticRule rule,
            int... expectedLineNumbers) {
        return assertViolations(g, sourceCodes, rule, true, expectedLineNumbers);
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String sourceCode,
            StaticRule rule,
            int... expectedLineNumbers) {
        return assertViolations(g, sourceCode, rule, true, expectedLineNumbers);
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String sourceCode,
            StaticRule rule,
            boolean renderXml,
            int... expectedLineNumbers) {
        return assertViolations(g, new String[] {sourceCode}, rule, renderXml, expectedLineNumbers);
    }

    public static List<Violation> assertViolations(
            GraphTraversalSource g,
            String[] sourceCodes,
            StaticRule rule,
            boolean renderXml,
            int... expectedLineNumbers) {
        buildGraphForFls(g, sourceCodes, renderXml);
        List<Violation> violations = rule.run(g);
        MatcherAssert.assertThat(
                "Unexpected number of violations: " + violations,
                violations,
                hasSize(equalTo(expectedLineNumbers.length)));
        LOGGER.info("Found violations: " + violations);

        for (int i = 0; i < expectedLineNumbers.length; i++) {
            Violation violation = violations.get(i);
            MatcherAssert.assertThat(
                    violation.getSourceLineNumber(), equalTo(expectedLineNumbers[i]));
        }

        return violations;
    }

    public static List<Integer> getPathSizes(List<ApexPath> paths) {
        return paths.stream()
                .map(p -> p.verticesInCurrentMethod().size())
                .collect(Collectors.toList());
    }

    public static ApexPath getPathOfSize(List<ApexPath> paths, int size) {
        List<ApexPath> filteredPaths =
                paths.stream()
                        .filter(p -> p.verticesInCurrentMethod().size() == size)
                        .collect(Collectors.toList());

        if (filteredPaths.size() != 1) {
            fail();
        }

        return filteredPaths.get(0);
    }

    public static ApexPath getPathEndingAtVertex(List<ApexPath> paths, BaseSFVertex vertex) {
        List<ApexPath> filteredPaths =
                paths.stream()
                        .filter(p -> vertex.equals(p.lastVertex()))
                        .collect(Collectors.toList());

        if (filteredPaths.size() != 1) {
            fail();
        }

        return filteredPaths.get(0);
    }

    public static ApexPath getSingleApexPath(Config config, String methodName) {
        ApexPathExpanderConfig expanderConfig =
                ApexPathExpanderConfig.Builder.get().expandMethodCalls(true).build();

        return getSingleApexPath(config, expanderConfig, methodName);
    }

    public static ApexPath getSingleApexPath(
            Config config, ApexPathExpanderConfig expanderConfig, String methodName) {
        buildGraph(config);

        MethodVertex methodVertex =
                SFVertexFactory.load(
                        config.g,
                        config.g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, methodName));

        List<ApexPath> paths = ApexPathUtil.getForwardPaths(config.g, methodVertex, expanderConfig);
        MatcherAssert.assertThat(paths, hasSize(equalTo(1)));

        return paths.get(0);
    }

    public static ApexPath getSingleApexPath(
            GraphTraversalSource g, String sourceCode, String methodName) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(RENDER_XML).build();
        return getSingleApexPath(config, methodName);
    }

    public static ApexPath getSingleApexPath(
            GraphTraversalSource g, String[] sourceCode, String methodName) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(RENDER_XML).build();
        return getSingleApexPath(config, methodName);
    }

    public static ApexPath getSingleApexPath(
            GraphTraversalSource g, String[] sourceCode, String methodName, boolean renderXml) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(renderXml).build();
        return getSingleApexPath(config, methodName);
    }

    public static ApexPath getSingleApexPath(
            GraphTraversalSource g, String sourceCode, String methodName, boolean renderXml) {
        return getSingleApexPath(g, new String[] {sourceCode}, methodName, renderXml);
    }

    public static List<ApexPath> getApexPaths(Config config, String methodName) {
        ApexPathExpanderConfig apexPathExpanderConfig =
                ApexPathExpanderConfig.Builder.get().expandMethodCalls(true).build();
        return getApexPaths(config, apexPathExpanderConfig, methodName);
    }

    public static List<ApexPath> getApexPaths(
            Config config, ApexPathExpanderConfig apexPathExpanderConfig, String methodName) {
        TestUtil.buildGraph(config);

        MethodVertex methodVertex =
                SFVertexFactory.load(
                        config.g,
                        config.g.V()
                                .hasLabel(ASTConstants.NodeType.METHOD)
                                .has(Schema.NAME, methodName));

        return ApexPathUtil.getForwardPaths(config.g, methodVertex, apexPathExpanderConfig);
    }

    public static List<ApexPath> getApexPaths(
            GraphTraversalSource g, String[] sourceCode, String methodName, boolean renderXml) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(renderXml).build();
        return getApexPaths(config, methodName);
    }

    public static List<ApexPath> getApexPaths(
            GraphTraversalSource g, String sourceCode, String methodName, boolean renderXml) {
        Config config = Config.Builder.get(g, sourceCode).renderXml(renderXml).build();
        return getApexPaths(config, methodName);
    }

    public static String apexValueToString(Optional<?> apexValue) {
        return apexValueToString((ApexValue<?>) apexValue.get());
    }

    public static String apexValueToString(ApexValue<?> apexValue) {
        MatcherAssert.assertThat(
                "#apexValueToString invoked on indeterminant value=" + apexValue,
                apexValue.isDeterminant(),
                equalTo(true));

        if (apexValue instanceof ApexSimpleValue) {
            return ((ApexSimpleValue) apexValue).getValue().get().toString();
        } else if (apexValue instanceof DescribeSObjectResult) {
            return apexValueToString(((DescribeSObjectResult) apexValue).getSObjectType().get());
        } else if (apexValue instanceof SObjectType) {
            return apexValueToString(((SObjectType) apexValue).getType());
        } else if (apexValue instanceof ApexForLoopValue) {
            ApexForLoopValue apexForLoopValue = (ApexForLoopValue) apexValue;
            List<String> list =
                    apexForLoopValue.getForLoopValues().stream()
                            .map(v -> apexValueToString(v))
                            .collect(Collectors.toList());
            return String.join(", ", list);
        } else if (apexValue.getValueVertex().orElse(null) instanceof BinaryExpressionVertex) {
            ApexValue<?> value =
                    ApexValueUtil.applyBinaryExpression(
                                    (BinaryExpressionVertex) apexValue.getValueVertex().get(),
                                    DefaultNoOpScope.getInstance())
                            .orElse(null);
            if (value != null) {
                return apexValueToString(value);
            } else {
                throw new UnexpectedException(apexValue);
            }
        } else {
            return chainedVertexToString(apexValue.getValueVertex());
        }
    }

    public static String chainedVertexToString(Optional<ChainedVertex> vertex) {
        return chainedVertexToString(vertex.get());
    }

    public static String chainedVertexToString(ChainedVertex vertex) {
        if (vertex instanceof LiteralExpressionVertex.SFString) {
            return LiteralUtil.toString(vertex).get();
        } else {
            throw new UnexpectedException(vertex);
        }
    }

    private void writeGraph(GraphTraversalSource g, String path) {
        try {
            g.getGraph().io(IoCore.graphson()).writeGraph(path);
        } catch (IOException ioe) {
            // Swallow it
        }
    }

    private static Util.Config getUtilConfig(Config config, List<AstNodeWrapper<?>> compilations) {
        List<Util.CompilationDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < compilations.size(); i++) {
            descriptors.add(
                    new Util.CompilationDescriptor(FILENAME_PREFIX + i, compilations.get(i)));
        }
        Util.Config.Builder builder = Util.Config.Builder.get(config.g, descriptors);
        // These classes are populated by #getInMemoryGraph
        builder.skipBuilders(ApexStandardLibraryVertexBuilder.class);
        return builder.build();
    }

    /** Configuration object used to avoid method overload proliferation */
    public static final class Config {
        private final GraphTraversalSource g;
        private final String[] sourceCode;
        private final boolean renderJson;
        private final boolean renderXml;

        private Config(Builder builder) {
            this.g = builder.g;
            this.sourceCode = builder.sourceCode;
            this.renderJson = builder.renderJson;
            this.renderXml = builder.renderXml;
        }

        public static final class Builder {
            private final GraphTraversalSource g;
            private final String[] sourceCode;
            private boolean renderJson;
            private boolean renderXml;

            private Builder(GraphTraversalSource g, String[] sourceCode) {
                this.g = g;
                this.sourceCode = sourceCode;
                this.renderJson = RENDER_JSON;
                this.renderXml = RENDER_XML;
            }

            public static Builder get(GraphTraversalSource g, String[] sourceCode) {
                return new Builder(g, sourceCode);
            }

            public static Builder get(GraphTraversalSource g, String sourceCode) {
                return get(g, new String[] {sourceCode});
            }

            /**
             * Whether or not to render the graph as xml after the graph is built. Defaults to
             * {@link TestUtil#RENDER_XML}
             */
            public Builder renderXml(boolean renderXml) {
                this.renderXml = renderXml;
                return this;
            }

            /**
             * Whether or not to render the graph as json after the graph is built. Defaults to
             * {@link TestUtil#RENDER_JSON}
             */
            public Builder renderJson(boolean renderJson) {
                this.renderJson = renderJson;
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }

    /**
     * Resolves the {@code TestInfo} to a directory that contains files for the test
     *
     * <p>The convention is src/test/resources/test-files/class-name-split/testName
     *
     * <p>For example com.salesforce.graph.JustInTimeGraphTest#testStandardLibrary will load files
     * from
     *
     * <p>src/test/resources/test-files/com/salesforce/graph/JustInTimeGraphTest/testStandardLibrary
     */
    public static Path getTestFileDirectory(TestInfo testInfo) {
        try {
            Class<?> clazz = testInfo.getTestClass().get();
            Method method = testInfo.getTestMethod().get();
            String testDirectory =
                    new StringBuilder("test-files")
                            .append(File.separator)
                            .append(clazz.getCanonicalName().replace(".", File.separator))
                            .append(File.separator)
                            .append(method.getName())
                            .toString();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URI uri = classLoader.getResource(testDirectory).toURI();

            return Paths.get(uri);
        } catch (URISyntaxException ex) {
            throw new UnexpectedException(ex);
        }
    }

    /**
     * Load a set of files identified by {@code testInfo} and add them to {@code g}
     *
     * @param testInfo see {@link #getTestFileDirectory(TestInfo)}
     * @throws GraphUtil.GraphLoadException if there are any parse errors
     */
    public static void compileTestFiles(GraphTraversalSource g, TestInfo testInfo)
            throws GraphUtil.GraphLoadException {
        if (g.V().hasNext()) {
            throw new UnexpectedException(
                    "The graph passed to TestUtil.compileTestFiles() must be empty");
        }
        Path path = getTestFileDirectory(testInfo);
        GraphUtil.loadSourceFolders(g, Collections.singletonList(path.toString()));
    }

    public static RuleRunnerTarget createTarget(String targetFile, List<String> targetMethods) {
        String targetMethodsString =
                targetMethods.isEmpty() ? "" : "\"" + String.join("\",\"", targetMethods) + "\"";
        String jsonFormat = "{\"targetFile\": \"%1$s\", \"targetMethods\":[%2$s]}";
        return new Gson()
                .fromJson(
                        String.format(jsonFormat, targetFile, targetMethodsString),
                        RuleRunnerTarget.class);
    }

    private TestUtil() {}
}
