import "reflect-metadata";

import {container} from "tsyringe";
import {CustomRulePathManager} from './lib/CustomRulePathManager';
import {DefaultManager} from './lib/DefaultManager';
import {ESLintEngine} from './lib/eslint/ESLintEngine';
import {PmdEngine} from './lib/pmd/PmdEngine';
import {RuleManager} from './lib/RuleManager';
import {RulePathManager} from './lib/RulePathManager';
import LocalCatalog from './lib/services/LocalCatalog';

export const Services = {
	RuleManager: "RuleManager",
	RuleEngine: "RuleEngine",
	RuleCatalog: "RuleCatalog",
	RulePathManager: "RulePathManager"
};

container.register(Services.RuleManager, {
	useClass: DefaultManager
});
container.register(Services.RuleEngine, {
	useClass: PmdEngine
});
container.register(Services.RuleEngine, {
	useClass: ESLintEngine
});
container.register(Services.RuleCatalog, {
	useClass: LocalCatalog
});
container.register(Services.RulePathManager, {
	useClass: CustomRulePathManager
});

export const Controller = {
	container,
	createManager: async (): Promise<RuleManager> => {
		try {
			const manager = container.resolve<RuleManager>(Services.RuleManager);
			await manager.init();
			return manager;
		} catch (e) {
			console.log(e.stack);
		}
	},

	createRulePathManager: async (): Promise<RulePathManager> => {
		const manager = container.resolve<RulePathManager>(Services.RulePathManager);
		await manager.init();
		return manager;
	}
};
