# command.summary

Display the current state of configuration for Code Analyzer.

# command.description

Code Analyzer gives you the ability to configure settings that modify Code Analyzer's behavior, to override the tags and severity levels of rules, and to configure the engine specific settings.  Use this command to see the current state of this configuration. You can also save this state to a YAML-formatted file that you can modify for your needs.

To apply a custom configuration with Code Analyzer, either keep your custom configuration settings in a `code-analyzer.yml` file located in the current folder from which you are executing commands, or specify the location of your custom configuration file to the Code Analyzer commands with the --config-file flag.

We're continually improving Salesforce Code Analyzer. Tell us what you think! Give feedback at http://sfdc.co/CodeAnalyzerFeedback.

# command.examples

- Display the current state of the Code Analyzer configuration using the default behavior: display top level configuration, display the engine and rule override settings associated with all the rules that have a "Recommended" tag; and automatically apply any existing custom configuration settings found in a `code-analyzer.yml` or `code-analyzer.yaml` file in the current folder: 

  <%= config.bin %> <%= command.id %>

- This example is identical to the previous one, assuming that `./code-analyzer.yml` exists in your current folder.

  <%= config.bin %> <%= command.id %> --config-file ./code-analyzer.yml --rule-selector Recommended

- Write the current state of configuration to the file `code-analyzer.yml`, including any configuration from an existing `code-analyzer.yml` file. The command preserves all values from the original config, but overwrites any comments:

  <%= config.bin %> <%= command.id %> --config-file ./code-analyzer.yml --output-file code-analyzer.yml

- Display the configuration state for all rules, instead of just the recommended ones:

  <%= config.bin %> <%= command.id %> --rule-selector all

- Display the configuration state associated with recommended rules that are applicable to your workspace folder, `./src`:

  <%= config.bin %> <%= command.id %> --workspace ./src

- Display any relevant configuration settings associated with the rule name 'no-undef' from the 'eslint' engine:

  <%= config.bin %> <%= command.id %> --rule-selection eslint:no-undef

- Load an existing configuration file called `existing-config.yml`, and then write the configuration to a new file called `new-config.yml`, the configuration state that is applicable to all rules that are relevant to the workspace located in the current folder:

  <%= config.bin %> <%= command.id %> --config-file ./existing-config.yml --rule-selection all --workspace . --output-file ./subfolder-config.yml

# flags.workspace.summary

Set of files you want to include in the code analysis.

# flags.workspace.description

Use the --workspace flag to display only the configuration associated with the rules that apply to the files that make up your workspace. Typically, a workspace is a single project folder that contains all your files. But it can also consist of one or more folders, one or more files, and use glob patterns (wildcards). If you specify this flag multiple times, then your workspace is the sum of the files and folders.

This command uses the type of file in the workspace, such as JavaScript or Typescript, to determine the rules to include in the configuration state. For example, if your workspace contains only JavaScript files, the command doesn't include TypeScript rules. The command uses a file's extension to determine what kind of file it is, such as ".ts" for TypeScript.

Some engines may be configured to add additional rules based on what it finds in your workspace.  For example, if you set the "engines.eslint.auto_discover_eslint_config" value of your `code-analyzer.yml` file to true, then supplying your workspace allows the "eslint" engine to examine your files in order to find ESLint configuration files that could potentially add in additional rules.

# flags.rule-selector.summary

Selection of rules, based on engine name, severity level, rule name, tag, or a combination of criteria separated by colons.

# flags.rule-selector.description

Use the --rule-selector flag to display only the configuration associated with the rules based on specific criteria. You can select by engine, such as the rules associated with the "retire-js" or "eslint" engine. Or select by the severity of the rules, such as high or moderate. You can also select rules using tag values or rule names.

You can combine different criteria using colons to further filter the list; the colon works as an intersection.  For example, "--rule-selector eslint:Security" reduces the output to only contain the configuration state associated with the rules from the "eslint" engine that have the "Security" tag. To add multiple rule selectors together (a union), specify the --rule-selector flag multiple times, such as "--rule-selector eslint:Recommended --rule-selector retire-js:3".

If you don't specify this flag, then the command uses the "Recommended" tag rule selector.

Run `<%= config.bin %> <%= command.id %>  --rule-selector all` to display the configuration state associated with all possible rules available, and not just the recommended ones.

# flags.config-file.summary

Path to the existing configuration file used to customize the engines and rules.

# flags.config-file.description

Use this flag to apply the customizations from a custom Code Analyzer configuration file to be displayed alongside the current Code Analyzer configuration state.

If you don't specify this flag, then the command looks for and applies a file named `code-analyzer.yml` or `code-analyzer.yaml` in your current folder.

# flags.output-file.summary

Output file to write the configuration state to. The file is written in YAML format.

# flags.output-file.description

Use this flag to write the final config to a file, in addition to the terminal.
