# commandSummary

Provide detailed information about a rule that includes the rule's language (such as Apex or Java), the violation it detects, example code of the violation, and the rule's categories and rulesets.

# flags.rulenameSummary

The name of the rule.

# output.noMatchingRules

No rules were found with the name '%s'.

# output.multipleMatchingRules

%s rules with the name '%s' were found.

# examples.normalExample

- This example shows how to describe the ApexBadCrypto rule.

	<%= config.bin %> <%= command.id %> --rulename ApexBadCrypto
