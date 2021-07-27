import { Logger } from '@salesforce/core';
import {AsyncCreatable} from '@salesforce/kit';
import {ChildProcessWithoutNullStreams} from 'child_process';
import cspawn = require('cross-spawn');

export abstract class CommandLineSupport extends AsyncCreatable {

	private parentLogger: Logger;
	private parentInitialized: boolean;

	protected async init(): Promise<void> {

		if (this.parentInitialized) {
			return;
		}

		this.parentLogger = await Logger.child('CommandLineSupport');
		this.parentInitialized = true;
	}

	protected abstract buildClasspath(): Promise<string[]>;

	/**
	 * Accepts a child process created by child_process.spawn(), and a Promise's resolve and reject functions.
	 * Resolves/rejects the Promise once the child process finishes.
	 * @param cp
	 * @param res
	 * @param rej
	 */
	protected monitorChildProcess(cp: ChildProcessWithoutNullStreams, res: (string) => void, rej: (string) => void): void {
		let stdout = '';
		let stderr = '';

		// When data is passed back up to us, pop it onto the appropriate string.
		cp.stdout.on('data', data => {
			stdout += data;
		});
		cp.stderr.on('data', data => {
			stderr += data;
		});

		cp.on('exit', code => {
			this.parentLogger.trace(`monitorChildProcess has received exit code ${code}`);
			if (code === 0 || code === 4) {
				// If the exit code is 0, then no rule violations were found. If the exit code is 4, then it means that at least
				// one violation was found. In either case, CPD ran successfully, so we'll resolve the Promise.
				res(stdout);
			} else {
				// If we got any other error, it means something actually went wrong. We'll just reject with stderr for the ease
				// of upstream error handling.
				rej(stderr);
			}
		});
	}

	protected abstract buildCommandArray(): Promise<[string, string[]]>;

	protected async runCommand(): Promise<string> {
		const [command, args] = await this.buildCommandArray();

		return new Promise<string>((res, rej) => {
			const cp = cspawn.spawn(command, args);
			this.monitorChildProcess(cp, res, rej);
		});
	}
}
