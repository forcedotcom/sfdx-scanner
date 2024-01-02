import { Logger } from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import cspawn = require('cross-spawn');
import {OutputProcessor} from './OutputProcessor';
import {SpinnerManager, NoOpSpinnerManager} from './SpinnerManager';


export type ResultHandlerArgs = {
	code: number;
	isSuccess: boolean;
	stdout: string;
	stderr: string;
	res: (string) => void;
	rej: (string) => void;
};

export class CommandLineResultHandler {
	public handleResults(args: ResultHandlerArgs): void {
		if (args.isSuccess) {
			args.res(args.stdout);
		} else {
			args.rej(args.stderr);
		}
	}
}

/**
 * Parent class for wrappers around CLI child processes.
 *
 * This class handles spawning and monitoring a child process, processing its real-time output, and invoking
 * a handler on its final output.
 *
 * Subclasses must handle;
 * - Creating an array of arguments to be used for spawning the process
 * - Indicating whether a given status code is a successful or failed run
 *
 * TODO: The extension of {@link AsyncCreatable} imposes undesirable constraints on subclasses. If possible,
 *       we should seek to migrate away from {@link AsyncCreatable}, and either towards a fully synchronous
 *       model or towards a more flexible alternative (possibly one we write ourselves).
 */
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
	protected handleResults(args: ResultHandlerArgs): void {
		new CommandLineResultHandler().handleResults(args);
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
				// eslint-disable-next-line @typescript-eslint/no-floating-promises
				(async () => {
					if (!await this.outputProcessor.processRealtimeOutput(String(data))) {
						// hold onto data only if it was not processed
						stdout += data;
					}
				})();
			});
			cp.stderr.on('data', data => {
				// eslint-disable-next-line @typescript-eslint/no-floating-promises
				(async () => {
					if (!await this.outputProcessor.processRealtimeOutput(String(data))) {
						// hold onto data only if it was not processed
						stderr += data;
					}
				})();
			});

			cp.on('exit', code => {
				// eslint-disable-next-line @typescript-eslint/no-floating-promises
				(async () => {
					this.parentLogger.trace(`runCommand has received exit code ${code}`);
					const isSuccess = this.isSuccessfulExitCode(code);
					this.getSpinnerManager().stopSpinner(isSuccess);
					// The output processor's input is always stdout.
					await this.outputProcessor.processOutput(stdout);
					this.handleResults({
						code,
						isSuccess,
						stdout,
						stderr,
						res,
						rej
					});
				})();
			});
		});
	}
}
