import "reflect-metadata";

import {SfError} from '@salesforce/core';
import {container} from "tsyringe";
import {Config} from './lib/util/Config';
import {DEFAULT_SCANNER_PATH, ENV_VAR_NAMES, Services, AllowedEngineFilters} from './Constants';
import {RuleManager} from './lib/RuleManager';
import {RuleEngine} from './lib/services/RuleEngine';
import {RulePathManager} from './lib/RulePathManager';
import {RuleCatalog} from './lib/services/RuleCatalog';
import {Bundle, getMessage} from "./MessageCatalog";

/**
 * Converts an array of RuleEngines to a sorted, comma delimited
 * string of their names.
 */
function enginesToString(engines: RuleEngine[]): string {
	return engines.map(e => e.getName()).sort().join(', ');
}

// TODO: This is probably more appropriately called a Factory
export const Controller = {
	container,

	getCatalog: async (): Promise<RuleCatalog> => {
		const catalog = container.resolve<RuleCatalog>(Services.RuleCatalog);
		await catalog.init();
		return catalog;
	},

	getConfig: async (): Promise<Config> => {
		const config = container.resolve<Config>(Services.Config);
		await config.init();
		return config;
	},

	getSfdxScannerPath: (): string => {
		return process.env[ENV_VAR_NAMES.SCANNER_PATH_OVERRIDE] || DEFAULT_SCANNER_PATH;
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

		return engines;
	},

	getFilteredEngines: async (filteredNames: string[], engineOptions: Map<string, string> = new Map()): Promise<RuleEngine[]> => {
		const allEngines: RuleEngine[] = await Controller.getAllEngines();
		const engines = allEngines.filter(e => e.isEngineRequested(filteredNames, engineOptions));

		if (engines.length == 0) {
			const msg = getMessage(Bundle.Controller, 'NoFilteredEnginesFound', [filteredNames.join(','), AllowedEngineFilters.sort().join(', ')]);
			throw new SfError(msg);
		}

		return engines;
	},

	getEnabledEngines: async (engineOptions: Map<string,string> = new Map()): Promise<RuleEngine[]> => {
		const allEngines: RuleEngine[] = await Controller.getAllEngines();
		const engines: RuleEngine[] = [];

		for (const engine of allEngines) {
			if (await engine.isEnabled() && engine.isEngineRequested([/*no filter values*/], engineOptions)) {
				engines.push(engine);
			}
		}
		if (engines.length == 0) {
			const msg = getMessage(Bundle.Controller, 'NoEnabledEnginesFound', [enginesToString(allEngines)]);
			throw new SfError(msg);
		}

		return engines;
	}
};
