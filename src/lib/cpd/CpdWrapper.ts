import {Logger} from '@salesforce/core';
import {FileHandler} from '../util/FileHandler';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');
import { CommandLineSupport} from '../services/CommandLineSupport';
import {Controller} from "../../Controller";
import {PmdCommandInfo} from "../pmd/PmdCommandInfo";

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
		await super.init();
		this.logger = await Logger.child('CpdWrapper');
		this.initialized = true;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		const fileHandler = new FileHandler();
		const tmpPath = await fileHandler.tmpFileWithCleanup();
		await fileHandler.writeFile(tmpPath, this.path);

		const pmdCommandInfo: PmdCommandInfo = Controller.getActivePmdCommandInfo();
		const args: string[] = pmdCommandInfo.constructJavaCommandArgsForCpd(tmpPath, this.minimumTokens, this.language)

		this.logger.trace(`Preparing to execute CPD with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// CPD's convention is that an exit code of 0 indicates a successful run with no violations, and an exit code of
		// 4 indicates a successful run with at least one violation.
		return code === 0 || code === 4;
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
