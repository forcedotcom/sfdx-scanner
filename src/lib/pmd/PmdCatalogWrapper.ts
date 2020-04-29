import {Logger, Messages} from '@salesforce/core';
import {ChildProcessWithoutNullStreams} from "child_process";
import {SFDX_SCANNER_PATH} from '../../Constants';
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
const PMD_CATALOG_FILE = 'PmdCatalog.json';


export class PmdCatalogWrapper extends PmdSupport {
	private outputProcessor: OutputProcessor;
	private logger: Logger; // TODO: add relevant trace logs
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('PmdCatalogWrapper');
		this.outputProcessor = await OutputProcessor.create({});

		this.initialized = true;
	}

	public async getCatalog(): Promise<Catalog> {
		this.logger.trace(`Populating PmdCatalog JSON.`);
		await this.runCommand();
		return PmdCatalogWrapper.readCatalogFromFile();
	}

	private static getCatalogPath(): string {
		return path.join(SFDX_SCANNER_PATH, PMD_CATALOG_FILE);
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
		const args = [`-DcatalogName=${PMD_CATALOG_FILE}`, '-cp', classpathEntries.join(path.delimiter), MAIN_CLASS, ...parameters];

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
		const customEntries = await this.getRulePathEntries();

		// Add inbuilt PMD rule path entries
		const allPaths = this.addPmdJarPaths(customEntries);

		const parameters = [];
		const divider = '=';
		const joiner = ',';

		// For each language, build an argument that looks like:
		// "language=path1,path2,path3"
		allPaths.forEach((entries, language) => {
			const paths = Array.from(entries.values());
			parameters.push(language + divider + paths.join(joiner));
		});

		this.logger.trace(`Cataloger parameters have been built: ${parameters}`);
		return parameters;
	}

	/**
	 * Return a map where the key is the language and the value is a set of class/jar paths.  Start with the given
	 * default values, if provided.
	 */
	private addPmdJarPaths(defaults?: Map<string, Set<string>>): Map<string, Set<string>> {
		const result = new Map<string, Set<string>>();

		// For each supported language, add path to PMD's inbuilt rules
		SUPPORTED_LANGUAGES.forEach((language) => {
			const pmdJarName = PmdCatalogWrapper.derivePmdJarName(language);
			const defaultSet = defaults ? defaults.get(language) : null;
			let resultSet = result.get(language);
			if (!resultSet) {
				resultSet = new Set<string>();
				result.set(language, resultSet);
			}
			if (defaultSet) {
				for (const value of defaultSet.values()) {
					resultSet.add(value);
				}
			}
			resultSet.add(pmdJarName);
		});
		this.logger.trace(`Added PMD Jar paths: ${PrettyPrinter.stringifyMapofSets(result)}`);

		return result;
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
