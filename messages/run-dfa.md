# commandSummary

scan codebase with all DFA rules

# commandDescription

Scans codebase with all DFA rules by default.
	Specify the format of output and print results directly or as contents of a file that you provide with --outfile flag.

# flags.pathexplimitSummary

specify a path expansion  upper boundary to limit the complexity of code that Graph Engine analyzes. Alternatively, set the value using environment variable `SFGE_PATH_EXPANSION_LIMIT`

# flags.pathexplimitDescription

Specifies a path expansion upper boundary to limit the complexity of code Graph Engine analyzes  before failing fast. Set the value to -1 to remove any upper boundary. --pathexplimit inherits value from SFGE_PATH_EXPANSION_LIMIT env-var, if set. Its default value is derived from JVM heap space allocation.

# flags.ruledisablewarningviolationSummary

disable warning violations from Salesforce Graph Engine. Alternatively, set value using environment variable `SFGE_RULE_DISABLE_WARNING_VIOLATION`

# flags.ruledisablewarningviolationDescription

Disables warning violations, such as those on StripInaccessible READ access, to get only high-severity violations (default: false). Inherits value from SFGE_RULE_DISABLE_WARNING_VIOLATION env-var if set.

# flags.rulethreadcountSummary

specify number of threads that evaluate DFA rules. Alternatively, set value using environment variable `SFGE_RULE_THREAD_COUNT`. Default is 4

# flags.rulethreadcountDescription

Specifies number of rule evaluation threads, or how many entrypoints can be evaluated concurrently. Inherits value from SFGE_RULE_THREAD_COUNT env-var, if set. Default is 4.

# flags.rulethreadtimeoutSummary

specify timeout for individual rule threads in milliseconds. Alternatively, set the timeout value using environment variable `SFGE_RULE_THREAD_TIMEOUT`. Default: 900000 ms

# flags.rulethreadtimeoutDescription

Specifies time limit for evaluating a single entrypoint in milliseconds. Inherits value from SFGE_RULE_THREAD_TIMEOUT env-var if set. Default is 900,000 ms, or 15 minutes.

# flags.sfgejvmargsSummary

specify Java Virtual Machine (JVM) arguments to optimize Salesforce Graph Engine execution to your system (optional)

# flags.sfgejvmargsDescription

Specifies Java Virtual Machine arguments to override system defaults while executing Salesforce Graph Engine. For multiple arguments, add them to the same string separated by space.

# flags.targetSummary

source code location

# flags.targetDescription

Source code location. Use glob patterns or specify individual methods with #-syntax. Multiple values are specified as a comma-separated list. Default is ".".

# flags.withpilotSummary

allow pilot rules to execute

# flags.withpilotDescription

Allows pilot rules to execute.

# validations.methodLevelTargetCannotBeGlob

Method-level targets supplied to --target cannot be globs

# validations.methodLevelTargetMustBeRealFile

Method-level target %s must be a real file

# examples

The paths specified for --projectdir must contain all files specified through --target cumulatively.
	$ <%= config.bin %> <%= command.id %> --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"
	$ <%= config.bin %> <%= command.id %> --target "./**/*.cls" --projectdir "./"
	$ <%= config.bin %> <%= command.id %> --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"
This example fails because the set of files included in --target is larger than that contained in --projectdir:
	$ <%= config.bin %> <%= command.id %> --target "./**/*.cls" --projectdir "./myproject/"
Globs must be wrapped in quotes, as in these Windows and Unix examples, which evaluate rules against all .cls files in the current directory and subdirectories except for IgnoreMe.cls:
Unix example:
	$ <%= config.bin %> <%= command.id %> --target "./**/*.cls,!./**/IgnoreMe.cls" ...
Windows example:
	$ <%= config.bin %> <%= command.id %> --target ".\**\*.cls,!.\**\IgnoreMe.cls" ...
You can target individual methods within a file with a suffix hash (#) on the file's path, and with a semi-colon-delimited list of method names. This syntax is incompatible with globs and directories. This example evaluates rules against all methods named Method1 or Method2 in File1.cls, and all methods named Method3 in File2.cls:
	$ <%= config.bin %> <%= command.id %> --target "./File1.cls#Method1;Method2,./File2.cls#Method3" ...
Use --normalize-severity to output a normalized severity across all engines, in addition to the engine-specific severity. Normalized severity is 1 (high), 2 (moderate), and 3 (low):
	$ <%= config.bin %> <%= command.id %> --target "./some-project/" --projectdir "./some-project/" --format csv --normalize-severity
Use --severity-threshold to throw a non-zero exit code when rule violations of a specific normalized severity or greater are found. If there are any rule violations with a severity of 2 or 1, the exit code is equal to the severity of the most severe violation:
	$ <%= config.bin %> <%= command.id %> --target "./some-project/" --projectdir "./some-project/" --severity-threshold 2
use --rule-thread-count to allow more (or fewer) entrypoints to be evaluated concurrently:
	$ <%= config.bin %> <%= command.id %> --rule-thread-count 6 ...
Use --rule-thread-timeout to increase or decrease the maximum runtime for a single entrypoint evaluation. This increases the timeout from the 15-minute default to 150 minutes:
	$ <%= config.bin %> <%= command.id %> --rule-thread-timeout 9000000 ...
Use --sfgejvmargs to pass Java Virtual Machine args to override system defaults while executing Salesforce Graph Engine's rules.
The example overrides the system's default heap space allocation to 8 GB and decreases chances of encountering OutOfMemory error.
	$ <%= config.bin %> <%= command.id %> --sfgejvmargs "-Xmx8g" ...
Use --with-pilot to allow execution of pilot rules:
This example allows pilot rules in the "Performance" category to execute.
	$ <%= config.bin %> <%= command.id %> --category 'Performance' --with-pilot ...
