module.exports = {
  "commandDescription": "Provide detailed information about a rule.",
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
  `
  }
};
