import {CodeAnalyzerConfig, RuleSelection} from "@salesforce/code-analyzer-core";
import {ConfigModel, ConfigState, OutputFormat} from '../../src/lib/models/ConfigModel';

export class StubConfigModel implements ConfigModel {
	public toFormattedOutput(format: OutputFormat): string {
		return `# This is a leading comment\n`
			+ `Results formatted as ${format}`
	}
}

export class SpyConfigModel implements ConfigModel {
	private readonly userState: ConfigState;
	private readonly defaultState: ConfigState;

	private constructor(userState: ConfigState, defaultState: ConfigState) {
		this.userState = userState;
		this.defaultState = defaultState;
	}

	public toFormattedOutput(format: OutputFormat): string {
		return `Results formatted as ${format}`;
	}

	public getUserConfig(): CodeAnalyzerConfig {
		return this.userState.config;
	}

	public getUserRuleSelection(): RuleSelection {
		return this.userState.rules;
	}

	public getDefaultConfig(): CodeAnalyzerConfig {
		return this.defaultState.config;
	}

	public getDefaultRuleSelection(): RuleSelection {
		return this.defaultState.rules;
	}

	public static fromSelection(userState: ConfigState, defaultState: ConfigState): SpyConfigModel {
		return new SpyConfigModel(userState, defaultState);
	}
}
