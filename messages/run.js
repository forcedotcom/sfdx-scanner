module.exports = {
  "commandDescription": "Evaluate a selection of rules against a codebase.",
  "flags": {
    "rulenameDescription": "[Description of 'rulename' parameter]",                   // TODO: Change this once the flag is implemented.
    "categoryDescription": "One or more categories of rules to run. Multiple values can be specified as a comma-separated list.",
    "rulesetDescription": "One or more rulesets to run. Multiple values can be specified as a comma-separated list.",
    "severityDescription": "[Description of 'severity' parameter]",                   // TODO: Change this once the flag is implemented.
    "excluderuleDescription": "[Description of 'exclude-rule' parameter]",            // TODO: Change this once the flag is implemented.
    "orgDescription": "[Description of 'org' parameter]",                             // TODO: Change this once the flag is implemented.
    "suppresswarningsDescription": "[Description of 'suppress-warnings' parameter]",  // TODO: Change this once the flag is implemented.
    "targetDescription": "Source code location. May use glob patterns. Multiple values can be specified as a comma-separated list",
    "formatDescription": "Specifies output format with results written directly to the console.",
    "outfileDescription": "Write output to a file."
  },
  "validations": {
    "mustTargetSomething": "Please specify a codebase using --target.", // TODO: Once --org is implemented, rewrite this message.
    "outfileMustBeValid": "--outfile must be a well-formed filepath.",
    "outfileMustBeSupportedType": "--outfile must be of a supported type. Current options are .xml and .csv."
  },
  "output": {
    "noViolationsDetected": "No rule violations found.",
    "writtenToOutFile": "Rule violations have been added to %s."
  },
  "examples": `Invoking without specifying any rules causes all rules to be run.
  E.g., $ sfdx scanner:run --format xml --target "somefile.js"
    Evaluates all rules against somefile.js.
    
Specifying multiple categories or rulesets is treated as a logical OR.
  E.g., $ sfdx scanner:run --format xml --target "somefile.js" --category "Design,Best Practices" --ruleset "Braces"
    Evaluates all rules in the Design and Best Practices categories, and all rules in the Braces ruleset.
    
Wrap globs in quotes.
  Unix example:    $ sfdx scanner:run --target './**/*.js,!./**/IgnoreMe.js' ...
  Windows example: > sfdx scanner:run --target ".\\**\\*.js,!.\\**\\IgnoreMe.js" ...
    Evaluate rules against all .js files below the current directory, except for IgnoreMe.js.
`
};
