# command.summary

Display the current state of configuration for Code Analyzer.

# command.description

Code Analyzer gives you the ability to configure settings that modify Code Analyzer's behavior, to override the tags and severity levels of rules, and to configure the engine specific settings.  Use this command to see the current state of this configuration and to save this state to a YAML-formatted file that you can modify to our needs.

To apply a custom configuration with Code Analyzer, either keep your custom configuration settings in a 'code-analyzer.yml' file located in the current working directory from which you are executing commands, or specify the location of your custom configuration file to each of the Code Analyzer commands via the `--config-file` flag.

# command.examples

- Display the current state configuration for Code Analyzer based on the default recommendations, and a local `code-analyzer.yml` file if such a file exists: 

     <%= config.bin %> <%= command.id %>

- This example is identical to the previous one, if `./code-analyzer.yml` is a real file.

     <%= config.bin %> <%= command.id %> --config-file ./code-analyzer.yml --rule-selector Recommended

- Write the current state of configuration to `code-analyzer.yml`, including any configuration from an existing `code-analyzer.yml` file. This preserves all values from the original config, but will overwrite any comments:

     <%= config.bin %> <%= command.id %> --config-file ./code-analyzer.yml --output-file code-analyzer.yml

- This example is equivalent to the previous one:

     <%= config.bin %> <%= command.id %> --output-file code-analyzer.yml

- Display the configuration state for all rules, instead of just the recommended ones:

     <%= config.bin %> <%= command.id %> --rule-selector all
- Display the configuration state associated with recommended rules that are applicable to your workspace folder, `./src`:

     <%= config.bin %> <%= command.id %> --workspace ./src

# flags.workspace.summary

Set of files you want to include in the code analysis.

# flags.workspace.description

Specify this flag to limit the configuration state to rules applicable to the specified files. Typically, a workspace is a single project folder containing all your files, but it can also be a one or more folders, files, or glob patterns.

# flags.rule-selector.summary

Selection of rules, based on engine name, severity level, rule name, tag, or a combination of criteria separated by colons.

# flags.rule-selector.description

Use the --rule-selector flag to display only the configuration associated with the rules based on specific criteria. You can select by engine, such as the rules associated with the "retire-js" or "eslint" engine. Or select by the severity of the rules, such as high or moderate. You can also select rules using tag values or rule names.

You can combine different criteria using colons to further filter the list; the colon works as an intersection.  For example, "--rule-selector eslint:Security" reduces the output to only contain the configuration state associated with the rules from the "eslint" engine that have the "Security" tag. To add multiple rule selectors together (a union), specify the --rule-selector flag multiple times, such as "--rule-selector eslint:Recommended --rule-selector retire-js:3".

If this flag is not specified, then the 'Recommended' tag rule selector will be used.

Run `<%= config.bin %> <%= command.id %>  --rule-selector all` to display the configuration state associated with all possible rules available, and not just the recommended ones.

# flags.config-file.summary

Path to the existing configuration file used to customize the engines and rules.

# flags.config-file.description

Use this flag to apply the customizations from a custom Code Analyzer configuration file to be displayed alongside the current Code Analyzer configuration state.

If this flag is not specified, then by default Code Analyzer will look for and apply a file named "code-analyzer.yml" or "code-analyzer.yaml" in your current working directory.

# flags.output-file.summary

Output file that contains the final config. Will always be formatted as YAML.

# flags.output-file.description

Use this flag to write the final config to a file, in addition to the terminal.
