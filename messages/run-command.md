# command.summary

Analyze your code with a selection of rules to ensure good coding practices.

# command.description

You can scan your codebase with the recommended rules. Or use flags to filter the rules based on engines (such as "retire-js" or "eslint"), rule names, tags, and more. 

If you want to preview the list of rules before you actually run them, use the `code-analyzer rules` command, which also has the "--rules-selector", "--workspace", and "--config-file" flags that together define the list of rules to be run.

# command.examples

- Analyze code using the default behavior: analyze the files in the current folder (default workspace) using the Recommended rules; display the output in the terminal with the concise table view; and automatically apply rule or engine overrides if a "code-analyzer.yml" or "code-analyzer.yaml" file exists in the current folder:

    <%= config.bin %> <%= command.id %>

- The previous example is equivalent to this example:

    <%= config.bin %> <%= command.id %> --rule-selector Recommended --workspace . --view table --config-file ./code-analyzer.yml

- Analyze the files using the recommended "eslint" rules and show details of the violations:

    <%= config.bin %> <%= command.id %> --rule-selector eslint:Recommended --view detail

- Analyze the files using all the "eslint" rules:

    <%= config.bin %> <%= command.id %> --rule-selector eslint

- The previous example is equivalent to this example:

  <%= config.bin %> <%= command.id %>  --rule-selector eslint:all

- Analyze the files using all rules for all engines:

    <%= config.bin %> <%= command.id %> --rule-selector all

- Analyze files using the recommended "retire-js" rules analyze in a workspace that consists of all files in the folder "./other-source" and only the Apex class files (extension .cls) in the folder "./force-app":

    <%= config.bin %> <%= command.id %> --rule-selector retire-js:Recommended --workspace ./other-source --workspace ./force-app/**/*.cls

- Specify a custom configuration file and output the results to the "results.csv" file in CSV format; the commands fails if it finds a violation that exceeds the moderate severity level (3):

    <%= config.bin %> <%= command.id %> --config-file ./code-analyzer2.yml --output-file results.csv --severity-threshold 3

- Analyze the files using all the "eslint" engine rules that have a moderate severity (3) and the recommended "retire-js" engine rules with any severity:

    <%= config.bin %> <%= command.id %> --rule-selector eslint:3 --rule-selector retire-js:Recommended

- Analyze the files using only the "getter-return" rule of the "eslint" engine and any rule named "no-inner-declarations" from any engine:

    <%= config.bin %> <%= command.id %> --rule-selector eslint:getter-return --rule-selector no-inner-declarations

- Specify three method starting points to be applied to the path-based recommended rules of the Salesforce Graph Engine, "sfge", while specifying the "./src" folder as the workspace to be used to build the source graph:

    <%= config.bin %> <%= command.id %> --rule-selector sfge:Recommended --workspace ./src --path-start ./src/classes/Utils.cls#init --path-start ./src/classes/Helpers.cls#method1;method2

# flags.workspace.summary

Set of files you want to include in the code analysis. 

# flags.workspace.description

Typically, a workspace is a single project folder that contains all your files. But it can also consist of one or more folders, one or more files, and use glob patterns (wildcards). If you specify this flag multiple times, then your workspace is the sum of the files and folders. 

# flags.path-start.summary

Starting points within your workspace to restrict any path-based analysis rules to.

# flags.path-start.description

If you don't specify this flag, then any path-based analysis rules automatically discover and use all starting points found in your workspace. Use this flag to restrict the starting points to only those you want in your code analysis. 

This flag only applies to path-based analysis rules, which are of type DataFlow and Flow. These types of rules are only available from some engines, like the Salesforce Graph Engine, "sfge" for example.

If you specify a file or a folder as your starting point, then the analysis uses only the methods that have public or global accessibility. 

To specify individual methods as a starting point, use the syntax "<file>#methodName" to select a single method or "<file>#methodName1;methodName2" to select multiple methods. For example, "SomeClass.cls#method1" (single method) or "SomeClass.cls#method1;method2" (multiple methods).

You can use glob patterns (wildcards) only when specifying files and folders; you can't use glob patterns when specifying individual methods.

# flags.rule-selector.summary

Selection of rules, based on engine name, severity level, rule name, tag, or a combination of criteria separated by colons.

# flags.rule-selector.description

Use the --rule-selector flag to select the list of rules to run based on specific criteria.  For example, you can select by engine, such as the rules associated with the "retire-js" or "eslint" engine. Or select by the severity of the rules, such as high or moderate. You can also select rules using tag values or rule names. Every rule has a name, which is unique within the scope of an engine. Most rules have tags, although it's not required. An example of a tag is "Recommended".

You can combine different criteria using colons to further filter the list; the colon works as an intersection.  For example, "--rule-selector eslint:Security" runs rules associated only with the "eslint" engine that have the Security tag.  The flag "--rule-selector eslint:Security:3" flag runs the "eslint" rules that have the Security tag and moderate severity (3). To add multiple rule selectors together (a union), specify the --rule-selector flag multiple times, such as "--rule-selector eslint:Recommended --rule-selector retire-js:3".

Run `<%= config.bin %> code-analyzer rules --rule-selector all` to see the possible values for engine name, rule name, tags, and severity levels that you can use with this flag.

# flags.severity-threshold.summary

Severity level of a found violation that must be met or exceeded to cause this command to fail with a non-zero exit code.

# flags.severity-threshold.description

You can specify either a number (2) or its equivalent string (high). 

# flags.config-file.summary

Path to the configuration file used to customize the engines and rules.

# flags.config-file.description

Code Analyzer has an internal default configuration for its rule and engine properties. If you want to override these defaults, you can create a Code Analyzer configuration file.

We recommend that you name your Code Analyzer configuration file "code-analyzer.yml" or "code-analyzer.yaml" and put it at the root of your workspace. You then don't need to use this flag when you run the `<%= command.id %>` command from the root of your workspace, because it automatically looks for either file in the current folder, and if found, applies its rule overrides and engine settings. If you want to name the file something else, or put it in an alternative folder, then you must specify this flag.

To help you get started, use the `code-analyzer config` command to create your first Code Analyzer configuration file. With it, you can change the severity of an existing rule, change a rule's tags, and so on. Then use this flag to specify the file so that the command takes your customizations into account.

# flags.view.summary

Format to display the command results in the terminal. 

# flags.view.description

The format `table` is concise and shows minimal output, the format `detail` shows all available information.

# flags.output-file.summary

Output file that contains the analysis results. The file format depends on the extension you specify, such as .csv, .html, .xml, and so on. 

# flags.output-file.description

If you don't specify this flag, the command outputs the results in the terminal. Use this flag to print the results to a file; the format of the results depends on the extension you provide. For example, "--output-file results.csv" creates a comma-separated values file. You can specify one of these extensions:

- .csv
- .html or .htm
- .json
- .junit or .junit.xml
- .sarif or .sarif.json
- .xml

To output the results to multiple files, specify this flag multiple times.  For example: "--output-file ./out/results.json --output-file ./out/report.html" creates a JSON results file and an HTML file in the "./out" folder. 
