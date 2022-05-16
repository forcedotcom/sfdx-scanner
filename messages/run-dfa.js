module.exports = {
	"commandDescription": "evaluate a selection of DFA rules against a codebase",
	"commandDescriptionLong": `Scan codebase with all DFA rules by default.
	You can choose the format of output and decide between printing the results directly
	or as contents of a file that you provide with --outfile flag.`,
	"flags": {
		"formatDescription": "format of results",
		"formatDescriptionLong": "Specifies output format with results written directly to the console.",
		"ignoreparseerrorsDescription": "ignore compilation failures in scanned files (default: false). Alternatively, set value using environment variable `SFGE_IGNORE_PARSE_ERRORS`",
		"ignoreparseerrorsDescriptionLong": "ignore compilation failures in scanned files. Inherits value from SFGE_IGNORE_PARSE_ERRORS env-var if set.",
		"normalizesevDescription": "return normalized severity in addition to the engine specific severity",
		"normalizesevDescriptionLong": "A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity. For the html option, the normalized severity is displayed instead of the engine severity",
		"outfileDescription": "location of output file",
		"outfileDescriptionLong": "Write output to a file.",
		"projectdirDescription": "project's root directory, or the directory where the target location resides along with the remaining class files it references",
		"projectdirDescriptionLong": "Root project directory. Must be paths, not globs. Multiple values can be specified as a comma-separated list",
		"rulethreadcountDescription": "number of threads evaluating dfa rules (default: 4). Alternatively, set value using environment variable `SFGE_RULE_THREAD_COUNT`",
		"rulethreadcountDescriptionLong": "Specify number of rule evaluation threads, i.e. how many entrypoints can be evaluated concurrently. Default is 4. Inherits value from SFGE_RULE_THREAD_COUNT env-var if set.",
		"rulethreadtimeoutDescription": "timeout for individual rule threads, in milliseconds (default: 900000 ms). Alternatively, set value using environment variable `SFGE_RULE_THREAD_TIMEOUT`",
		"rulethreadtimeoutDescriptionLong": "Time limit for evaluating a single entrypoint. Value in milliseconds. Inherits from SFGE_RULE_THREAD_TIMEOUT env-var if set. Default is 900,000 ms, or 15 minutes.",
		"sevthresholdDescription": "throws an error when violations of specific severity (or more severe) are detected, invokes --normalize-severity",
		"sevthresholdDescriptionLong": "Throws an error if violations are found with equal or greater severity than provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation. Using this flag also invokes the --normalize-severity flag",
		"targetDescription": "location of source code",
		"targetDescriptionLong": "Source code location. May use glob patterns. Multiple values can be specified as a comma-separated list"
	},
	"validations": {
		"projectdirCannotBeGlob": "--projectdir cannot specify globs",
		"projectdirMustBeDir": "--projectdir must specify directories",
		"projectdirMustExist": "--projectdir must specify existing paths"
	},
	"examples": `The paths specified for --projectdir must cumulatively contain all files specified through --target.
		Good: $ sfdx scanner:run:dfa --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"
		Good: $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./"
		Good: $ sfdx scanner:run:dfa --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"
		Bad:  $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./myproject"
	Wrap globs in quotes.
		Unix example:    $ sfdx scanner:run:dfa --target './**/*.cls,!./**/IgnoreMe.cls' ...
		Windows example: > sfdx scanner:run:dfa --target ".\\**\\*.cls,!.\\**\\IgnoreMe.cls" ...
			Evaluate rules against all .cls files below the current directory, except for IgnoreMe.cls.
	Use --normalize-severity to output a normalized (across all engines) severity (1 [high], 2 [moderate], and 3 [low]) in addition to the engine specific severity (when shown).
		E.g., $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --format csv --normalize-severity
	Use --severity-threshold to throw a non-zero exit code when rule violations of a specific normalized severity (or greater) are found. For this example, if there are any rule violations with a severity of 2 or more (which includes 1-high and 2-moderate), the exit code will be equal to the severity of the most severe violation.
		E.g., $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --severity-threshold 2
	Use --rule-thread-count to allow more (or fewer) entrypoints to be evaluated concurrently.
		E.g., $ sfdx scanner:run:dfa --rule-thread-count 6
	Use --rule-thread-timeout to increase (or decrease) the maximum runtime for a single entrypoint evaluation.
		E.g., $ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...
			Increases timeout from 15 minutes (default) to 150 minutes.
`
};
