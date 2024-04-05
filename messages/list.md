# commandSummary

List basic information about all rules matching provided criteria.

# commandDescription

Filter the output to view a smaller set of rules. Use the `scanner rule describe` command to get information about a specific rule.

# flags.languageSummary

Select rules by language.

# flags.languageDescription

Specify multiple values as a comma-separated list.

# flags.categorySummary

Select rules by category.

# flags.categoryDescription

Specify multiple values as a comma-separated list.

# flags.rulesetSummary

Deprecated. Use category instead. Select rules by ruleset.

# flags.engineSummary

Select rules by engine.

# flags.engineDescription

Specify multiple values as a comma-separated list.

# rulesetDeprecation

The 'ruleset' command parameter is deprecated. Use 'category' instead

# columnNames.name

name

# columnNames.languages

languages

# columnNames.categories

categories

# columnNames.rulesets

rulesets [dep]

# columnNames.is-dfa

is dfa

# columnNames.is-pilot

is pilot

# columnNames.engine

engine

# yes

Y

# no

N

# examples

- This example invokes the command without filter criteria, which returns all rules.

  <%= config.bin %> <%= command.id %>

- This example returns all rules for Apex OR Javascript. Values supplied to a single filter are handled with a logical OR.

  <%= config.bin %> <%= command.id %> --language apex,javascript

- This example returns all rules that target Apex OR Javascript, AND are members of the Braces OR Security rulesets. The different filters are combined with a logical AND.

  <%= config.bin %> <%= command.id %> --language apex,javascript --ruleset Braces,Security
