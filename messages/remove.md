# commandSummary

Remove custom rules from the registry of available rules.

# commandDescription

Use the `-p|--path` parameter to specify one or more paths to remove. If you don't specify any parameters, the command lists all valid custom paths but doesn't remove any.

# flags.forceSummary

Bypass the confirmation prompt and immediately remove the rules.

# flags.pathSummary

One or more paths to remove

# flags.pathDescription

Specify multiple values with a comma-separated list.

# validations.pathCannotBeEmpty

Specify at least one path.

# errors.noMatchingPaths

No registered custom rules match the provided paths.

# output.aborted

The operation stopped.

# output.dryRunReturnedNoRules

No custom rules are registered.

# output.dryRunOutput

%i custom path(s) available for removal:
%s

# output.dryRunRuleTemplate

%s

# output.ruleTemplate

'%s', defined in %s

# output.deletionPrompt

These rules will be unregistered:
%s
Do you wish to proceed? (y/n)

# output.resultSummary

Success. These rules were unregistered: %s.

# examples

- This example runs the command without arguments to see a list of registered custom paths:

	<%= config.bin %> <%= command.id %>

- This example uses the --path parameter to deregister the rules defined in somerules.jar and any JARs/XMLs contained in the rules folder:

	<%= config.bin %> <%= command.id %> --path "~/path/to/somerules.jar,~/path/to/folder/containing/rules"

- This example uses the --force flag to bypass the confirmation prompt, removing all rules defined in somerules.jar.
By default, a list of all rules that will be unregistered is displayed, and the action must be confirmed. To bypass that confirmation, use the --force flag:

	<%= config.bin %> <%= command.id %> --force --path "~/path/to/somerules.jar"
