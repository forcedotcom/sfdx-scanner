module.exports = {
	"info": {
		"categoryImplicitlyRun": "Implicitly including %s rules from category '%s'",
		"jarAndXmlProcessed": "Cataloger: XML files collected from JAR [%s]: %s",
		"usingEngineConfigFile": "Using engine configuration file at %s",
		"generalInternalLog": "Log from Java: %s",
		"customEslintHeadsUp": "About to run Eslint with custom config in %s. Please make sure your current directory has all the required NPM dependencies.",
		"customPmdHeadsUp": "About to run PMD with custom config in %s. Please make sure that any custom rule references have already been added to the plugin through scanner:rule:add command.",
		"pmdRuleSkipped": "Omitting results for PMD rule \"%s\". Reason: %s.",
		"unmatchedPathExtensionCpd": "Path extensions for the following files will not be processed by CPD: %s",
		"sfgeInfoLog": "%s",
		"sfgeMetaInfoCollected": "Loaded %s: [ %s ]",
		"sfgeFinishedCompilingFiles": "Compiled %s files.",
		"sfgeStartedBuildingGraph": "Building graph.",
		"sfgeFinishedBuildingGraph": "Added all compilation units to graph.",
		"sfgePathEntryPointsIdentified": "Identified %s path entry point(s).",
		"sfgeViolationsInPathProgress": "Detected %s violation(s) from %s path(s) on %s/%s entry point(s).",
		"sfgeCompletedPathAnalysis": "Overall, analyzed %s path(s) from %s entry point(s). Detected %s violation(s).",
		"telemetry": "This message is unused."
	},
	"warning": {
		"invalidCategorySkipped": "Cataloger: Skipping invalid PMD Category file '%s'.",
		"invalidRulesetSkipped": "Cataloger: Skipping invalid PMD Ruleset file '%s'.",
		"xmlDropped": "Cataloger: Dropping XML file [%s] since its path does not conform to Rulesets or Category.",
		"langMarkedForDeprecation": "Future releases will not include PMD support for %s. If this would cause you hardship, please log an issue on github.com/forcedotcom/sfdx-scanner",
		"customRuleFileNotFound": "Custom rule file path [%s] for language [%s] was not found.",
		"pmdSkippedFile": "PMD failed to evaluate against file '%s'. Message: %s",
		"pmdSuppressedViolation": "PMD suppressed violation against file '%s'. Message: %s. Suppression Type: %s. User Message: %s",
		"unexpectedPmdNodeType": "Encountered unexpected PMD node of type '%s'",
		"multipleMethodTargetMatches": "Total of %s methods in file %s matched name #%s",
		"noMethodTargetMatches": "No methods in file %s matched name #%s()",
		"pmdConfigError": "PMD failed to evaluate rule '%s'. Message: %s",
		"sfgeWarnLog": "%s"
	},
	"error": {
		"internal": {
			"unexpectedError": "INTERNAL ERROR: Unexpected error occurred while cataloging rules: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.",
			"mainInvalidArgument": "INTERNAL ERROR: Invalid arguments passed to Main. Details: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.",
			"jsonWriteFailed": "INTERNAL ERROR: Failed to write JSON to file: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.",
			"classpathDoesNotExist": "INTERNAL ERROR: Path does not exist: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.",
			"xmlMissingInClasspath": "INTERNAL ERROR: XML resource [%s] found in jar, but not in Classpath. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.",
			"sfgeErrorLog": "%s"
		},
		"external": {
			"errorMessageAbove": "Please see error details displayed above.",
			"genericErrorMessage": "ERROR: An unexpected error occurred. Please log an issue on github.com/forcedotcom/sfdx-scanner.",
			"jarNotReadable": "ERROR: Unable to read resource JAR: %s",
			"dirNotReadable": "ERROR: Unable to walk directory: %s",
			"multipleRuleDesc": "ERROR: PMD Rule [%s] has %s 'description' elements. Please reduce this number to 1.",
			"recursionLimitReached": "ERROR: PMD Ruleset [%s] references rule [%s] through 10 or more layers of indirection. Please reduce this number.",
			"xmlNotReadable": "ERROR: Error occurred while reading file [%s]: %s",
			"xmlNotParsable": "ERROR: Could not parse XML file [%s]: %s",
			"duplicateXmlPath": "ERROR: XML path [%s] defined in jar [%s] collides with previously defined path in jar [%s]. You will need to remove one of the jars by executing the following command 'sfdx scanner:rule:remove --force --path <jar-to-remove>'",
			"sfgeIncompleteAnalysis": "ERROR: SFGE encountered an error and couldn't complete analysis: %s"
		}
	}

}
