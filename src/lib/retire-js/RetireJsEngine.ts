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
	// We'll be generating aliases for directories, and we want a way to make sure their names are all unique.
	private static NEXT_ALIAS_IDX = 0;

	private dirAliasMap: Map<string, string> = new Map();

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

	private async createPathAlias(tmpParent: string, basePath: string): Promise<string> {
		// We want to make sure each duplicate file's path is unique, to avoid collisions. If we haven't already created
		// a unique alias for this path's directory, we need to do that now.
		const baseDir = path.dirname(basePath);
		if (!this.dirAliasMap.has(baseDir)) {
			const aliasDir = `TMP_${RetireJsEngine.NEXT_ALIAS_IDX++}`;
			this.dirAliasMap.set(baseDir, aliasDir);
			const absoluteAlias = path.join(tmpParent, aliasDir);
			return new Promise((res, rej) => {
				// Create a directory beneath the temporary parent directory with the name of the alias.
				fs.mkdir(absoluteAlias, (err) => {
					if (err) {
						rej(err);
					} else {
						res(absoluteAlias);
					}
				});
			});
		}
		return path.join(tmpParent, this.dirAliasMap.get(baseDir));
	}


	private async createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		// Create a temporary parent directory into which we'll transplant all of our target files.
		const tmpParent: string = await new FileHandler().tmpDirWithCleanup();
		const fileCopyPromises: Promise<void>[] = [];

		// We want to duplicate all of the targeted files into our temporary directory.
		for (const target of targets) {
			for (const p of target.paths) {
				// Create a unique alias directory into which we should duplicate all of these files.
				// We'll use an `await` for this line because this operation needs to be atomic.
				const pathAlias: string = await this.createPathAlias(tmpParent, p);

				// Once we've got a path, ew can copy the original target file to the child directory, thereby preserving
				// path uniqueness. No race condition exists, so no need for an `await`.
				fileCopyPromises.push(new Promise((res, rej) => {
					fs.copyFile(p, path.join(pathAlias, path.basename(p)), (err) => {
						if (err) {
							rej(err);
						} else {
							res();
						}
					});
				}));
			}
		}

		// Wait for all of the files to be copied.
		await Promise.all(fileCopyPromises);
		// If we successfully created all of the duplicate files, then we can return the temporary parent directory.
		return tmpParent;
	}


}
