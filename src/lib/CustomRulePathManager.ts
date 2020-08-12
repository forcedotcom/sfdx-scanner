import path = require('path');
import {Logger, SfdxError} from '@salesforce/core';
import {injectable, injectAll} from 'tsyringe';
import {CUSTOM_PATHS_FILE} from '../Constants';
import {RulePathManager} from './RulePathManager';
import {RuleEngine} from './services/RuleEngine';
import {FileHandler} from './util/FileHandler';
import * as PrettyPrinter from './util/PrettyPrinter';
import { Controller } from '../ioc.config';

export type RulePathEntry = Map<string, Set<string>>;
export type RulePathMap = Map<string, RulePathEntry>;

const EMPTY_JSON_FILE = '{}';

@injectable()
export class CustomRulePathManager implements RulePathManager {
	private initialized: boolean;
	private logger!: Logger;
	private engines: RuleEngine[];
	private pathsByLanguageByEngine: RulePathMap;
	private fileHandler: FileHandler;
	private sfdxScannerPath: string;

	constructor(@injectAll("RuleEngine") engines?: RuleEngine[]) {
		this.engines = engines;
	}

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('CustomRulePathManager');

		for (const engine of this.engines) {
			await engine.init();
		}

		this.pathsByLanguageByEngine = new Map();
		this.fileHandler = new FileHandler();
		this.sfdxScannerPath = Controller.getSfdxScannerPath();

		this.logger.trace(`Initializing CustomRulePathManager.`);
		// Read from the JSON and use it to populate the map.
		let data = null;
		try {
			data = await this.readRulePathFile();
		} catch (e) {
			// An ENOENT error is fine, because it just means the file doesn't exist yet. We'll respond by spoofing a JSON with
			// no information in it.
			if (e.code === 'ENOENT') {
				this.logger.trace(`CustomRulePath file does not exist yet. In the process of creating a new file.`);
				data = EMPTY_JSON_FILE;
			} else {
				//  Any other error needs to be rethrown, and since it could be arcane or weird, we'll also prepend it with a
				//  header so it's clear where it came from.
				throw SfdxError.create('@salesforce/sfdx-scanner', 'add', 'errors.readCustomRulePathFileFailed', [e.message]);
			}
		}
		// If file existed but was empty, replace the whitespace/blank with empty JSON
		if ('' === data.trim()) {
			this.logger.trace(`CustomRulePath file existed, but was empty.`);
			data = EMPTY_JSON_FILE;
		}

		// Now that we've got the file contents, let's turn it into a JSON.
		const json = JSON.parse(data);
		this.pathsByLanguageByEngine = CustomRulePathManager.convertJsonDataToMap(json);
		this.logger.trace(`Initialized CustomRulePathManager. pathsByLanguageByEngine: ${PrettyPrinter.stringifyMapOfMaps(this.pathsByLanguageByEngine)}`);

		this.initialized = true;
	}

	public async addPathsForLanguage(language: string, paths: string[]): Promise<string[]> {
		this.logger.trace(`About to add paths[${paths}] for language ${language}`);
		const classpathEntries = await this.expandPaths(paths);
		// Identify the engine for each path and put them in the appropriate map and inner map.
		classpathEntries.forEach((entry) => {
			const engine = this.determineEngineForPath(entry);
			if (!this.hasPathsForEngine(engine.getName())) {
				this.logger.trace(`Creating new entry for engine ${engine.getName()}`);
				this.addPathsForEngine(engine.getName());
			}
			if (!this.getPathsByEngine(engine.getName()).has(language)) {
				this.logger.trace(`Creating new entry for language ${language} in engine ${engine.getName()}`);
				this.getPathsByEngine(engine.getName()).set(language, new Set([entry]));
			} else {
				this.getPathsByEngine(engine.getName()).get(language).add(entry);
			}
		});
		// Now, write the changes to the file.
		await this.saveCustomClasspaths();
		return classpathEntries;
	}

	private hasPathsForEngine(name: string): boolean {
		return this.pathsByLanguageByEngine.has(name);
	}

	private addPathsForEngine(name: string): void {
		this.pathsByLanguageByEngine.set(name, new Map());
	}

	private getPathsByEngine(name: string): RulePathEntry {
		return this.pathsByLanguageByEngine.get(name);
	}

	public getAllPaths(): string[] {
		// We'll combine every entry set for every language in every engine into a single array. We don't care about
		// uniqueness right now.
		let rawResults = [];

		this.engines.forEach((engine) => {
			if (this.hasPathsForEngine(engine.getName())) {
				const pathsByLanguage = this.getPathsByEngine(engine.getName());
				for (const pathSet of pathsByLanguage.values()) {
					rawResults = [...rawResults, ...Array.from(pathSet)];
				}
			}
		});
		return [...new Set(rawResults)];
	}

	public async getMatchingPaths(paths: string[]): Promise<string[]> {
		this.logger.trace(`Returning paths that match patterns [${paths}]`);

		// Expand the patterns into actual paths. E.g., expand directories into the rule objects they contain, etc.
		const expandedPaths = await this.expandPaths(paths);

		// Now that we've got the possible paths, we need to see which ones are actually present.
		return expandedPaths.filter((p) => {
			// Determine the engine associated with this path.
			const engine = this.determineEngineForPath(p).getName();

			// If there's anything mapped for that engine, check whether this path is mapped to any of the languages
			// under that engine.
			if (this.hasPathsForEngine(engine)) {
				const pathsByLanguage = this.getPathsByEngine(engine);
				const matchedPath = Array.from(pathsByLanguage.values()).some(pathSet => pathSet.has(p));
				return matchedPath;
			}
			return false;
		});
	}

	public async removePaths(paths: string[]): Promise<string[]> {
		this.logger.trace(`Removing paths [${paths}]`);

		// Expand the patterns into actual paths that we can delete.
		const expandedPaths = await this.expandPaths(paths);
		// For logging and display purposes, we'll want to track the paths that we actually delete.
		const deletedPaths = [];

		expandedPaths.forEach((p) => {
			// Determine the engine associated with the provided path.
			const engine = this.determineEngineForPath(p).getName();
			// If we have custom rules associated with that engine, attempt to delete the path from any languages on that
			// engine.
			if (this.hasPathsForEngine(engine)) {
				const pathsByLanguage = this.getPathsByEngine(engine);
				Array.from(pathsByLanguage.values()).forEach((pathSet) => {
					if (pathSet.delete(p)) {
						deletedPaths.push(p);
					}
				});
			}
		});
		// Write the changes to the file.
		await this.saveCustomClasspaths();
		return deletedPaths;
	}

	public getRulePathEntries(engine: string): Map<string, Set<string>> {
		if (!this.hasPathsForEngine(engine)) {
			this.logger.trace(`CustomRulePath does not have entries for engine ${engine}`);
			return new Map();
		}

		return this.getPathsByEngine(engine);
	}

	private async saveCustomClasspaths(): Promise<void> {
		try {
			const fileContent = JSON.stringify(this.convertMapToJson(), null, 4);
			this.logger.trace(`Writing file content to CustomRulePath file [${this.getRulePathFile()}]: ${fileContent}`);
			await this.fileHandler.mkdirIfNotExists(this.sfdxScannerPath);
			await this.fileHandler.writeFile(this.getRulePathFile(), fileContent);
		} catch (e) {
			// If the write failed, the error might be arcane or confusing, so we'll want to prepend the error with a header
			// so it's at least obvious what failed, if not how or why.
			throw SfdxError.create('@salesforce/sfdx-scanner', 'add', 'errors.writeCustomRulePathFileFailed', [e.message]);
		}
	}

	private static convertJsonDataToMap(json): RulePathMap {
		const map = new Map();
		for (const key of Object.keys(json)) {
			const engine = key;
			const val = json[key];
			const innerMap = new Map();
			for (const lang of Object.keys(val)) {
				innerMap.set(lang, new Set(val[lang]));
			}
			map.set(engine, innerMap);
		}
		return map;
	}

	private determineEngineForPath(path: string): RuleEngine {
		return this.engines.find(e => e.matchPath(path));
	}

	private convertMapToJson(): object {
		const json = {};
		this.pathsByLanguageByEngine.forEach((pathsByLang, engine) => {
			const innerObj = {};
			pathsByLang.forEach((paths, lang) => {
				innerObj[lang] = Array.from(paths);
			});
			json[engine.toString()] = innerObj;
		});
		return json;
	}

	private getFileName(): string {
		// We must allow for env variables to override the default catalog name. This must be recomputed in case those variables
		// have different values in different test runs.
		return process.env.CUSTOM_PATHS_FILE || CUSTOM_PATHS_FILE;
	}

	private getRulePathFile(): string {
		return path.join(this.sfdxScannerPath, this.getFileName());
	}

	public async readRulePathFile(): Promise<string> {
		const rulePathFile = this.getRulePathFile();
		const data = await this.fileHandler.readFile(rulePathFile);
		this.logger.trace(`CustomRulePath content from ${rulePathFile}: ${data}`);
		return data;
	}

	private async expandPaths(paths: string[]): Promise<string[]> {
		const classpathEntries: string[] = [];
		for (const p of paths) {
			let stats;
			try {
				this.logger.trace(`Fetching stats for path ${p}`);
				stats = await this.fileHandler.stats(p);
			} catch (e) {
				throw SfdxError.create('@salesforce/sfdx-scanner', 'add', 'errors.invalidFilePath', [p]);
			}
			if (stats.isFile()) {
				if (p.endsWith(".jar") || p.endsWith(".xml")) {
					// Simple filename check for .jar is enough.
					this.logger.trace(`Adding File directly provided as a path: ${p}`);
					classpathEntries.push(p);
				}
			} else if (stats.isDirectory()) {
				// TODO: Once we add support for other engines, we'll need to check whether the directory contains things other than JARs.
				// Look inside directories for jar files, but not recursively.
				const files = await this.fileHandler.readDir(p);
				for (const file of files) {
					if (file.endsWith(".jar") || file.endsWith(".xml")) {
						const filePath = path.resolve(p, file);
						this.logger.trace(`Adding File found inside a directory provided as a path: ${filePath}`);
						classpathEntries.push(filePath);
					}
				}
			}
		}
		return classpathEntries;
	}
}

