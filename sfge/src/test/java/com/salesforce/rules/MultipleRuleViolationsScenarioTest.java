package com.salesforce.rules;

import com.salesforce.TestUtil;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.testutils.BasePathBasedRuleTest;
import java.util.List;
import java.util.TreeSet;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class MultipleRuleViolationsScenarioTest extends BasePathBasedRuleTest {
    @Test
    public void testFlsAndMmsRules() {
        String sourceCode =
                "public class MyClass {\n"
                        + "   void foo(String[] objectNames) {\n"
                        + "       for (String objectName: objectNames) {\n"
                        + "           Map<String,SObjectType> schemaDescribe = Schema.getGlobalDescribe();\n"
                        + "           SObjectType sot = schemaDescribe.get(objectName);\n"
                        + "           if (sot.getDescribe().isAccessible()){\n"
                        + "               SObject so = sot.newSObject();\n"
                        + "               insert so;\n"
                        + "           }\n"
                        + "       }\n"
                        + "   }\n"
                        + "}\n";

        List<Violation> violations =
                TestUtil.getViolations(
                        g,
                        new String[] {sourceCode},
                        "MyClass",
                        "foo",
                        false,
                        ApexFlsViolationRule.getInstance(),
                        MultipleMassSchemaLookupRule.getInstance());

        // Including an additional step that happens in the actual process.
        // Any missed field in the violation would show up here as a NPE on comparator.
        final TreeSet<Violation> sortedViolations = new TreeSet<>();
        sortedViolations.addAll(violations);

        MatcherAssert.assertThat(sortedViolations, Matchers.hasSize(2));
    }
}
