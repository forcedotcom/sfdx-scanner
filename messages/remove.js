module.exports = {
	"commandDescription": "[Short description]",
	"commandDescriptionLong": "[Longer description]",
	"flags": {
		"forceDescription": "[Description of --force flag]",
		"forceDescriptionLong": "[Longer description of --force flag]",
		"pathDescription": "[Description of --path flag]",
		"pathDescriptionLong": "[Longer description of --path flag]",
		"languageDescription": "[Description of --language flag]",
		"languageDescriptionLong": "[Longer description of --language flag]"
	},
	"validations": {
		"languageCannotBeEmpty": "Language cannot be empty",
		"pathCannotBeEmpty": "Path cannot be empty"
	},
	"errors": {
		"noMatchingPaths": "No registered custom rules match the provided paths."
	},
	"output": {
		"aborted": "Operation aborted.",
		// Use a bit of leading whitespace so the rules hang underneath the initial line.
		"ruleTemplate": "   '%s', defined in %s",
		"deletionPrompt": "NOTE: This action will unregister the following %i rule(s):\n%s\nDo you wish to proceed? (y/n)",
		"resultSummary": "Successfully unregistered all rules defined in %s."
	},
	"examples": `Some examples will go here`
};
