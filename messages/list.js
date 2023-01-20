module.exports = {
	"commandDescription": "list basic information about all rules matching provided criteria",
	"commandDescriptionLong": `Lists all the rules available in the catalog. You can filter the output to view a smaller set of rules. To get more information about a specific rule, use the \`scanner:rule:describe\` command.`,
	"flags": {
		"languageDescription": "select rules by language",
		"languageDescriptionLong": "Selects rules by language. Enter multiple values as a comma-separated list.",
		"categoryDescription": "select rules by category",
		"categoryDescriptionLong": "Selects rules by category. Enter multiple values as a comma-separated list.",
		"rulesetDescription": "[deprecated] select rules by ruleset",
		"rulesetDescriptionLong": "[deprecated] Selects rules by ruleset. Enter multiple values as a comma-separated list.",
		'engineDescription': "select rules by engine",
		'engineDescriptionLong': "Selects rules by engine. Enter multiple engines as a comma-separated list."
	},
	"rulesetDeprecation": "The 'ruleset' command parameter is deprecated. Use 'category' instead",
	"columnNames": {
		"name": "name",
		"languages": "languages",
		"categories": "categories",
		"rulesets": "rulesets [dep]",
		"is-dfa": "is dfa",
		"engine": "engine"
	},
	"yes": "Y",
	"no": "N",
	"examples": `
This example invokes the command without filter criteria, which returns all rules.
	$ sfdx scanner:rule:list

This example returns all rules for Apex OR Javascript. Values supplied to a single filter are handled with a logical OR.
	$ sfdx scanner:rule:list --language apex,javascript

This example returns all rules except those in the Design or Best Practices categories. Exclude categories by specifying the negation operator and enclosing the values in single quotes.
	$ sfdx scanner:rule:list --category '!Design,!Best Practices'

This example returns all rules that target Apex OR Javascript, AND are members of the Braces OR Security rulesets.
The different filters are combined with a logical AND.
	$ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
`
};
