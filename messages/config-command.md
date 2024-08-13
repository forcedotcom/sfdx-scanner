# command.summary

See the current state of the config for Code Analyzer.

# command.description

See a YAML-formatted config object describing Code Analyzer's default configuration. Or see a final config created from
the resolution of an existing config, rule selectors, and a workspace. Optionally, write the results to a file.

# command.examples

- Display the default configuration for Code Analyzer:

     <%= config.bin %> <%= command.id %>

- Display the default configuration for Code Analyzer and write it to a file:

     <%= config.bin %> <%= command.id %> --output-file code-analyzer.yml

# flags.workspace.summary

*DRAFT*

# flags.workspace.description

*DRAFT*

# flags.rule-selector.summary

*DRAFT*

# flags.rule-selector.description

*DRAFT*

# flags.config-file.summary

*DRAFT*

# flags.config-file.description

*DRAFT*

# flags.output-file.summary

Output file that contains the final config. Will always be formatted as YAML.

# flags.output-file.description

Use this flag to write the final config to a file, in addition to the terminal.
