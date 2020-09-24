import {Logger} from '@salesforce/core';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {RuleEngine} from '../services/RuleEngine';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {ENGINE} from '../../Constants';
//import childProcess = require('child_process');
//import path = require('path');

// Unlike the other engines we use, RetireJS doesn't really have "rules" per se. So we sorta have to synthesize a
// "catalog" out of RetireJS's normal behavior.
const retireJsCatalog: Catalog = {
	rules: [{
		engine: ENGINE.RETIRE_JS.valueOf(),
		sourcepackage: ENGINE.RETIRE_JS.valueOf(),
		// Give this rule an informative name, specific enough that we're able to supplement it with other rules later.
		name: ' insecure-bundled-dependencies',
		description: 'Identify bundled libraries/modules with known vulnerabilities.',
		categories: ['Insecure Dependencies'],
		rulesets: [],
		languages: ['javascript'],
		defaultEnabled: true
	}],
	categories: [{
		engine: ENGINE.RETIRE_JS.valueOf(),
		name: 'Insecure Dependencies',
		paths: []
	}],
	rulesets: []
};

export class RetireJsEngine implements RuleEngine {
	public static ENGINE_ENUM: ENGINE = ENGINE.RETIRE_JS;
	public static ENGINE_NAME: string = ENGINE.RETIRE_JS.valueOf();
	// RetireJS isn't really built to be invoked programmatically, so we'll need to invoke it as a CLI command. However, we
	// can't assume that they have the module installed globally. So what we're doing here is identifying the path to the
	// locally-scoped `retire` module, and then using that to derive a path to the CLI-executable JS script.
	//private static RETIRE_JS_PATH: string = require.resolve('retire').replace(path.join('lib', 'retire.js'), path.join('bin', 'retire'));

	private logger: Logger;
	private config: Config;
	private initialized: boolean;

	public getName(): string {
		return RetireJsEngine.ENGINE_NAME;
	}

	public async getTargetPatterns(): Promise<string[]> {
		return this.config.getTargetPatterns(ENGINE.RETIRE_JS);
	}

	public async getCatalog(): Promise<Catalog> {
		return retireJsCatalog;
	}

	// TODO: We need to actually implement this method.
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		return [];
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();
		this.initialized = true;
	}

	public matchPath(path: string): boolean {
		this.logger.trace(`Engine RetireJS does not support custom rules: ${path}`);
		return false;
	}

	public async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(RetireJsEngine.ENGINE_ENUM);
	}
}
