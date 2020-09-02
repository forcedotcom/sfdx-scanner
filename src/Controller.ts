import "reflect-metadata";

import {Logger} from '@salesforce/core';
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

	getEnabledEngines: async (): Promise<RuleEngine[]> => {
		const engines: RuleEngine[] = [];
		const logger: Logger = await Logger.child('Controller');

		for (const engine of container.resolveAll<RuleEngine>(Services.RuleEngine)) {
			await engine.init();
			if (engine.isEnabled()) {
				engines.push(engine);
			} else {
				logger.trace(`Engine '${engine.getName()}' is disabled}`);
			}
		}

		if (engines.length == 0) {
			throw new Error('No engines found');
		}
		return engines;
	}
};
