module.exports = {
	"commandDescription": "evaluate a selection of rules against a codebase",
	"commandDescriptionLong": `Scan codebase with all rules by default
	or with a chosen set of rules if using rulename/category/ruleset filters.
	You can choose the format of output and decide between printing the results directly
	or as contents of a file that you provide with --outfile flag.`,
	"flags": {
		"rulenameDescription": "[description of 'rulename' parameter]",                   // TODO: Change this once the flag is implemented.
		"categoryDescription": "categor(ies) of rules to run",
		"categoryDescriptionLong": "One or more categories of rules to run. Multiple values can be specified as a comma-separated list.",
		"rulesetDescription": "ruleset(s) of rules to run",
		"rulesetDescriptionLong": "One or more rulesets to run. Multiple values can be specified as a comma-separated list.",
		"severityDescription": "[description of 'severity' parameter]",                   // TODO: Change this once the flag is implemented.
		"excluderuleDescription": "[description of 'exclude-rule' parameter]",            // TODO: Change this once the flag is implemented.
		"orgDescription": "[description of 'org' parameter]",                             // TODO: Change this once the flag is implemented.
		"suppresswarningsDescription": "[description of 'suppress-warnings' parameter]",  // TODO: Change this once the flag is implemented.
		"targetDescription": "location of source code",
		"targetDescriptionLong": "Source code location. May use glob patterns. Multiple values can be specified as a comma-separated list",
		"formatDescription": "format of results",
		"formatDescriptionLong": "Specifies output format with results written directly to the console.",
		"outfileDescription": "location of output file",
		"outfileDescriptionLong": "Write output to a file.",
		"envDescription": "JSON-formatted string, overrides ESLint's default environment variables",
		"envDescriptionLong": "JSON-formatted string. Overrides ESLint's default environmental variables.",
		"envParamDeprecationWarning": "--env parameter is being deprecated, and will be removed in 3.0.0.",
		"tsconfigDescription": "Location of tsconfig.json file",
		"tsconfigDescriptionLong": "Location of tsconfig.json file used by eslint-typescript engine."
	},
	"validations": {
		"mustTargetSomething": "Please specify a codebase using --target.", // TODO: Once --org is implemented, rewrite this message.
		"outfileFormatMismatch": "Your chosen format %s does not appear to match your output file type of %s.",
		"outfileMustBeValid": "--outfile must be a well-formed filepath.",
		"outfileMustBeSupportedType": "--outfile must be of a supported type. Current options are .xml and .csv."
	},
	"output": {
		"noViolationsDetected": "No rule violations found.",
		"invalidEnvJson": "--env parameter must be a well-formed JSON.",
		"writtenToOutFile": "Rule violations have been written to %s."
	},
	"examples": `Invoking without specifying any rules causes all rules to be run.
	E.g., $ sfdx scanner:run --format xml --target "somefile.js"
		Evaluates all rules against somefile.js.

	Specifying multiple categories or rulesets is treated as a logical OR.
		E.g., $ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices" --ruleset "Braces"
			Evaluates all rules in the Design and Best Practices categories, and all rules in the Braces ruleset.

	Wrap globs in quotes.
		Unix example:    $ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
		Windows example: > sfdx scanner:run --target ".\\**\\*.js,!.\\**\\IgnoreMe.js" ...
			Evaluate rules against all .js files below the current directory, except for IgnoreMe.js.

	Specify tsconfig.json if the current working directory does not contain the tsconfig.json that corresponds to the TypeScript files being scanned.
		E.g., sfdx scanner:run --target "/my-project/**/*.ts" --tsconfig "/my-project/tsconfig.json"
			Scans the project contained in '/my-project' if the current working directory is another directory.
`
};
