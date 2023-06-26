package sfdc.sfdx.scanner.pmd;

import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogRule;
import sfdc.sfdx.scanner.telemetry.TelemetryUtil;

import java.util.*;

public class Pmd7CompatibilityChecker {
    private static final String BAD_PROP_TEMPLATE = "%s tag requires property '%s' to be '%s'";
    private static final String EXPECTED_XPATH_CLASS = "net.sourceforge.pmd.lang.rule.XPathRule";

    public void validatePmd7Readiness(List<PmdCatalogRule> rules) {
        int unreadyRuleCount = 0;
        for (PmdCatalogRule rule : rules) {
            // Indirect references shouldn't be checked for PMD7 readiness.
            if (ruleIsIndirectRef(rule)) {
                continue;
            }
            // Built-in rules shouldn't be checked for PMD7 readiness.
            if (rule.isStandard()) {
                continue;
            }
            // Check for missing/wrong properties.
            Set<String> propWarningSet = new HashSet<>();
            if (ruleLacksLanguageProp(rule)) {
                propWarningSet.add(String.format(BAD_PROP_TEMPLATE, "<rule>", PmdCatalogRule.ATTR_LANGUAGE, rule.getLanguage()));
            }
            if (ruleIsBadXpath(rule)) {
                propWarningSet.add(String.format(BAD_PROP_TEMPLATE, "<rule>", PmdCatalogRule.ATTR_CLASS, EXPECTED_XPATH_CLASS));
            }
            // If any properties are bad, throw a warning about it and increment our total.
            if (!propWarningSet.isEmpty()) {
                String badPropString = String.join("; ", propWarningSet);
                CliMessager.getInstance().addMessage(
                    "Rule " + rule.getName() + " is PMD7-incompatible",
                    EventKey.WARNING_PMD7_INCOMPATIBLE_RULE,
                    rule.getName(), badPropString
                );
                unreadyRuleCount += 1;
            }
        }
        // If any rules were unready, send a telemetry event about it.
        if (unreadyRuleCount > 0) {
            TelemetryUtil.postTelemetry(new TelemetryData(unreadyRuleCount));
        }
    }

    /**
     * Indicates whether the rule is an indirect reference to a definition residing elsewhere.
     */
    private boolean ruleIsIndirectRef(PmdCatalogRule rule) {
        // If the rule's element has a `ref` property, it's an indirect reference to
        // the rule's actual declaration.
        return rule.getElement().hasAttribute(PmdCatalogRule.ATTR_REF);
    }

    private boolean ruleIsBadXpath(PmdCatalogRule rule) {
        if (!rule.isXpath()) {
            return false;
        }
        String classProp = rule.getElement().getAttribute(PmdCatalogRule.ATTR_CLASS);
        return !classProp.equalsIgnoreCase(EXPECTED_XPATH_CLASS);
    }

    private boolean ruleLacksLanguageProp(PmdCatalogRule rule) {
        return !rule.getElement().hasAttribute(PmdCatalogRule.ATTR_LANGUAGE);
    }

    private static class TelemetryData extends TelemetryUtil.AbstractTelemetryData {
        private final int unreadyRuleCount;

        public TelemetryData(int unreadyRuleCount) {
            this.unreadyRuleCount = unreadyRuleCount;
        }
    }
}
