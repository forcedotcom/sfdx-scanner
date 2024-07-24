import {Logger} from '@salesforce/core';
import {PmdSupport, PmdSupportOptions} from './PmdSupport';
import * as JreSetupManager from './../JreSetupManager';
import path = require('path');
import {FileHandler} from '../util/FileHandler';
import {Controller} from "../../Controller";
import {PmdCommandInfo} from "./PmdCommandInfo";

type PmdWrapperOptions = PmdSupportOptions & {
	targets: string[];
	rules: string;
	/**
	 * Any extra files that need to be added to the classpath, NOT including files that declare rules (since those files
	 * are handled elsewhere).
	 */
	supplementalClasspath: string[];
	reportFile: string;
};

export default class PmdWrapper extends PmdSupport {
	private targets: string[];
	private readonly rules: string;
	private supplementalClasspath: string[];
	private logger: Logger;
	private initialized: boolean;
	private readonly reportFile: string;

	public constructor(opts: PmdWrapperOptions) {
		super(opts);
		this.targets = opts.targets;
		this.rules = opts.rules;
		this.supplementalClasspath = opts.supplementalClasspath;
		this.reportFile = opts.reportFile;
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('PmdWrapper');
		this.initialized = true;
	}

	public async execute(): Promise<string> {
		return super.runCommand();
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');

		// Operating systems impose limits on the maximum length of a command line invocation. This can be problematic
		// when scanning a large number of files. Store the list of files to scan in a temp file. Pass the location
		// of the temp file to PMD. The temp file is cleaned up when the process exits.
		const fileHandler = new FileHandler();
		const tmpPath = await fileHandler.tmpFileWithCleanup();
		await fileHandler.writeFile(tmpPath, this.targets.join(','));

		const pmdCommandInfo: PmdCommandInfo = Controller.getActivePmdCommandInfo();
		const classPathsForExternalRules: string[] = this.buildSharedClasspath().concat(this.supplementalClasspath);
		const args: string[] = pmdCommandInfo.constructJavaCommandArgsForPmd(tmpPath, classPathsForExternalRules, this.rules, this.reportFile);

		this.logger.trace(`Preparing to execute PMD with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected isSuccessfulExitCode(code: number): boolean {
		// PMD's convention is that an exit code of 0 indicates a successful run with no violations, and an exit code of
		// 4 indicates a successful run with at least one violation.
		return code === 0 || code === 4;
	}
}
