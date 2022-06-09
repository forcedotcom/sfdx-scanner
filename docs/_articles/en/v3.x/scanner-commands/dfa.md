---
title: Salesforce Code Analyzer Plug-In Command Reference
lang: en
---

## sfdx scanner:run:dfa
Execute dataflow-analysis-based rules on a target codebase. This command runs for a longer time than `scanner:run`. Also, this execution requires a path to context of where the target code resides.

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

  --verbose
      emit additional command output to stdout
```

## Environment-variable-based Controls

### *SFGE-RULE-THREAD-COUNT*
Default value is 4. Modify this variable to adjust the number of threads that will each execute DFA-based rules. Equivalent flag on `scanner:run:dfa` command is `--rule-thread-count`.

### *SFGE-RULE-THREAD-TIMEOUT*
Default value is 900,000ms (15 minutes). Modify this variable to adjust how long DFA-based rules can execute before timing out. You can use this to allow SFGE to run for longer to analyze more complex code. Equivalent flag on `scanner:run:dfa` command is `--rule-thread-timeout`.

### *SFGE-IGNORE-PARSE-ERRORS*
By default, this value is true. Set this variable to false to force SFGE to ignore parse errors. This is not recommended since the analysis results will be incorrect. Equivalent flag on `scanner:run:dfa` command is `--ignore-parse-errors`.

## Example
  The paths specified for `--projectdir` must cumulatively contain all files specified through `--target`.

Good: 
          
    $ sfdx scanner:run:dfa --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"


Good: 

    $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./"
  		
Good: 

    $ sfdx scanner:run:dfa --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"
  		
Bad:  

    $ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./myproject"


Wrap globs in quotes.

Unix example:    

    $ sfdx scanner:run:dfa --target './**/*.cls,!./**/IgnoreMe.cls' ...


Windows example: 

    > sfdx scanner:run:dfa --target ".\**\*.cls,!.\**\IgnoreMe.cls" ...


Evaluate rules against all .cls files below the current directory, except for IgnoreMe.cls.

Individual methods within a file may be targeted by suffixing the file's path with a hash (#), and a semi-colon-delimited
	list of method names. This syntax is incompatible with globs and directories. E.g.,
		
	$ sfdx scanner:run:dfa --target "./File1.cls#Method1;Method2,./File2.cls#Method3" ...

Evaluates rules against ALL methods named `Method1` or `Method2` in `File1.cls`, and ALL methods named `Method3` in `File2.cls`.


Use `--normalize-severity` to output a normalized (across all engines) severity (1 [high], 2 [moderate], and 3 [low]) in 
  addition to the engine specific severity (when shown).
  Example:

  	$ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --format csv --normalize-severity


Use `--severity-threshold` to throw a non-zero exit code when rule violations of a specific normalized severity (or 
  greater) are found. For this example, if there are any rule violations with a severity of 2 or more (which includes 
  1-high and 2-moderate), the exit code will be equal to the severity of the most severe violation.
Example:

    $ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --severity-threshold 2


Use `--rule-thread-count` to allow more (or fewer) entrypoints to be evaluated concurrently.
Example:
    
    $ sfdx scanner:run:dfa --rule-thread-count 6


Use `--rule-thread-timeout` to increase (or decrease) the maximum runtime for a single entrypoint evaluation.
Example:

    $ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...
  			
Increases timeout from 15 minutes (default) to 150 minutes.


## Demo
![DFA Example](./assets/images/dfa.gif)
