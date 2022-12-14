module.exports = {
	"flags": {
		"formatDescription": "specify results output format",
		"formatDescriptionLong": "Specifies output format with results written directly to the console.",
		"normalizesevDescription": "return normalized severity 1 (high), 2 (moderate), and 3 (low), and the engine-specific severity",
		"normalizesevDescriptionLong": "Returns normalized severity 1 (high), 2 (moderate), and 3 (low), and the engine-specific severity. For the html option, the normalized severity is displayed instead of the engine severity.",
		"outfileDescription": "write output to a file",
		"outfileDescriptionLong": "Writes output to a file.",
		"sevthresholdDescription": "throw an error when a violation threshold is reached, the --normalize-severity is invoked, and severity levels are reset to the baseline",
		"sevthresholdDescriptionLong": "Throws an error when violations are found with equal or greater severity than the provided value. --normalize-severity is invoked and severity levels are reset to the baseline. Normalized severity values are: 1 (high), 2 (moderate), and 3 (low). Exit code is the most severe violation.",
	},
	"validations": {
		"cannotWriteTableToFile": "Format 'table' can't be written to a file. Specify a different format.",
		"outfileFormatMismatch": "The selected output format doesn't match the output file type. Output format: %s. Output file type: %s.",
		"outfileMustBeValid": "--outfile must be a well-formed filepath.",
		"outfileMustBeSupportedType": "--outfile must be of a supported type: .csv; .xml; .json; .html; .sarif."
	}
}
