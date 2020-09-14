//import {Logger} from '@salesforce/core';
import {Controller} from '../../ioc.config';
import {Config} from '../util/Config';
import {DependencyChecker} from '../services/DependencyChecker';
import {DEPCHECK} from '../../Constants';

export class RetireJsDepChecker implements DependencyChecker {
	public static NAME: string = DEPCHECK.RETIRE_JS.valueOf();

	//private logger: Logger;
	private config: Config;
	private initialized: boolean;

	public getName(): string {
		return RetireJsDepChecker.NAME;
	}

	public async run(): Promise<any> {

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
