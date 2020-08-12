import "reflect-metadata";

import {container} from "tsyringe";
import {CustomRulePathManager} from './lib/CustomRulePathManager';
import {DefaultRuleManager} from './lib/DefaultRuleManager';
import {JavascriptEslintEngine} from './lib/eslint/EslintEngine';
import {TypescriptEslintEngine} from './lib/eslint/EslintEngine';
import {PmdEngine} from './lib/pmd/PmdEngine';
import {RuleManager} from './lib/RuleManager';
import {RulePathManager} from './lib/RulePathManager';
import LocalCatalog from './lib/services/LocalCatalog';
import {Config} from './lib/util/Config';
import {ProdOverrides, EnvOverridable} from './Constants';


export const Services = {
	Config: "Config",
	RuleManager: "RuleManager",
	RuleEngine: "RuleEngine",
	RuleCatalog: "RuleCatalog",
	RulePathManager: "RulePathManager",
	EnvOverridable: "EnvOverridable"
};

function setupProd(): void {
	container.register(Services.EnvOverridable, ProdOverrides);
}

function registerAll(): void {
	container.registerSingleton(Services.Config, Config);
	container.registerSingleton(Services.RuleManager, DefaultRuleManager);
	container.registerSingleton(Services.RuleEngine, PmdEngine);
	container.registerSingleton(Services.RuleEngine, JavascriptEslintEngine);
	container.registerSingleton(Services.RuleEngine, TypescriptEslintEngine);
	container.registerSingleton(Services.RuleCatalog, LocalCatalog);
	container.registerSingleton(Services.RulePathManager, CustomRulePathManager);
}

export const Controller = {
	container,

	registerAll: (): void => {
		registerAll();
	},

	getConfig: async (): Promise<Config> => {
		const config = container.resolve<Config>(Services.Config);
		await config.init();
		return config;
	},

	getSfdxScannerPath: (): string => {
		const envOverrides = container.resolve<EnvOverridable>(Services.EnvOverridable);
		return envOverrides.getSfdxScannerPath();
	},

	createRuleManager: async (): Promise<RuleManager> => {
		const manager = container.resolve<RuleManager>(Services.RuleManager);
		await manager.init();
		return manager;
	},

	createRulePathManager: async (): Promise<RulePathManager> => {
		const manager = container.resolve<RulePathManager>(Services.RulePathManager);
		await manager.init();
		return manager;
	}
};

setupProd();
registerAll();
