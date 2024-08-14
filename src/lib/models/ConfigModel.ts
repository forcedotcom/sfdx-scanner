
export enum OutputFormat {
	YAML = "YAML"
}

export interface ConfigModel {
	toFormattedOutput(format: OutputFormat): string;
}

export class DummyConfigModel {
	toFormattedOutput(_format: OutputFormat): string {
		return DUMMY_CONFIG;
	}
}

export const DUMMY_CONFIG: string =
`# The absolute folder path to which all other path values in this configuration may be relative to.
# If not specified or if equal to null, then the value is automatically chosen to be the parent folder of your Code Analyzer
# configuration file if it exists, or the current working directory otherwise.
config_root: null # Last calculated by the config command as: '/Users/stephen.carter/temp'

# Folder where to store log files. May be an absolute path or a path relative to config_root.
# If not specified or if equal to null, then the value is automatically chosen to be your machine's default temporary directory
log_folder: null  # Last calculated by the config command as: '/var/folders/1x/9xkd2bj500z58k2s5b8f2y980000gp/T/'

# (Use at your own risk. This is an experimental property that is not officially supported.)
# List of EnginePlugin module paths to dynamically add to Code Analyzer. Each path may be absolute or relative to config_root.
custom-engine_plugin_modules: []

# Rule override settings of the format rules.<engine_name>.<rule_name>.<property_name> = <override_value> where
#   * <engine_name> is the name of the engine containing the rule that you want to override
#   * <rule_name> is the name of the rule that you want to override
#   * <property_name> can either be:
#      'severity' - [Optional] The severity level value that you want to use to override the default severity level for the rule
#                   Possible values: 1 or 'Critical', 2 or 'High', 3 or 'Moderate', 4 or 'Low', 5 or 'Info'
#      'tags'     - [Optional] The string array of tag values that you want to use to override the default tags for the rule
#
# [Example usage]:
# rules:
#   eslint:
#     sort-vars:
#       severity: Info
#       tags: ["Recommended", "Suggestion"]
rules: {} # Remove this empty object {} when you are ready to specify your first rule override

# Engine specific custom configuration settings of the format engines.<engine_name>.<property_name> = <value> where
#   <engine_name> is the name of the engine containing the rule that you want to override
#   <property_name> is the name of a property associated with the engine that you would like to set
# Each engine will have its own set of properties that will be available that will help you customize that particular engine's
# behavior. See <LINK_COMING_SOON> to learn more.
engines:

  # Custom configuration settings for the 'eslint' engine
  # See <LINK_COMING_SOON> to learn more about these settings.
  eslint:

    # Whether to turn off the 'eslint' engine so that it is not included when running Code Analyzer commands
    disable_engine: false

    # Whether to have Code Analyzer automatically discover/apply any ESLint configuration and ignore files from your workspace
    auto_discover_eslint_config: false

    # Your project's main ESLint configuration file. May be an absolute path or a path relative to the config_root.
    # If null and auto_discover_eslint_config is true, then Code Analyzer will attempt to discover/apply it automatically.
    # Currently only legacy ESLInt config files are supported.
    # See https://eslint.org/docs/v8.x/use/configure/configuration-files to learn more.
    eslint_config_file: null

    # Your project's ".eslintignore" file. May be an absolute path or a path relative to the config_root.
    # If null and auto_discover_eslint_config is true, then Code Analyzer will attempt to discover/apply it automatically.
    # See https://eslint.org/docs/v8.x/use/configure/ignore#the-eslintignore-file to learn more.
    eslint_ignore_file: null

    # Whether to turn off the default base configuration that supplies the standard ESLint rules for javascript files
    disable_javascript_base_config: false

    # Whether to turn off the default base configuration that supplies the LWC rules for javascript files
    disable_lwc_base_config: false

    # Whether to turn off the default base configuration that supplies the standard rules for typescript files
    disable_typescript_base_config: false

    # Extensions of the javascript files in your workspace that will be associated with javascript and LWC rules
    javascript_file_extensions: ['.js', '.cjs', '.mjs']

    # Extensions of the typescript files in your workspace that will be associated with typescript rules
    typescript_file_extensions: ['.ts']

  # Custom configuration settings for the 'regex' engine
  # See <LINK_COMING_SOON> to learn more about these settings.
  regex:
    # Whether to turn off the 'regex' engine so that it is not included when running Code Analyzer commands
    disable_engine: false

    # Custom rules to be added to the 'regex' engine of the format custom_rules.<rule_name>.<rule_property_name> = <value> where
    #   * <rule_name> is the name you would like to give to your custom rule
    #   * <rule_property_name> is the name of one of the rule properties. You may specify the following rule properties:
    #     'regex'             - The regular expression that triggers a violation when matched against the contents of a file.
    #     'file_extensions'   - The extensions of the files that you would like to test the regular expression against.
    #     'description'       - A description of the rule's purpose
    #     'violation_message' - [Optional] The message emitted when a rule violation occurs.
    #                           This message is intended to help the user understand the violation.
    #                           Default: 'A match of the regular expression <regex> was found for rule <rule_name>: <description>'
    #     'severity'          - [Optional] The severity level to apply to this rule by default.
    #                           Possible values: 1 or 'Critical', 2 or 'High', 3 or 'Moderate', 4 or 'Low', 5 or 'Info'
    #                           Default: 'Moderate'
    #     'tags'              - [Optional] The string array of tag values to apply to this rule by default.
    #                           Default: ['Recommended']
    #
    # [Example usage]:
    # engines:
    #   regex:
    #     custom_rules:
    #       NoTodoComments:
    #         regex: /\\/\\/[ \\t]*TODO/gi
    #         file_extensions: [".cls", ".trigger"]
    #         description: "Prevents TODO comments from being in apex code."
    #         violation_message: "A comment with a TODO statement was found. Please remove TODO statements from your apex code."
    #         severity: "Info"
    #         tags: ['TechDebt']
    #

    custom_rules: {} # Remove this empty object {} when you are ready to define your first custom rule

  # Custom configuration settings for the 'retire-js' engine
  # See <LINK_COMING_SOON> to learn more about these settings.
  retire-js:
    # Whether to turn off the 'retire-js' engine so that it is not included when running Code Analyzer commands
    disable_engine: false
`;
