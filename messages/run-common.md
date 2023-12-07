# flags.categorySummary

one or more categories of rules to run

# flags.categoryDescription

One or more categories of rules to run. Specify multiple values as a comma-separated list.

# flags.formatSummary

specify results output format

# flags.formatDescription

Specifies results output format written directly to the console.

# flags.normalizesevSummary

return normalized severity 1 (high), 2 (moderate), and 3 (low), and the engine-specific severity

# flags.normalizesevDescription

Returns normalized severity 1 (high), 2 (moderate), and 3 (low), and the engine-specific severity. For the html option, the normalized severity is displayed instead of the engine severity.

# flags.outfileSummary

write output to a file

# flags.outfileDescription

Writes output to a file.

# flags.projectdirSummary

provide root directory of project

# flags.projectdirDescription

Provides the relative or absolute root project directory used to set the context for Graph Engine's analysis. Project directory must be a path, not a glob. Specify multiple values as a comma-separated list.

# flags.sevthresholdSummary

throw an error when a violation threshold is reached, the --normalize-severity is invoked, and severity levels are reset to the baseline

# flags.sevthresholdDescription

Throws an error when violations are found with equal or greater severity than the provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation. Using this flag also invokes the --normalize-severity flag.

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
