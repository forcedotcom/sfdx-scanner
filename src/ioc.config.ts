import "reflect-metadata";

import {container} from "tsyringe";
import {CustomRulePathManager} from './lib/CustomRulePathManager';
import {DefaultRuleManager} from './lib/DefaultRuleManager';
import {JavascriptEslintEngine} from './lib/eslint/EslintEngine';
import {TypescriptEslintEngine} from './lib/eslint/EslintEngine';
import {PmdEngine} from './lib/pmd/PmdEngine';
import LocalCatalog from './lib/services/LocalCatalog';
import {Config} from './lib/util/Config';
import {ProdOverrides, Services} from './Constants';

function setupProd(): void {
	container.register(Services.EnvOverridable, ProdOverrides);
}

/**
 * Initialize the ioc container with singletons common to test and prod
 */
export function registerAll(): void {
	container.registerSingleton(Services.Config, Config);
	container.registerSingleton(Services.RuleManager, DefaultRuleManager);
	container.registerSingleton(Services.RuleEngine, PmdEngine);
	container.registerSingleton(Services.RuleEngine, JavascriptEslintEngine);
	container.registerSingleton(Services.RuleEngine, TypescriptEslintEngine);
	container.registerSingleton(Services.RuleCatalog, LocalCatalog);
	container.registerSingleton(Services.RulePathManager, CustomRulePathManager);
}

/**
 * Initialize the ioc container for a production environment
 */
export function initContainer(): void {
	setupProd();
	registerAll();
}
