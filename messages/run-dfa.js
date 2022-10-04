module.exports = {
	"commandDescription": "scan codebase with all DFA rules",
	"commandDescriptionLong": `Scans codebase with all DFA rules by default.
	Specify the format of output and print results directly or as contents of a file that you provide with --outfile flag.`,
	"flags": {
		"formatDescription": "specify results output format",
		"formatDescriptionLong": "Specifies results output format written directly to the console.",
		"ignoreparseerrorsDescription": "ignore compilation failures in scanned files. Alternatively, set value using environment variable `SFGE_IGNORE_PARSE_ERRORS`.",
		"ignoreparseerrorsDescriptionLong": "Ignores compilation failures in scanned files (default: false). Inherits value from SFGE_IGNORE_PARSE_ERRORS env-var if set.",
		"normalizesevDescription": "return normalized severity in addition to the engine-specific severity",
		"normalizesevDescriptionLong": "Returns normalized severity 1 (high), 2 (moderate), and 3 (low) and the engine-specific severity. For the html option, normalized severity is displayed instead of the engine severity.",
		"outfileDescription": "write output to a file",
		"outfileDescriptionLong": "Writes output to a file.",
		"projectdirDescription": "provide root directory of project",
		"projectdirDescriptionLong": "Provides the relative or absolute root project directory used to set the context for Graph Engine's analysis. Project directory must be a path, not a glob. Specify multiple values as a comma-separated list.",
		"ruledisablewarningviolationDescription": "disable warning violations from Salesforce Graph Engine. Alternatively, set value using environment variable `SFGE_RUlE_DISABLE_WARNING_VIOLATION`",
		"ruledisablewarningviolationDescriptionLong": "Disables warning violations, such as those on StripInaccessible READ access, to get only high-severity violations (default: false). Inherits value from SFGE_RULE_DISABLE_WARNING_VIOLATION env-var if set.",
		"rulethreadcountDescription": "specify number of threads that evaluate DFA rules. Alternatively, set value using environment variable `SFGE_RULE_THREAD_COUNT`. Default is 4",
		"rulethreadcountDescriptionLong": "Specifies number of rule evaluation threads, or how many entrypoints can be evaluated concurrently. Inherits value from SFGE_RULE_THREAD_COUNT env-var, if set. Default is 4.",
		"rulethreadtimeoutDescription": "specify timeout for individual rule threads in milliseconds. Alternatively, set the timeout value using environment variable `SFGE_RULE_THREAD_TIMEOUT`. Default: 90000 ms",
		"rulethreadtimeoutDescriptionLong": "Specifies time limit for evaluating a single entrypoint in milliseconds. Inherits value from SFGE_RULE_THREAD_TIMEOUT env-var if set. Default is 900,000 ms, or 15 minutes.",
		"sevthresholdDescription": "throw an error when violations of specific or higher severity are detected, and invoke --normalize-severity",
		"sevthresholdDescriptionLong": "Throws an error when violations are found with equal or greater severity than provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation. Using this flag also invokes the --normalize-severity flag.",
		"sfgejvmargsDescription": "specify Java Virtual Machine (JVM) arguments to optimize Salesforce Graph Engine execution to your system (optional)",
		"sfgejvmargsDescriptionLong": "Specifies Java Virtual Machine arguments to override system defaults while executing Salesforce Graph Engine. For multiple arguments, add them to the same string separated by space.",
		"targetDescription": "return location of source code",
		"targetDescriptionLong": "Returns the source code location. Use glob patterns or specify individual methods with #-syntax. Multiple values are specified as a comma-separated list."
	},
	"validations": {
		"methodLevelTargetCannotBeGlob": "Method-level targets supplied to --target cannot be globs",
		"methodLevelTargetMustBeRealFile": "Method-level target %s must be a real file",
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
	Individual methods within a file may be targeted by suffixing the file's path with a hash (#), and a semi-colon-delimited list of method names. This syntax is incompatible with globs and directories.
		E.g., $ sfdx scanner:run:dfa --target "./File1.cls#Method1;Method2,./File2.cls#Method3" ...
			Evaluates rules against ALL methods named Method1 or Method2 in File1.cls, and ALL methods named Method3 in File2.cls.
	Use --normalize-severity to output a normalized (across all engines) severity (1 [high], 2 [moderate], and 3 [low]) in addition to the engine specific severity (when shown).
		E.g., $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --format csv --normalize-severity
	Use --severity-threshold to throw a non-zero exit code when rule violations of a specific normalized severity (or greater) are found. For this example, if there are any rule violations with a severity of 2 or more (which includes 1-high and 2-moderate), the exit code will be equal to the severity of the most severe violation.
		E.g., $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --severity-threshold 2
	Use --rule-thread-count to allow more (or fewer) entrypoints to be evaluated concurrently.
		E.g., $ sfdx scanner:run:dfa --rule-thread-count 6 ...
	Use --rule-thread-timeout to increase (or decrease) the maximum runtime for a single entrypoint evaluation.
		E.g., $ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...
			Increases timeout from 15 minutes (default) to 150 minutes.
	Use --sfgejvmargs to pass JVM args to override system defaults while executing Salesforce Graph Engine's rules.
		E.g., $ sfdx scanner:run:dfa --sfgejvmargs "-Xmx8g" ...
			Overrides system's default heapspace allocation to 8g and decreases chances of encountering OutOfMemory error.
`
};
