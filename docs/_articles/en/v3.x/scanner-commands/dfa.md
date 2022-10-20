---
title: Salesforce Code Analyzer Command Reference
lang: en
---

## sfdx scanner:run:dfa
Execute dataflow-analysis-based rules on a target codebase. This command runs for a longer time than `scanner:run`. Also, this execution requires a path to context of where the target code resides.

Important: If your codebase is complex, increase the Java heap space to avoid OutOfMemory Errors. For more information, read ["OutOfMemory: Java heap space" Error](./en/v3.x/salesforce-graph-engine/working-with-sfge/#understand-outofmemory-java-heap-space-error).

## Usage
```bash
  $ sfdx scanner:run:dfa -t <array> -p <array> [-f csv|html|json|junit|sarif|table|xml] [-o <string>] [-s <integer> | 
  --json] [--normalize-severity] [--rule-thread-count <integer>] [--rule-thread-timeout <integer>] [--ignore-parse-errors]
   [--verbose] [--loglevel trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL]
```

## Options
```bash
  -f, --format=(csv|html|json|junit|sarif|table|xml)
      format of results

  -o, --outfile=_outfile_
      location of output file

  -p, --projectdir=_projectdir_
      (required) directory where the target location resides in or the context of the remaining files in the target\'s project

  -s, --severity-threshold=_severity-threshold_
      throws an error when violations of specific severity (or
      more severe) are detected, invokes --normalize-severity

  -t, --target=_target_
      (required) location of classes that contain entry points to analyze

  --ignore-parse-errors
      ignore compilation failures in scanned files (default: false). Alternatively, set value using environment variable `SFGE_IGNORE_PARSE_ERRORS`

  --json
      format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATAL)
      [default: warn] logging level for this command invocation

  --normalize-severity
       A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity

  --rule-thread-count=_rule-thread-count_
      number of threads evaluating dfa rules (default: 4). Alternatively, set value using environment variable `SFGE_RULE_THREAD_COUNT`

  --rule-thread-timeout=rule-thread-timeout
      timeout for individual rule threads, in milliseconds (default: 900000 ms). Alternatively, set value using environment variable `SFGE_RULE_THREAD_TIMEOUT`

  --sfgejvmargs=_sfgejvm_args_
      JVM arguments to override system defaults while executing Graph Engine. For multiple arguments, add them to the same string separated by space. Alternatively, set value using the environment variable `SFGE_JVM_ARGS`

  --verbose
      emit additional command output to stdout
```

## Environment-variable-based Controls

### *SFGE_JVM_ARGS*
Set SFGE_JVM_ARGS to work around [OutOfMemory errors](./en/v3.x/salesforce-graph-engine/working-with-sfge/#outofmemory-java-heap-space-error) and other JVM issues while executing scanner:run:dfa command. Refer to JVM documentation for more info. The equivalent flag on the `scanner:run:dfa` command is `--sfgejvmargs`.

### *SFGE_RULE_DISABLE_WARNING_VIOLATION*
Set to true to suppress warning violations, such as those related to `StripInaccessable` READ access (default: false). The equivalent flag on the `scanner:run:dfa` command is `--rule-disable-warning-violation`.

### *SFGE_RULE_THREAD_COUNT*
Modify SFGE_RULE_THREAD_COUNT to adjust the number of threads that will each execute DFA-based rules (default: 4). The equivalent flag on the `scanner:run:dfa` command is `--rule-thread-count`.

### *SFGE_RULE_THREAD_TIMEOUT*
Modify SFGE_RULE_THREAD_COUNT to adjust how long DFA-based rules execute before timing out (default: 900,000 ms or 15 minutes). Allows Salesforce Graph Engine to run longer and analyze more complex code. The equivalent flag on the `scanner:run:dfa` command is `--rule-thread-timeout`.

## Examples

These examples follow Graph Engine syntax: The paths for all files in `--projectdir` are specified through `--target`. When using globs, wrap them in quotes.

Good Examples: 
          
    $ sfdx scanner:run:dfa --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"

    $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./"

    $ sfdx scanner:run:dfa --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"
  		
Bad Example:  

    $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./myproject"

These two examples evaluate rules against all `.cls` files below the current directory, except for `IgnoreMe.cls`.

Unix example:    

    $ sfdx scanner:run:dfa --target './**/*.cls,!./**/IgnoreMe.cls' ...


Windows example: 

    > sfdx scanner:run:dfa --target ".\**\*.cls,!.\**\IgnoreMe.cls" ...

This example targets individual methods within a file. It uses a suffix of the file's path plus a hash (#) and a semi-colon-delimited list of method names. This syntax is incompatible with globs and directories. This example also evaluates rules against all methods named `Method1` or `Method2` in `File1.cls`, and all methods named `Method3` in `File2.cls`.
		
	$ sfdx scanner:run:dfa --target "./File1.cls#Method1;Method2,./File2.cls#Method3" ...

This example uses `--normalize-severity` to output a normalized severity across all engines in addition to the engine-specific severity. Values are 1 (high), 2 (moderate), and 3 (low).

  	$ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --format csv --normalize-severity


This example uses `--severity-threshold` to throw a non-zero exit code when rule violations of a specific normalized severity or 
  greater are found. When there are rule violations with moderate (1) or high (2) severity, the exit code equals the severity of the most severe violation.

    $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --severity-threshold 2


This example uses `--rule-thread-count` so more or fewer entry points can be evaluated concurrently.
    
    $ sfdx scanner:run:dfa --rule-thread-count 6


This example uses `--rule-thread-timeout` to increase or decrease the maximum runtime for a single entry point evaluation. You can increase the timeout from 15 minutes (default) up to 150 minutes.

    $ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...
  
This example uses `--sfgejvmargs` to pass JVM args to override system defaults while executing Graph Engine's rules. It overrides the system's default heapspace allocation to 2 GB and decreases the likelihood of encountering an OutOfMemory error.
		
    $ sfdx scanner:run:dfa --sfgejvmargs "-Xmx2g" ...

## Demo
![DFA Example](./assets/images/dfa.gif)
