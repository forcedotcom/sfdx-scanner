# warning.targetSkipped

The specified target wasn't processed by any engines. Use the --engine parameter to select a different engine or specify a different target. Specified target: %s.

# warning.targetsSkipped

The specified targets weren't processed by any engines: %s. Review your target and engine combinations and try again.

# warning.pathsDoubleProcessed

One or more files were processed by eslint and eslint-lwc simultaneously. To remove possible duplicate violations, customize the targetPatterns property for eslint and eslint-lwc engines in %s on these files: %s.

# error.cannotRunDfaAndNonDfaConcurrently

DFA engines %s cannot be run concurrently with non-DFA engines %s