import {CodeAnalyzerConfig, RuleSelection} from '@salesforce/code-analyzer-core';
import {BundleName, getMessage} from '../messages';

export enum OutputFormat {
	YAML = "YAML"
}

export interface ConfigModel {
	toFormattedOutput(format: OutputFormat): string;
}

export type ConfigModelGeneratorFunction = (rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection) => ConfigModel;

export class AnnotatedConfigModel implements ConfigModel {
	private readonly rawConfig: CodeAnalyzerConfig;
	private readonly ruleSelection: RuleSelection;

	private constructor(rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection) {
		this.rawConfig = rawConfig;
		this.ruleSelection = ruleSelection;
	}

	toFormattedOutput(_format: OutputFormat): string {
		return yamlFormatConfig(this.rawConfig, this.ruleSelection);
	}

	public static fromSelection(rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection): AnnotatedConfigModel {
		return new AnnotatedConfigModel(rawConfig, ruleSelection);
	}
}

function yamlFormatConfig(rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection): string {
	const defaultConfig = CodeAnalyzerConfig.withDefaults();
	return yamlFormatDerivedProperty('config_root', rawConfig.getConfigRoot(), defaultConfig.getConfigRoot())
		+ '\n'
		+ yamlFormatDerivedProperty('log_folder', rawConfig.getLogFolder(), defaultConfig.getLogFolder())
		+ '\n'
		+ yamlFormatRuleSelection(ruleSelection)
		+ '\n'
		+ DUMMY_ENGINE_SECTION
		+ '\n';
}

function yamlFormatDerivedProperty(propertyName: string, actualValue: string, defaultValue: string): string {
	// Get the annotation and prepend each line with `# ` to make it a YAML comment.
	const annotation: string = getMessage(BundleName.ConfigModel, `annotation.${propertyName}`)
		.replace(/^./gm, s => `# ${s}`);


	const value: string = actualValue === defaultValue
		? `null # ${getMessage(BundleName.ConfigModel, 'template.last-calculated-as', [actualValue])}`
		: actualValue;

	return `${annotation}\n`
		+ `${propertyName}: ${value}\n`;
}

function yamlFormatRuleSelection(ruleSelection: RuleSelection): string {
	// Get the annotation and prepend each line with '# ' to make it a YAML comment.
	const annotation: string = getMessage(BundleName.ConfigModel, `annotation.rules`)
		.replace(/^./gm, s => `# ${s}`);

	if (ruleSelection.getCount() === 0) {
		return `${annotation}\n`
			+ `rules: {} # Remove this empty object {} when you are ready to specify your first rule override`;
	}

	let results = annotation + '\n'
		+ 'rules:\n';

	for (const engine of ruleSelection.getEngineNames()) {
		results += `  ${engine}:\n`;
		for (const rule of ruleSelection.getRulesFor(engine)) {
			const ruleName: string = rule.getName();
			const severity = `${rule.getSeverityLevel()}`;
			const tags = rule.getTags();
			const tagString = tags.length > 0 ? `["${tags.join('", "')}"]` : `[]`;
			// This section is built by hand instead of using a message, because the message catalog dislikes indentation.
			results += `    ${ruleName}:\n`
				+ `      severity: ${severity}\n`
				+ `      tags: ${tagString}\n`;
		}
	}

	return results;
}

const DUMMY_ENGINE_SECTION: string =
`# Engine specific custom configuration settings of the format engines.<engine_name>.<property_name> = <value> where
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
    custom_rules: {} # Remove this empty object {} when you are ready to define your first custom rule

  # Custom configuration settings for the 'retire-js' engine
  # See <LINK_COMING_SOON> to learn more about these settings.
  retire-js:

    # Whether to turn off the 'retire-js' engine so that it is not included when running Code Analyzer commands
    disable_engine: false`;
