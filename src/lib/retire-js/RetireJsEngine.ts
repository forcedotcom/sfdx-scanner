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
// "catalog" out of RetireJS's normal behavior and its permutations.
const INSECURE_BUNDLED_DEPS = 'insecure-bundled-dependencies';
const retireJsCatalog: Catalog = {
	rules: [{
		engine: ENGINE.RETIRE_JS.valueOf(),
		sourcepackage: ENGINE.RETIRE_JS.valueOf(),
		// Give this rule an informative name, specific enough that we're able to supplement it with other rules later.
		name: INSECURE_BUNDLED_DEPS,
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

/**
 * The various permutations of RetireJS are each handled with separate rules, so we'll use this structure to associate
 * a particular invocation of RetireJS with a particular rule.
 */
type RetireJsInvocation = {
	args: string[];
	rule: string;
};

/**
 * These next few classes define the format of the JSON output by RetireJS.
 */
type RetireJsVulnerability = {
	severity: string;
};

type RetireJsResult = {
	version: string;
	component: string;
	detection?: string;
	vulnerabilities: RetireJsVulnerability[];
};

type RetireJsData = {
	file: string;
	results: RetireJsResult[];
};

type RetireJsOutput = {
	version: string;
	data: RetireJsData[];
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
	protected aliasesByOriginalPath: Map<string, string> = new Map();
	protected originalPathsByAlias: Map<string, string> = new Map();

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

	// eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/require-await, no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		// RetireJS doesn't accept individual files. It only accepts directories. So we need to resolve all of the files
		// we were given into a directory that we can pass into RetireJS.
		const tmpDir = await this.createTmpDirWithDuplicatedTargets(target);
		const invocationArray: RetireJsInvocation[] = this.buildCliInvocations(rules, tmpDir);

		const retireJsPromises: Promise<RuleResult[]>[] = [];
		for (const invocation of invocationArray) {
			retireJsPromises.push(this.executeRetireJs(invocation));
		}

		// We can combine the results into a single array using .reduce() instead of the more verbose for-loop.
		return (await Promise.all(retireJsPromises)).reduce((all, next) => [...all, ...next], []);
	}

	private buildCliInvocations(rules: Rule[], target: string): RetireJsInvocation[] {
		const invocationArray: RetireJsInvocation[] = [];
		for (const rule of rules) {
			switch (rule.name) {
				case INSECURE_BUNDLED_DEPS:
					// This rule is looking for files that contain insecure libraries, e.g. .min.js or similar.
					// So we use --js and --jspath to make retire-js only examine JS files and skip node modules.
					invocationArray.push({
						args: ['--js', '--jspath', target, '--outputformat', 'json'],
						rule: rule.name
					});
					break;
				default:
					throw new SfdxError(`Unexpected retire-js rule: ${rule.name}`);
			}
		}
		return invocationArray;
	}

	private async executeRetireJs(invocation: RetireJsInvocation): Promise<RuleResult[]> {
		return new Promise<RuleResult[]>((res, rej) => {
			const cp = childProcess.spawn(RetireJsEngine.RETIRE_JS_PATH, invocation.args);

			// We only care about StdErr, since that's where the vulnerability entries will be logged to.
			let stderr = '';

			// When data is passed back up to us, pop it onto the appropriate string.
			cp.stderr.on('data', data => {
				stderr += data;
			});

			cp.on('exit', code => {
				this.logger.trace(`executeRetireJs has received exit code ${code}`);
				if (code === 0) {
					// If RetireJS exits with code 0, then it ran successfully and found no vulnerabilities. We can resolve
					// to an empty array.
					res([]);
				} else if (code === 13) {
					// If RetireJS exits with code 13, then it ran successfully, but found at least one vulnerability.
					// Convert the output into RuleResult objects and resolve to that.
					res(this.processOutput(stderr, invocation.rule));
				} else {
					// If RetireJS exits with any other code, then it means something went wrong. We'll reject with stderr
					// for the ease of upstream error handling.
					rej(stderr);
				}
			});
		});
	}

	protected processOutput(cmdOutput: string, ruleName: string): RuleResult[] {
		// The output from the CLI should be a valid JSON.
		let outputJson = null;
		try {
			outputJson = JSON.parse(cmdOutput);
		} catch (e) {
			throw new SfdxError(`Could not parse RetireJS output: ${e.message || e}`);
		}
		if (RetireJsEngine.validateRetireJsOutput(outputJson)) {
			const ruleResults: RuleResult[] = [];
			for (const data of outputJson.data) {
				// First, we need to de-alias the file.
				const aliasDir = path.dirname(data.file);
				const originalPath = path.join(this.originalPathsByAlias.get(aliasDir), path.basename(data.file));

				// Each entry in the `data` array yields a single RuleResult.
				const ruleResult: RuleResult = {
					engine: ENGINE.RETIRE_JS.valueOf(),
					fileName: originalPath,
					violations: []
				};

				// Each `result` entry generates one RuleViolation.
				for (const result of data.results) {
					ruleResult.violations.push({
						line: 1,
						column: 1,
						ruleName: ruleName,
						// Sweep the vulnerabilities to find the most severe one.
						severity: result.vulnerabilities
							.map(vuln => this.retireSevToScannerSev(vuln.severity))
							.reduce((min, sev) => min > sev ? sev: min, 9000),
						message: `${result.component} v${result.version} is insecure. Please upgrade to latest version.`,
						category: 'Insecure Dependencies'
					});
				}
				ruleResults.push(ruleResult);
			}
			return ruleResults;
		} else {
			// It's theoretically impossible to reach this point, because it means that RetireJS finished successfully
			// but returned something we don't recognize.
			throw new SfdxError(`retire-js output did not match expected structure`);
		}
	}

	private retireSevToScannerSev(sev: string): number {
		switch (sev.toLowerCase()) {
			case 'low':
				return 3;
			case 'medium':
				return 2;
			case 'high':
				return 1;
			default:
				throw new SfdxError(`retire-js encountered unexpected severity value of ${sev}.`);
		}
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private static validateRetireJsOutput(parsedOutput: any): parsedOutput is RetireJsOutput {
		return (parsedOutput as RetireJsOutput).version != undefined;
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


	protected async createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
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
					// NOTE: We recognize that there are potential performance concerns for copying a possibly-large number
					// of possibly large files. If those issues become a problem, we should consider alternatives such as
					// symlink() (if RetireJS supports it) or even a different implementation.
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
