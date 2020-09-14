import {Logger} from '@salesforce/core';
import {Controller} from '../../ioc.config';
import {Config} from '../util/Config';
import {DependencyChecker} from '../services/DependencyChecker';
import {DEPCHECK} from '../../Constants';
import childProcess = require('child_process');
import path = require('path');


export class RetireJsDepChecker implements DependencyChecker {
	public static NAME: string = DEPCHECK.RETIRE_JS.valueOf();
	// RetireJS isn't really built to be invoked programmatically, so we'll need to invoke it as a CLI command. However, we
	// can't assume that they have the module installed globally. So what we're doing here is identifying the path to the
	// locally-scoped `retire` module, and then using that to derive a path to the CLI-executable JS script.
	private static RETIRE_JS_PATH: string = require.resolve('retire')
		.replace(path.join('lib', 'retire.js'), path.join('bin', 'retire'));

	private logger: Logger;
	private config: Config;
	private initialized: boolean;

	public getName(): string {
		return RetireJsDepChecker.NAME;
	}

	// TODO: There's a lot about this method that will need to change as we implement a more robust version and connect
	//  it to commands. E.g., we'll need to add flags, better processing, etc. But for now, the goal is just to have
	//  something that calls RetireJS, and this is good enough for that.
	public async run(target: string): Promise<[boolean, string, string]> {
		let stdout = '';
		let stderr = '';
		this.logger.trace(`Preparing to run RetireJS against target ${target}`);
		return new Promise<[boolean, string, string]>((res, rej) => {
			const cp = childProcess.spawn(RetireJsDepChecker.RETIRE_JS_PATH, [target]);

			// When data is passed back up to us, pop it onto the appropriate string.
			cp.stdout.on('data', data => {
				stdout += data;
			});
			cp.stderr.on('data', data => {
				stderr += data;
			});

			cp.on('exit', code => {
				this.logger.trace(`retirejs call exited with code ${code}`);
				if (code === 0 || code === 13) {
					// If RetireJS exits with code 0, it ran successfully and found no vulnerabilities. If it exits with
					// code 13, then it ran successfully and found at least one vulnerability. Either way, we have meaningful
					// output to return, and the Promise should resolve successfully.
					res([!!code, stdout, stderr]);
				} else {
					// If we got any other code, it means something went wrong. We'll reject with stderr for the ease of
					// upstream error handling.
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
		return this.config.isDepCheckerEnabled(this.getName());
	}

}
