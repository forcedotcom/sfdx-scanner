import "reflect-metadata";

import {SfdxError} from '@salesforce/core';
import {container} from "tsyringe";
import {Config} from './lib/util/Config';
import {EnvOverridable, Services} from './Constants';
import {RuleManager} from './lib/RuleManager';
import {RuleEngine} from './lib/services/RuleEngine'
import {DependencyChecker} from './lib/services/DependencyChecker';
import {RulePathManager} from './lib/RulePathManager';

/**
 * Converts an array of RuleEngines to a sorted, comma delimited
 * string of their names.
 */
function servicesToString(services: (RuleEngine|DependencyChecker)[]): string {
	return services.map(s => s.getName()).sort().join(', ');
}

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

		return engines;
	},

	getFilteredEngines: async (filteredNames: string[]): Promise<RuleEngine[]> => {
		const allEngines: RuleEngine[] = await Controller.getAllEngines();
		const engines = allEngines.filter(e => filteredNames.includes(e.getName()));

		if (engines.length == 0) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'Controller', 'NoFilteredEnginesFound', [filteredNames.join(','), servicesToString(allEngines)]);
		}

		return engines;
	},

	getEnabledEngines: async (): Promise<RuleEngine[]> => {
		const allEngines: RuleEngine[] = await Controller.getAllEngines();
		const engines: RuleEngine[] = allEngines.filter(e => e.isEnabled());

		if (engines.length == 0) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'Controller', 'NoEnabledEnginesFound', [servicesToString(allEngines)]);
		}

		return engines;
	},

	getAllDepCheckers: async(): Promise<DependencyChecker[]> => {
		const depCheckers: DependencyChecker[] = [];
		for (const depcheck of container.resolveAll<DependencyChecker>(Services.DependencyChecker)) {
			await depcheck.init();
			depCheckers.push(depcheck);
		}
		return depCheckers;
	},

	getEnabledDepCheckers: async (): Promise<DependencyChecker[]> => {
		const allDepCheckers: DependencyChecker[] = await Controller.getAllDepCheckers();
		const enabledCheckers: DependencyChecker[] = allDepCheckers.filter(d => d.isEnabled());

		if (enabledCheckers.length === 0) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'Controller', 'NoEnabledDepCheckersFound', [servicesToString(allDepCheckers)]);
		}

		return enabledCheckers;
	}
};
