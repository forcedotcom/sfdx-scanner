import {CodeAnalyzer, CodeAnalyzerConfig, Rule, RuleSelection} from '@salesforce/code-analyzer-core';

import {ConfigLoader} from '../loaders/ConfigLoader';
import {EngineLoader} from '../loaders/EngineLoader';
import {RuleViewer} from '../viewers/RuleViewer';

export type RulesDependencies = {
	configLoader: ConfigLoader;
	engineLoader: EngineLoader;
	viewer: RuleViewer;
}

export type RulesInput = {
	'config-file'?: string;
	'rule-selector': string[];
}

export class RulesAction {
	public execute(dependencies: RulesDependencies, input: RulesInput): void {
		// Step 1: Get the config.
		const config: CodeAnalyzerConfig = dependencies.configLoader.loadConfig(input['config-file']);

		// Step 2: Get a Core instance.
		const core: CodeAnalyzer = new CodeAnalyzer(config);

		// Step 3: Load our engines.
		const enginePlugins = dependencies.engineLoader.loadEngines();

		// Step 4: Add the engines to Core.
		for (const enginePlugin of enginePlugins) {
			core.addEnginePlugin(enginePlugin);
		}

		// Step 5: Select the rules.
		const ruleSelection: RuleSelection = core.selectRules(...input["rule-selector"]);

		// Step 6: Convert the rule selection into a list.
		const rules: Rule[] = core.getEngineNames().map(name => ruleSelection.getRulesFor(name)).reduce((all, next) => [...all, ...next], []);

		// Step 7: Show the rules to the user.
		dependencies.viewer.view(rules);
	}
}
