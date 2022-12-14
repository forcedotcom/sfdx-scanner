module.exports = {
	"commandDescription": "scan a codebase with a selection of rules",
	"commandDescriptionLong": `Scans a codebase with a selection of rules. You can scan the codebase with all the rules in the registry, or use parameters to filter the rules based on rulename, category, or ruleset. You can specify the format of the output, such as XML or JUnit. You can print the output to the console (default) or to a file using the --outfile parameter.`,
	"flags": {
		"categoryDescription": "one or more categories of rules to run",
		"categoryDescriptionLong": "One or more categories of rules to run. Specify multiple values as a comma-separated list.",
		"rulesetDescription": "[deprecated] rulesets to run",
		"rulesetDescriptionLong": "[deprecated] One or more rulesets to run. Specify multiple values as a comma-separated list.",
		"targetDescription": "source code location",
		"targetDescriptionLong": "Source code location. May use glob patterns. Specify multiple values as a comma-separated list.",
		"envDescription": "[deprecated] override ESLint's default environment variables, in JSON-formatted string",
		"envDescriptionLong": "[deprecated] Overrides ESLint's default environmental variables, in JSON-formatted string.",
		"envParamDeprecationWarning": "--env parameter is being deprecated, and will be removed in a future release.",
		"tsconfigDescription": "location of tsconfig.json file",
		"tsconfigDescriptionLong": "Location of tsconfig.json file used by eslint-typescript engine.",
		'engineDescription': "specify which engines to run",
		'engineDescriptionLong': "Specifies one or more engines to run. Submit multiple values as a comma-separated list.",
		'eslintConfigDescription': 'specify the location of eslintrc config to customize eslint engine',
		'eslintConfigDescriptionLong': 'Specifies the location of eslintrc config to customize eslint engine.',
		'pmdConfigDescription': 'specify location of PMD rule reference XML file to customize rule selection',
		'pmdConfigDescriptionLong': 'Specifies the location of PMD rule reference XML file to customize rule selection.',
		"verboseViolationsDescription": "return retire-js violation message details",
		"verboseViolationsDescriptionLong": "Returns retire-js violation messages details about each vulnerability, including summary, Common Vulnerabilities and Exposures (CVE), and URLs."
	},
	"validations": {
		"methodLevelTargetingDisallowed": "The target '%s' is invalid because method-level targeting isn't supported with this command.",
		"tsConfigEslintConfigExclusive": "A --tsconfig flag can't be specified with an --eslintconfig flag. Review your tsconfig path in the eslint config file under 'parseOptions.project'.",
	},
	"output": {
		"invalidEnvJson": "--env parameter must be a well-formed JSON.",
		"filtersIgnoredCustom": "Rule filters will be ignored by engines that are run with custom config using --pmdconfig or --eslintconfig flags. Modify your config file to include your filters."
	},
	"rulesetDeprecation": "The 'ruleset' command parameter is deprecated. Use 'category' instead.",
	"examples": `This example evaluates all rules against somefile.js.
Invoking code analyzer without specifying any rules causes all rules to be run.
	$ sfdx scanner:run --format xml --target "somefile.js"

This example evaluates all rules in the Design and Best Practices categories.
When you specify multiple categories or rulesets, the results are combined with a logical OR.
	$ sfdx scanner:run --format xml --target "somefile.js" --

This example evaluates all rules except those in the Design or Best Practices categories.
Exclude categories by specifying the negation operator and enclosing the values in single quotes.
	$ sfdx scanner:run --format xml --target "somefile.js" --category '!Design,!Best Practices'

Wrap globs in quotes. These examples evaluate rules against all .js files in the current directory, except for IgnoreMe.js.
Unix example:
	$ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
Windows example:
	$ sfdx scanner:run --target ".\\**\\*.js,!.\\**\\IgnoreMe.js" ...

This example scans the project contained in '/my-project' if the current working directory is another directory.
Specify tsconfig.json if the current working directory does not contain the tsconfig.json that corresponds to the TypeScript files being scanned.
	$ sfdx scanner:run --target "/my-project/**/*.ts" --tsconfig "/my-project/tsconfig.json"

This example evaluates rules against somefile.js, including Jasmine in the environment variables.
Uses --env to override the default ESLint environment variables to add frameworks.
	$ sfdx scanner:run --target "somefile.js" --env '{"jasmine": true}'

This example evaluates rules aginst somefile.js using eslint-lwc and pmd engines.
Use --engine to include or exclude engines. Any engine listed will be run, regardless of its current 'disabled' attribute.
	$ sfdx scanner:run --target "somefile.js" --engine "eslint-lwc,pmd"

This example executes CPD engine against known file extensions in "/some/dir". CPD helps detect blocks of code duplication in selected languages.
Use --engine to invoke engines that are not enabled by default.
	$ sfdx scanner:run --target "/some/dir" --engine cpd

This example executes rules defined in pmd_rule_ref.xml against the files in 'src'.
To use PMD with your own rule reference file, use --pmdconfig. Note that rule filters are not applied.
	$ sfdx scanner:run --target "src" --pmdconfig "pmd_rule_ref.xml"

This example uses a custom config to scan the files in 'src'.
To use ESLint with your own .eslintrc.json file, use --eslintconfig. Make sure that the directory you run the command from has all the NPM dependencies installed.
	$ sfdx scanner:run --target "src" --eslintconfig "/home/my/setup/.eslintrc.json"

This example uses --normalize-severity to output normalized severity and engine-specific severity across all engines. Normalized severity is: 1 (high), 2 (moderate), and 3 (low).
	$ sfdx scanner:run --target "/some-project/" --format csv --normalize-severity

This example uses --severity-threshold to throw a non-zero exit code when rule violations of normalized severity 2 or greater are found. If any violations with the specified severity (or greater) are found, the exit code equals the severity of the most severe violation.
	$ sfdx scanner:run --target "/some-project/" --severity-threshold 2
`
};
