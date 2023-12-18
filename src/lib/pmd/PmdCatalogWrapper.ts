import {Logger} from '@salesforce/core';
import {Catalog} from '../../types';
import {FileHandler} from '../util/FileHandler';
import * as JreSetupManager from './../JreSetupManager';
import {ResultHandlerArgs} from '../services/CommandLineSupport';
import {PmdSupport, PmdSupportOptions} from './PmdSupport';
import path = require('path');
import {BundleName, getMessage} from "../../MessageCatalog";

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/pmd
const PMD_CATALOGER_LIB = path.join(__dirname, '..', '..', '..', 'dist', 'pmd-cataloger', 'lib');
const MAIN_CLASS = 'sfdc.sfdx.scanner.pmd.Main';

export class PmdCatalogWrapper extends PmdSupport {
	private logger: Logger;
	private initialized: boolean;
	private catalogFilePath: path.ParsedPath;

	constructor(opts: PmdSupportOptions) {
		super(opts);
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('PmdCatalogWrapper');
		this.catalogFilePath = path.parse(await new FileHandler().tmpFileWithCleanup());
		this.initialized = true;
	}

	public async getCatalog(): Promise<Catalog> {
		this.logger.trace(`Populating PmdCatalog JSON.`);
		await this.runCommand();
		return this.readCatalogFromFile();
	}

	private async readCatalogFromFile(): Promise<Catalog> {
		const rawCatalog = await new FileHandler().readFile(path.format(this.catalogFilePath));
		return JSON.parse(rawCatalog) as Catalog;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// The classpath needs the cataloger's lib folder. Note that the classpath is not wrapped in quotes
		// like it would be if we invoked through the CLI, because child_process.spawn() hates that.
		const classpath: string = [`${PMD_CATALOGER_LIB}/*`, ...this.buildSharedClasspath()].join(path.delimiter);
		const languageArgs: string[] = this.buildLanguageArgs();
		this.logger.trace(`Cataloger parameters have been built: ${JSON.stringify(languageArgs)}`);

		const args = [`-DcatalogHome=${this.catalogFilePath.dir}`, `-DcatalogName=${this.catalogFilePath.base}`, '-cp', classpath, MAIN_CLASS, ...languageArgs];

		this.logger.trace(`Preparing to execute PMD Cataloger with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	/**
	 * Constructs the arguments for the PMD Cataloger so it knows which rules to catalog for which languages.
	 * @private
	 */
	private buildLanguageArgs(): string[] {
		const languageArgs: string[] = [];

		// For each language, build an argument that looks like:
		// "language=path1,path2,path3"
		this.rulePathsByLanguage.forEach((rulePaths, language) => {
			if (rulePaths && rulePaths.size > 0) {
				languageArgs.push(`${language}=${[...rulePaths].join(',')}`);
			}
		});
		this.logger.trace(`Cataloger parameters have been built: ${JSON.stringify(languageArgs)}`);
		return languageArgs;
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
			args.rej(getMessage(BundleName.EventKeyTemplates, 'error.external.errorMessageAbove'));
		}
	}

}
