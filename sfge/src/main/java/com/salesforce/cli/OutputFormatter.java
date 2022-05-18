package com.salesforce.cli;

import com.google.gson.Gson;
import com.salesforce.rules.AbstractRule;
import com.salesforce.rules.Violation;
import java.util.List;
import java.util.stream.Collectors;

public class OutputFormatter {

    public String formatViolationJsons(List<Violation> violations) {
        Gson gson = new Gson();

        return "VIOLATIONS_START" + gson.toJson(violations) + "VIOLATIONS_END";
    }

    public String formatRuleJsons(List<AbstractRule> rules) {
        List<AbstractRule.Descriptor> descriptors =
                rules.stream().map(AbstractRule::getDescriptor).collect(Collectors.toList());

        Gson gson = new Gson();

        return "CATALOG_START" + gson.toJson(descriptors) + "CATALOG_END";
    }
}
