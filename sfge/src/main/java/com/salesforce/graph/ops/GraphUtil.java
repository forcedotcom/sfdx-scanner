package com.salesforce.graph.ops;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasLabel;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.or;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outE;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.apex.jorje.TopLevelWrapper;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.SfgeException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.Util;
import com.salesforce.graph.cache.VertexCacheKey;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.io.FileHandler;
import com.salesforce.rules.ops.ProgressListener;
import com.salesforce.rules.ops.ProgressListenerProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

public final class GraphUtil {
    private static final Logger LOGGER = LogManager.getLogger(GraphUtil.class);

    /**
     * Retrieve a properly configured graph. All code should use this method instead of directly
     * instantiating a graph.
     *
     * <p>The graph is write once read many. The cost of index maintenance is not a big concern.
     * Because of this we create a lot of indexes.
     *
     * @return a GraphTraversalSource that has been configured with the correct indexes.
     */
    public static GraphTraversalSource getGraph() {
        TinkerGraph graph = TinkerGraph.open();
        graph.createIndex(T.id.name(), Vertex.class);
        graph.createIndex(T.label.name(), Vertex.class);
        graph.createIndex(Schema.ARITY, Vertex.class);
        graph.createIndex(Schema.CHILD_INDEX, Vertex.class);
        graph.createIndex(Schema.CONSTRUCTOR, Vertex.class);
        graph.createIndex(Schema.DEFINING_TYPE, Vertex.class);
        graph.createIndex(Schema.FILE_NAME, Vertex.class);
        graph.createIndex(Schema.FIRST_CHILD, Vertex.class);
        graph.createIndex(Schema.FULL_METHOD_NAME, Vertex.class);
        graph.createIndex(Schema.IS_STANDARD, Vertex.class);
        graph.createIndex(Schema.LAST_CHILD, Vertex.class);
        graph.createIndex(Schema.METHOD_NAME, Vertex.class);
        graph.createIndex(Schema.NAME, Vertex.class);
        graph.createIndex(T.id.name(), Edge.class);
        graph.createIndex(T.label.name(), Edge.class);
        return graph.traversal();
    }

    /**
     * Cache key for {@link #getControlFlowVertex(GraphTraversalSource, BaseSFVertex)}. This method
     * may be called many times and can be expensive.
     */
    private static final class ControlFlowVertexCacheKey extends VertexCacheKey {
        private ControlFlowVertexCacheKey(BaseSFVertex vertex) {
            super(vertex);
        }
    }

    /**
     * Traverses up the parent hierarchy to find the first vertex with an {@link Schema#CFG_PATH}
     * edge or the vertex is a FieldDeclarationVertex.
     */
    public static Optional<BaseSFVertex> getControlFlowVertex(
            GraphTraversalSource g, BaseSFVertex vertex) {
        final ControlFlowVertexCacheKey key = new ControlFlowVertexCacheKey(vertex);
        return VertexCacheProvider.get()
                .get(
                        key,
                        () -> {
                            BaseSFVertex result;
                            if (g.V(vertex.getId())
                                    .or(inE(Schema.CFG_PATH), outE(Schema.CFG_PATH))
                                    .hasNext()) {
                                result = vertex;
                            } else {
                                result =
                                        SFVertexFactory.loadSingleOrNull(
                                                g,
                                                g.V(vertex.getId())
                                                        .repeat(out(Schema.PARENT))
                                                        .until(
                                                                or(
                                                                        inE(Schema.CFG_PATH),
                                                                        outE(Schema.CFG_PATH))));

                                // Walking a path of class fields. These vertices don't have edges
                                if (result == null) {
                                    result =
                                            SFVertexFactory.loadSingleOrNull(
                                                    g,
                                                    g.V(vertex.getId())
                                                            .repeat(out(Schema.PARENT))
                                                            .until(
                                                                    hasLabel(
                                                                            ASTConstants.NodeType
                                                                                    .FIELD_DECLARATION)));
                                }
                            }

                            return result != null ? result : BaseSFVertex.NULL_VALUE;
                        });
    }

    /** Loads all .cls files from the given directories and subdirectories into the graph. */
    public static void loadSourceFolders(GraphTraversalSource g, List<String> sourceFolders)
            throws GraphLoadException {
        List<Util.CompilationDescriptor> comps = new ArrayList<>();
        for (String sourceFolder : sourceFolders) {
            comps.addAll(buildFolderComps(sourceFolder));
        }

        // Verify all TopLevelWrappers have unique names
        final TreeMap<String, Util.CompilationDescriptor> uniqueNames = CollectionUtil.newTreeMap();
        for (Util.CompilationDescriptor comp : comps) {
            final AstNodeWrapper<?> nodeWrapper = comp.getCompilation();
            if (nodeWrapper instanceof TopLevelWrapper) {
                final String definingType = nodeWrapper.getDefiningType();
                final Util.CompilationDescriptor previous = uniqueNames.put(definingType, comp);
                if (previous != null) {
                    throw new GraphLoadException(
                            definingType
                                    + " is defined in multiple files. Files=["
                                    + previous.getFileName()
                                    + ", "
                                    + comp.getFileName()
                                    + "]");
                }
            }
        }

        final ProgressListener progressListener = ProgressListenerProvider.get();

        // Let progress listener know that we've finished compiling all the files in project
        progressListener.finishedFileCompilation();

        Util.Config config = Util.Config.Builder.get(g, comps).build();

        // Let progress listener know what we are doing
        progressListener.startedBuildingGraph();
        Util.buildGraph(config);
        progressListener.completedBuildingGraph();
    }

    private static List<Util.CompilationDescriptor> buildFolderComps(String sourceFolder)
            throws GraphLoadException {
        List<Util.CompilationDescriptor> comps = new ArrayList<>();
        Path path = new File(sourceFolder).toPath();
        SourceFileVisitor sourceFileVisitor = new SourceFileVisitor(comps);
        try {
            Files.walkFileTree(path, sourceFileVisitor);
        } catch (IOException ex) {
            throw new GraphLoadException(
                    "Could not read file/directory " + sourceFileVisitor.lastVisitedFile, ex);
        } catch (JorjeUtil.JorjeCompilationException ex) {
            throw new GraphLoadException(
                    String.format(
                            UserFacingMessages.FIX_COMPILATION_ERRORS,
                            sourceFileVisitor.lastVisitedFile),
                    ex);
        }

        return comps;
    }

    private static Optional<Util.CompilationDescriptor> loadFile(Path path) throws IOException {
        String pathString = path.toString();
        final ProgressListener progressListener = ProgressListenerProvider.get();

        if (!pathString.toLowerCase(Locale.ROOT).endsWith(".cls")) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Skipping file. path=" + pathString);
            }
            return Optional.empty();
        } else {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Loading file. path=" + pathString);
                }
                String sourceCode = FileHandler.getInstance().readTargetFile(pathString);
                AstNodeWrapper<?> astNodeWrapper = JorjeUtil.compileApexFromString(sourceCode);
                // Let progress listener know that we've compiled another file
                progressListener.compiledAnotherFile();
                return Optional.of(new Util.CompilationDescriptor(pathString, astNodeWrapper));
            } catch (JorjeUtil.JorjeCompilationException ex) {
                throw ex;
            }
        }
    }

    private GraphUtil() {}

    private static final class SourceFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Util.CompilationDescriptor> comps;
        private Path lastVisitedFile;

        /**
         * @param comps - The master list of compilation descriptors, which will be built out by
         *     this class's #visitFile() method.
         */
        private SourceFileVisitor(List<Util.CompilationDescriptor> comps) {
            this.comps = comps;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {
            // Before attempting to load the file, set lastVisitedPath to the path we're about to
            // try. That way, if an
            // exception is thrown, we have a reference to the path of the offending file.
            lastVisitedFile = file;
            // Load the file, and add it to the list of compilation descriptors.
            loadFile(file).ifPresent(comps::add);
            return FileVisitResult.CONTINUE;
        }
    }

    public static class GraphLoadException extends SfgeException {
        private GraphLoadException(String message, Throwable cause) {
            super(message, cause);
        }

        private GraphLoadException(String message) {
            super(message);
        }
    }
}
