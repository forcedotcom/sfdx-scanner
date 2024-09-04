import * as yaml from 'js-yaml';
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

export enum OutputFormat {
	RAW_YAML = "RAW_YAML",
	STYLED_YAML = "STYLED_YAML"
}

export interface ConfigModel {
	toFormattedOutput(format: OutputFormat): string;
}

export type ConfigState = {
	config: CodeAnalyzerConfig;
	core: CodeAnalyzer;
	rules: RuleSelection;
}

export type ConfigModelGeneratorFunction = (userState: ConfigState, defaultState: ConfigState) => ConfigModel;

export class AnnotatedConfigModel implements ConfigModel {
	private readonly userState: ConfigState;
	private readonly defaultState: ConfigState;

	private constructor(userState: ConfigState, defaultState: ConfigState) {
		this.userState = userState;
		this.defaultState = defaultState;
	}

	toFormattedOutput(format: OutputFormat): string {
		// istanbul ignore else: Should be impossible
		if (format === OutputFormat.STYLED_YAML) {
			return toYaml(this.userState, this.defaultState, true);
		} else if (format === OutputFormat.RAW_YAML) {
			return toYaml(this.userState, this.defaultState, false);
		} else {
			throw new Error(`Unsupported`)
		}
	}

	public static fromSelection(userState: ConfigState, defaultState: ConfigState): AnnotatedConfigModel {
		return new AnnotatedConfigModel(userState, defaultState);
	}
}

function toYaml(userState: ConfigState, defaultState: ConfigState, styled: boolean): string {
	let result: string = '';

	// First, add the header.
	const topLevelDescription: ConfigDescription = CodeAnalyzerConfig.getConfigDescription();
	result += `${toYamlComment(topLevelDescription.overview!, styled)}\n`;
	result += '\n';

	// Next add `config_root`
	result += `${toYamlComment(topLevelDescription.fieldDescriptions!.config_root, styled)}\n`;
	result += `config_root: ${toYamlWithDerivedValueComment(userState.config.getConfigRoot(), defaultState.config.getConfigRoot(), styled)}\n`;
	result += '\n';

	// Then `log_folder'
	result += `${toYamlComment(topLevelDescription.fieldDescriptions!.log_folder, styled)}\n`;
	result += `log_folder: ${toYamlWithDerivedValueComment(userState.config.getLogFolder(), defaultState.config.getLogFolder(), styled)}\n`;
	result += '\n';

	// Then the `rules` section
	result += `${toYamlComment(topLevelDescription.fieldDescriptions!.rules, styled)}\n`;
	result += `${toYamlRules(userState, defaultState, styled)}\n`;
	result += '\n';

	// Then the `engines` section
	result += `${toYamlComment(topLevelDescription.fieldDescriptions!.engines, styled)}\n`;
	result += `${toYamlEngines(userState, styled)}`;

	return result;
}

function toYamlComment(comment: string, styled: boolean, indentLength: number = 0): string {
	// At the start of the string, and at the start of every line...
	return comment.replace(/^.*/gm, s => {
		// ...Inject a `# ` at the start to make it a comment...
		let commented = `# ${s}`;
		// ...Apply styling if requested...
		commented = styled ? makeGrey(commented) : commented;
		// ...And indent to the requested amount
		return indent(commented, indentLength);
	});
}

function toYamlWithDerivedValueComment(userValue: string, defaultValue: string, styled: boolean): string {
	if (userValue == null || userValue === defaultValue) {
		const comment = getMessage(BundleName.ConfigModel, 'template.last-calculated-as', [defaultValue]);
		return `null ${toYamlComment(comment, styled)}`;
	} else {
		return `${userValue}`;
	}
}

function toYamlRules(userState: ConfigState, defaultState: ConfigState, styled: boolean): string {
	if (userState.rules.getCount() === 0) {
		const comment = getMessage(BundleName.ConfigModel, 'template.yaml.remove-empty-object');
		return `rules: {} ${toYamlComment(comment, styled)}`;
	}
	let results: string = 'rules:\n';
	for (const engineName of userState.core.getEngineNames()) {
		const userRulesForEngine = userState.rules.getRulesFor(engineName);
		if (userRulesForEngine.length > 0) {
			results += indent(`${engineName}:\n`, 2);
			for (const userRule of userRulesForEngine) {
				const defaultRule = getDefaultRule(defaultState, engineName, userRule.getName());
				results += indent(toYamlRule(userRule, defaultRule, styled), 4);
			}
		}
	}
	return results;
}

function getDefaultRule(defaultState: ConfigState, engineName: string, ruleName: string): Rule|null {
	try {
		return defaultState.rules.getRule(engineName, ruleName);
	} catch (e) {
		return null;
	}
}

function toYamlRule(userRule: Rule, defaultRule: Rule|null, styled: boolean): string {
	const ruleName: string = userRule.getName();
	const userSeverity: SeverityLevel = userRule.getSeverityLevel();
	const userTags: string[] = userRule.getTags();

	let severityYaml = yaml.dump({severity: userSeverity});
	let tagsYaml = yaml.dump({tags: userTags});

	if (defaultRule != null) {
		const defaultSeverity: SeverityLevel = defaultRule.getSeverityLevel();
		const defaultTagsJson: string = JSON.stringify(defaultRule.getTags());

		if (userSeverity !== defaultSeverity) {
			const comment = getMessage(BundleName.ConfigModel, 'template.modified-from', [defaultSeverity]);
			severityYaml = severityYaml.replace('\n', ` ${toYamlComment(comment, styled)}\n`);
		}

		if (JSON.stringify(userTags) !== defaultTagsJson) {
			const comment = getMessage(BundleName.ConfigModel, 'template.modified-from', [defaultTagsJson]);
			// The YAML spec requires a trailing newline, so we know that there's at least one newline somewhere in the
			// string. If we inject a comment before the first newline we encounter, then it will look clean.
			tagsYaml = tagsYaml.replace('\n', ` ${toYamlComment(comment, styled)}\n`);
		}
	}
	return `${ruleName}:\n${indent(severityYaml, 2)}${indent(tagsYaml, 2)}`;
}

function toYamlEngines(userState: ConfigState, styled: boolean): string {
	let results: string = 'engines:\n'

	for (const engineName of userState.core.getEngineNames()) {
		const engineConfigDescriptor = userState.core.getEngineConfigDescription(engineName);
		const engineConfig = userState.core.getEngineConfig(engineName);

		if (engineConfigDescriptor.overview) {
			results += `${toYamlComment(engineConfigDescriptor.overview, styled, 2)}\n`;
		}
		results += indent(`${engineName}:`, 2) + '\n';
		// By fiat, the field description will always include, at minimum, an entry for "disable_field", so we can
		// assume that the object is not undefined.
		for (const configField of Object.keys(engineConfigDescriptor.fieldDescriptions!)) {
			const fieldDescription = engineConfigDescriptor.fieldDescriptions![configField];
			const fieldValue = engineConfig[configField] ?? null;
			results += toYamlComment(fieldDescription, styled, 4) + '\n';
			results += indent(`${yaml.dump({[configField]: fieldValue})}`, 4);
		}
	}
	return results;
}
