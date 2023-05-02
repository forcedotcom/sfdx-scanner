package com.salesforce.graph.source;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.TestUtil;
import com.salesforce.messaging.CliMessager;
import com.salesforce.rules.AbstractPathBasedRule;
import com.salesforce.rules.AbstractRuleRunner;
import java.util.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** These tests largely verify the static methods present in {@link ApexPathSource}. */
public class ApexPathSourceTest {
    private GraphTraversalSource g;

    /** */
    // spotless:off
    private static final String SOURCE_TEMPLATE =
            // Make the class's name configurable.
            "public class %s {\n"
                    // AuraEnabled methods have a built-in supplier.
                    + "    @AuraEnabled\n"
                    + "    public boolean auraMethod() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    // PageReference methods also have a built-in supplier.
                    + "    public PageReference pageRefMethod() {\n"
                    + "        return null;\n"
                    + "    }\n"
                    // Private methods do not have a built-in supplier.
                    + "    private boolean privateBool() {\n"
                    + "        return true;\n"
                    + "    }\n"
                    + "}\n";

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
        CliMessager.getInstance().resetMessages();
    }

    @AfterEach
    public void teardown() {
        CliMessager.getInstance().resetMessages();
    }

    private void runTest(
            String[] sourceCodes,
            List<AbstractPathBasedRule> rules,
            List<AbstractRuleRunner.RuleRunnerTarget> targets,
            Map<String, Boolean> expectationsMap) {
        // Build the graph.
        TestUtil.buildGraph(g, sourceCodes);
        // Load the sources.
        List<ApexPathSource> pathSources = ApexPathSource.getApexPathSources(g, rules, targets);
        // Verify the right number of sources was returned.
        MatcherAssert.assertThat(pathSources, hasSize(equalTo(expectationsMap.size())));
        // Verify that each of the sources is one we expected to receive.
        for (ApexPathSource pathSource : pathSources) {
            String methodKey = pathSource.getMethodVertex().generateUniqueKey();
            assertTrue(expectationsMap.containsKey(methodKey));
            assertEquals(expectationsMap.get(methodKey), pathSource.isForceTargeted());
        }
    }

    /**
     * If rules are submitted to {@link ApexPathSource#getApexPathSources}, only sources of interest
     * to those rules are returned.
     */
    @Test
    public void getApexPathSources_respectsRuleSourcePreferences() {
        // Create two sources.
        String[] sourceCodes = {
            String.format(SOURCE_TEMPLATE, "MyClass1"), String.format(SOURCE_TEMPLATE, "MyClass2")
        };
        // Use our custom rule, which is only interested in AuraEnabled methods.
        List<AbstractPathBasedRule> rules = Collections.singletonList(new AuraInterestedTestRule());
        // Use an empty list of targets so all files are checked.
        List<AbstractRuleRunner.RuleRunnerTarget> targets = new ArrayList<>();
        // We expect the aura-enabled methods to be returned and not force-targeted.
        Map<String, Boolean> expectations = new HashMap<>();
        expectations.put("MyClass1#auraMethod@3", false);
        expectations.put("MyClass2#auraMethod@3", false);
        runTest(sourceCodes, rules, targets, expectations);
    }

    /**
     * If no rules are submitted to {@link ApexPathSource#getApexPathSources}, then all source types
     * are included.
     */
    @Test
    public void getApexPathSources_noRulesMeansAllSources() {
        // Create one source.
        String[] sourceCodes = {String.format(SOURCE_TEMPLATE, "MyClass1")};
        // Use an empty list of rules.
        List<AbstractPathBasedRule> rules = new ArrayList<>();
        // Use an empty target list.
        List<AbstractRuleRunner.RuleRunnerTarget> targets = new ArrayList<>();
        // We expect the aura-enabled and page-reference method to be returned and not
        // force-targeted.
        Map<String, Boolean> expectations = new HashMap<>();
        expectations.put("MyClass1#auraMethod@3", false);
        expectations.put("MyClass1#pageRefMethod@6", false);
        runTest(sourceCodes, rules, targets, expectations);
    }

    /**
     * If file-level targets are submitted to {@link ApexPathSource#getApexPathSources}, only
     * sources in those files are returned.
     */
    @Test
    public void getApexPathSources_respectsFileLevelTargeting() {
        // Create two sources.
        String[] sourceCodes = {
            String.format(SOURCE_TEMPLATE, "MyClass1"), String.format(SOURCE_TEMPLATE, "MyClass2")
        };

        // Use an empty list of rules.
        List<AbstractPathBasedRule> rules = new ArrayList<>();
        // Create a target for just one file.
        List<AbstractRuleRunner.RuleRunnerTarget> targets =
                Collections.singletonList(TestUtil.createTarget("TestCode0", new ArrayList<>()));
        // We expect only the sources in the first file to be included, and to not be
        // force-targeted.
        Map<String, Boolean> expectations = new HashMap<>();
        expectations.put("MyClass1#auraMethod@3", false);
        expectations.put("MyClass1#pageRefMethod@6", false);
        runTest(sourceCodes, rules, targets, expectations);
    }

    /**
     * If method-level targets are submitted to {@link ApexPathSource#getApexPathSources}, they are
     * respected even if they don't match the rules.
     */
    @Test
    public void getApexPathSources_respectsMethodLevelTargeting() {
        // Create two sources.
        String[] sourceCodes = {
            String.format(SOURCE_TEMPLATE, "MyClass1"), String.format(SOURCE_TEMPLATE, "MyClass2")
        };
        // Use a rule to target just the aura-enabled sources.
        List<AbstractPathBasedRule> rules = Collections.singletonList(new AuraInterestedTestRule());
        // Create a method-level target for one of the page-reference sources.
        List<AbstractRuleRunner.RuleRunnerTarget> targets =
                Collections.singletonList(
                        TestUtil.createTarget(
                                "TestCode0", Collections.singletonList("pageRefMethod")));
        // We expect the aura-enabled sources to be non-force-targeted, and the page ref to be
        // force-targeted.
        Map<String, Boolean> expectations = new HashMap<>();
        expectations.put("MyClass1#auraMethod@3", false);
        expectations.put("MyClass2#auraMethod@3", false);
        expectations.put("MyClass1#pageRefMethod@6", true);
    }

    /**
     * If a method-level target is also passively targeted by a rule, it's still marked as
     * force-targeted.
     */
    @Test
    public void getApexPathSources_methodLevelTargetOverridesAll() {
        // Create two sources.
        String[] sourceCodes = {
            String.format(SOURCE_TEMPLATE, "MyClass1"), String.format(SOURCE_TEMPLATE, "MyClass2")
        };
        // Use a rule to target just the aura-enabled sources.
        List<AbstractPathBasedRule> rules = Collections.singletonList(new AuraInterestedTestRule());
        // Create a method-level target for one of the aura-enabled sources.
        List<AbstractRuleRunner.RuleRunnerTarget> targets =
                Collections.singletonList(
                        TestUtil.createTarget(
                                "TestCode0", Collections.singletonList("auraMethod")));
        // We expect the aura-enabled sources to be non-force-targeted, and the page ref to be
        // force-targeted.
        Map<String, Boolean> expectations = new HashMap<>();
        expectations.put("MyClass1#auraMethod@3", true);
        expectations.put("MyClass2#auraMethod@3", false);
    }

    /**
     * Rule for use in testing. Does nothing interesting, but is interested in AuraEnabled sources.
     */
    private static class AuraInterestedTestRule extends AbstractPathBasedRule {

        @Override
        public List<ApexPathSource.Type> getSourceTypes() {
            // Express interest in just the Aura Enabled sources.
            return Collections.singletonList(ApexPathSource.Type.ANNOTATION_AURA_ENABLED);
        }

        @Override
        protected int getSeverity() {
            // Basically a no-op
            return 0;
        }

        @Override
        protected String getDescription() {
            // Basically a no-op
            return null;
        }

        @Override
        protected String getCategory() {
            // Basically a no-op
            return null;
        }
    }
}
