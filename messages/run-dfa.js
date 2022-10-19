module.exports = {
	"commandDescription": "scan codebase with all DFA rules",
	"commandDescriptionLong": `Scans codebase with all DFA rules by default.
	Specify the format of output and print results directly or as contents of a file that you provide with --outfile flag.`,
	"flags": {
		"formatDescription": "specify results output format",
		"formatDescriptionLong": "Specifies results output format written directly to the console.",
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
	"examples": `The paths specified for --projectdir must contain all files specified through --target cumulatively.
	$ sfdx sacnner:run:dfa --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"
	$ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./"
	$ sfdx scanner:run:dfa --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"
This example fails because the set of files included in --target is larger than that contained in --projectdir:
	$ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./myproject/"
Globs must be wrapped in quotes, as in these Windows and Unix examples, which evaluate rules against all .cls files in the current directory and subdirectories except for IgnoreMe.cls:
Unix example:
	$ sfdx scanner:run:dfa --target "./**/*.cls,!./**/IgnoreMe.cls" ...
Windows example:
	$ sfdx scanner:run:dfa --target ".\\**\\*.cls,!.\\**\\IgnoreMe.cls" ...
You can target individual methods within a file with a suffix hash (#) on the file's path, and with a semi-colon-delimited list of method names. This syntax is incompatible with globs and directories. This example evaluates rules against all methods named Method1 or Method2 in File1.cls, and all methods named Method3 in File2.cls:
	$ sfdx scanner:run:dfa --target "./File1.cls#Method1;Method2,./File2.cls#Method3" ...
Use --normalize-severity to output a normalized severity across all engines, in addition to the engine-specific severity. Normalized severity is 1 (high), 2 (moderate), and 3 (low):
	$ sfdx scanner:run:dfa --target "./some-project/" --projectdir "./some-project/" --format csv --normalize-severity
Use --severity-threshold to throw a non-zero exit code when rule violations of a specific normalized severity or greater are found. If there are any rule violations with a severity of 2 or 1, the exit code is equal to the severity of the most severe violation:
	$ sfdx scanner:run:dfa --target "./some-project/" --projectdir "./some-project/" --severity-threshold 2
use --rule-thread-count to allow more (or fewer) entrypoints to be evaluated concurrently:
	$ sfdx scanner:run:dfa --rule-thread-count 6 ...
Use --rule-thread-timeout to increase or decrease the maximum runtime for a single entrypoint evaluation. This increases the timeout from the 15-minute default to 150 minutes:
	$ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...
Use --sfgejvmargs to pass Java Virtual Machine args to override system defaults while executing Salesforce Graph Engine's rules.
The example overrides the system's default heap space allocation to 8 GB and decreases chances of encountering OutOfMemory error.
	$ sfdx scanner:run:dfa --sfgejvmargs "-Xmx8g" ...
`
};
