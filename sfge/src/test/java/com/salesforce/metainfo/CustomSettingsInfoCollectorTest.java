package com.salesforce.metainfo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.salesforce.TestUtil;
import com.salesforce.graph.MetadataInfo;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.rules.ApexFlsViolationRule;
import com.salesforce.rules.Violation;
import com.salesforce.rules.fls.apex.operations.FlsConstants;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class CustomSettingsInfoCollectorTest {
    private static final String CUSTOM_SETTINGS_NAME = "My_Cust_Set__c";
    private GraphTraversalSource g;

    @BeforeEach
    public void setup() throws Exception {
        g = GraphUtil.getGraph();
    }

    @Test
    public void loadCustomSettings_testWithClassFolder(TestInfo testInfo) throws Exception {
        verifyBaseDirIsAvailable(testInfo, "classes");
    }

    @Test
    public void loadCustomSettings_testWithRootFolder(TestInfo testInfo) throws Exception {
        verifyBaseDirIsAvailable(testInfo, "root");
    }

    @Test
    public void testCustomSettingRegistered(TestInfo testInfo) {
        final MetaInfoCollector metaInfoCollector =
                MetaInfoCollectorProvider.getCustomSettingsInfoCollector();
        try {
            loadProjectFiles(testInfo, "classes", (CustomSettingInfoCollector) metaInfoCollector);

            final MetadataInfo metadataInfo = MetadataInfoProvider.get();
            metadataInfo.initialize(g);
            MatcherAssert.assertThat(
                    metadataInfo.isCustomSetting(CUSTOM_SETTINGS_NAME), Matchers.equalTo(true));
        } finally {
            MetaInfoCollectorTestProvider.removeCustomSettingsInfoCollector();
        }
    }

    /**
     * This test attempts to test the singleton behavior of {@link CustomSettingInfoCollector}.
     * Similar to {@link com.salesforce.graph.MetadataInfoTest#testCustomSettingIssue(TestInfo)}
     */
    @Test
    public void testThreadHandoverIssue(TestInfo testInfo) throws Exception {
        final CustomSettingInfoCollector customSettingsInfoCollector =
                (CustomSettingInfoCollector)
                        MetaInfoCollectorProvider.getCustomSettingsInfoCollector();
        loadProjectFiles(testInfo, "classes", customSettingsInfoCollector);

        Runnable runnable =
                () -> {
                    MatcherAssert.assertThat(
                            customSettingsInfoCollector.getMetaInfoCollected(),
                            contains(CUSTOM_SETTINGS_NAME));
                };

        // Execute the thread and wait for it to complete
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(runnable);
        future.get();
    }

    /**
     * Similar to {@link
     * com.salesforce.rules.fls.apex.CustomSettingsFlsTest#testSafeValueMethodParam(String,
     * FlsConstants.FlsValidationType)} The difference is that it uses test files setup Custom
     * settings object meta information and expects source code to identify custom settings object
     * type.
     */
    @Test
    public void testCustomSettingsFlsRule(TestInfo testInfo) {
        try {
            final CustomSettingInfoCollector customSettingInfoCollector =
                    new CustomSettingInfoCollector();

            // This does not contain the actual class we want to compile.
            // Only loads custom settings information from project files.
            loadProjectFiles(testInfo, "classes", customSettingInfoCollector);

            // Make sure we have the custom settings registered
            MatcherAssert.assertThat(
                    customSettingInfoCollector.getMetaInfoCollected(),
                    contains(CUSTOM_SETTINGS_NAME));

            String[] sourceCode = {
                "public class MyClass {\n"
                        + "    public static void foo("
                        + CUSTOM_SETTINGS_NAME
                        + " custSet) {\n"
                        + "    	if (Schema.SObjectType."
                        + CUSTOM_SETTINGS_NAME
                        + ".isCreateable()) {\n"
                        + "    		insert custSet;\n"
                        + "    	}\n"
                        + "    }\n"
                        + "}\n"
            };

            // Execute ApexFlsViolationRule rule on the source code
            final List<Violation> violations =
                    TestUtil.getViolations(
                            g,
                            sourceCode,
                            ApexFlsViolationRule.getInstance(),
                            "MyClass",
                            "foo",
                            false);

            // No violations should be created since a custom setting requires only
            // an object level check
            MatcherAssert.assertThat(violations, Matchers.empty());

        } finally {
            MetaInfoCollectorTestProvider.removeCustomSettingsInfoCollector();
        }
    }

    private void verifyBaseDirIsAvailable(TestInfo testInfo, String baseDir)
            throws GraphUtil.GraphLoadException {
        TestUtil.compileTestFiles(g, testInfo);
        try {
            final CustomSettingInfoCollector customSettingsInfoCollector =
                    new CustomSettingInfoCollector();
            loadProjectFiles(testInfo, baseDir, customSettingsInfoCollector);
            Set<String> referencedNames = customSettingsInfoCollector.getMetaInfoCollected();

            MatcherAssert.assertThat(referencedNames, hasSize(equalTo(1)));
            MatcherAssert.assertThat(referencedNames, contains(CUSTOM_SETTINGS_NAME));
        } finally {
            MetaInfoCollectorTestProvider.removeCustomSettingsInfoCollector();
        }
    }

    private void loadProjectFiles(
            TestInfo testInfo,
            String baseDir,
            CustomSettingInfoCollector customSettingsInfoCollector) {
        MetaInfoCollectorTestProvider.setCustomSettingsInfoCollector(customSettingsInfoCollector);
        String classesFolder = TestUtil.getTestFileDirectory(testInfo).resolve(baseDir).toString();
        customSettingsInfoCollector.loadProjectFiles(Collections.singletonList(classesFolder));
        return;
    }
}
