import {ConfigModel} from '../../src/lib/models/ConfigModel';
import {ConfigWriter} from '../../src/lib/writers/ConfigWriter';

export class SpyConfigWriter implements ConfigWriter {
	private callHistory: ConfigModel[] = [];

	public write(config: ConfigModel): void {
		this.callHistory.push(config);
	}

	public getCallHistory(): ConfigModel[] {
		return this.callHistory;
	}
}
