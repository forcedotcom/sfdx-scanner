import {Logger, SfdxError} from '@salesforce/core';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {RuleEngine} from '../services/RuleEngine';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {ENGINE} from '../../Constants';
import {StaticResourceHandler, StaticResourceType} from '../util/StaticResourceHandler';
import {FileHandler} from '../util/FileHandler';
import cspawn = require('cross-spawn');
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
	protected zipDstByZipSrc: Map<string, string> = new Map();

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

	isCustomConfigBased(): boolean {
		return false;
	}

	shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean {
		// If the engine was not filtered out, no reason to not run it
		return true;
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
			const cp = cspawn(RetireJsEngine.RETIRE_JS_PATH, invocation.args);

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

	protected async createTmpDirWithDuplicatedTargets(targets: RuleTarget[]): Promise<string> {
		// Create a temporary parent directory into which we'll transplant all of our target files.
		const tmpParent: string = await this.fh.tmpDirWithCleanup();

		// Iterate through all of our targets to generate alias information. Importantly, we will NOT be actually creating
		// any new directories or files at this point.
		for (const target of targets) {
			for (const originalPath of target.paths) {
				// At this point, we can't alias all types of files to the same extent. So we'll use different submethods
				// to handle them.
				const ext: string = path.extname(originalPath.toLowerCase());
				const srType = ext === '.resource' ? await this.srh.identifyStaticResourceType(originalPath) : null;
				if (ext === '.js' || srType === StaticResourceType.JS) {
					this.aliasJsFile(tmpParent, originalPath);
				} else if (ext === '.zip' || srType === StaticResourceType.ZIP) {
					this.aliasZipFile(tmpParent, originalPath);
				}
			}
		}

		// Now that we've generated aliases, we should create the directories first.
		await this.createAliasDirectories();

		// Once we've created all of the alias directories, we can symlink all of the JS files with no fear of race conditions.
		try {
			await this.duplicateJsFiles();
		} catch (e) {
			// Catch any error and give it a slightly more informative header.
			throw new SfdxError(`RetireJS: ${e.message || e}`);
		}
		// Finally, we can handle all of the ZIP files.
		await this.extractZips();

		// Everything has been duplicated, extracted, or whatever else we were supposed to do to it. We're done. Return
		// the parent directory that holds it all.
		return tmpParent;
	}

	protected aliasJsFile(tmpParent: string, originalPath: string): void {
		// We'll need to derive an aliased subdirectory to duplicate this file into, so we can prevent name collision.
		const aliasDir: string = this.deriveDirectoryAlias(tmpParent, originalPath);

		// Static Resource files need to be changed to `.js` files so RetireJS can see them. We'll also give them aliases
		// to make sure that they can't conflict with `.js` files in the same directory.
		const aliasFile: string = path.extname(originalPath) === '.resource' ? this.getNextFileAlias() : path.basename(originalPath);

		const fullAlias = path.join(aliasDir, aliasFile);

		// Map the original file by the alias we're using, so we can look back to it later.
		this.originalFilesByAlias.set(fullAlias, originalPath);
	}

	protected aliasZipFile(tmpParent: string, originalPath: string): void {
		// We'll need to derive an aliased subdirectory to duplicate this file into, so we can prevent name collision.
		const aliasDir: string = this.deriveDirectoryAlias(tmpParent, originalPath);

		// ZIPs require an additional layer of aliasing, since two ZIPs in the same directory could have similar contents.
		// We'll derive this layer of aliasing based on the name of the ZIP.
		const zipLayer = `${path.basename(originalPath, path.extname(originalPath))}-extracted`;
		this.zipDstByZipSrc.set(originalPath, path.join(aliasDir, zipLayer));
	}

	protected deriveDirectoryAlias(tmpParent: string, originalPath: string): string {
		// By converting the directory portion of each path into a unique alias, we can make sure that the alias paths
		// remain unique, and thereby avoid name collision.
		const originalDir = path.dirname(originalPath);
		if (!this.aliasDirsByOriginalDir.has(originalDir)) {
			this.aliasDirsByOriginalDir.set(originalDir, path.join(tmpParent, this.getNextDirAlias()));
		}
		return this.aliasDirsByOriginalDir.get(originalDir);
	}

	protected async createAliasDirectories(): Promise<void[]> {
		const dirCreationPromises: Promise<void>[] = [];
		for (const aliasDir of this.aliasDirsByOriginalDir.values()) {
			dirCreationPromises.push(this.fh.mkdirIfNotExists(aliasDir));
		}
		await Promise.all(dirCreationPromises);

		// ZIP extraction folders should be created only after all of the base alias directories have been created.
		const zipDestPromises: Promise<void>[] = [];
		for (const zipDst of this.zipDstByZipSrc.values()) {
			zipDestPromises.push(this.fh.mkdirIfNotExists(zipDst));
		}
		return Promise.all(zipDestPromises);
	}

	protected async duplicateJsFiles(): Promise<void[]> {
		const dupPromises: Promise<void>[] = [];
		for (const [alias, original] of this.originalFilesByAlias.entries()) {
			dupPromises.push(this.fh.duplicateFile(original, alias));
		}
		return Promise.all(dupPromises);
	}

	protected async extractZips(): Promise<void[]> {
		const zipExtractionPromises: Promise<void>[] = [];
		for (const [zipSrc, zipDst] of this.zipDstByZipSrc.entries()) {
			zipExtractionPromises.push(new Promise((res, rej) => {
				const zip = new StreamZip({
					file: zipSrc,
					storeEntries: true
				});

				zip.on('error', rej);

				zip.on('ready', () => {
					// Before we do the extraction, we want to map the aliased JS files within the ZIP back to the ZIP itself,
					// since the ZIP is the real original file.
					for (const {name} of Object.values(zip.entries())) {
						if (path.extname(name).toLowerCase() === '.js') {
							this.originalFilesByAlias.set(path.join(zipDst, name), zipSrc);
						}
					}
					// Passing null as the first parameter to this method causes it to extract the entire ZIP.
					zip.extract(null, zipDst, (err) => {
						if (err) {
							rej(err);
						} else {
							res();
						}
					});
				});
			}));
		}

		return Promise.all(zipExtractionPromises);
	}
}
