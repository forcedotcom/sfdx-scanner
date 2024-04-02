# commandSummary

Detailed information about a rule that includes the rule's language (such as Apex or Java), the violation it detects, example code of the violation, and the rule's categories and rulesets.

# flags.rulenameSummary

The name of the rule.

# output.noMatchingRules

No rules were found with the name '%s'.

# output.multipleMatchingRules

%s rules with the name '%s' were found.

# examples.normalExample

- This example shows a typical describe result:

	<%= config.bin %> <%= command.id %> --rulename ExampleRule
	name:        AvoidWithStatement
	categories:   Best Practices
	rulesets:    Controversial Ecmascript
	languages:   javascript
	description: Avoid using with - it's bad news
	message:     Avoid using with - it's bad news
