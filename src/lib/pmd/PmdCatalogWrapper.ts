import {Logger, Messages} from '@salesforce/core';
import {ChildProcessWithoutNullStreams} from "child_process";
import {PMD_CATALOG, SFDX_SCANNER_PATH} from '../../Constants';
import {Catalog} from '../../types';
import {FileHandler} from '../util/FileHandler';
import * as PrettyPrinter from '../util/PrettyPrinter';
import * as JreSetupManager from './../JreSetupManager';
import {OutputProcessor} from './OutputProcessor';
import {PMD_LIB, PMD_VERSION, PmdSupport} from './PmdSupport';
import path = require('path');

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/pmd
const PMD_CATALOGER_LIB = path.join(__dirname, '..', '..', '..', 'dist', 'pmd-cataloger', 'lib');
const SUPPORTED_LANGUAGES = ['apex', 'javascript'];
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export class PmdCatalogWrapper extends PmdSupport {
	private outputProcessor: OutputProcessor;
	private logger: Logger; // TODO: add relevant trace logs
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) return;

		this.outputProcessor = await OutputProcessor.create({});
		this.logger = await Logger.child('PmdCatalogWrapper');

		this.initialized = true;
	}

	public async getCatalog(): Promise<Catalog> {
		this.logger.trace(`Populating PmdCatalog JSON.`);
		await this.runCommand();
		return PmdCatalogWrapper.readCatalogFromFile();
	}

	private static getCatalogName(): string {
		// We must allow for env variables to override the default catalog name. This must be recomputed in case those variables
		// have different values in different test runs.
		return process.env.PMD_CATALOG_NAME || PMD_CATALOG;
	}

	private static getCatalogPath(): string {
		return path.join(SFDX_SCANNER_PATH, PmdCatalogWrapper.getCatalogName());
	}

	private static async readCatalogFromFile(): Promise<Catalog> {
		const rawCatalog = await new FileHandler().readFile(PmdCatalogWrapper.getCatalogPath());
		return JSON.parse(rawCatalog);
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
		// is intended for child_process.spawn(), which freaks out if you do that.
		const [classpathEntries, parameters] = await Promise.all([this.buildClasspath(), this.buildCatalogerParameters()]);
		const args = [`-DcatalogName=${PmdCatalogWrapper.getCatalogName()}`, '-cp', classpathEntries.join(path.delimiter), MAIN_CLASS, ...parameters];

		this.logger.trace(`Preparing to execute PMD Cataloger with command: "${command}", args: "${args}"`);
		return [command, args];
	}

	protected async buildClasspath(): Promise<string[]> {
		const catalogerLibs = `${PMD_CATALOGER_LIB}/*`;
		const classpathEntries = await super.buildClasspath();
		classpathEntries.push(catalogerLibs);
		return classpathEntries;
	}

	private async buildCatalogerParameters(): Promise<string[]> {
		// Get custom rule path entries
		const rulePathEntries = await this.getRulePathEntries();

		// Add inbuilt PMD rule path entries
		this.addPmdJarPaths(rulePathEntries);

		const parameters = [];
		const divider = '=';
		const joiner = ',';

		// For each language, build an argument that looks like:
		// "language=path1,path2,path3"
		rulePathEntries.forEach((entries, language) => {
			const paths = Array.from(entries.values());
			parameters.push(language + divider + paths.join(joiner));
		});

		this.logger.trace(`Cataloger parameters have been built: ${parameters}`);
		return parameters;
	}

	private addPmdJarPaths(rulePathEntries: Map<string, Set<string>>): void {
		// For each supported language, add path to PMD's inbuilt rules
		SUPPORTED_LANGUAGES.forEach((language) => {
			const pmdJarName = PmdCatalogWrapper.derivePmdJarName(language);

			// TODO: logic to add entries should be encapsulated away from here.
			// Duplicates some logic in CustomRulePathManager. Consider refactoring
			if (!rulePathEntries.has(language)) {
				rulePathEntries.set(language, new Set<string>());
			}
			rulePathEntries.get(language).add(pmdJarName);
		});
		this.logger.trace(`Added PMD Jar paths: ${PrettyPrinter.stringifyMapofSets(rulePathEntries)}`);
	}


	/**
	 * PMD library holds the same naming structure for each language
	 */
	private static derivePmdJarName(language: string): string {
		return path.join(PMD_LIB, "pmd-" + language + "-" + PMD_VERSION + ".jar");
	}

	/**
	 * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject function.
	 * Resolves/rejects the Promise once the child process finishes.
	 * @param cp
	 * @param res
	 * @param rej
	 */
	protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: ([boolean, string]) => void, rej: (string) => void): void {
		let stdout = '';

		// When data is passed back up to us, pop it onto the appropriate string.
		cp.stdout.on('data', data => {
			stdout += data;
		});

		// When the child process exits, if it exited with a zero code we can resolve, otherwise we'll reject.
		cp.on('exit', code => {
			this.outputProcessor.processOutput(stdout);
			this.logger.trace(`monitorChildProcess has received exit code ${code}`);
			if (code === 0) {
				res([!!code, stdout]);
			} else {
				rej(messages.getMessage('error.external.errorMessageAbove'));
			}
		});
	}


}
