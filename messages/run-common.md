# flags.categorySummary

One or more categories of rules to run. Specify multiple values as a comma-separated list.

# flags.formatSummary

Specify results the output format for results written directly to the console.

# flags.normalizesevSummary

Normalized severity 1 (high), 2 (moderate), and 3 (low), and the engine-specific severity. For the html option, the normalized severity is displayed instead of the engine severity.

# flags.outfileSummary

Output written to a file

# flags.projectdirSummary

The relative or absolute root project directories used to set the context for Graph Engine's analysis.

# flags.projectdirDescription

Specify multiple values as a comma-separated list. Each project directory must be a path, not a glob. If --projectdir isn’t specified, a default value is calculated. The default value is a directory that contains all the target files.

# flags.sevthresholdSummary

An error when violations are found with equal or greater severity than the provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation. Using this flag also invokes the --normalize-severity flag.

# internal.outfileMustBeValid

The %s environment variable must be a well-formed filepath.

# internal.outfileMustBeSupportedType

The %s environment variable must be of a supported type: .csv; .xml; .json; .html; .sarif.

# validations.cannotWriteTableToFile

Format 'table' can't be written to a file. Specify a different format.

# validations.outfileFormatMismatch

The selected output format doesn't match the output file type. Output format: %s. Output file type: %s.

# validations.outfileMustBeValid

--outfile must be a well-formed filepath.

# validations.outfileMustBeSupportedType

--outfile must be of a supported type: .csv; .xml; .json; .html; .sarif.

# validations.projectdirCannotBeGlob

--projectdir cannot specify globs

# validations.projectdirMustBeDir

--projectdir must specify directories

# validations.projectdirMustExist

--projectdir must specify existing paths

# validations.noFilesFoundInTarget

No files were found in the target. --target must contain at least one file.

# info.resolvedTarget

The --target flag wasn't specified so the default target '.' will be used.

# info.resolvedProjectDir

The --projectdir flag wasn’t specified so the calculated project directory '%s' will be used.
