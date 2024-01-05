import "reflect-metadata";

import {container} from "tsyringe";
import {CustomRulePathManager} from './lib/CustomRulePathManager';
import {DefaultRuleManager} from './lib/DefaultRuleManager';
import {JavascriptEslintEngine} from './lib/eslint/EslintEngine';
import {LWCEslintEngine} from './lib/eslint/EslintEngine';
import {TypescriptEslintEngine} from './lib/eslint/EslintEngine';
import {CustomEslintEngine} from './lib/eslint/CustomEslintEngine';
import {RetireJsEngine} from './lib/retire-js/RetireJsEngine';
import {SfgeDfaEngine} from './lib/sfge/SfgeDfaEngine';
import {SfgePathlessEngine} from './lib/sfge/SfgePathlessEngine';
import {AppExchangePmdEngine, CustomPmdEngine, PmdEngine} from './lib/pmd/PmdEngine';
import LocalCatalog from './lib/services/LocalCatalog';
import {Config} from './lib/util/Config';
import {Services} from './Constants';
import {CpdEngine} from "./lib/cpd/CpdEngine";

/**
 * Initialize the ioc container with singletons common to test and prod
 */
export function registerAll(): void {
	if (!container.isRegistered(Services.Config)) {

		// TODO: We should revisit each of these and ask ourselves which ones we actually need as registered singletons.
		container.registerSingleton(Services.Config, Config);
		container.registerSingleton(Services.RuleManager, DefaultRuleManager);
		container.registerSingleton(Services.RuleEngine, PmdEngine);
		container.registerSingleton(Services.RuleEngine, CustomPmdEngine);
		container.registerSingleton(Services.RuleEngine, AppExchangePmdEngine);
		container.registerSingleton(Services.RuleEngine, JavascriptEslintEngine);
		container.registerSingleton(Services.RuleEngine, LWCEslintEngine);
		container.registerSingleton(Services.RuleEngine, TypescriptEslintEngine);
		container.registerSingleton(Services.RuleEngine, CustomEslintEngine);
		container.registerSingleton(Services.RuleEngine, RetireJsEngine);
		container.registerSingleton(Services.RuleEngine, CpdEngine);
		container.registerSingleton(Services.RuleEngine, SfgeDfaEngine);
		container.registerSingleton(Services.RuleEngine, SfgePathlessEngine);
		container.registerSingleton(Services.RuleCatalog, LocalCatalog);
		container.registerSingleton(Services.RulePathManager, CustomRulePathManager);
	}
}

/**
 * Initialize the ioc container for a production environment
 */
export function initContainer(): void {
	registerAll();
}
