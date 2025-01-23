import {dump as yamlDump} from 'js-yaml';
import {
	CodeAnalyzer,
	CodeAnalyzerConfig,
	ConfigDescription,
	ConfigFieldDescription,
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

export class AnnotatedConfigModel implements ConfigModel {
	private readonly codeAnalyzer: CodeAnalyzer;
	private readonly userRules: RuleSelection;
	private readonly allDefaultRules: RuleSelection;

	// Note that it is important that we calculate the relevant engines list based on (the user rule selection with no
	// config) plus (user rule selection with user config) since we still want to show the "disable_engine" config value
	// in the output if a user even if selects an engine that is currently disabled. But we don't want to the engine
	// configs not associated with the user's rule selection, thus we can't use the engines from allDefaultRules.
	private readonly  relevantEngines: Set<string>;

	constructor(codeAnalyzer: CodeAnalyzer, userRules: RuleSelection, allDefaultRules: RuleSelection, relevantEngines: Set<string>) {
		this.codeAnalyzer = codeAnalyzer;
		this.userRules = userRules;
		this.allDefaultRules = allDefaultRules;
		this.relevantEngines = relevantEngines;
	}

	toFormattedOutput(format: OutputFormat): string {
		// istanbul ignore else: Should be impossible
		if (format === OutputFormat.STYLED_YAML) {
			return new StyledYamlFormatter(this.codeAnalyzer, this.userRules, this.allDefaultRules, this.relevantEngines).toYaml();
		} else if (format === OutputFormat.RAW_YAML) {
			return new PlainYamlFormatter(this.codeAnalyzer, this.userRules, this.allDefaultRules, this.relevantEngines).toYaml();
		} else {
			throw new Error(`Unsupported`)
		}
	}
}

abstract class YamlFormatter {
	private readonly config: CodeAnalyzerConfig;
	private readonly codeAnalyzer: CodeAnalyzer;
	private readonly userRules: RuleSelection;
	private readonly allDefaultRules: RuleSelection;
	private readonly relevantEngines: Set<string>;

	protected constructor(codeAnalyzer: CodeAnalyzer, userRules: RuleSelection, allDefaultRules: RuleSelection, relevantEngines: Set<string>) {
		this.config = codeAnalyzer.getConfig();
		this.codeAnalyzer = codeAnalyzer;
		this.userRules = userRules;
		this.allDefaultRules = allDefaultRules;
		this.relevantEngines = relevantEngines;
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

	private toYamlFieldWithFieldDescription(fieldName: string, resolvedValue: unknown, fieldDescription: ConfigFieldDescription): string {
		const resolvedValueJson: string = JSON.stringify(resolvedValue);
		const defaultValueJson: string = JSON.stringify(fieldDescription.defaultValue);

		let yamlField: string;
		if (!fieldDescription.wasSuppliedByUser && resolvedValueJson !== defaultValueJson) {
			// Whenever the user did not supply the value themselves but the resolved value is different from the
			// default value, this means the value was not a "fixed" value but a value "calculated" at runtime.
			// Since "calculated" values often depend on the specific environment, we do not want to actually hard code
			// this value into the config since checking in the config to CI/CD system may create a different value.
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.last-calculated-as', [resolvedValueJson]);
			yamlField = this.toYamlUncheckedFieldWithInlineComment(fieldName, fieldDescription.defaultValue, commentText);
		} else {
			yamlField = this.toYamlField(fieldName, resolvedValue, fieldDescription.defaultValue);
		}

		return this.toYamlComment(fieldDescription.descriptionText) + "\n" + yamlField
	}

	private toYamlField(fieldName: string, resolvedValue: unknown, defaultValue: unknown): string {
		const resolvedValueJson: string = JSON.stringify(resolvedValue);
		const defaultValueJson: string = JSON.stringify(defaultValue);

		if (resolvedValueJson === defaultValueJson) {
			return this.toYamlUncheckedField(fieldName, resolvedValue);
		}

		const commentText: string = getMessage(BundleName.ConfigModel, 'template.modified-from', [defaultValueJson]);
		resolvedValue = replaceAbsolutePathsWithRelativePathsWherePossible(resolvedValue, this.config.getConfigRoot() + path.sep);
		return this.toYamlUncheckedFieldWithInlineComment(fieldName, resolvedValue, commentText);
	}

	toYaml(): string {
		const topLevelDescription: ConfigDescription = this.config.getConfigDescription();
		return this.toYamlSectionHeadingComment(topLevelDescription.overview) + '\n' +
			'\n' +
			this.toYamlFieldWithFieldDescription('config_root', this.config.getConfigRoot(),
				topLevelDescription.fieldDescriptions.config_root) + '\n' +
			'\n' +
			this.toYamlFieldWithFieldDescription('log_folder', this.config.getLogFolder(),
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
		if (this.userRules.getCount() === 0) {
			const commentText: string = getMessage(BundleName.ConfigModel, 'template.yaml.no-rules-selected');
			return `rules: {} ${this.toYamlComment(commentText)}`;
		}

		let yamlCode: string = 'rules:\n';
		for (const engineName of this.userRules.getEngineNames()) {
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
		for (const userRule of this.userRules.getRulesFor(engineName)) {
			const defaultRule: Rule|null = this.getDefaultRuleFor(engineName, userRule.getName());
			yamlCode += indent(this.toYamlRuleOverridesForRule(userRule, defaultRule), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}

	private getDefaultRuleFor(engineName: string, ruleName: string): Rule|null {
		try {
			return this.allDefaultRules.getRule(engineName, ruleName);
		} catch (_e) {
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
		const engineConfigDescriptor: ConfigDescription = this.codeAnalyzer.getEngineConfigDescription(engineName);
		const userEngineConfig: EngineConfig = this.codeAnalyzer.getEngineConfig(engineName);

		let yamlCode: string = '\n' +
			this.toYamlSectionHeadingComment(engineConfigDescriptor.overview) + '\n' +
			`${engineName}:\n`;
		// By fiat, the field description will always include, at minimum, an entry for "disable_engine", so we can
		// assume that the object is not undefined.
		for (const configField of Object.keys(engineConfigDescriptor.fieldDescriptions)) {
			const fieldDescription: ConfigFieldDescription = engineConfigDescriptor.fieldDescriptions[configField];
			const resolvedValue = userEngineConfig[configField] ?? fieldDescription.defaultValue;
			// Add a leading newline to visually break up the property from the previous one.
			yamlCode += '\n' +
				indent(this.toYamlFieldWithFieldDescription(configField, resolvedValue, fieldDescription), 2) + '\n';
		}
		return yamlCode.trimEnd();
	}
}

class PlainYamlFormatter extends YamlFormatter {
	constructor(codeAnalyzer: CodeAnalyzer, userRules: RuleSelection, allDefaultRules: RuleSelection, relevantEngines: Set<string>) {
		super(codeAnalyzer, userRules, allDefaultRules, relevantEngines);
	}

	protected toYamlComment(commentText: string): string {
		return commentText.replace(/^.*/gm, s => `# ${s}`);
	}
}

class StyledYamlFormatter extends YamlFormatter {
	constructor(codeAnalyzer: CodeAnalyzer, userRules: RuleSelection, allDefaultRules: RuleSelection, relevantEngines: Set<string>) {
		super(codeAnalyzer, userRules, allDefaultRules, relevantEngines);
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
