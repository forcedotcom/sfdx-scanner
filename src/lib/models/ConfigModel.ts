import {dump as yamlDump} from 'js-yaml';
import {
	CodeAnalyzer,
	CodeAnalyzerConfig,
	ConfigDescription,
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

	private toYamlField(fieldName: string, userValue: unknown, defaultValue: unknown): string {
		if (looksLikeAPathValue(userValue) && userValue === defaultValue) {
			// We special handle a path value when it is equal to the default value, making it equal null because
			// chances are it is a derived file or folder value based on the specific environment that we do not want to
			// actually want to hard code since checking in the config to CI/CD system may create a different value
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.last-calculated-as', [JSON.stringify(userValue)]);
			return this.toYamlUncheckedFieldWithInlineComment(fieldName, null, commentText);
		} else if (JSON.stringify(userValue) === JSON.stringify(defaultValue)) {
			return this.toYamlUncheckedField(fieldName, userValue);
		} else {
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.modified-from', [JSON.stringify(defaultValue)]);
			return this.toYamlUncheckedFieldWithInlineComment(fieldName, userValue, commentText);
		}
	}

	toYaml(): string {
		const topLevelDescription: ConfigDescription = CodeAnalyzerConfig.getConfigDescription();
		return this.toYamlSectionHeadingComment(topLevelDescription.overview!) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions!.config_root) + '\n' +
			this.toYamlField('config_root', this.userContext.config.getConfigRoot(), this.defaultContext.config.getConfigRoot()) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions!.log_folder) + '\n' +
			this.toYamlField('log_folder', this.userContext.config.getLogFolder(), this.defaultContext.config.getLogFolder()) + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions!.rules) + '\n' +
			this.toYamlRuleOverrides() + '\n' +
			'\n' +
			this.toYamlComment(topLevelDescription.fieldDescriptions!.engines) + '\n' +
			this.toYamlEngineOverrides() + '\n';
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
		const engineConfigDescriptor = this.userContext.core.getEngineConfigDescription(engineName);
		const userEngineConfig = this.userContext.core.getEngineConfig(engineName);
		const defaultEngineConfig = this.defaultContext.core.getEngineConfig(engineName);

		let yamlCode: string = '\n' +
			this.toYamlSectionHeadingComment(engineConfigDescriptor.overview!) + '\n' +
			`${engineName}:\n`;
		// By fiat, the field description will always include, at minimum, an entry for "disable_engine", so we can
		// assume that the object is not undefined.
		for (const configField of Object.keys(engineConfigDescriptor.fieldDescriptions!)) {
			const fieldDescription = engineConfigDescriptor.fieldDescriptions![configField];
			const defaultValue = defaultEngineConfig[configField] ?? null;
			const userValue = userEngineConfig[configField] ?? defaultValue;
			// Add a leading newline to visually break up the property from the previous one.
			yamlCode += '\n' +
				indent(this.toYamlComment(fieldDescription), 2) + '\n' +
				indent(this.toYamlField(configField, userValue, defaultValue), 2) + '\n';
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

function looksLikeAPathValue(value: unknown) {
	return typeof(value) === 'string' && !value.includes('\n') && value.includes(path.sep);
}
