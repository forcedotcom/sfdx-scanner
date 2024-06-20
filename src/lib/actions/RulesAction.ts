import {CodeAnalyzer, CodeAnalyzerConfig, Rule, RuleSelection} from '@salesforce/code-analyzer-core';

import {CodeAnalyzerConfigFactory} from '../factories/CodeAnalyzerConfigFactory';
import {EnginePluginsFactory} from '../factories/EnginePluginsFactory';
import {RuleViewer} from '../viewers/RuleViewer';

export type RulesDependencies = {
	configFactory: CodeAnalyzerConfigFactory;
	pluginsFactory: EnginePluginsFactory;
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

		const enginePlugins = this.dependencies.pluginsFactory.create();
		const enginePluginModules = config.getCustomEnginePluginModules()
			.map(pluginModule => require.resolve(pluginModule, {paths: [process.cwd()]})); // TODO: Remove this line as soon as it is moved to the core module.
		const addEnginePromises: Promise<void>[] = [
			...enginePlugins.map(enginePlugin => core.addEnginePlugin(enginePlugin)),
			...enginePluginModules.map(pluginModule => core.dynamicallyAddEnginePlugin(pluginModule))
		];
		await Promise.all(addEnginePromises);

		const ruleSelection: RuleSelection = core.selectRules(...input["rule-selector"]);
		const rules: Rule[] = core.getEngineNames().flatMap(name => ruleSelection.getRulesFor(name));

		this.dependencies.viewer.view(rules);
	}

	public static createAction(dependencies: RulesDependencies): RulesAction {
		return new RulesAction(dependencies);
	}
}
