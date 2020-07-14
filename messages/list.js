module.exports = {
	"commandDescription": "Lists basic information about all rules matching provided criteria",
	"commandDescriptionLong": `Lists all the rules available in the catalog. To look at a smaller set of rules,
	use the filter options available. To get more information about a specific rule,
	you can use the scanner:rule:describe command.
	Please make sure your machine has Java 8 or greater setup correctly.`,
	"flags": {
		"languageDescription": "language(s) to filter list by",
		"languageDescriptionLong": "Select rules by language. Enter multiple values as a comma-separated list.",
		"categoryDescription": "categories to filter list by",
		"categoryDescriptionLong": "Select rules by category. Enter multiple values as a comma-separated list.",
		"rulesetDescription": "ruleset(s) to filter list by",
		"rulesetDescriptionLong": "Select rules by ruleset. Enter multiple values as a comma-separated list.",
		"severityDescription": "[Description of 'severity' parameter]", // Change this when we implement the flag.
		"standardDescription": "[Description of 'standard' parameter]", // Change this when we implement the flag.
		"customDescription": "[Description of 'custom' parameter]"      // Change this when we implement the flag.
	},
	"examples": `Invoking with no filter criteria returns all rules.
	E.g., $ sfdx scanner:rule:list
		Returns a table containing all rules.
	
The values supplied to a single filter are handled with a logical OR.
	E.g., $ sfdx scanner:rule:list --language apex,javascript
		Returns all rules for Apex OR Javascript.

Different filters are combined with a logical AND.
	E.g., $ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
		Returns all rules that:
		1) Target Apex OR Javascript,
		AND...
		2) Are members of the Braces OR Security rulesets.
	`
};
