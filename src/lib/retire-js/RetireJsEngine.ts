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
	info: string[];
	below?: string;
	atOrAbove?: string;
	severity: string;
	identifiers: {
		summary: string;
	};
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
		return (await Promise.all(retireJsPromises)).reduce((acc, r) => [...acc, ...r], []);
	}

	private buildCliInvocations(rules: Rule[], target: string): RetireJsInvocation[] {
		const invocationArray: RetireJsInvocation[] = [];
		for (const rule of rules) {
			switch (rule.name) {
				case 'insecure-bundled-dependencies':
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

	private processOutput(cmdOutput: string, ruleName: string): RuleResult[] {
		// The output from the CLI should be a valid JSON.
		const outputJson = JSON.parse(cmdOutput);
		if (RetireJsEngine.validateRetireJsOutput(outputJson)) {
			const ruleResults: RuleResult[] = [];
			for (const data of outputJson.data) {
				// First, we need to de-alias the file.
				const aliasDir = path.dirname(data.file);
				const originalPath = path.join(this.originalPathsByAlias.get(aliasDir), path.basename(data.file));

				// Each `data` entry yields a single RuleResult.
				const ruleResult: RuleResult = {
					engine: ENGINE.RETIRE_JS.valueOf(),
					fileName: originalPath,
					violations: []
				};
				// Each `result` entry uses its own `vulnerabilities` array to generate one or more RuleViolations for
				// the parent RuleResult.
				for (const result of data.results) {
					for (const vuln of result.vulnerabilities) {
						const msg = this.generateViolationMessage(vuln, result.version);
						ruleResult.violations.push({
							line: 1,
							column: 1,
							ruleName: ruleName,
							severity: this.retireSevToScannerSev(vuln.severity),
							message: msg,
							category: 'Insecure Dependencies'
						});
					}
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

	/**
	 * Generates a violation message of the format "[Impacted versions]: [Summary of issue]".
	 * @param vuln
	 */
	private generateViolationMessage(vuln: RetireJsVulnerability, currentVersion: string): string {
		// Our message needs to include information about which versions are free of the specific vulnerability.
		let secureVersion = null;
		if (vuln.below) {
			// The `below` property is the version in which the described vulnerability was PATCHED. Users can upgrade to
			// that version to resolve the vulnerability. We want to encourage upgrading, so we'll use the `below` property
			// if we can.
			secureVersion = `>=v${vuln.below}`;
		} else if (vuln.atOrAbove) {
			// The `atOrAbove` property is the version in which the described vulnerability was INTRODUCED. Users can
			// downgrade to a version below that version to resolve the vulnerability. We want to discourage downgrading,
			// so we'll only provide this information if we couldn't provide a `below` property.
			secureVersion = `<v${vuln.atOrAbove}`;
		} else {
			// If there's neither an `atOrAbove` or `below` property, then the vulnerability is inescapable. Womp womp.
			secureVersion = 'None';
		}

		return `Found: v${currentVersion}. Fixed in: ${secureVersion}. Summary: ${vuln.identifiers.summary}`;
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
