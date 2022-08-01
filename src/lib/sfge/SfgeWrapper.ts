import path = require('path');
import {Logger} from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {Controller} from '../../Controller';
import * as JreSetupManager from '../JreSetupManager';
import {uxEvents, EVENTS} from '../ScannerEvents';
import {Rule, SfgeConfig, RuleTarget} from '../../types';
import {CommandLineSupport, CommandLineResultHandler, ResultHandlerArgs} from '../services/CommandLineSupport';
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

interface SfgeExecutionWrapperOptions {
	targets: RuleTarget[];
	projectDirs: string[];
	command: string;
	rules: Rule[];
	spinnerManager: SpinnerManager;
	ruleThreadCount?: number;
	ruleThreadTimeout?: number;
	ignoreParseErrors?: boolean;
}

interface SfgeCatalogWrapperOptions {
	command: string;
	ruleType: string;
	spinnerManager: SpinnerManager;
	ruleThreadCount?: number;
	ruleThreadTimeout?: number;
	ignoreParseErrors?: boolean;
}

type SfgeTarget = {
	targetFile: string;
	targetMethods: string[];
};

type SfgeInput = {
	targets: SfgeTarget[];
	projectDirs: string[];
	rulesToRun: string[];
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

abstract class SfgeWrapper extends CommandLineSupport {
	protected logger: Logger;
	protected initialized: boolean;
	protected fh: FileHandler;
	private command: string;
	private logFilePath: string;
	private spinnerManager: SpinnerManager;
	private ruleThreadCount: number;
	private ruleThreadTimeout: number;
	private ignoreParseErrors: boolean;

	protected constructor(options: SfgeExecutionWrapperOptions|SfgeCatalogWrapperOptions) {
		super(options);
		this.command = options.command;
		this.spinnerManager = options.spinnerManager;
		this.ruleThreadCount = options.ruleThreadCount;
		this.ruleThreadTimeout = options.ruleThreadTimeout;
		this.ignoreParseErrors = options.ignoreParseErrors;
	}

	protected async init(): Promise<void> {
		await super.init();
		this.fh = new FileHandler();
		this.logFilePath = path.join(Controller.getSfdxScannerPath(), SFGE_LOG_FILE);
	}

	protected buildClasspath(): Promise<string[]> {
		return Promise.resolve([`${SFGE_LIB}/*`]);
	}

	protected isSuccessfulExitCode(code: number): boolean {
		return code === EXIT_NO_VIOLATIONS || code === EXIT_WITH_VIOLATIONS;
	}

	protected handleResults(args: ResultHandlerArgs) {
		new CommandLineResultHandler().handleResults(args);
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
		args.push(MAIN_CLASS, this.command, ...(await this.getSupplementalArgs()));

		this.logger.trace(`Preparing to execute sfge with command: "${command}", args: "${JSON.stringify(args)}"`);
		return [command, args];
	}

	protected abstract getSupplementalArgs(): Promise<string[]>;

	protected async execute(): Promise<string> {
		return super.runCommand();
	}
}

export class SfgeCatalogWrapper extends SfgeWrapper {
	private ruleType: string;

	constructor(options: SfgeCatalogWrapperOptions) {
		super(options);
		this.ruleType = options.ruleType;
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child("SfgeCatalogWrapper");
		this.initialized = true;
	}

	/**
	 * @override
	 * @protected
	 */
	protected getSupplementalArgs(): Promise<string[]> {
		return Promise.resolve([this.ruleType]);
	}

	public static async getCatalog(ruleType: string): Promise<string> {
		const wrapper = await SfgeCatalogWrapper.create({
			command: CATALOG_COMMAND,
			ruleType,
			// Cataloging shouldn't take very long, so no need for a functional spinner here.
			spinnerManager: new NoOpSpinnerManager()
		});
		return wrapper.execute();
	}
}

export class SfgeExecutionWrapper extends SfgeWrapper {
	private targets: RuleTarget[];
	private projectDirs: string[];
	private rules: Rule[];

	constructor(options: SfgeExecutionWrapperOptions) {
		super(options);
		this.targets = options.targets;
		this.projectDirs = options.projectDirs;
		this.rules = options.rules;
	}

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		await super.init();
		this.logger = await Logger.child('SfgeExecutionWrapper');
		this.initialized = true;
	}

	/**
	 *
	 * @protected
	 * @override
	 */
	protected async getSupplementalArgs(): Promise<string[]> {
		const inputObject: SfgeInput = this.createInputJson();
		const inputFile = await this.createInputFile(inputObject);

		this.logger.trace(`Stored the names of ${this.targets.length} targeted files and ${this.projectDirs.length} source directories in ${inputFile}`);
		this.logger.trace(`Rules to be executed: ${JSON.stringify(inputObject.rulesToRun)}`);
		return [inputFile];
	}

	private createInputJson(): SfgeInput {
		const inputJson: SfgeInput = {
			targets: [],
			projectDirs: this.projectDirs,
			rulesToRun: this.rules.map(rule => rule.name)
		};
		this.targets.forEach(t => {
			// If the target specifies individual methods in a file, then create one object encompassing the file and
			// those methods.
			// NOTE: This code assumes that method-level targets cannot have multiple paths in the `paths` property.
			// If that assumption is ever invalidated, then this code must change.
			if (t.methods.length > 0) {
				inputJson.targets.push({
					targetFile: t.paths[0],
					targetMethods: t.methods
				});
			} else {
				// Otherwise, the target is a collection of paths encompassing whole files, and each path should be its
				// own subject.
				t.paths.forEach(p => {
					inputJson.targets.push({
						targetFile: p,
						targetMethods: []
					});
				});
			}
		});
		return inputJson;
	}

	private async createInputFile(input: SfgeInput): Promise<string> {
		const inputFile = await this.fh.tmpFileWithCleanup();
		await this.fh.writeFile(inputFile, JSON.stringify(input));
		return inputFile;
	}

	public static async runSfge(targets: RuleTarget[], rules: Rule[], sfgeConfig: SfgeConfig): Promise<string> {
		const wrapper = await SfgeExecutionWrapper.create({
			targets,
			projectDirs: sfgeConfig.projectDirs,
			command: EXEC_COMMAND,
			rules,
			// Running rules could take quite a while, so we should use a functional spinner.
			spinnerManager: await SfgeSpinnerManager.create({}),
			ruleThreadCount: sfgeConfig.ruleThreadCount,
			ruleThreadTimeout: sfgeConfig.ruleThreadTimeout,
			ignoreParseErrors: sfgeConfig.ignoreParseErrors
		});
		return wrapper.execute();
	}
}
