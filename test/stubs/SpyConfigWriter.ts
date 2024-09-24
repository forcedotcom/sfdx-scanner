import {ConfigModel} from '../../src/lib/models/ConfigModel';
import {ConfigWriter} from '../../src/lib/writers/ConfigWriter';

export class SpyConfigWriter implements ConfigWriter {
	private callHistory: ConfigModel[] = [];

	public write(config: ConfigModel): Promise<void> {
		this.callHistory.push(config);
		return Promise.resolve();
	}

	public getCallHistory(): ConfigModel[] {
		return this.callHistory;
	}
}
