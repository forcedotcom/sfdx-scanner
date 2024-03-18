# info.categoryImplicitlyRun

Implicitly including %s rules from category '%s'

# info.jarAndXmlProcessed

Cataloger: XML files collected from JAR [%s]: %s

# info.usingEngineConfigFile

Using engine configuration file at %s

# info.generalInternalLog

Log from Java: %s

# info.customEslintHeadsUp

About to run Eslint with custom config in %s. Please make sure your current directory has all the required NPM dependencies.

# info.customPmdHeadsUp

About to run PMD with custom config in %s. Please make sure that any custom rule references have already been added to the plugin through scanner:rule:add command.

# info.pmdRuleSkipped

Omitting results for PMD rule "%s". Reason: %s.

# info.unmatchedPathExtensionCpd

Path extensions for the following files will not be processed by CPD: %s

# info.sfgeInfoLog

%s

# info.sfgeMetaInfoCollected

Loaded %s: [ %s ]

# info.sfgeFinishedCompilingFiles

Compiled %s files.

# info.sfgeStartedBuildingGraph

Building graph.

# info.sfgeFinishedBuildingGraph

Added all compilation units to graph.

# info.sfgePathEntryPointsIdentified

Identified %s path entry point(s).

# info.sfgeViolationsInPathProgress

Detected %s violation(s) from %s path(s) on %s/%s entry point(s).

# info.sfgeCompletedPathAnalysis

Overall, analyzed %s path(s) from %s entry point(s). Detected %s violation(s).

# info.telemetry

This message is unused.

# warning.invalidCategorySkipped

Cataloger: Skipping invalid PMD Category file '%s'.

# warning.invalidRulesetSkipped

Cataloger: Skipping invalid PMD Ruleset file '%s'.

# warning.xmlDropped

Cataloger: Dropping XML file [%s] since its path does not conform to Rulesets or Category.

# warning.langMarkedForDeprecation

Future releases will not include PMD support for %s. If this would cause you hardship, please log an issue on github.com/forcedotcom/sfdx-scanner

# warning.customRuleFileNotFound

Custom rule file path [%s] for language [%s] was not found.

# warning.pmdSkippedFile

PMD failed to evaluate against file '%s'. Message: %s

# warning.pmdSuppressedViolation

PMD suppressed violation against file '%s'. Message: %s. Suppression Type: %s. User Message: %s

# warning.unexpectedPmdNodeType

Encountered unexpected PMD node of type '%s'

# warning.multipleMethodTargetMatches

Total of %s methods in file %s matched name #%s

# warning.noMethodTargetMatches

No methods in file %s matched name #%s()

# warning.pmdConfigError

PMD failed to evaluate rule '%s'. Message: %s

# warning.sfgeWarnLog

%s

# error.internal.unexpectedError

INTERNAL ERROR: Unexpected error occurred while cataloging rules: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.

# error.internal.mainInvalidArgument

INTERNAL ERROR: Invalid arguments passed to Main. Details: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.

# error.internal.jsonWriteFailed

INTERNAL ERROR: Failed to write JSON to file: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.

# error.internal.classpathDoesNotExist

INTERNAL ERROR: Path does not exist: %s. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.

# error.internal.xmlMissingInClasspath

INTERNAL ERROR: XML resource [%s] found in jar, but not in Classpath. Please log an issue with us at github.com/forcedotcom/sfdx-scanner.

# error.external.errorMessageAbove

Please see error details displayed above.

# error.external.genericErrorMessage

ERROR: An unexpected error occurred. Please log an issue on github.com/forcedotcom/sfdx-scanner.

# error.external.jarNotReadable

ERROR: Unable to read resource JAR: %s

# error.external.dirNotReadable

ERROR: Unable to walk directory: %s

# error.external.multipleRuleDesc

ERROR: PMD Rule [%s] has %s 'description' elements. Please reduce this number to 1.

# error.external.recursionLimitReached

ERROR: PMD Ruleset [%s] references rule [%s] through 10 or more layers of indirection. Please reduce this number.

# error.external.xmlNotReadable

ERROR: Error occurred while reading file [%s]: %s

# error.external.xmlNotParsable

ERROR: Could not parse XML file [%s]: %s

# error.external.duplicateXmlPath

ERROR: XML path [%s] defined in jar [%s] collides with previously defined path in jar [%s]. You will need to remove one of the jars by executing the following command '<%= config.bin %> scanner rule remove --force --path <jar-to-remove>'

# error.external.sfgeIncompleteAnalysis

ERROR: Salesforce Graph Engine encountered an error and couldn't complete analysis: %s

# error.external.sfgeErrorLog

%s
