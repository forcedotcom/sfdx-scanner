module.exports = {
	"commandDescription": `add custom rules to Salesforce Code Analyzer's registry`,
	"commandDescriptionLong": `Adds custom rules to Salesforce Code Analyzer's registry so that you can run them along with the built-in rules. Compile and test custom rules separately before adding them.`,
	"flags": {
		"languageDescription": "language that the custom rules are evaluated against",
		"languageDescriptionLong": "Language that the custom rules are evaluated against.",
		"pathDescription": "one or more paths (such as a directory or JAR file) to custom rule definitions",
		"pathDescriptionLong": `One or more paths (such as a directory or JAR file) to  custom rule definitions. Specify multiple values as a comma-separated list.`
	},
	"validations": {
		"languageCannotBeEmpty": "Specify a language",
    "pathCannotBeEmpty": "Specify a path"
	},
	"examples": `Bundle custom PMD rules in JAR files. Follow PMD conventions, such as defining the custom rules in XML files under a \`/category/\` directory.
See PMD's documentation for more information on writing rules.

This example shows how to specify two JAR files directly.
	$ sfdx scanner:rule:add --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar"
		Successfully added rules for apex.
		2 path(s) added:
		/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar

This example shows how to specify a directory containing one or more JARs, all of which are added to the registry.
	$ sfdx scanner:rule:add --language apex --path "/Users/me/rules"
		Successfully added rules for apex.`
};
