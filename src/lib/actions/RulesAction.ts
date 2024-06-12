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

	private constructor(dependencies: RulesDependencies) {
		this.dependencies = dependencies;
	}

	public async execute(input: RulesInput): Promise<void> {
		const config: CodeAnalyzerConfig = this.dependencies.configFactory.create(input['config-file']);
		const core: CodeAnalyzer = new CodeAnalyzer(config);

		const enginePlugins = this.dependencies.engineFactory.create();
		const addEnginePromises: Promise<void>[] = enginePlugins.map(e => core.addEnginePlugin(e));
		await Promise.all(addEnginePromises);

		const ruleSelection: RuleSelection = core.selectRules(...input["rule-selector"]);
		const rules: Rule[] = core.getEngineNames().flatMap(name => ruleSelection.getRulesFor(name));

		this.dependencies.viewer.view(rules);
	}

	public static createAction(dependencies: RulesDependencies): RulesAction {
		return new RulesAction(dependencies);
	}
}
