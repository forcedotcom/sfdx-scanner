# commandSummary

list basic information about all rules matching provided criteria

# commandDescription

Lists all the rules available in the catalog. You can filter the output to view a smaller set of rules. To get more information about a specific rule, use the `scanner rule describe` command.

# flags.languageSummary

select rules by language

# flags.languageDescription

Selects rules by language. Enter multiple values as a comma-separated list.

# flags.categorySummary

select rules by category

# flags.categoryDescription

Selects rules by category. Enter multiple values as a comma-separated list.

# flags.rulesetSummary

[deprecated] select rules by ruleset

# flags.rulesetDescription

[deprecated] Selects rules by ruleset. Enter multiple values as a comma-separated list.

# flags.engineSummary

select rules by engine

# flags.engineDescription

Selects rules by engine. Enter multiple engines as a comma-separated list.

# flags.previewPmd7Summary

use PMD version %s to list PMD and CPD rules

# flags.previewPmd7Description

Uses PMD version %s instead of %s to list PMD and CPD rules.

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

This example invokes the command without filter criteria, which returns all rules.
	$ <%= config.bin %> <%= command.id %>

This example returns all rules for Apex OR Javascript. Values supplied to a single filter are handled with a logical OR.
	$ <%= config.bin %> <%= command.id %> --language apex,javascript

This example returns all rules except those in the Design or Best Practices categories. Exclude categories by specifying the negation operator and enclosing the values in single quotes.
	$ <%= config.bin %> <%= command.id %> --category '!Design,!Best Practices'

This example returns all rules that target Apex OR Javascript, AND are members of the Braces OR Security rulesets.
The different filters are combined with a logical AND.
	$ <%= config.bin %> <%= command.id %> --language apex,javascript --ruleset Braces,Security
