import {Logger} from '@salesforce/core';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {RuleEngine} from '../services/RuleEngine';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {ENGINE} from '../../Constants';
import {FileHandler} from '../util/FileHandler';
//import childProcess = require('child_process');
import path = require('path');
import fs = require('fs');

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
	private static RETIRE_JS_PATH: string = require.resolve('retire').replace(path.join('lib', 'retire.js'), path.join('bin', 'retire'));

	private pathAliasMap: Map<string, string> = new Map();

	private logger: Logger;
	private config: Config;
	private initialized: boolean;

	public getName(): string {
		return RetireJsEngine.ENGINE_NAME;
	}

	public async getTargetPatterns(): Promise<string[]> {
		return this.config.getTargetPatterns(ENGINE.RETIRE_JS);
	}

	public getCatalog(): Promise<Catalog> {
		return Promise.resolve(retireJsCatalog);
	}

	// TODO: We need to actually implement this method.
	// eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/require-await, no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		console.log(`Identified RetireJS exe path as ${RetireJsEngine.RETIRE_JS_PATH}`);
		console.log(`Targets are: ${JSON.stringify(target)}`);

		// RetireJS doesn't accept individual files. It only accepts directories. So we need to resolve all of the files
		// we were given into a directory that we can pass into RetireJS.
		const tmpDir = await this.createTmpDirWithDuplicatedTargets(target);
		console.log(`Created parent directory ${tmpDir}`);
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

	private async createIntermediateDir(dir): Promise<void> {
		return new Promise((res, rej) => {
			fs.mkdir(dir, (err) => {
				if (err) {
					rej(err);
				} else {
					res();
				}
			});
		});
	}

	private async createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<any> {
		// Create a temporary parent directory into which we'll transplant all of our target files.
		const tmpParent: string = await new FileHandler().tmpDirWithCleanup();
		const fileCopyPromises: Promise<void>[] = [];

		// We want to duplicate all of the targeted files into our temporary directory.
		let targetCount = 0;
		for (const target of targets) {
			for (const p of target.paths) {
				// We want to make sure that every duplicate file has a unique path to avoid collision. The target paths
				// are all absolute paths, and you can't just slam absolute paths together. So we need to get creative.
				// We'll associate the directory portion of each path with a unique alias...
				const dirPrefix = path.dirname(p);
				// ...Then use the alias to create a directory below the temporary parent, unless we've already done so.
				if (!this.pathAliasMap.has(dirPrefix)) {
					const dirAlias = `TMP_${targetCount++}`;
					this.pathAliasMap.set(dirPrefix, dirAlias);
					await this.createIntermediateDir(path.join(tmpParent, dirAlias));
				}
				// Then we'll copy the original target file to the child directory, thereby preserving path uniqueness.
				fileCopyPromises.push(new Promise((res, rej) => {
					fs.copyFile(p, path.join(tmpParent, this.pathAliasMap.get(dirPrefix), path.basename(p)), (err) => {
						if (err) {
							rej(err);
						} else {
							res();
						}
					});
				}));
			}
		}

		await Promise.all(fileCopyPromises);
		// If we successfully created all of the duplicate files, then we can return the temporary parent directory.
		return tmpParent;
	}
}
