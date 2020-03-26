module.exports = {
  "commandDescription": "Provide detailed information about a specified rule.",
  "flags": {
    "rulenameDescription": "The name of a rule."
  },
  "output": {
    "noMatchingRules": "No rules exist with the name '{0}'",
    "multipleMatchingRules": "Found {0} rules with the name '{1}'"
  },
  "examples": {
    // The example for when only one rule matches the provided name.
    "normalExample": `$ sfdx scanner:rule:describe --rulename ExampleRule
  name:        ExampleRule
  categories:  ExampleCategory
  rulesets:    Ruleset1
               Ruleset2
               Ruleset3
  languages:   apex
  description: Short description of rule
  message:     ExampleRule Violated.
  `,
    // The example for when no rules match the provided name.
    "noRulesExample": `$ sfdx scanner:rule:describe --rulename FakeRule
  WARNING: No rules exist with name 'FakeRule'
  `,
    // The example for when multiple rules match the provided name.
    "multipleRulesExample": `$ sfdx scanner:rule:describe --rulename DuplicateRule
  WARNING: Found X rules with the name 'Duplicate Rule'
  === Rule #1
  name:        DuplicateRule
  ...
  === Rule #2
  name:        DuplicateRule
  ...
  === Rule #X
  name:        DuplicateRule
  `
  }
};
