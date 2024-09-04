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

	private constructor(userState: ConfigState, _defaultState: ConfigState) {
		this.userState = userState;
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

	public static fromSelection(userState: ConfigState, defaultState: ConfigState): SpyConfigModel {
		return new SpyConfigModel(userState, defaultState);
	}
}
