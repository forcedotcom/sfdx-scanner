package com.salesforce.cli;

import com.salesforce.TestUtil;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.PathBasedRuleRunner;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Executes path expansion on physical files to verify accuracy of {@link FilesToEntriesMap} created.
 */
public class PathEntryCaptureTest {
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() throws Exception {
        g = GraphUtil.getGraph();
    }

    @Test
    public void testEntryOnKeyFile(TestInfo testInfo) throws GraphUtil.GraphLoadException {
        String entryClass = "EntryAndKeyClass";
        String entryMethod = "doSomething";
        String keyClass = "EntryAndKeyClass";

        TestUtil.compileTestFiles(g, testInfo);
        FilesToEntriesMap filesToEntriesMap = executePathBasedRuleRunner(entryClass, entryMethod);

        Verifier verifier = new Verifier(filesToEntriesMap, testInfo);
        verifier.assertKeyCount(1);
        verifier.assertFileToEntryExists(keyClass, entryClass, entryMethod);
    }

    @Test
    public void testEntryNotOnKeyFile(TestInfo testInfo) throws GraphUtil.GraphLoadException {
        String entryClass = "EntryClass";
        String entryMethod = "doSomething";
        String keyClass = "KeyClass";

        TestUtil.compileTestFiles(g, testInfo);
        FilesToEntriesMap filesToEntriesMap = executePathBasedRuleRunner(entryClass, entryMethod);

        Verifier verifier = new Verifier(filesToEntriesMap, testInfo);
        verifier.assertKeyCount(2);
        verifier.assertFileToEntryExists(keyClass, entryClass, entryMethod);
        verifier.assertFileToEntryExists(entryClass, entryClass, entryMethod);
    }

    @Test
    public void testNoKeyForUnusedFile(TestInfo testInfo) throws GraphUtil.GraphLoadException {
        String entryClass = "EntryClass";
        String entryMethod = "doSomething";
        String unusedClass = "UnusedClass";

        TestUtil.compileTestFiles(g, testInfo);
        FilesToEntriesMap filesToEntriesMap = executePathBasedRuleRunner(entryClass, entryMethod);

        Verifier verifier = new Verifier(filesToEntriesMap, testInfo);
        verifier.assertFileToEntryDoesNotExist(unusedClass, entryClass, entryMethod);
    }

    @Test
    public void testMultiplePathsToDifferentClasses(TestInfo testInfo) throws GraphUtil.GraphLoadException {
        String entryClass = "EntryClass";
        String entryMethod = "doSomething";

        TestUtil.compileTestFiles(g, testInfo);
        FilesToEntriesMap filesToEntriesMap = executePathBasedRuleRunner(entryClass, entryMethod);

        Verifier verifier = new Verifier(filesToEntriesMap, testInfo);
        verifier.assertKeyCount(3);
        verifier.assertFileToEntryExists("KeyClass1", entryClass, entryMethod);
        verifier.assertFileToEntryExists("KeyClass2", entryClass, entryMethod);
        verifier.assertFileToEntryExists(entryClass, entryClass, entryMethod);
    }



    private FilesToEntriesMap executePathBasedRuleRunner(String entryClass, String entryMethod) {
        MethodVertex methodVertex = TestUtil.getMethodVertex(g, entryClass, entryMethod);
        List<AbstractPathBasedRule> rules =
            Collections.singletonList(ApexFlsViolationRule.getInstance());

        // Define a PathBasedRuleRunner to apply the rule against the method vertex.
        PathBasedRuleRunner runner = new PathBasedRuleRunner(g, rules, methodVertex);

        Result result = runner.runRules();
        return result.getFilesToEntriesMap();
    }

    /**
     * Helper class to offload the brunt of assertions and make the tests more readable.
     */
    class Verifier {
        private final HashMap<String, HashSet<FilesToEntriesMap.Entry>> map;
        private final Path testFileDirectory;

        Verifier(FilesToEntriesMap filesToEntriesMap, TestInfo testInfo) {
            this.map = filesToEntriesMap.getMap();
            this.testFileDirectory = TestUtil.getTestFileDirectory(testInfo);
        }

        void assertKeyCount(int expectedCount) {
            assertThat("Incorrect key count.", map, Matchers.aMapWithSize(expectedCount));
        }

        void assertFileToEntryExists(String keyClass, String entryClass, String entryMethod) {
            assertThat("Expected key file not found: " + keyClass, map.keySet(), hasItem(getAbsolutePath(testFileDirectory, keyClass)));

            HashSet<FilesToEntriesMap.Entry> entries = map.get(getAbsolutePath(testFileDirectory, keyClass));
            assertThat("Expected entry not found.", entries, hasItem(getExpectedEntry(testFileDirectory, entryClass, entryMethod)));
        }

        void assertFileToEntryDoesNotExist(String keyClass, String entryClass, String entryMethod) {
            assertThat("Unxpected key file found: " + keyClass, map.keySet(), not(hasItem(getAbsolutePath(testFileDirectory, keyClass))));

            HashSet<FilesToEntriesMap.Entry> entries = map.get(getAbsolutePath(testFileDirectory, keyClass));
            assertThat("Unexpected entry found.", entries, not(hasItem(getExpectedEntry(testFileDirectory, entryClass, entryMethod))));
        }

        private FilesToEntriesMap.Entry getExpectedEntry(Path testFileDirectory, String entryClass, String entryMethod) {
            return new FilesToEntriesMap.Entry(getAbsolutePath(testFileDirectory, entryClass), entryMethod);
        }

        private String getAbsolutePath(Path testFileDirectory, String entryClass) {
            return testFileDirectory.resolve(entryClass + ".cls").toAbsolutePath().toString();
        }
    }
}
