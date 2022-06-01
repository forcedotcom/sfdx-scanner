import {Logger, Messages} from '@salesforce/core';
import {Catalog} from '../../types';
import {FileHandler} from '../util/FileHandler';
import * as PrettyPrinter from '../util/PrettyPrinter';
import * as JreSetupManager from './../JreSetupManager';
import {ResultHandlerArgs} from '../services/CommandLineSupport';
import * as PmdLanguageManager from './PmdLanguageManager';
import {PmdSupport} from './PmdSupport';
import { PMD_LIB } from '../../Constants';
import path = require('path');
import {uxEvents, EVENTS} from '../ScannerEvents';
import { Controller } from '../../Controller';
import { PMD_CATALOG_FILE, PMD_VERSION } from '../../Constants';

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'EventKeyTemplates');

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/pmd
const PMD_CATALOGER_LIB = path.join(__dirname, '..', '..', '..', 'dist', 'pmd-cataloger', 'lib');
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export class PmdCatalogWrapper extends PmdSupport {
	private logger: Logger; // TODO: add relevant trace logs
	private initialized: boolean;
	private sfdxScannerPath: string;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('PmdCatalogWrapper');
		this.sfdxScannerPath = Controller.getSfdxScannerPath();

		this.initialized = true;
	}

	public async getCatalog(): Promise<Catalog> {
		this.logger.trace(`Populating PmdCatalog JSON.`);
		await this.runCommand();
		return this.readCatalogFromFile();
	}

	private getCatalogPath(): string {
		return path.join(this.sfdxScannerPath, PMD_CATALOG_FILE);
	}

	private async readCatalogFromFile(): Promise<Catalog> {
		const rawCatalog = await new FileHandler().readFile(this.getCatalogPath());
		return JSON.parse(rawCatalog) as Catalog;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
		// is intended for child_process.spawn(), which freaks out if you do that.
		const classpathEntries = await this.buildClasspath();
		const parameters = await this.buildCatalogerParameters();
		const args = [`-DcatalogHome=${this.sfdxScannerPath}`, `-DcatalogName=${PMD_CATALOG_FILE}`, '-cp', classpathEntries.join(path.delimiter), MAIN_CLASS, ...parameters];

		this.logger.trace(`Preparing to execute PMD Cataloger with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected async buildClasspath(): Promise<string[]> {
		const catalogerLibs = `${PMD_CATALOGER_LIB}/*`;
		const classpathEntries = await super.buildSharedClasspath();
		classpathEntries.push(catalogerLibs);
		return classpathEntries;
	}

	private async buildCatalogerParameters(): Promise<string[]> {
		const pathSetMap = await this.getRulePathEntries();
		const parameters: string[] = [];
		const divider = '=';
		const joiner = ',';

		// For each language, build an argument that looks like:
		// "language=path1,path2,path3"
		pathSetMap.forEach((entries, language) => {
			const paths = Array.from(entries.values());

			// add parameter only if paths are available for real
			if (paths && paths.length > 0) {
				parameters.push(language + divider + paths.join(joiner));
			}
		});

		this.logger.trace(`Cataloger parameters have been built: ${JSON.stringify(parameters)}`);
		return parameters;
	}

	/**
	 * Return a map where the key is the language and the value is a set of class/jar paths.  Start with the given
	 * default values, if provided.
	 */
	protected async getRulePathEntries(): Promise<Map<string, Set<string>>> {
		const pathSetMap = new Map<string, Set<string>>();

		const customRulePaths: Map<string, Set<string>> = await this.getCustomRulePathEntries();
		const fileHandler = new FileHandler();


		// Iterate through the custom paths.
		for (const [langKey, paths] of customRulePaths.entries()) {
			// If the language by which these paths are mapped can be de-aliased into one of PMD's default-supported
			// languages, we should use the name PMD recognizes. That way, if they have custom paths for 'ecmascript'
			// and 'js', we'll turn both of those into 'javascript'.
			// If we can't de-alias the key, we should at least convert it to lowercase. Otherwise 'python', 'PYTHON',
			// and 'PyThOn' would all be considered different languages.
			const lang = (await PmdLanguageManager.resolveLanguageAlias(langKey)) || langKey.toLowerCase();
			this.logger.trace(`Custom paths mapped to ${langKey} are using converted key ${lang}`);

			// Add this language's custom paths to the pathSetMap so they're cataloged properly.
			const pathSet = pathSetMap.get(lang) || new Set<string>();
			if (paths) {
				for (const value of paths.values()) {
					if (await fileHandler.exists(value)) {
						pathSet.add(value);
					} else {
						// The catalog file may have been deleted or moved. Show the user a warning.
						uxEvents.emit(EVENTS.WARNING_ALWAYS, messages.getMessage('warning.customRuleFileNotFound', [value, lang]));
					}
				}
			}
			pathSetMap.set(lang, pathSet);
		}

		// Now, we'll want to add the default PMD JARs for any activated languages.
		const supportedLanguages = await PmdLanguageManager.getSupportedLanguages();
		supportedLanguages.forEach((language) => {
			const pmdJarName = PmdCatalogWrapper.derivePmdJarName(language);
			const pathSet = pathSetMap.get(language) || new Set<string>();
			pathSet.add(pmdJarName);
			this.logger.trace(`Adding JAR ${pmdJarName}, the default PMD JAR for language ${language}`);
			pathSetMap.set(language, pathSet);
		});
		this.logger.trace(`Found PMD rule paths ${PrettyPrinter.stringifyMapOfSets(pathSetMap)}`);
		return pathSetMap;
	}


	/**
	 * PMD library holds the same naming structure for each language
	 */
	private static derivePmdJarName(language: string): string {
		return path.join(PMD_LIB, "pmd-" + language + "-" + PMD_VERSION + ".jar");
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// By internal convention, 0 indicates success and any non-0 code indicates failure.
		return code === 0;
	}

	protected handleResults(args: ResultHandlerArgs): void {
		if (this.isSuccessfulExitCode(args.code)) {
			args.res(args.stdout);
		} else {
			// If the process errored out, then one of the Messages logged by the parent class already indicates that.
			// So rather than returning stderr (which will be confusing and likely unhelpful, just return a hardcoded
			// string indicating that the cause was logged elsewhere.
			args.rej(messages.getMessage('error.external.errorMessageAbove'));
		}
	}

}
