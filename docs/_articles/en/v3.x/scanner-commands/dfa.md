---
title: Salesforce Code Analyzer Plug-In Command Reference
lang: en
---

## sfdx scanner:run:dfa
evaluate a selection of rules against a codebase

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

  -o, --outfile=outfile
      location of output file

  -p, --projectdir=projectdir
      (required) Root project directory

  -s, --severity-threshold=severity-threshold
      throws an error when violations of specific severity (or
      more severe) are detected, invokes --normalize-severity

  -t, --target=target
      (required) [REVISIT THESE] location of source code

  --ignore-parse-errors
      ignore compilation failures in scanned files

  --json
      format output as json

  --loglevel=(trace|debug|info|warn|error|fatal|TRACE|DEBUG|INFO|WARN|ERROR|FATA
  L)
      [default: warn] logging level for this command invocation

  --normalize-severity
       A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity

  --rule-thread-count=rule-thread-count
      Number of threads evaluating rules (default is 4)

  --rule-thread-timeout=rule-thread-timeout
      Timeout for individual rule threads, in milliseconds (default is 900,000 ms)

  --verbose
      emit additional command output to stdout
```

## Example
  The paths specified for --projectdir must cumulatively contain all files specified through --target.
  		Good: `$ sfdx scanner:run:dfa --target "./myproject/main/default/classes/*.cls" --projectdir "./myproject/"`
  		Good: `$ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./"`
  		Good: `$ sfdx scanner:run:dfa --target "./dir1/file1.cls,./dir2/file2.cls" --projectdir "./dir1/,./dir2/"`
  		Bad:  `$ sfdx scanner:run:dfa --target "./**/*.cls" --projectdir "./myproject"`
  	Wrap globs in quotes.
  		Unix example:    `$ sfdx scanner:run:dfa --target './**/*.cls,!./**/IgnoreMe.cls' ...`
  		Windows example: `> sfdx scanner:run:dfa --target ".\**\*.cls,!.\**\IgnoreMe.cls" ...`
  			Evaluate rules against all .cls files below the current directory, except for IgnoreMe.cls.
  	Use --normalize-severity to output a normalized (across all engines) severity (1 [high], 2 [moderate], and 3 [low]) in 
  addition to the engine specific severity (when shown).
  		E.g., `$ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --format csv --normalize-severity`
  	Use --severity-threshold to throw a non-zero exit code when rule violations of a specific normalized severity (or 
  greater) are found. For this example, if there are any rule violations with a severity of 2 or more (which includes 
  1-high and 2-moderate), the exit code will be equal to the severity of the most severe violation.
  		E.g., `$ sfdx scanner:run:dfa --target "/some-project/" --projectdir "/some-project/" --severity-threshold 2`
  	Use --rule-thread-count to allow more (or fewer) entrypoints to be evaluated concurrently.
  		E.g., `$ sfdx scanner:run:dfa --rule-thread-count 6`
  	Use --rule-thread-timeout to increase (or decrease) the maximum runtime for a single entrypoint evaluation.
  		E.g., `$ sfdx scanner:run:dfa --rule-thread-timeout 9000000 ...`
  			Increases timeout from 15 minutes (default) to 150 minutes.