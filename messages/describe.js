module.exports = {
	"commandDescription": "provide detailed information about a rule",
	"commandDescriptionLong": `Use this command to better understand a particular rule.
	For each rule, you can find information about the language it works on,
	the violation it detects as well as an example code of how the violation looks.
	The description also includes the categories and rulesets that the rule belongs to.`,
	"flags": {
		"rulenameDescription": "the name of a rule",
		"rulenameDescriptionLong": "Name of the rule to describe in more detail."
	},
	"output": {
		"noMatchingRules": "No rules exist with the name '%s'",
		"multipleMatchingRules": "Found %s rules with the name '%s'"
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
