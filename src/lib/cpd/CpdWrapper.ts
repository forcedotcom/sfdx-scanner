import {Logger} from '@salesforce/core';
import {FileHandler} from '../util/FileHandler';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');
import { PMD_LIB } from '../../Constants';
import { CommandLineSupport } from '../pmd/CommandLineSupport';

const MAIN_CLASS = 'net.sourceforge.pmd.cpd.CPD';
const HEAP_SIZE = '-Xmx1024m';

interface CpdWrapperOptions {
	path: string;
	language: string;
	minimumTokens: number;
}

export default class CpdWrapper extends CommandLineSupport {

	path: string;
	language: string;
	minimumTokens: number;
	logger: Logger; 
	private initialized: boolean;

	protected async init(): Promise<void> {

		if (this.initialized) {
			return;
		}
		super.init();
		this.logger = await Logger.child('CpdWrapper');
		this.initialized = true;
	}

	protected async buildClasspath(): Promise<string[]> {
		return [`${PMD_LIB}/*`];
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		const classpath = await this.buildClasspath();

		const fileHandler = new FileHandler();
		const tmpPath = await fileHandler.tmpFileWithCleanup();
		await fileHandler.writeFile(tmpPath, this.path);

		const args = ['-cp', classpath.join(path.delimiter), HEAP_SIZE, MAIN_CLASS, '--filelist', tmpPath,
			'--format', 'xml', '--minimum-tokens', String(this.minimumTokens), '--language', this.language];

		this.logger.trace(`Preparing to execute CPD with command: "${command}", args: "${args}"`);
		return [command, args];
	}

	public static async execute(path: string, language: string, minimumTokens: number): Promise<string> {
		const myCpd = await CpdWrapper.create({
			path: path,
			language: language,
			minimumTokens: minimumTokens
		});
		return myCpd.execute();
	}

	private async execute(): Promise<string> {
		return super.runCommand();
	}

	constructor(options: CpdWrapperOptions) {
		super(options);
		this.path = options.path;
		this.language = options.language;
		this.minimumTokens = options.minimumTokens;
	}

}