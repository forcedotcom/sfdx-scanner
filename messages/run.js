module.exports = {
	"commandDescription": "evaluate a selection of rules against a codebase",
	"commandDescriptionLong": `Scan codebase with all rules by default
	or with a chosen set of rules if using rulename/category/ruleset filters.
	You can choose the format of output and decide between printing the results directly
	or as contents of a file that you provide with --outfile flag.`,
	"flags": {
		"categoryDescription": "categor(ies) of rules to run",
		"categoryDescriptionLong": "One or more categories of rules to run. Multiple values can be specified as a comma-separated list.",
		"rulesetDescription": "[deprecated] ruleset(s) of rules to run",
		"rulesetDescriptionLong": "[Deprecated] One or more rulesets to run. Multiple values can be specified as a comma-separated list.",
		"targetDescription": "location of source code",
		"targetDescriptionLong": "Source code location. May use glob patterns. Multiple values can be specified as a comma-separated list",
		"formatDescription": "format of results",
		"formatDescriptionLong": "Specifies output format with results written directly to the console.",
		"outfileDescription": "location of output file",
		"outfileDescriptionLong": "Write output to a file.",
		"envDescription": "JSON-formatted string, overrides ESLint's default environment variables",
		"envDescriptionLong": "JSON-formatted string. Overrides ESLint's default environmental variables.",
		"envParamDeprecationWarning": "--env parameter is being deprecated, and will be removed in 3.0.0.",
		"tsconfigDescription": "location of tsconfig.json file",
		"tsconfigDescriptionLong": "Location of tsconfig.json file used by eslint-typescript engine.",
		"vceDescription": "throws an error when violations are detected",
		"vceDescriptionLong": "Throws an error when violations are detected. Exit code is the most severe violation.",
		'engineDescription': "engine(s) to run",
		'engineDescriptionLong': "One or more engines to run. Multiple values can be specified as a comma-separated list.",
		'eslintConfigDescription': 'location of eslintrc config to customize eslint engine',
		'eslintConfigDescriptionLong': 'Location of eslintrc to customize eslint engine',
		'pmdConfigDescription': 'location of PMD rule reference XML file to customize rule selection',
		'pmdConfigDescriptionLong': 'Location of PMD rule reference XML file to customize rule selection'
	},
	"validations": {
		"outfileFormatMismatch": "Your chosen format %s does not appear to match your output file type of %s.",
		"outfileMustBeValid": "--outfile must be a well-formed filepath.",
		"outfileMustBeSupportedType": "--outfile must be of a supported type. Current options are .xml and .csv.",
		"tsConfigEslintConfigExclusive": "You cannot specify --tsconfig flag if you have specified --eslintconfig flag. Please provide tsconfig path within the eslint config file under 'parseOptions.project'."
	},
	"output": {
		"noViolationsDetected": "Executed engines: %s. No rule violations found.",
		"invalidEnvJson": "--env parameter must be a well-formed JSON.",
		"engineSummaryTemplate": "Executed %s, found %s violation(s) across %s file(s).",
		"writtenToOutFile": "Rule violations have been written to %s.",
		"writtenToConsole": "Rule violations logged to console above.",
		"sevDetectionSummary": "Detected rule violations of severity %s or lower.",
		"pleaseSeeAbove": "Please see the logs above.",
		"filtersIgnoredCustom": "Rule filters will be ignored by engines that are run with custom config (using --pmdconfig or --eslintconfig flags). Please modify your config file to reflect the filtering you need."
	},
	"rulesetDeprecation": "'ruleset' command parameter is deprecated. Please use 'category' instead",
	"examples": `Invoking without specifying any rules causes all rules to be run.
	E.g., $ sfdx scanner:run --format xml --target "somefile.js"
		Evaluates all rules against somefile.js.

	Specifying multiple categories is treated as a logical OR.
		E.g., $ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices"
			Evaluates all rules in the Design or Best Practices categories.

	Categories can be excluded by specifying the negation operator, the values must be enclosed in single quotes.
		E.g., $ sfdx scanner:run --format xml --target "somefile.js" --category '!Design,!Best Practices'
			Evaluates all rules except those in the Design or Best Practices categories.

	Wrap globs in quotes.
		Unix example:    $ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
		Windows example: > sfdx scanner:run --target ".\\**\\*.js,!.\\**\\IgnoreMe.js" ...
			Evaluate rules against all .js files below the current directory, except for IgnoreMe.js.

	Specify tsconfig.json if the current working directory does not contain the tsconfig.json that corresponds to the TypeScript files being scanned.
		E.g., sfdx scanner:run --target "/my-project/**/*.ts" --tsconfig "/my-project/tsconfig.json"
			Scans the project contained in '/my-project' if the current working directory is another directory.

	Use --env to override the default ESLint environment variables to add frameworks.
		E.g., $ sfdx scanner:run --target "somefile.js" --env '{"jasmine": true}'
			Evaluates rules against somefile.js, including Jasmine in the environment variables.

	Use --violations-cause-error to throw exit with a non-zero code when violations are found.
		E.g., $ sfdx scanner:run --target "somefile.js" --violations-cause-error
			Evaluates rules against somefile.js. If any rules are violated, the exit code will be the severity of the most severe violation.

	Use --engine to include or exclude engines. Any engine listed will be run, regardless of its current 'disabled' attribute.
		E.g., $ sfdx scanner:run --target "somefile.js" --engine "eslint-lwc,pmd"
			Evaluates rules against somefile.js, using eslint-lwc and pmd engines.

	To use PMD with your own rule reference file, use --pmdconfig. Note that rule filters are not applied.
		E.g, $ sfdx scanner:run --target "src" --pmdconfig "pmd_rule_ref.xml"

	To use Eslint with your own .eslintrc.json file, use --eslintconfig. Make sure that the directory you run the command from has all the NPM dependencies installed.
		E.g., $ sfdx scanner:run --target "src" --eslintconfig "/home/my/setup/.eslintrc.json"
	`
};
