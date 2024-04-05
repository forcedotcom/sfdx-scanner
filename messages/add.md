# commandSummary

Add custom rules to Salesforce Code Analyzer's registry to run them along with the built-in rules.

# commandDescription

Bundle custom PMD rules in JAR files. Follow PMD conventions, such as defining the custom rules in XML files under a `/category/` directory. Compile and test custom rules separately before adding them. See PMD's documentation for more information on writing rules.

# flags.languageSummary

Language that the custom rules are evaluated against.

# flags.pathSummary

One or more paths (such as a directory or JAR file) to custom rule definitions.

# flags.pathDescription

Specify multiple values as a comma-separated list.

# validations.languageCannotBeEmpty

Specify a language.

# validations.pathCannotBeEmpty

Specify a path.

# output.successfullyAddedRules

Successfully added rules for %s.

# output.resultSummary

%s Path(s) added: %s

# examples

- This example shows how to specify two JAR files directly.

  <%= config.bin %> <%= command.id %> --language apex --path "/Users/me/rules/Jar1.jar,/Users/me/rules/Jar2.jar"

- This example shows how to specify a directory containing one or more JARs, all of which are added to the registry.

  <%= config.bin %> <%= command.id %> --language apex --path "/Users/me/rules"
