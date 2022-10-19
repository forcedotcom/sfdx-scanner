module.exports = {
	"commandDescription": "provide detailed information about a rule",
	"commandDescriptionLong": `Provides detailed information about a rule. Information includes the rule's language (such as Apex or Java), the violation it detects, and example code of the violation. The command output also includes the rule's categories and rulesets.`,
	"flags": {
		"rulenameDescription": "the name of a rule",
		"rulenameDescriptionLong": "The name of the rule."
	},
	"output": {
		"noMatchingRules": "No rules were found with the name '%s'.",
		"multipleMatchingRules": "%s rules with the name '%s' were found."
	},
	"examples": {
		// The example for when only one rule matches the provided name.
		"normalExample": `$ sfdx scanner:rule:describe --rulename ExampleRule
	name:        AvoidWithStatement
	categories:   Best Practices
	rulesets:    Controversial Ecmascript
	languages:   javascript
	description: Avoid using with - it's bad news
	message:     Avoid using with - it's bad news
	`
	}
};
