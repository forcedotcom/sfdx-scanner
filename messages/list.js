module.exports = {
	"commandDescription": "lists basic information about all rules matching provided criteria",
	"commandDescriptionLong": `Lists all the rules available in the catalog. To look at a smaller set of rules,
	use the filter options available. To get more information about a specific rule,
	you can use the scanner:rule:describe command.`,
	"flags": {
		"languageDescription": "language(s) to filter list by",
		"languageDescriptionLong": "Filters the list based on the specified languages. Specify multiple languages as a comma-separated list. See the PMD CLI documentation for a list of supported languages.",
		"categoryDescription": "categories to filter list by",
		"categoryDescriptionLong": "Select rules by category. Enter multiple values as a comma-separated list. E.g., 'Best Practices',Performance ",
		"rulesetDescription": "ruleset(s) to filter list by",
		"rulesetDescriptionLong": "Select rules by ruleset. Enter multiple values as a comma-separated list.",
		"severityDescription": "[description of 'severity' parameter]", // Change this when we implement the flag.
		"standardDescription": "[description of 'standard' parameter]", // Change this when we implement the flag.
		"customDescription": "[description of 'custom' parameter]"      // Change this when we implement the flag.
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
