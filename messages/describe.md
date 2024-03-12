# commandSummary

provide detailed information about a rule

# commandDescription

Provides detailed information about a rule. Information includes the rule's language (such as Apex or Java), the violation it detects, and example code of the violation. The command output also includes the rule's categories and rulesets.

# flags.rulenameSummary

the name of the rule

# flags.rulenameDescription

The name of the rule.

# flags.previewPmd7Summary

use PMD version %s to describe PMD and CPD rules

# flags.previewPmd7Description

Uses PMD version %s instead of %s to describe PMD and CPD rules.

# output.noMatchingRules

No rules were found with the name '%s'.

# output.multipleMatchingRules

%s rules with the name '%s' were found.

# examples.normalExample

$ <%= config.bin %> <%= command.id %> --rulename ExampleRule
	name:        AvoidWithStatement
	categories:   Best Practices
	rulesets:    Controversial Ecmascript
	languages:   javascript
	description: Avoid using with - it's bad news
	message:     Avoid using with - it's bad news
