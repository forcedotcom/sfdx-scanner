import {Logger} from '@salesforce/core';
import {injectable, injectAll} from 'tsyringe';
import {OUTPUT_FORMAT} from '../Constants';
import {DependencyChecker} from './services/DependencyChecker';
import {DependencyManager} from './DependencyManager';

@injectable()
export class DefaultDependencyManager implements DependencyManager {
	private readonly checkers: DependencyChecker[];
	private logger: Logger;
	private initialized: boolean;

	constructor(
		@injectAll("DependencyChecker") checkers?: DependencyChecker[]
	) {
		this.checkers = checkers;
	}

	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('DefaultDependencyManager');
		for (const checker of this.checkers) {
			await checker.init();
		}

		this.initialized = true;
	}

	public async scanForInsecureDependencies(targets: string[], format: OUTPUT_FORMAT): Promise<any> {

	}
}
