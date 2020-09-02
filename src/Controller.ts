import "reflect-metadata";

import {container} from "tsyringe";
import {Config} from './lib/util/Config';
import {EnvOverridable, Services} from './Constants';
import {RuleManager} from './lib/RuleManager';
import {RuleEngine} from './lib/services/RuleEngine'
import {RulePathManager} from './lib/RulePathManager';

// TODO: This is probably more appropriately called a Factory
export const Controller = {
	container,

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
	},

	getAllEngines: async (): Promise<RuleEngine[]> => {
		const engines: RuleEngine[] = [];
		for (const engine of container.resolveAll<RuleEngine>(Services.RuleEngine)) {
			await engine.init();
			engines.push(engine);
		}

		if (engines.length == 0) {
			throw new Error('No engines found');
		}
		return engines;
	},

	getFilteredEngines: async (filteredNames: string[]): Promise<RuleEngine[]> => {
		const engines: RuleEngine[] = (await Controller.getAllEngines()).filter(e => filteredNames.includes(e.getName()));

		if (engines.length == 0) {
			throw new Error('No engines found');
		}
		return engines;
	},

	getEnabledEngines: async (): Promise<RuleEngine[]> => {
		const engines: RuleEngine[] = (await Controller.getAllEngines()).filter(e => e.isEnabled());

		if (engines.length == 0) {
			throw new Error('No engines found');
		}
		return engines;
	}
};
