import {CodeAnalyzerConfig, RuleSelection} from "@salesforce/code-analyzer-core";
import {ConfigModel, OutputFormat} from '../../src/lib/models/ConfigModel';

export class StubConfigModel implements ConfigModel {
	public toFormattedOutput(format: OutputFormat): string {
		return `# This is a leading comment\n`
			+ `Results formatted as ${format}`
	}
}

export class SpyConfigModel implements ConfigModel {
	private readonly rawConfig: CodeAnalyzerConfig;
	private readonly ruleSelection: RuleSelection;

	private constructor(rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection) {
		this.rawConfig = rawConfig;
		this.ruleSelection = ruleSelection;
	}

	public toFormattedOutput(format: OutputFormat): string {
		return `Results formatted as ${format}`;
	}

	public getRawConfig(): CodeAnalyzerConfig {
		return this.rawConfig;
	}

	public getRuleSelection(): RuleSelection {
		return this.ruleSelection;
	}

	public static fromSelection(rawConfig: CodeAnalyzerConfig, ruleSelection: RuleSelection): SpyConfigModel {
		return new SpyConfigModel(rawConfig, ruleSelection);
	}
}
