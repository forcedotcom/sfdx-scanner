import {Logger, SfdxError} from '@salesforce/core';
import {Controller} from '../Controller';
import {OUTPUT_FORMAT} from '../Constants';
import {DependencyChecker} from './services/DependencyChecker';
import {DependencyResult} from '../types';
import {DependencyManager} from './DependencyManager';

export class DefaultDependencyManager implements DependencyManager {
	private logger: Logger;
	private initialized: boolean;


	async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child('DefaultDependencyManager');
		this.initialized = true;
	}

	public async scanForInsecureDependencies(target: string, format: OUTPUT_FORMAT): Promise<any> {
		const resultPromises: Promise<DependencyResult[]>[] = [];

		const depCheckers: DependencyChecker[] = await Controller.getEnabledDepCheckers();
		for (const d of depCheckers) {
			// Spin up a Promise for each dependency checker.
			this.logger.trace(`${d.getName()} is eligible to execute`);
			// TODO: As we add more functionality, there will definitely need to be changes here.
			resultPromises.push(d.run(target));
		}

		try {
			// TODO: As we add more functionality and flesh out our design, there will definitely need to be changes here.
			const results = (await Promise.all(resultPromises)).reduce((acc, v) => [...acc, ...v], []);
			this.logger.trace(`Received vulnerabilities ${results}`);
			return results;
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}
}
