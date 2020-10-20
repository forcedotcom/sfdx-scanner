import {Logger, SfdxError} from '@salesforce/core';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {RuleEngine} from '../services/RuleEngine';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {ENGINE} from '../../Constants';
import {StaticResourceHandler, StaticResourceType} from '../util/StaticResourceHandler';
import {FileHandler} from '../util/FileHandler';
import childProcess = require('child_process');
import path = require('path');
import StreamZip = require('node-stream-zip');

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
	// generating aliases associated with portions of the original path. We need incrementing indexes to generate unique
	// aliases, and we need maps to track relationships between aliases and originals.
	private static NEXT_TMPDIR_IDX = 0;
	private static NEXT_TMPFILE_IDX = 0;

	protected aliasDirsByOriginalDir: Map<string, string> = new Map();
	protected originalFilesByAlias: Map<string, string> = new Map();

	private logger: Logger;
	private fh: FileHandler;
	private srh: StaticResourceHandler;
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
			const ruleResultsByFile: Map<string, RuleResult> = new Map();
			for (const data of outputJson.data) {
				// First, we need to de-alias the file using the descriptor.
				const originalFile = this.originalFilesByAlias.get(data.file);

				// Each entry in the `data` array yields a single RuleResult. If we already have a RuleResult associated
				// with this original file (happens when a ZIP contains multiple insecure files), we'll keep using that.
				// Otherwise, we need to build a new one.
				const ruleResult: RuleResult = ruleResultsByFile.has(originalFile)
					? ruleResultsByFile.get(originalFile)
					: {
						engine: ENGINE.RETIRE_JS.valueOf(),
						fileName: originalFile,
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

				// Map the result object so we can continue using it if need be.
				ruleResultsByFile.set(originalFile, ruleResult);
			}
			// Once we exit the loop, we can return all of the results in our Map.
			return Array.from(ruleResultsByFile.values());
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
		const castParsedOutput = parsedOutput as RetireJsOutput;
		return castParsedOutput.version != undefined && castParsedOutput.data != undefined;
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.fh = new FileHandler();
		this.srh = new StaticResourceHandler();
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

	private getNextDirAlias(): string {
		return `TMPDIR_${RetireJsEngine.NEXT_TMPDIR_IDX++}`;
	}

	private getNextFileAlias(): string {
		return `TMPFILE_${RetireJsEngine.NEXT_TMPFILE_IDX++}.js`;
	}

	private async createAliasDirectory(tmpParent: string, originalPath: string): Promise<string> {
		// By converting the directory portion of each path into a unique alias, we can make sure that the alias paths
		// remain unique (and thereby avoid name collision) without renaming the files themselves.
		const originalDir = path.dirname(originalPath);
		if (!this.aliasDirsByOriginalDir.has(originalDir)) {
			const aliasDir = path.join(tmpParent, this.getNextDirAlias());
			this.aliasDirsByOriginalDir.set(originalDir, aliasDir);
			await this.fh.mkdirIfNotExists(aliasDir);
		}
		return this.aliasDirsByOriginalDir.get(originalDir);
	}



	private async duplicateStaticResource(tmpParent: string, fileName: string): Promise<void> {
		const resourceType: StaticResourceType = await this.srh.identifyStaticResourceType(fileName);

		switch (resourceType) {
			case StaticResourceType.JS:
				return this.duplicateJsFile(tmpParent, fileName);
			case StaticResourceType.ZIP:
				return this.duplicateZipFile(tmpParent, fileName);
			case StaticResourceType.OTHER:
				this.logger.trace(`Static Resource ${fileName} is neither JS nor ZIP. It is being skipped.`);
				return;
			case null:
				this.logger.trace(`File ${fileName} is not actually a Static Resource. It is being skipped.`);
				return;
			default:
				throw new SfdxError('Should be impossible to reach');
		}
	}

	private async duplicateJsFile(tmpParent: string, fileName: string): Promise<void> {
		// We'll need to derive an aliased subdirectory to duplicate this file into, so we can prevent name collision.
		// We'll use an `await` for this line because it's important for this operation to be atomic.
		const aliasDir: string = await this.createAliasDirectory(tmpParent, fileName);

		// Static Resource files need to be changed to `.js` files so RetireJS can see them. We'll also give them aliases
		// to make sure that they can't conflict with `.js` files in the same directory.
		const aliasFile: string = path.extname(fileName) === '.resource' ? this.getNextFileAlias() : path.basename(fileName);

		const fullAlias = path.join(aliasDir, aliasFile);

		// Map the original file by the alias we're using, so we can look back to it later.
		this.originalFilesByAlias.set(fullAlias, fileName);

		// Now we can duplicate the file. No race condition exists, so no need for an `await`.
		return this.fh.symlinkFile(fileName, fullAlias);
	}

	private async duplicateZipFile(tmpParent: string, zipFileName: string): Promise<void> {
		// We'll need to derive an aliased subdirectory to unpack this ZIP into, so we can prevent name collision.
		// We'll use an `await` for this line because it's important for this operation to be atomic.
		let aliasDir: string = await this.createAliasDirectory(tmpParent, zipFileName);
		// It's possible for two ZIPs in the same directory to contain a file with the same name. To prevent name collision
		// in this case, we'll turn the name of the zip itself into one additional level of directory aliasing.
		const zipLayer = `${path.basename(zipFileName, path.extname(zipFileName))}-extracted`;
		aliasDir = path.join(aliasDir, zipLayer);
		// This operation doesn't need to be blocking, so we'll use a Promise chain instead of `await`. Slightly messier,
		// but should keep our performance decent.
		return new Promise((res, rej) => {
			this.fh.mkdirIfNotExists(aliasDir)
				.then(() => {
					const zip = new StreamZip({
						file: zipFileName,
						storeEntries: true
					});

					zip.on('error', rej);

					zip.on('ready', () => {
						// Before we do the extraction, we want to map the contained JS-type files by their full alias.
						for (const {name} of Object.values(zip.entries())) {
							if (path.extname(name).toLowerCase() === '.js') {
								this.originalFilesByAlias.set(path.join(aliasDir, name), zipFileName);
							}
						}
						// The null first parameter causes us to extract the entire ZIP.
						zip.extract(null, aliasDir, (err) => {
							if (err) {
								rej(err);
							} else {
								res();
							}
						});
					});
				});
		});
	}


	protected async createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		// Create a temporary parent directory into which we'll transplant all of our target files.
		const tmpParent: string = await this.fh.tmpDirWithCleanup();
		const fileCopyPromises: Promise<void>[] = [];

		// We want to duplicate all of the targeted files into our temporary directory.
		for (const target of targets) {
			for (const p of target.paths) {
				const ext: string = path.extname(p).toLowerCase();
				if (ext === '.resource') {
					fileCopyPromises.push(this.duplicateStaticResource(tmpParent, p));
				} else if (ext === '.zip') {
					fileCopyPromises.push(this.duplicateZipFile(tmpParent, p));
				} else if (ext === '.js') {
					fileCopyPromises.push(this.duplicateJsFile(tmpParent, p));
				}
			}
		}

		// Wait for all of the files to be copied.
		await Promise.all(fileCopyPromises);
		// If we successfully created all of the duplicate files, then we can return the temporary parent directory.
		return tmpParent;
	}


}
