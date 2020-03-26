module.exports = {
  "commandDescription": "Lists basic information about all rules matching provided criteria",
  "flags": {
    "languageDescription": "Language(s) to filter by. Enter multiple values as a comma-separated list.",
    "categoryDescription": "One or more categories to filter by. Enter multiple values as a comma-separated list.",
    "rulesetDescription": "Ruleset() to filter by. Enter multiple values as a comma-separated list.",
    "severityDescription": "[Description of 'severity' parameter]", // Change this when we implement the flag.
    "standardDescription": "[Description of 'standard' parameter]", // Change this when we implement the flag.
    "customDescription": "[Description of 'custom' parameter]"      // Change this when we implement the flag.
  },
  "examples": `Invoking with no filter criteria returns all rules.
  E.g., $ sfdx scanner:rule:list
    Returns a table containing all rules.
  
When applying multiple filters to a single column, the result is a logical OR.
  E.g., $ sfdx scanner:rule:list --language apex,javascript
    Returns all rules for Apex OR Javascript

When applying filters to multiple columns, the result is a logical AND.
  E.g., $ sfdx scanner:rule:list --language apex,javascript --ruleset Braces,Security
    Returns all rules that:
    1) Target Apex OR Javascript
    AND...
    2) Are members of the Braces OR Security rulesets.
  `
};
