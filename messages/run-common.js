module.exports = {
	"flags": {
		"formatDescription": "format of results",
		"formatDescriptionLong": "Specifies output format with results written directly to the console.",
		"ignoreparseerrorsDescription": "ignore compilation failures in scanned files (default: false). Alternatively, set value using environment variable `SFGE_IGNORE_PARSE_ERRORS`",
		"ignoreparseerrorsDescriptionLong": "ignore compilation failures in scanned files. Inherits value from SFGE_IGNORE_PARSE_ERRORS env-var if set.",
		"normalizesevDescription": "A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity",
		"normalizesevDescriptionLong": "A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity. For the html option, the normalized severity is displayed instead of the engine severity",
		"outfileDescription": "location of output file",
		"outfileDescriptionLong": "Write output to a file.",
		"projectdirDescription": "root directory of project",
		"projectdirDescriptionLong": "Root project directory. Must be paths, not globs. Multiple values can be specified as a comma-separated list",
		"targetDescription": "location of source code",
		"sevthresholdDescription": "throws an error when violations of specific severity (or more severe) are detected, invokes --normalize-severity",
		"sevthresholdDescriptionLong": "Throws an error if violations are found with equal or greater severity than provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation. Using this flag also invokes the --normalize-severity flag"
	},
	"validations": {
		"cannotWriteTableToFile": "Format 'table' cannot be written to a file. Please specify a different format.",
		"outfileFormatMismatch": "Your chosen format %s does not appear to match your output file type of %s.",
		"outfileMustBeSupportedType": "--outfile must be of a supported type. Current options are: .csv; .xml; .json; .html; .sarif.",
		"outfileMustBeValid": "--outfile must be a well-formed filepath.",
		"projectdirCannotBeGlob": "--projectdir cannot specify globs",
		"projectdirMustBeDir": "--projectdir must specify directories",
		"projectdirMustExist": "--projectdir must specify existing paths",
	}
}
