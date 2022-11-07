package com.salesforce.apex;

import com.salesforce.apex.jorje.JorjeNode;
import com.salesforce.apex.jorje.JorjeUtil;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is responsible for loading the set of standard Apex classes. The Apex classes are
 * stored as resources in the jar. The standard objects are derived from the sources found here
 * https://git.soma.salesforce.com/apex/apex-jorje/tree/master/apex-jorje-lsp/src/main/resources/StandardApexLibrary
 */
@SuppressWarnings(
        "PMD") // Disabling checks since the fixes are more complicated and require further testing
public final class StandardLibraryLoader {
    private static final Logger LOGGER = LogManager.getLogger(StandardLibraryLoader.class);

    private static final String APEX_FILE_EXTENSION = ".cls";
    private static final String JAR_SCHEME = "jar";

    /**
     * A mapping from an unqualified name to its fully qualified name for all root vertex types i.e.
     * SObjectField->Schema.SObjectField. See {@link
     * com.salesforce.apex.jorje.ASTConstants.NodeType#ROOT_VERTICES}
     */
    private static final TreeMap<String, String> CANONICAL_NAMES;

    /**
     * Resource directory where all stub files are located. This is located in src/main/resources
     */
    private static final String STANDARD_APEX_LIBRARY_DIRECTORY = "StandardApexLibrary";

    /**
     * Contains the directory names to the compiled Apex. This allows the vertex to have the correct
     * name. For instance file StandardApexLibrary/Schema/Foo.cls would be imported with a class
     * name of Schema.Foo.
     */
    private static final Map<List<String>, List<JorjeNode>> PACKAGE_TO_COMPILATIONS;

    static {
        // Load and compile cls files that represent the Apex Standard classes
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        FileSystem fs = null;
        try {
            final Path path;
            final URI uri = classLoader.getResource(STANDARD_APEX_LIBRARY_DIRECTORY).toURI();
            // The files need to be handled differently if we are running with class files or
            // bundled in a jar
            if (uri.getScheme().startsWith(JAR_SCHEME)) {
                fs = FileSystems.newFileSystem(uri, new HashMap<>());
                path = fs.getPath(STANDARD_APEX_LIBRARY_DIRECTORY);
            } else {
                path = Paths.get(uri);
            }
            ApexFileVisitor apexFileVisitor = new ApexFileVisitor();
            Files.walkFileTree(path, apexFileVisitor);
            PACKAGE_TO_COMPILATIONS = apexFileVisitor.getPackageToCompilations();

            CANONICAL_NAMES = CollectionUtil.newTreeMap();
            initializeCanonicalNames();
        } catch (URISyntaxException | IOException ex) {
            throw new UnexpectedException(ex);
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    public static Map<List<String>, List<JorjeNode>> getPackageToCompilations() {
        return PACKAGE_TO_COMPILATIONS;
    }

    // TODO: Move ApexStandardLibraryUtil to this package and tighten up access
    /**
     * Some classes can be referred to by multiple names. This will convert the defining type to the
     * canonical name if it exists, if not the original value is returned. For instance {@link
     * ApexStandardLibraryUtil.VariableNames#S_OBJECT_TYPE} is converted to {@link
     * ApexStandardLibraryUtil.Type#SCHEMA_S_OBJECT_TYPE}
     */
    public static String getCanonicalName(String definingType) {
        if (CANONICAL_NAMES.containsKey(definingType)) {
            return CANONICAL_NAMES.get(definingType);
        } else {
            return ApexStandardLibraryUtil.convertArrayToList(definingType).orElse(definingType);
        }
    }

    private static void initializeCanonicalNames() {
        for (Map.Entry<List<String>, List<JorjeNode>> entry : PACKAGE_TO_COMPILATIONS.entrySet()) {
            final List<String> packages = entry.getKey();
            final List<JorjeNode> jorjeNodes = entry.getValue();
            if (packages.size() != 1) {
                throw new UnexpectedException(packages);
            }

            visitCanonicalNameNodes(packages, jorjeNodes);
        }
    }

    private static void visitCanonicalNameNodes(List<String> packages, List<JorjeNode> jorjeNodes) {
        CanonicalNameVisitor visitor = new CanonicalNameVisitor(packages, CANONICAL_NAMES);
        for (JorjeNode jorjeNode : jorjeNodes) {
            jorjeNode.accept(visitor);
        }
    }

    /** Finds all Apex files under StandardApexLibrary and compiles them */
    private static final class ApexFileVisitor extends SimpleFileVisitor<Path> {
        private final Map<List<String>, List<JorjeNode>> packageToCompilations;

        private ApexFileVisitor() {
            this.packageToCompilations = new HashMap<>();
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            loadFile(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * @return the results in an immutable form
         */
        private Map<List<String>, List<JorjeNode>> getPackageToCompilations() {
            Map<List<String>, List<JorjeNode>> result = new HashMap<>();
            for (Map.Entry<List<String>, List<JorjeNode>> entry :
                    packageToCompilations.entrySet()) {
                result.put(
                        Collections.unmodifiableList(entry.getKey()),
                        Collections.unmodifiableList(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }

        private void loadFile(Path path) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Path=" + path.toString());
            }
            final String fileName = path.getFileName().toString();
            if (fileName.endsWith(APEX_FILE_EXTENSION)) {
                final List<String> packages = new ArrayList<>();
                Path parent = path.getParent();
                // Traverse the path until we reach the root directory that contains the apex files.
                // Any parent directories
                // are treated as package names.
                while (!parent.endsWith(STANDARD_APEX_LIBRARY_DIRECTORY)) {
                    // Always add in the first slot since we are traversing backwards
                    // i.e. we will encounter Package2 first if the file is
                    // Package1/Package2/Foo.cls
                    packages.add(0, parent.getFileName().toString());
                    parent = parent.getParent();
                }
                // Read the file and save it's output in the map
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Compiling. packages=" + packages + ", class=" + fileName);
                }
                final List<JorjeNode> compilations =
                        packageToCompilations.computeIfAbsent(
                                Collections.unmodifiableList(packages), k -> new ArrayList());
                try {
                    compilations.add(
                            JorjeUtil.compileApexFromString(new String(Files.readAllBytes(path))));
                } catch (IOException ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }

    private StandardLibraryLoader() {}
}
