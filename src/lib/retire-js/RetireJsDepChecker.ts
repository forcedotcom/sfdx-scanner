/**
 *
 * @author Josh Feingold
 */
import {Logger} from '@salesforce/core';
import {DependencyResult} from '../../types';
import {Controller} from '../../Controller';
import {Config} from '../util/Config';
import {DependencyChecker} from '../services/DependencyChecker';
import {DEPCHECK} from '../../Constants';
import childProcess = require('child_process');
import path = require('path');

export class RetireJsDepChecker implements DependencyChecker {
	public static DEPCHECK_ENUM: DEPCHECK = DEPCHECK.RETIRE_JS;
	public static DEPCHECK_NAME: string = DEPCHECK.RETIRE_JS.valueOf();
	// RetireJS isn't really built to be invoked programmatically, so we'll need to invoke it as a CLI command. However, we
	// can't assume that they have the module installed globally. So what we're doing here is identifying the path to the
	// locally-scoped `retire` module, and then using that to derive a path to the CLI-executable JS script.
	private static RETIRE_JS_PATH: string = require.resolve('retire')
		.replace(path.join('lib', 'retire.js'), path.join('bin', 'retire'));

	private logger: Logger;
	private config: Config;
	private initialized: boolean;

	public getName(): string {
		return RetireJsDepChecker.DEPCHECK_NAME;
	}

	protected buildCommandArgs(target: string): string[] {
		return ['--path', target, '--format', 'json'];
	}

	// TODO: There's a lot about this method that will need to change as we implement a more robust version and connect
	//  it to commands. E.g., we'll need to add flags, better processing, etc. But for now, the goal is just to have
	//  something that calls RetireJS, and this is good enough for that.
	public async run(target: string): Promise<DependencyResult[]> {
		let stdout = '';
		let stderr = '';
		this.logger.trace(`Preparing to run RetireJS against target ${target}`);
		return new Promise<DependencyResult[]>((res, rej) => {
			const cp = childProcess.spawn(RetireJsDepChecker.RETIRE_JS_PATH, this.buildCommandArgs(target));

			// When data is passed back up to us, pop it onto the appropriate string.
			cp.stdout.on('data', data => {
				stdout += data;
			});
			cp.stderr.on('data', data => {
				stderr += data;
			});

			cp.on('exit', code => {
				this.logger.trace(`retirejs call exited with code ${code}`);
				if (code === 0) {
					// If RetireJS exited with code 0, then it ran successfully and found no vulnerabilities. So we can
					// just resolve to an empty array.
					res([]);
				} else if (code === 13) {
					// If RetireJS exited with code 13, then it ran successfully but found at least one vulnerability.
					// So we want to pull the results out of the log, process them, and return them.
					res(this.processResults(stdout));
				} else {
					// If we got any other code, then it means something went wrong. We'll reject with stderr for the ease
					// of upstream error handling.
					rej(stderr);
				}
			});
		});
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		//this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();

		this.initialized = true;

	}

	public isEnabled(): boolean {
		return this.config.isDepCheckerEnabled(RetireJsDepChecker.DEPCHECK_ENUM);
	}

	protected processResults(stdout: string): DependencyResult[] {
		const jsonSubstr = stdout.slice(stdout.indexOf('{'), stdout.lastIndexOf('}') + 1);
		return JSON.parse(jsonSubstr).data;
	}

}
