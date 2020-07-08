module.exports = {
	 "info": {
		"categoryImplicitlyRun": "Implicitly including %s rules from category '%s'",
		"jarAndXmlProcessed": "XML files collected from JAR [%s]: %s",
		"usingEngineConfigFile": "Using engine configuration file at %s"
	},
	"warning": {
		"invalidCategorySkipped": "Cataloger skipped invalid PMD Category file '%s'.",
		"invalidRulesetSkipped": "Cataloger skipped invalid PMD Ruleset file '%s'.",
		"xmlDropped": "Dropping XML file [%s] since its path does not conform to Rulesets or Category.",
		"pmdSkippedFile": "PMD failed to evaluate against file '%s'. Message: %s",
		"customRuleFileNotFound": "Custom rule file path [%s] for language [%s] was not found.",
},
	"error": {
		"internal": {
			"unexpectedError": "Unexpected error occurred while cataloging rules: %s",
			"mainInvalidArgument": "Invalid arguments passed to Main. Details: %s",
			"jsonWriteFailed": "Failed to write JSON to file: %s",
			"classpathDoesNotExist": "Path does not exist: %s",
			"xmlMissingInClasspath": "XML resource [%s] found in jar, but not in Classpath"
		},
		"external": {
			"errorMessageAbove": "Please see error details displayed above.",
			"genericErrorMessage": "ERROR: An internal error occurred. [TODO: Information on how to contact us and what details to provide]",
			"jarNotReadable": "ERROR: Unable to read resource JAR: %s",
			"dirNotReadable": "ERROR: Unable to walk directory: %s",
			"multipleRuleDesc": "ERROR: PMD Rule [%s] has %s 'description' elements. Please reduce this number to 1.",
			"recursionLimitReached": "ERROR: PMD Ruleset [%s] references rule [%s] through 10 or more layers of indirection. Please reduce this number.",
			"xmlNotReadable": "ERROR: Error occurred while reading file [%s]: %s",
			"xmlNotParsable": "ERROR: Could not parse XML file [%s]: %s",
			"duplicateXmlPath": "ERROR: XML path [%s] defined in jar [%s] collides with previously defined path in jar [%s]. You will need to remove one of the jars by executing the following command 'sfdx scanner:rule:remove --force --path <jar-to-remove>'"
		}
	}

}
