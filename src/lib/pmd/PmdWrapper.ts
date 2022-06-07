import {Logger} from '@salesforce/core';
import {Format, PmdSupport} from './PmdSupport';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');
import { CommandLineResultHandler, ResultHandlerArgs } from '../services/CommandLineSupport';
import {FileHandler} from '../util/FileHandler';

const MAIN_CLASS = 'net.sourceforge.pmd.PMD';
const HEAP_SIZE = '-Xmx1024m';

interface PmdWrapperOptions {
	path: string;
	rules: string;
	reportFormat?: Format;
	reportFile?: string;
}

export default class PmdWrapper extends PmdSupport {


	path: string;
	rules: string;
	reportFormat: Format;
	reportFile: string;
	logger: Logger; // TODO: Add relevant trace log lines
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('PmdWrapper');
		this.initialized = true;
	}

	protected async buildClasspath(): Promise<string[]> {
		return super.buildSharedClasspath();
	}

	public static async execute(path: string, rules: string, reportFormat?: Format, reportFile?: string): Promise<string> {
		const myPmd = await PmdWrapper.create({
			path: path,
			rules: rules,
			reportFormat: reportFormat,
			reportFile: reportFile
		});
		return myPmd.execute();
	}

	private async execute(): Promise<string> {
		return super.runCommand();
	}

	constructor(options: PmdWrapperOptions) {
		super(options);
		this.path = options.path;
		this.rules = options.rules;
		this.reportFormat = options.reportFormat || Format.XML;
		this.reportFile = options.reportFile || null;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// Start with the arguments we know we'll always need.
		// NOTE: If we were going to run this command from the CLI directly, then we'd wrap the classpath in quotes, but this
		// is intended for child_process.spawn(), which freaks out if you do that.
		const classpath = await this.buildClasspath();
		// Operating systems impose limits on the maximum length of a command line invocation. This can be problematic
		// when scannning a large number of files. Store the list of files to scan in a temp file. Pass the location
		// of the temp file to PMD. The temp file is cleaned up when the process exits.
		const fileHandler = new FileHandler();
		const tmpPath = await fileHandler.tmpFileWithCleanup();
		await fileHandler.writeFile(tmpPath, this.path);
		const args = ['-cp', classpath.join(path.delimiter), HEAP_SIZE, MAIN_CLASS, '-filelist', tmpPath,
			'-format', this.reportFormat];
		if (this.rules.length > 0) {
			args.push('-rulesets', this.rules);
		}

		// Then add anything else that's dynamically included based on other input.
		if (this.reportFile) {
			args.push('-reportfile', this.reportFile);
		}

		this.logger.trace(`Preparing to execute PMD with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// PMD's convention is that an exit code of 0 indicates a successful run with no violations, and an exit code of
		// 4 indicates a successful run with at least one violation.
		return code === 0 || code === 4;
	}

	protected handleResults(args: ResultHandlerArgs): void {
		new CommandLineResultHandler().handleResults(args);
	}
}
