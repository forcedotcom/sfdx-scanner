import { Logger } from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import cspawn = require('cross-spawn');
import {OutputProcessor} from './OutputProcessor';
import {SpinnerManager, NoOpSpinnerManager} from './SpinnerManager';


export type ResultHandlerArgs = {
	code: number;
	stdout: string;
	stderr: string;
	res: (string) => void;
	rej: (string) => void;
};

export abstract class CommandLineSupport extends AsyncCreatable {

	private parentLogger: Logger;
	private parentInitialized: boolean;
	private outputProcessor: OutputProcessor;

	protected async init(): Promise<void> {

		if (this.parentInitialized) {
			return;
		}

		this.parentLogger = await Logger.child('CommandLineSupport');
		this.outputProcessor = await OutputProcessor.create({});
		this.parentInitialized = true;
	}

	protected abstract buildClasspath(): Promise<string[]>;

	/**
	 * Returns a {@link SpinnerManager} implementation to be used while waiting for the child process to complete. This
	 * default implementation returns a {@link NoOpSpinnerManager}, but subclasses may override to return another object
	 * if needed.
	 * @protected
	 */
	protected getSpinnerManager(): SpinnerManager {
		return new NoOpSpinnerManager();
	}

	/**
	 * Perform any job-specific processing on the results of a child process execution, and either resolve or reject as
	 * needed.
	 * @param args
	 * @protected
	 */
	protected abstract handleResults(args: ResultHandlerArgs): void;

	protected defaultResultHandler(args: ResultHandlerArgs): void {
		if (this.isSuccessfulExitCode(args.code)) {
			args.res(args.stdout);
		} else {
			args.rej(args.stderr);
		}
	}

	protected abstract isSuccessfulExitCode(code: number): boolean;

	protected abstract buildCommandArray(): Promise<[string, string[]]>;

	protected async runCommand(): Promise<string> {
		const [command, args] = await this.buildCommandArray();

		return new Promise<string>((res, rej) => {
			const cp = cspawn.spawn(command, args);

			let stdout = '';
			let stderr = '';
			this.getSpinnerManager().startSpinner();

			// When data is passed back up to us, pop it onto the appropriate string.
			cp.stdout.on('data', data => {
				stdout += data;
			});
			cp.stderr.on('data', data => {
				stderr += data;
			});

			cp.on('exit', code => {
				this.parentLogger.trace(`runCommand has received exit code ${code}`);
				this.getSpinnerManager().stopSpinner(this.isSuccessfulExitCode(code));
				// The output processor's input is always stdout.
				this.outputProcessor.processOutput(stdout);
				this.handleResults({
					code,
					stdout,
					stderr,
					res,
					rej
				});
			});
		});
	}
}
