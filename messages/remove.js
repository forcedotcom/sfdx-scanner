module.exports = {
	"commandDescription": "remove custom rules from the registry of available rules",
	"commandDescriptionLong": `Removes custom rules from the registry of available rules. Use the \`-p|--path\` parameter to specify one or more paths to remove. If you don't specify any parameters, the command lists all valid custom paths but doesn't remove any.`,
	"flags": {
		"forceDescription": "bypass the confirmation prompt and immediately remove the rules",
		"forceDescriptionLong": "Bypasses the confirmation prompt and immediately removes the rules.",
		"pathDescription": "one or more paths to remove",
		"pathDescriptionLong": "One or more paths to remove. Specify multiple values with a comma-separated list."
	},
	"validations": {
		"pathCannotBeEmpty": "Specify at least one path."
	},
	"errors": {
		"noMatchingPaths": "No registered custom rules match the provided paths."
	},
	"output": {
		"aborted": "The operation stopped.",
		"dryRunReturnedNoRules": "No custom rules are registered.",
		"dryRunOutput": "%i custom path(s) available for removal:\n%s",
		// Use a bit of leading whitespace so the paths hang underneath the initial line.
		"dryRunRuleTemplate": "   %s",
		// Use a bit of leading whitespace so the rules hang underneath the initial line.
		"ruleTemplate": "   '%s', defined in %s",
		"deletionPrompt": "These rules will be unregistered:\n%s\nDo you wish to proceed? (y/n)",
		"resultSummary": "Success. These rules were unregistered: %s.",
	},
	"examples": `This example runs the command without arguments to see a list of registered custom paths.
	$ sfdx scanner:rule:remove

This example uses the --path parameter to deregister the rules defined in somerules.jar and any JARs/XMLs contained in the rules folder.
	$ sfdx scanner:rule:remove --path "~/path/to/somerules.jar,~/path/to/folder/containing/rules"

This example uses the --force flag to bypass the confirmation prompt, removing all rules defined in somerules.jar.
By default, a list of all rules that will be unregistered is displayed, and the action must be confirmed. To bypass that confirmation, use the --force flag.
	$ sfdx scanner:rule:remove --force --path "~/path/to/somerules.jar"
`
};
