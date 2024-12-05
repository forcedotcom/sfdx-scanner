import {dump as yamlDump} from 'js-yaml';
import {
	CodeAnalyzer,
	CodeAnalyzerConfig,
	ConfigDescription, ConfigFieldDescription,
	EngineConfig,
	Rule,
	RuleSelection,
	SeverityLevel
} from '@salesforce/code-analyzer-core';
import {indent, makeGrey} from '../utils/StylingUtil';
import {BundleName, getMessage} from '../messages';
import path from "node:path";

export enum OutputFormat {
	RAW_YAML = "RAW_YAML",
	STYLED_YAML = "STYLED_YAML"
}

export interface ConfigModel {
	toFormattedOutput(format: OutputFormat): string;
}

export type ConfigContext = {
	config: CodeAnalyzerConfig;
	core: CodeAnalyzer;
	rules: RuleSelection;
}

export type ConfigModelGeneratorFunction = (relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) => ConfigModel;

export class AnnotatedConfigModel implements ConfigModel {
	private readonly relevantEngines: Set<string>;
	private readonly userContext: ConfigContext;
	private readonly defaultContext: ConfigContext;

	private constructor(relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) {
		this.relevantEngines = relevantEngines;
		this.userContext = userContext;
		this.defaultContext = defaultContext;
	}

	toFormattedOutput(format: OutputFormat): string {
		// istanbul ignore else: Should be impossible
		if (format === OutputFormat.STYLED_YAML) {
			return new StyledYamlFormatter(this.relevantEngines, this.userContext, this.defaultContext).toYaml();
		} else if (format === OutputFormat.RAW_YAML) {
			return new PlainYamlFormatter(this.relevantEngines, this.userContext, this.defaultContext).toYaml();
		} else {
			throw new Error(`Unsupported`)
		}
	}

	public static fromSelection(relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext): AnnotatedConfigModel {
		return new AnnotatedConfigModel(relevantEngines, userContext, defaultContext);
	}
}

abstract class YamlFormatter {
	private readonly relevantEngines: Set<string>;
	private readonly userContext: ConfigContext;
	private readonly defaultContext: ConfigContext;

	protected constructor(relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) {
		this.relevantEngines = relevantEngines;
		this.userContext = userContext;
		this.defaultContext = defaultContext;
	}

	protected abstract toYamlComment(commentText: string): string

	private toYamlSectionHeadingComment(commentText: string): string {
		const horizontalLine: string = '='.repeat(70);
		return this.toYamlComment(`${horizontalLine}\n${commentText}\n${horizontalLine}`);
	}

	private toYamlUncheckedField(fieldName: string, fieldValue: unknown): string {
		return yamlDump({[fieldName]: fieldValue}).trim();
	}

	private toYamlUncheckedFieldWithInlineComment(fieldName: string, fieldValue: unknown, commentText: string): string {
		const yamlCode: string = this.toYamlUncheckedField(fieldName, fieldValue);
		const comment: string = this.toYamlComment(commentText);
		return yamlCode.replace(/(\r?\n|$)/, ` ${comment}$1`);
	}

	private toYamlFieldUsingFieldDescription(fieldName: string, resolvedValue: unknown, fieldDescription: ConfigFieldDescription): string {
		const resolvedValueJson: string = JSON.stringify(resolvedValue);
		const defaultValueJson: string = JSON.stringify(fieldDescription.defaultValue);

		if (!fieldDescription.wasSuppliedByUser && resolvedValueJson !== defaultValueJson) {
			// Whenever the user did not supply the value themselves but the resolved value is different from the
			// default value, this means the value was not a "fixed" value but a value "calculated" at runtime.
			// Since "calculated" values often depend on the specific environment, we do not want to actually hard code
			// this value into the config since checking in the config to CI/CD system may create a different value.
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.last-calculated-as', [resolvedValueJson]);
			return this.toYamlUncheckedFieldWithInlineComment(fieldName, fieldDescription.defaultValue, commentText);
		}

		return this.toYamlField(fieldName, resolvedValue, fieldDescription.defaultValue);
	}

	private toYamlField(fieldName: string, resolvedValue: unknown, defaultValue: unknown): string {
		const resolvedValueJson: string = JSON.stringify(resolvedValue);
		const defaultValueJson: string = JSON.stringify(defaultValue);

		if (resolvedValueJson === defaultValueJson) {
			return this.toYamlUncheckedField(fieldName, resolvedValue);
		}

		const commentText: string = getMessage(BundleName.ConfigModel, 'template.modified-from', [defaultValueJson]);
		resolvedValue = replaceAbsolutePathsWithRelativePathsWherePossible(resolvedValue, this.userContext.config.getConfigRoot() + path.sep);
		return this.toYamlUncheckedFieldWithInlineComment(fieldName, resolvedValue, commentText);
	}

	toYaml(): string {
		const topLevelDescription: ConfigDescription = this.userContext.config.getConfigDescription();
		return this.toYamlSectionHeadingComment(topLevelDescription.overview) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions.config_root.descriptionText) + '\n' +
			this.toYamlFieldUsingFieldDescription('config_root', this.userContext.config.getConfigRoot(),
				topLevelDescription.fieldDescriptions.config_root) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions.log_folder.descriptionText) + '\n' +
			this.toYamlFieldUsingFieldDescription('log_folder', this.userContext.config.getLogFolder(),
				topLevelDescription.fieldDescriptions.log_folder) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions.rules.descriptionText) + '\n' +
			this.toYamlRuleOverrides() + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions.engines.descriptionText) + '\n' +
			this.toYamlEngineOverrides() + '\n' +
			'\n' +
			this.toYamlSectionHeadingComment(getMessage(BundleName.ConfigModel, 'template.common.end-of-config')) + '\n';
	}

	private toYamlRuleOverrides(): string {
		if (this.userContext.rules.getCount() === 0) {
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.yaml.no-rules-selected');
			return `rules: {} ${this.toYamlComment(commentText)}`;
		}

		let yamlCode: string = 'rules:\n';
		for (const engineName of this.userContext.rules.getEngineNames()) {
			yamlCode += '\n';
			yamlCode += indent(this.toYamlRuleOverridesForEngine(engineName), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}

	private toYamlRuleOverridesForEngine(engineName: string): string {
		const engineConfigHeader: string = getMessage(BundleName.ConfigModel, 'template.rule-overrides-section',
			[engineName.toUpperCase()]);
		let yamlCode: string = this.toYamlSectionHeadingComment(engineConfigHeader) + '\n';
		yamlCode += `${engineName}:\n`;
		for (const userRule of this.userContext.rules.getRulesFor(engineName)) {
			const defaultRule: Rule|null = this.getDefaultRuleFor(engineName, userRule.getName());
			yamlCode += indent(this.toYamlRuleOverridesForRule(userRule, defaultRule), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}

	private getDefaultRuleFor(engineName: string, ruleName: string): Rule|null {
		try {
			return this.defaultContext.rules.getRule(engineName, ruleName);
		} catch (e) {
			// istanbul ignore next
			return null;
		}
	}

	private toYamlRuleOverridesForRule(userRule: Rule, defaultRule: Rule|null): string {
		const userSeverity: SeverityLevel = userRule.getSeverityLevel();
		const userTags: string[] = userRule.getTags();
		return `"${userRule.getName()}":\n` +
			indent(this.toYamlField('severity', userSeverity, defaultRule !== null ? defaultRule.getSeverityLevel() : userSeverity), 2) + '\n' +
			indent(this.toYamlField('tags', userTags, defaultRule !== null ? defaultRule.getTags() : userTags), 2);
	}

	private toYamlEngineOverrides(): string {
		if (this.relevantEngines.size === 0) {
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.yaml.no-engines-selected');
			return `engines: {} ${this.toYamlComment(commentText)}`;
		}

		let yamlCode: string = 'engines:\n'
		for (const engineName of this.relevantEngines.keys()) {
			yamlCode += indent(this.toYamlEngineOverridesForEngine(engineName), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}

	private toYamlEngineOverridesForEngine(engineName: string): string {
		const engineConfigDescriptor: ConfigDescription = this.userContext.core.getEngineConfigDescription(engineName);
		const userEngineConfig: EngineConfig = this.userContext.core.getEngineConfig(engineName);

		let yamlCode: string = '\n' +
			this.toYamlSectionHeadingComment(engineConfigDescriptor.overview) + '\n' +
			`${engineName}:\n`;
		// By fiat, the field description will always include, at minimum, an entry for "disable_engine", so we can
		// assume that the object is not undefined.
		for (const configField of Object.keys(engineConfigDescriptor.fieldDescriptions)) {
			const fieldDescription: ConfigFieldDescription = engineConfigDescriptor.fieldDescriptions[configField];
			const userValue = userEngineConfig[configField] ?? fieldDescription.defaultValue;
			// Add a leading newline to visually break up the property from the previous one.
			yamlCode += '\n' +
				indent(this.toYamlComment(fieldDescription.descriptionText), 2) + '\n' +
				indent(this.toYamlFieldUsingFieldDescription(configField, userValue, fieldDescription), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}
}

class PlainYamlFormatter extends YamlFormatter {
	constructor(relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) {
		super(relevantEngines, userContext, defaultContext);
	}

	protected toYamlComment(commentText: string): string {
		return commentText.replace(/^.*/gm, s => `# ${s}`);
	}
}

class StyledYamlFormatter extends YamlFormatter {
	constructor(relevantEngines: Set<string>, userContext: ConfigContext, defaultContext: ConfigContext) {
		super(relevantEngines, userContext, defaultContext);
	}

	protected toYamlComment(commentText: string): string {
		return commentText.replace(/^.*/gm, s => makeGrey(`# ${s}`));
	}
}

function replaceAbsolutePathsWithRelativePathsWherePossible(value: unknown, parentFolder: string): unknown {
	if (typeof value === 'string') {
		// Check if the string starts with the parent folder
		if (value.startsWith(parentFolder)) {
			// Strip the parent folder from the start of the string
			return value.substring(parentFolder.length);
		}
		return value; // Return unchanged if it doesn't start with the parent folder
	} else if (Array.isArray(value)) {
		// If value is an array, recursively process each element
		return value.map(item => replaceAbsolutePathsWithRelativePathsWherePossible(item, parentFolder));
	} else if (typeof value === 'object' && value !== null) {
		// If value is an object, recursively process each key-value pair
		const updatedObject: object = {};
		for (const key in value) {
			updatedObject[key] = replaceAbsolutePathsWithRelativePathsWherePossible(value[key], parentFolder);
		}
		return updatedObject;
	}
	// Return the value unchanged if it's a number, boolean, or null
	return value;
}
