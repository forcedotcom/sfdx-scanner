import {Logger, SfdxError} from '@salesforce/core';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {RuleEngine} from '../services/RuleEngine';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {ENGINE} from '../../Constants';
import {FileHandler} from '../util/FileHandler';
import childProcess = require('child_process');
import path = require('path');
import fs = require('fs');

// Unlike the other engines we use, RetireJS doesn't really have "rules" per se. So we sorta have to synthesize a
// "catalog" out of RetireJS's normal behavior.
const retireJsCatalog: Catalog = {
	rules: [{
		engine: ENGINE.RETIRE_JS.valueOf(),
		sourcepackage: ENGINE.RETIRE_JS.valueOf(),
		// Give this rule an informative name, specific enough that we're able to supplement it with other rules later.
		name: 'insecure-bundled-dependencies',
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

type RetireJsResult = {
	version: string,
	data: {
		file: string,
		results: {
			version: string,
			component: string,
			detection: string,
			vulnerabilities: {
				info: string[],
				below?: string,
				atOrAbove?: string,
				severity: string
			}[]
		}[]
	}[]
};

export class RetireJsEngine implements RuleEngine {
	public static ENGINE_ENUM: ENGINE = ENGINE.RETIRE_JS;
	public static ENGINE_NAME: string = ENGINE.RETIRE_JS.valueOf();
	// RetireJS isn't really built to be invoked programmatically, so we'll need to invoke it as a CLI command. However, we
	// can't assume that they have the module installed globally. So what we're doing here is identifying the path to the
	// locally-scoped `retire` module, and then using that to derive a path to the CLI-executable JS script.
	private static RETIRE_JS_PATH: string = require.resolve('retire').replace(path.join('lib', 'retire.js'), path.join('bin', 'retire'));

	// When we're duplicating files, we want to make sure that each duplicate file's path is unique. We'll do that by
	// generating aliases associated with the directory portion of each original file's path. We need an incrementing
	// index to generate unique aliases, and we need maps from both the aliases to the original and vice versa.
	private static NEXT_ALIAS_IDX = 0;
	private aliasesByOriginalPath: Map<string, string> = new Map();
	private originalPathsByAlias: Map<string, string> = new Map();

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
		const argArrays: string[][] = this.buildCliCommands(rules, tmpDir);

		const retireJsPromises: Promise<any>[] = [];
		for (const argArray of argArrays) {
			retireJsPromises.push(this.executeRetireJs(argArray));
		}
		const res: RuleResult[] = await Promise.all(retireJsPromises);
		console.log('results: ' + JSON.stringify(res));
		return [];
	}

	private buildCliCommands(rules: Rule[], target: string): string[][] {
		const argsArrays: string[][] = [];
		for (const rule of rules) {
			switch (rule.name) {
				case 'insecure-bundled-dependencies':
					// This rule is looking for files that contain insecure libraries, e.g. .min.js or similar.
					// So we use --js and --jspath to make retire-js only examine JS files and skip node modules.
					argsArrays.push(['--js', '--jspath', target, '--outputformat', 'json']);
					break;
				default:
					throw new SfdxError(`Unexpected retire-js rule: ${rule.name}`);
			}
		}
		return argsArrays;
	}

	private async executeRetireJs(args: string[]): Promise<RuleResult[]> {
		return new Promise<any>((res, rej) => {
			const cp = childProcess.spawn(RetireJsEngine.RETIRE_JS_PATH, args);

			let stdout = '';
			let stderr = '';

			// When data is passed back up to us, pop it onto the appropriate string.
			cp.stdout.on('data', data => {
				stdout += data;
			});
			cp.stderr.on('data', data => {
				stderr += data;
			});

			cp.on('exit', code => {
				this.logger.trace(`monitorChildProcess has received exit code ${code}`);
				if (code === 0) {
					// If RetireJS exits with code 0, then it ran successfully and found no vulnerabilities. We can resolve
					// to an empty array.
					console.log('no insecurities');
					res([]);
				} else if (code === 13) {
					// If RetireJS exits with code 13, then it ran successfully, but found at least one vulnerability.
					// Convert the output into RuleResult objects and resolve to that.
					console.log('yes insecurities');
					console.log('out lens? ' + stdout.length + ', ' + stderr.length);
					res(this.processOutput(stderr));
				} else {
					// If RetireJS exits with any other code, then it means something went wrong. We'll reject with stderr
					// for the ease of upstream error handling.
					console.log('some weird errors');
					rej(stderr);
				}
			});
		});
	}

	private processOutput(cmdOutput: string): RuleResult[] {
		// The output from the CLI should be a valid JSON.
		const outputJson = JSON.parse(cmdOutput);
		if (RetireJsEngine.validatePotentialResult(outputJson)) {
			for (const data of outputJson.data) {
				// First, we need to de-alias the file.
				const aliasDir = path.dirname(data.file);
				const originalPath = path.join(this.originalPathsByAlias.get(aliasDir), path.basename(data.file));

				console.log(`Followed alias back to file ${originalPath}`);
			}
		} else {
			// TODO: Throw some kind of error here.
		}
		return [];

	}

	private static validatePotentialResult(parsedOutput: any): parsedOutput is RetireJsResult {
		return (parsedOutput as RetireJsResult).version != undefined;
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
		if (!this.aliasesByOriginalPath.has(baseDir)) {
			const aliasDir = path.join(tmpParent, `TMP_${RetireJsEngine.NEXT_ALIAS_IDX++}`);
			this.aliasesByOriginalPath.set(baseDir, aliasDir);
			this.originalPathsByAlias.set(aliasDir, baseDir);
			return new Promise((res, rej) => {
				// Create a directory beneath the temporary parent directory with the name of the alias.
				fs.mkdir(aliasDir, (err) => {
					if (err) {
						rej(err);
					} else {
						res(aliasDir);
					}
				});
			});
		}
		return this.aliasesByOriginalPath.get(baseDir);
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
