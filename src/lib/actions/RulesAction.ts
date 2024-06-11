import {CodeAnalyzer, CodeAnalyzerConfig, Rule, RuleSelection} from '@salesforce/code-analyzer-core';

import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginFactory} from '../factories/EnginePluginFactory';
import {RuleViewer} from '../viewers/RuleViewer';

export type RulesDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	engineFactory: EnginePluginFactory;
	viewer: RuleViewer;
}

export type RulesInput = {
	'config-file'?: string;
	'rule-selector': string[];
}

export class RulesAction {
	private readonly dependencies: RulesDependencies;

	public constructor(dependencies: RulesDependencies) {
		this.dependencies = dependencies;
	}

	public execute(input: RulesInput): void {
		const config: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const core: CodeAnalyzer = new CodeAnalyzer(config);

		const enginePlugins = this.dependencies.engineFactory.create();
		for (const enginePlugin of enginePlugins) {
			core.addEnginePlugin(enginePlugin);
		}

		const ruleSelection: RuleSelection = core.selectRules(...input["rule-selector"]);
		const rules: Rule[] = core.getEngineNames().flatMap(name => ruleSelection.getRulesFor(name));

		this.dependencies.viewer.view(rules);
	}
}
