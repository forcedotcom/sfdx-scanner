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

export abstract class CommandLineSupport extends AsyncCreatable {

	private parentLogger: Logger;
	private parentInitialized: boolean;
	protected outputProcessor: OutputProcessor;

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
				if (!this.handleLiveOut(''+data)) {
					stdout += data;
				}
			});
			cp.stderr.on('data', data => {
				if (!this.handleLiveErr(''+data)) {
					stderr += data;
				}
			});

			cp.on('exit', code => {
				this.parentLogger.trace(`runCommand has received exit code ${code}`);
				const isSuccess = this.isSuccessfulExitCode(code);
				this.getSpinnerManager().stopSpinner(isSuccess);
				// The output processor's input is always stdout.
				this.outputProcessor.processOutput(stdout);
				this.handleResults({
					code,
					isSuccess,
					stdout,
					stderr,
					res,
					rej
				});
			});
		});
	}

	/**
	 * Handles output in realtime.
	 * Individual engines can override this hook to have custom implementation.
	 * @param data that was received as stdout
	 * @returns true if data was handled and false if it needs to be handled later
	 */
	protected handleLiveOut(data: string): boolean {
		// By default, we handle all data at the end
		return false;
	}

	/**
	 * Handles error messages in realtime.
	 * Individual engines can override this hook to have custom implementation.
	 * @param err that was received as on stderr
	 * @returns true if data was handled and false if it needs to be handled later
	 */
	protected handleLiveErr(err: string): boolean {
		// By default, we handle all the errors at the end
		return false;
	}
}
