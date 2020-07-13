module.exports = {
	"commandDescription": "removes custom rules from the registry of available rules",
	"commandDescriptionLong": `Removes custom rules from the registry of available rules. Use the --path parameter to
specify one or more paths to remove, or omit it to receive a list of all valid custom paths.`,
	"flags": {
		"forceDescription": "bypass the confirmation prompt and immediately unregister the rules",
		"forceDescriptionLong": "Bypass the confirmation prompt and immediately unregister the rules.",
		"pathDescription": "one or more paths to deregister",
		"pathDescriptionLong": "One or more paths to deregister. Specify multiple values with a comma-separated list."
	},
	"validations": {
		"pathCannotBeEmpty": "Path cannot be empty"
	},
	"errors": {
		"noMatchingPaths": "No registered custom rules match the provided paths."
	},
	"output": {
		"aborted": "Operation aborted.",
		"dryRunReturnedNoRules": "No custom rules currently registered.",
		"dryRunOutput": "%i custom path(s) available for removal:\n%s",
		// Use a bit of leading whitespace so the paths hang underneath the initial line.
		"dryRunRuleTemplate": "   %s",
		// Use a bit of leading whitespace so the rules hang underneath the initial line.
		"ruleTemplate": "   '%s', defined in %s",
		"deletionPrompt": "NOTE: This action will unregister the following %i rule(s):\n%s\nDo you wish to proceed? (y/n)",
		"resultSummary": "Successfully unregistered all rules defined in %s.",
	},
	"examples": `Run the command with no arguments to see a list of all currently registered custom paths.
	E.g., $ sfdx scanner:rule:remove
		Returns all registered custom paths.

You may use the --path parameter to specify one or more paths to remove.
	E.g., $ sfdx scanner:rule:remove --path "~/path/to/somerules.jar,~/path/to/folder/containing/rules"
		Deregisters the rules defined in somerules.jar and any JARs contained in the rules folder.

By default, a list of all the rules that will be deregistered is displayed, and the action must be confirmed.
The --force flag may be used to bypass that confirmation.
	E.g., $ sfdx scanner:rule:remove --force --path "~/path/to/somerules.jar"
		Deregisters somerules.jar without requiring confirmation.
`
};
