package sfdc.sfdx.scanner.pmd;

import com.salesforce.messaging.CliMessager;
import com.salesforce.messaging.EventKey;
import sfdc.sfdx.scanner.pmd.catalog.PmdCatalogRule;
import sfdc.sfdx.scanner.telemetry.TelemetryUtil;

import java.util.ArrayList;
import java.util.List;

public class Pmd7CompatibilityChecker {

    public void validatePmd7Readiness(List<PmdCatalogRule> rules) {
        int unreadyRuleCount = 0;
        for (PmdCatalogRule rule : rules) {
            // Indirect references shouldn't be checked for PMD7 readiness.
            if (ruleIsIndirectRef(rule)) {
                continue;
            }
            // Check for missing properties.
            List<String> missingProps = new ArrayList<>();
            if (ruleLacksLanguageProp(rule)) {
                missingProps.add(PmdCatalogRule.ATTR_LANGUAGE);
            }
            // If any properties are missing, throw a warning about it and increment our total.
            if (!missingProps.isEmpty()) {
                String missingPropString = String.join(",", missingProps);
                CliMessager.getInstance().addMessage(
                    "Rule " + rule.getName() + " lacks PMD7-mandatory properties " + missingPropString,
                    EventKey.WARNING_PMD7_INCOMPATIBLE_RULE,
                    rule.getName(), missingPropString
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
