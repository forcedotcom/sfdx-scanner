import path = require('path');
import {Logger} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {Controller} from '../../Controller';
import * as JreSetupManager from '../JreSetupManager';
import {uxEvents, EVENTS} from '../ScannerEvents';
import {Rule, SfgeConfig} from '../../types';
import {CommandLineSupport} from '../services/CommandLineSupport';
import {SpinnerManager, NoOpSpinnerManager} from '../services/SpinnerManager';
import {FileHandler} from '../util/FileHandler';

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src/lib/sfge
const SFGE_LIB = path.join(__dirname, '..', '..', '..', 'dist', 'sfge', 'lib');

const MAIN_CLASS = "com.salesforce.Main";
const EXEC_COMMAND = "execute";
const CATALOG_COMMAND = "catalog";
const SFGE_LOG_FILE = 'sfge.log';

/**
 * By fiat, an exit code of 0 indicates a successful SFGE run with no violations detected.
 */
const EXIT_NO_VIOLATIONS = 0;
/**
 * By fiat, an exit code of 4 indicates a successful SFGE run in which violations were detected.
 */
const EXIT_WITH_VIOLATIONS = 4;

interface SfgeWrapperOptions {
	targetFiles: string[];
	projectDirs: string[];
	command: string;
	rules: Rule[];
	spinnerManager: SpinnerManager;
	ruleThreadCount?: number;
	ruleThreadTimeout?: number;
	ignoreParseErrors?: boolean;
}

type SfgeTarget = {
	targetFile: string;
	targetMethods: string[];
};

class SfgeSpinnerManager extends AsyncCreatable implements SpinnerManager {
	private initialized: boolean;
	private intervalId: NodeJS.Timeout;
	private logFilePath: string;

	protected init(): Promise<void> {
		if (this.initialized) {
			return Promise.resolve();
		}
		this.logFilePath = path.join(Controller.getSfdxScannerPath(), SFGE_LOG_FILE);
		this.initialized = true;
		return Promise.resolve();
	}

	public startSpinner(): void {
		uxEvents.emit(EVENTS.START_SPINNER, `Evaluating SFGE rules. See ${this.logFilePath} for logs`, "Please wait");

		let intervalCount = 0;
		this.intervalId = setInterval(() => {
			uxEvents.emit(EVENTS.UPDATE_SPINNER, "Please wait." + ".".repeat(intervalCount));
			intervalCount += 1;
		}, 30000);
	}

	public stopSpinner(success: boolean): void {
		clearInterval(this.intervalId);
		uxEvents.emit(EVENTS.STOP_SPINNER, success ? "Done": "Error");
	}
}

export class SfgeWrapper extends CommandLineSupport {
	private logger: Logger;
	private initialized: boolean;
	private fh: FileHandler;
	private targetFiles: string[];
	private projectDirs: string[];
	private command: string;
	private rules: Rule[];
	private logFilePath: string;
	private spinnerManager: SpinnerManager;
	private ruleThreadCount: number;
	private ruleThreadTimeout: number;
	private ignoreParseErrors: boolean;

	constructor(options: SfgeWrapperOptions) {
		super(options);
		this.targetFiles = options.targetFiles;
		this.projectDirs = options.projectDirs;
		this.command = options.command;
		this.rules = options.rules;
		this.spinnerManager = options.spinnerManager;
		this.ruleThreadCount = options.ruleThreadCount;
		this.ruleThreadTimeout = options.ruleThreadTimeout;
		this.ignoreParseErrors = options.ignoreParseErrors;
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('SfgeWrapper');
		this.fh = new FileHandler();
		this.logFilePath = path.join(Controller.getSfdxScannerPath(), SFGE_LOG_FILE);
		this.initialized = true;
	}

	protected buildClasspath(): Promise<string[]> {
		return Promise.resolve([`${SFGE_LIB}/*`]);
	}

	private async createInputFile(targetFiles: string[]): Promise<string> {
		const inputFile = await this.fh.tmpFileWithCleanup();
		await this.fh.writeFile(inputFile, targetFiles.join('\n'));
		return inputFile;
	}

	protected isSuccessfulExitCode(code: number): boolean {
		return code === EXIT_NO_VIOLATIONS || code === EXIT_WITH_VIOLATIONS;
	}

	/**
	 * Returns a spinner that will be used while waiting for the child process to complete. For the CATALOG flow, this will
	 * be a {@link NoOpSpinnerManager}, and for the EXECUTE flow, this will be a {@link SfgeSpinnerManager}.
	 * @protected
	 * @override
	 */
	protected getSpinnerManager(): SpinnerManager {
		return this.spinnerManager;
	}

	protected async buildCommandArray(): Promise<[string, string[]]> {
		const javaHome = await JreSetupManager.verifyJreSetup();
		const command = path.join(javaHome, 'bin', 'java');
		const classpath = await this.buildClasspath();

		const targetListFile = await this.createInputFile([this.createTargetJsons()]);
		const sourceListFile = await this.createInputFile(this.projectDirs);
		const rulesToRun = this.rules.map(rule => rule.name).join(',');

		this.logger.trace(`Stored the names of ${this.targetFiles.length} targeted files in ${targetListFile}`);
		this.logger.trace(`Stored the names of ${this.projectDirs.length} source directories in ${sourceListFile}`);
		this.logger.trace(`Rules to be executed: ${rulesToRun}`);

		const args = [`-Dsfge_log_name=${this.logFilePath}`, '-cp', classpath.join(path.delimiter)];
		if (this.ruleThreadCount != null) {
			args.push(`-DSFGE_RULE_THREAD_COUNT=${this.ruleThreadCount}`);
		}
		if (this.ruleThreadTimeout != null) {
			args.push(`-DSFGE_RULE_THREAD_TIMEOUT=${this.ruleThreadTimeout}`);
		}
		if (this.ignoreParseErrors != null) {
			args.push(`-DSFGE_IGNORE_PARSE_ERRORS=${this.ignoreParseErrors.toString()}`);
		}
		args.push(MAIN_CLASS, this.command, targetListFile, sourceListFile, rulesToRun);

		this.logger.trace(`Preparing to execute sfge with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	private async execute(): Promise<string> {
		return super.runCommand();
	}

	public static async getCatalog() {
		const wrapper = await SfgeWrapper.create({
			targetFiles: [],
			projectDirs: [],
			command: CATALOG_COMMAND,
			rules: [],
			// Cataloging shouldn't take very long, so no need for a functional spinner here.
			spinnerManager: new NoOpSpinnerManager()
		});
		return wrapper.execute();
	}

	private createTargetJsons(): string {
		// TODO: For now, the target files can only be file-level instead of method-level. When that changes, this code
		//  will change too.
		const targetJsons: SfgeTarget[] = this.targetFiles.map(t => {
			return {
				targetFile: t,
				targetMethods: []
			};
		});
		return JSON.stringify(targetJsons);
	}

	public static async runSfge(targetPaths: string[], rules: Rule[], sfgeConfig: SfgeConfig): Promise<string> {
		const wrapper = await SfgeWrapper.create({
			targetFiles: targetPaths,
			projectDirs: sfgeConfig.projectDirs,
			command: EXEC_COMMAND,
			rules: rules,
			// Running rules could take quite a while, so we should use a functional spinner.
			spinnerManager: await SfgeSpinnerManager.create({}),
			ruleThreadCount: sfgeConfig.ruleThreadCount,
			ruleThreadTimeout: sfgeConfig.ruleThreadTimeout,
			ignoreParseErrors: sfgeConfig.ignoreParseErrors
		});
		return wrapper.execute();
	}
}
